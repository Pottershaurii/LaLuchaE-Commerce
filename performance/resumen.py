"""Genera el resumen en Markdown de la prueba de capacidad (GitHub Step Summary).

Uso: python3 performance/resumen.py <carpeta_resultados>
Lee: volumen.txt, explain-antes.txt, explain-despues.txt, indices.txt, arranque.txt,
     admin-usuarios.txt, k6-resumen.json, k6-codigo.txt, recursos.csv
"""
import csv
import json
import os
import re
import sys

DIR = sys.argv[1] if len(sys.argv) > 1 else "resultados"


def leer(nombre):
    ruta = os.path.join(DIR, nombre)
    if not os.path.exists(ruta):
        return None
    with open(ruta, encoding="utf-8", errors="replace") as f:
        return f.read()


def explain(texto):
    """Devuelve {titulo: {plan, ms, buffers}} a partir de la salida de psql."""
    res = {}
    if not texto:
        return res
    for bloque in re.split(r"^### ", texto, flags=re.M)[1:]:
        titulo = bloque.splitlines()[0].strip()
        nodos = re.findall(
            r"((?:Parallel )?(?:Seq Scan|Index Scan|Index Only Scan|Bitmap Index Scan)(?: using \w+)? on \w+)", bloque)
        nodos = [n for n in nodos if " on roles" not in n and " on productos" not in n]
        if re.search(r"^\s+(?:->\s+)?Sort ", bloque, flags=re.M):
            nodos.append("Sort")
        ms = re.search(r"Execution Time: ([\d.]+) ms", bloque)
        # Primer "Buffers:" = total del nodo raiz
        buf = re.search(r"Buffers: shared ((?:hit|read)=\d+(?: read=\d+)?)", bloque)
        total = sum(int(x) for x in re.findall(r"=(\d+)", buf.group(1))) if buf else None
        res[titulo] = {"plan": " + ".join(dict.fromkeys(nodos)) or "-", "ms": float(ms.group(1)) if ms else None,
                       "buffers": total}
    return res


def fmt_ms(v):
    return "-" if v is None else (f"{v:.3f} ms" if v < 10 else f"{v:.1f} ms")


print("# Capacidad y Rendimiento – La Lucha E-commerce\n")
print("Réplica del hardware de producción dentro del pipeline: **API 1 vCPU / 1.75 GB** (App Service B1) y "
      "**PostgreSQL 16 con 1 vCPU / 2 GB / 50 conexiones** (Flexible Server B1ms).\n")

vol = leer("volumen.txt")
if vol:
    datos = dict(l.split("|") for l in vol.strip().splitlines() if "|" in l)
    print(f"**Datos de prueba (proyección a 3 años):** {int(datos.get('usuarios', 0)):,} usuarios · "
          f"{int(datos.get('pedidos', 0)):,} pedidos · {int(datos.get('pedido_detalle', 0)):,} líneas de detalle · "
          f"base de datos de {datos.get('tamano_mb', '?')} MB\n".replace(",", " "))

# ---------------------------------------------------------------- EXPLAIN
antes, despues = explain(leer("explain-antes.txt")), explain(leer("explain-despues.txt"))
if antes:
    print("## 1. Consultas críticas: EXPLAIN ANALYZE antes y después de los índices\n")
    print("| Consulta | Plan antes | Tiempo antes | Plan después | Tiempo después | Bloques leídos | Mejora |")
    print("|---|---|---|---|---|---|---|")
    for titulo, a in antes.items():
        d = despues.get(titulo, {})
        mejora = "-"
        if a.get("ms") and d.get("ms"):
            x = a["ms"] / d["ms"]
            mejora = "sin cambio (ya indexada)" if x < 1.5 else f"**{x:.0f}× más rápida**"
        bloques = f"{a.get('buffers', '-')} → {d.get('buffers', '-')}"
        print(f"| {titulo} | `{a['plan']}` | {fmt_ms(a['ms'])} | `{d.get('plan', '-')}` | {fmt_ms(d.get('ms'))} "
              f"| {bloques} | {mejora} |")
    idx = leer("indices.txt")
    if idx:
        print("\n<details><summary>Índices del esquema después de RFC-05</summary>\n")
        print("| Índice | Tamaño |\n|---|---|")
        for l in idx.strip().splitlines():
            print(f"| {l} |")
        print("\n</details>\n")

# ---------------------------------------------------------------- k6
k6 = leer("k6-resumen.json")
codigo = (leer("k6-codigo.txt") or "").strip()
if k6:
    m = json.loads(k6)["metrics"]
    print("## 2. Prueba de carga k6: hora pico (20 TPS) y el doble (40 TPS)\n")
    estado = "✅ todos los umbrales se cumplen" if codigo == "0" else f"❌ umbrales superados (código {codigo})"
    reqs = m.get("http_reqs", {})
    fallos = m.get("http_req_failed", {})
    print(f"**Resultado:** {estado} · **{int(reqs.get('count', 0)):,} peticiones** a "
          f"{reqs.get('rate', 0):.1f} req/s de promedio · errores: {100 * fallos.get('value', 0):.2f} %\n".replace(",", " "))
    print("| Flujo | Peticiones | Promedio | Mediana | p90 | p95 | p99 | Máximo | Umbral | Cumple |")
    print("|---|---|---|---|---|---|---|---|---|---|")
    for nombre, clave in (("Navegación (GET /api/cliente/perfil)", "navegacion"),
                          ("Inicio de sesión (POST /api/auth/login)", "inicio_sesion")):
        d = m.get(f"http_req_duration{{scenario:{clave}}}")
        if not d:
            continue
        umbral = next(iter(d.get("thresholds", {})), "-")
        # En --summary-export, el valor del umbral es true cuando se SUPERO (fallo)
        ok = "✅" if not any(d.get("thresholds", {}).values()) else "❌"
        n = m.get(f"http_reqs{{scenario:{clave}}}", {}).get("count", "-")
        print(f"| {nombre} | {n} | {d['avg']:.0f} ms | {d['med']:.0f} ms | {d['p(90)']:.0f} ms | {d['p(95)']:.0f} ms "
              f"| {d['p(99)']:.0f} ms | {d['max']:.0f} ms | `{umbral}` | {ok} |")
    print()

# ---------------------------------------------------------------- recursos
rec = leer("recursos.csv")
if rec:
    filas = list(csv.DictReader(rec.splitlines()))

    def num(s):
        return float(re.sub(r"[^\d.]", "", s or "0") or 0)

    def mib(s):
        v = (s or "0").split("/")[0].strip()
        n = num(v)
        return n * 1024 if "GiB" in v else (n / 1024 if "KiB" in v else n)

    print("## 3. Consumo de recursos durante la carga\n")
    print("| Contenedor | Límite | CPU máx. | CPU promedio | Memoria máx. | Memoria % máx. |")
    print("|---|---|---|---|---|---|")
    for nombre, limite in (("api", "1 vCPU / 1.75 GB"), ("db", "1 vCPU / 2 GB")):
        fs = [f for f in filas if f.get("contenedor") == nombre]
        if not fs:
            continue
        cpus = [num(f["cpu"]) for f in fs]
        print(f"| {'API (Spring Boot)' if nombre == 'api' else 'PostgreSQL'} | {limite} | {max(cpus):.0f} % "
              f"| {sum(cpus) / len(cpus):.0f} % | {max(mib(f['memoria']) for f in fs):.0f} MiB "
              f"| {max(num(f['memoria_pct']) for f in fs):.0f} % |")
    conns = [int(num(f.get("conexiones_api"))) for f in filas if f.get("conexiones_api")]
    if conns:
        print(f"\n**Conexiones del pool HikariCP abiertas en PostgreSQL:** máximo {max(conns)} "
              f"(límite configurado: 10 por instancia).\n")

# ---------------------------------------------------------------- otros
arr = leer("arranque.txt")
adm = leer("admin-usuarios.txt")
if arr or adm:
    print("## 4. Otras mediciones\n")
    if arr:
        print(f"- **Arranque del API:** {arr.strip()}")
    if adm:
        filas = [l.split() for l in adm.strip().splitlines() if l.strip()]
        if filas:
            t = [float(f[1]) * 1000 for f in filas]
            kb = int(filas[-1][2]) / 1024
            tam = f"{kb:,.0f}".replace(",", " ")
            print(f"- **`GET /api/admin/usuarios` sin paginación:** {tam} KB por respuesta, "
                  f"{min(t):.0f}–{max(t):.0f} ms (HTTP {filas[-1][0]}). Con 50 000 usuarios conviene paginar.")
