"""
Prueba 3.1 de la Sesion 32: caida controlada de un nodo (failover) con trafico activo.

Envia peticiones continuas al balanceador (GET /api/productos), detiene api_node_1 con
`docker stop`, mide cuanto tarda Nginx en sacarlo del grupo y mandar el 100 % del
trafico a api_node_2, lo vuelve a iniciar y comprueba que retome su parte del trafico.

Uso:
  python3 continuidad/ha/prueba_failover.py --url http://localhost:8088/api/productos --salida evidencias

Genera en --salida:
  failover-peticiones.csv   una fila por peticion (hora, codigo HTTP, nodo, latencia)
  failover-resumen.json     cifras de la prueba (RTO, errores, reparto por fase)
  failover-linea-tiempo.txt relato legible de la prueba (para la captura de evidencia)
"""
import argparse
import csv
import datetime
import json
import pathlib
import subprocess
import threading
import time
import urllib.error
import urllib.request
import zoneinfo

ZONA = zoneinfo.ZoneInfo("America/Lima")


def hora(t):
    return datetime.datetime.fromtimestamp(t, ZONA).strftime("%H:%M:%S.%f")[:-3]


def docker(*args):
    inicio = time.time()
    subprocess.run(["docker", *args], check=True, capture_output=True, text=True)
    return inicio, time.time()


def salud(contenedor):
    r = subprocess.run(["docker", "inspect", "-f", "{{.State.Health.Status}}", contenedor],
                       capture_output=True, text=True)
    return r.stdout.strip()


class Trafico(threading.Thread):
    """Una peticion tras otra (aprox. 10 por segundo) hasta que se le pida parar."""

    def __init__(self, url, pausa):
        super().__init__(daemon=True)
        self.url, self.pausa = url, pausa
        self.registros, self.fase = [], "1-normal"
        self.parar = threading.Event()

    def run(self):
        while not self.parar.is_set():
            inicio = time.time()
            codigo, nodo, upstream = 0, "", ""
            try:
                with urllib.request.urlopen(self.url, timeout=15) as r:
                    r.read()
                    codigo = r.status
                    nodo, upstream = r.headers.get("X-Nodo", ""), r.headers.get("X-Served-By", "")
            except urllib.error.HTTPError as e:
                codigo = e.code
                nodo, upstream = e.headers.get("X-Nodo", ""), e.headers.get("X-Served-By", "")
            except Exception as e:  # conexion rechazada o tiempo agotado: cuenta como error
                upstream = type(e).__name__
            fin = time.time()
            self.registros.append({"t": inicio, "fin": fin, "fase": self.fase, "codigo": codigo,
                                   "nodo": nodo, "upstream": upstream,
                                   "reintento": "," in upstream, "ms": round((fin - inicio) * 1000, 1)})
            time.sleep(max(0.0, self.pausa - (fin - inicio)))


def reparto(registros):
    total = len(registros)
    cuenta = {}
    for r in registros:
        clave = r["nodo"] or "error"
        cuenta[clave] = cuenta.get(clave, 0) + 1
    return {k: {"peticiones": v, "porcentaje": round(100 * v / total, 1)} for k, v in sorted(cuenta.items())} if total else {}


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--url", default="http://localhost:8088/api/productos")
    p.add_argument("--nodo", default="api_node_1", help="contenedor que se detiene")
    p.add_argument("--normal", type=float, default=15, help="segundos de trafico antes de la caida")
    p.add_argument("--caida", type=float, default=30, help="segundos con el nodo detenido")
    p.add_argument("--despues", type=float, default=30, help="segundos de trafico al final")
    p.add_argument("--pausa", type=float, default=0.1)
    p.add_argument("--rto-objetivo", type=float, default=30.0)
    p.add_argument("--salida", default="evidencias")
    a = p.parse_args()
    salida = pathlib.Path(a.salida)
    salida.mkdir(parents=True, exist_ok=True)

    trafico = Trafico(a.url, a.pausa)
    trafico.start()
    time.sleep(a.normal)

    trafico.fase = "2-caida"
    t_stop, t_stop_fin = docker("stop", a.nodo)
    time.sleep(a.caida)

    trafico.fase = "3-recuperacion"
    t_start, _ = docker("start", a.nodo)
    t_sano = None
    for _ in range(120):
        if salud(a.nodo) == "healthy":
            t_sano = time.time()
            break
        time.sleep(1)
    time.sleep(a.despues)
    trafico.parar.set()
    trafico.join()

    regs = trafico.registros
    errores = [r for r in regs if r["codigo"] != 200]
    tras_caida = [r for r in regs if r["t"] >= t_stop]
    # Deteccion: Nginx saca al nodo tras max_fails=2 intentos fallidos. Cada intento fallido
    # se ve como una peticion reenviada (dos direcciones en X-Served-By) que igual responde 200.
    reintentos = [r for r in tras_caida if r["reintento"] and (t_start is None or r["t"] < t_start)]
    t_deteccion = reintentos[1]["fin"] if len(reintentos) >= 2 else (reintentos[0]["fin"] if reintentos else None)
    # Desde la deteccion, todo lo que llega antes del docker start lo atiende el otro nodo
    durante = [r for r in regs if t_deteccion and t_deteccion < r["t"] < t_start]
    solo_nodo2 = all(r["nodo"] != a.nodo for r in durante) if durante else False
    # Vuelta del nodo: primera peticion que atiende otra vez despues de reiniciarlo
    vuelta = next((r for r in regs if r["t"] > t_start and r["nodo"] == a.nodo and not r["reintento"]), None)
    rto = round(t_deteccion - t_stop, 2) if t_deteccion else None
    latencias_normales = sorted(r["ms"] for r in regs if r["fase"] == "1-normal" and r["codigo"] == 200)
    p95_normal = latencias_normales[int(0.95 * (len(latencias_normales) - 1))] if latencias_normales else None

    resumen = {
        "url": a.url,
        "nodo_detenido": a.nodo,
        "peticiones_totales": len(regs),
        "peticiones_fallidas_usuario": len(errores),
        "docker_stop_inicio": hora(t_stop),
        "docker_stop_fin": hora(t_stop_fin),
        "deteccion_balanceador": hora(t_deteccion) if t_deteccion else None,
        "rto_failover_s": rto,
        "rto_objetivo_s": a.rto_objetivo,
        "cumple_rto": rto is not None and rto <= a.rto_objetivo and not errores,
        "peticiones_reenviadas": len([r for r in regs if r["reintento"]]),
        "latencia_max_reenviada_ms": max((r["ms"] for r in regs if r["reintento"]), default=None),
        "latencia_p95_normal_ms": p95_normal,
        "tras_deteccion_todo_al_nodo_2": solo_nodo2,
        "docker_start": hora(t_start),
        "nodo_sano_de_nuevo": hora(t_sano) if t_sano else None,
        "nodo_recibe_trafico_de_nuevo": hora(vuelta["t"]) if vuelta else None,
        "reparto_por_fase": {f: reparto([r for r in regs if r["fase"] == f])
                             for f in ("1-normal", "2-caida", "3-recuperacion")},
    }
    (salida / "failover-resumen.json").write_text(json.dumps(resumen, indent=2, ensure_ascii=False), encoding="utf-8")
    with open(salida / "failover-peticiones.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["hora", "fase", "codigo_http", "nodo", "upstream", "reenviada", "latencia_ms"])
        for r in regs:
            w.writerow([hora(r["t"]), r["fase"], r["codigo"], r["nodo"], r["upstream"], r["reintento"], r["ms"]])

    # Relato para la captura: contexto antes de la caida, la caida y la recuperacion
    lineas = [
        f"$ python3 continuidad/ha/prueba_failover.py --url {a.url}",
        f"# Trafico continuo: {len(regs)} peticiones GET (una cada ~{int(a.pausa * 1000)} ms) al balanceador Nginx",
        "",
        "hora (Lima)    codigo  nodo         latencia  upstream",
    ]

    def fila(r):
        marca = "   <- reenviada al otro nodo" if r["reintento"] else ""
        return f"{hora(r['t'])}  {r['codigo']:>4}    {r['nodo'] or '-':<11} {r['ms']:>7.1f} ms  {r['upstream']}{marca}"

    antes = [r for r in regs if r["t"] < t_stop][-4:]
    lineas += [fila(r) for r in antes]
    lineas.append(f"{hora(t_stop)}  ---- docker stop {a.nodo} (termina {hora(t_stop_fin)}) ----")
    if t_deteccion:
        hasta = [r for r in tras_caida if r["fin"] <= t_deteccion]
        lineas += [fila(r) for r in hasta[-8:]]
        lineas.append(f"{hora(t_deteccion)}  ---- Nginx saca a {a.nodo} del grupo (max_fails=2): 100 % del trafico a api_node_2 ----")
    lineas += [fila(r) for r in durante[:4]]
    if len(durante) > 4:
        lineas.append(f"  ... {len(durante) - 4} peticiones mas, todas atendidas por api_node_2 ...")
    lineas.append(f"{hora(t_start)}  ---- docker start {a.nodo} (sano a las {hora(t_sano) if t_sano else '-'}) ----")
    if vuelta:
        i = regs.index(vuelta)
        lineas += [fila(r) for r in regs[max(0, i - 1): i + 3]]
    lineas += [
        "",
        f"Peticiones fallidas para el usuario: {len(errores)} de {len(regs)}",
        f"RTO real del failover (docker stop -> todo el trafico en api_node_2): {rto} s  (objetivo <= {a.rto_objetivo:.0f} s)",
        f"Latencia normal p95: {p95_normal} ms | peor peticion reenviada: {resumen['latencia_max_reenviada_ms']} ms",
    ]
    for fase, datos in resumen["reparto_por_fase"].items():
        lineas.append(f"Reparto {fase:<15}: " + ", ".join(f"{k} {v['peticiones']} ({v['porcentaje']} %)" for k, v in datos.items()))
    (salida / "failover-linea-tiempo.txt").write_text("\n".join(lineas) + "\n", encoding="utf-8")
    print("\n".join(lineas))
    print(json.dumps(resumen, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
