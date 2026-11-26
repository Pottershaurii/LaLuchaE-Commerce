"""
Capturas de evidencia para el informe (Sesiones 30 y 32) con fecha y hora visibles.

Ejemplos:
  python3 continuidad/capturar_evidencia.py --texto evidencias/failover.txt --titulo "Prueba 3.1" --salida evidencias/f.png
  python3 continuidad/capturar_evidencia.py --url http://localhost:8088/api/health --titulo "Balanceador" --salida evidencias/b.png

Requiere: pip install playwright && python -m playwright install chromium
"""
import argparse
import datetime
import html
import json
import os
import pathlib
import re
import tempfile
import zoneinfo

from playwright.sync_api import TimeoutError as PlaywrightTimeout
from playwright.sync_api import sync_playwright

ZONA = zoneinfo.ZoneInfo("America/Lima")

ESTILO_BASE = """
body { margin: 0; background: #0f1115; color: #e6e6e6; font: 15px/1.45 Consolas, 'DejaVu Sans Mono', monospace; }
.caja { padding: 28px 32px 70px; }
h1 { font: 600 18px 'Segoe UI', Arial, sans-serif; color: #fff; margin: 0 0 14px; }
pre { white-space: pre-wrap; margin: 0; }
.cmd { color: #7ee787; } .com { color: #8b949e; }
table { border-collapse: collapse; width: 100%; font: 14px 'Segoe UI', Arial, sans-serif; background: #fff; color: #222; }
th { background: #a6192e; color: #fff; text-align: left; padding: 8px 10px; }
td { border-bottom: 1px solid #ddd; padding: 8px 10px; vertical-align: top; }
.p1 { background: #c62828; color: #fff; border-radius: 4px; padding: 2px 8px; font-weight: 600; }
.p2 { background: #ef6c00; color: #fff; border-radius: 4px; padding: 2px 8px; font-weight: 600; }
.p3, .p4 { background: #1565c0; color: #fff; border-radius: 4px; padding: 2px 8px; font-weight: 600; }
.claro { background: #f6f7f9; color: #222; }
.claro h1 { color: #222; }
"""


def origen():
    corrida = os.environ.get("GITHUB_RUN_NUMBER")
    commit = os.environ.get("GITHUB_SHA", "")[:7]
    if corrida:
        return f"GitHub Actions · corrida #{corrida} · commit {commit}"
    return "Equipo local"


def pagina_texto(ruta, titulo):
    lineas = []
    for linea in pathlib.Path(ruta).read_text(encoding="utf-8").splitlines():
        texto = html.escape(linea)
        if linea.startswith("$ "):
            texto = f'<span class="cmd">{texto}</span>'
        elif linea.startswith("#"):
            texto = f'<span class="com">{texto}</span>'
        lineas.append(texto)
    return f"<html><head><meta charset='utf-8'><style>{ESTILO_BASE}</style></head><body><div class='caja'>" \
           f"<h1>{html.escape(titulo)}</h1><pre>{chr(10).join(lineas)}</pre></div></body></html>"


def pagina_tickets(ruta, titulo):
    tickets = json.loads(pathlib.Path(ruta).read_text(encoding="utf-8"))
    filas = []
    for t in tickets:
        prioridad = html.escape(t.get("prioridad") or "")
        fecha = t.get("fechaCreacion") or ""
        try:
            fecha = datetime.datetime.fromisoformat(fecha.replace("Z", "+00:00")).astimezone(ZONA).strftime("%d/%m/%Y %H:%M:%S")
        except ValueError:
            pass
        filas.append(
            "<tr>"
            f"<td><b>{html.escape(t.get('codigo') or '')}</b></td>"
            f"<td>{html.escape(t.get('titulo') or '')}<br><small>{html.escape(t.get('modulo') or '')} · reporta: {html.escape(t.get('reportadoPor') or '')}</small></td>"
            f"<td><span class='{prioridad.lower()}'>{prioridad} · {html.escape(t.get('prioridadNombre') or '')}</span></td>"
            f"<td>{html.escape(t.get('estado') or '')}</td>"
            f"<td>N{t.get('nivel') or ''} · {html.escape(t.get('asignado') or '')}</td>"
            f"<td>{html.escape(fecha)}</td>"
            "</tr>")
    cuerpo = "".join(filas) or "<tr><td colspan='6'>Sin tickets activos</td></tr>"
    return f"<html><head><meta charset='utf-8'><style>{ESTILO_BASE}</style></head><body class='claro'><div class='caja'>" \
           f"<h1>{html.escape(titulo)}</h1><p style='font-family:Segoe UI,Arial'>GET /api/incidentes (sesión ADMIN) · tickets activos en el módulo de incidentes</p>" \
           "<table><tr><th>Código</th><th>Título</th><th>Prioridad</th><th>Estado</th><th>Asignado a</th><th>Creado (hora de Lima)</th></tr>" \
           f"{cuerpo}</table></div></body></html>"


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--url")
    p.add_argument("--texto")
    p.add_argument("--tickets")
    p.add_argument("--titulo", required=True)
    p.add_argument("--salida", required=True)
    p.add_argument("--esperar-texto", default=None, help="regex que debe aparecer antes de capturar")
    p.add_argument("--clic", default=None, help="regex del nombre del boton a pulsar antes de capturar")
    p.add_argument("--esperar-ms", type=int, default=2500)
    p.add_argument("--ancho", type=int, default=1600)
    p.add_argument("--alto", type=int, default=950)
    p.add_argument("--completa", action="store_true", help="captura la pagina completa")
    a = p.parse_args()

    url = a.url
    if a.texto or a.tickets:
        contenido = pagina_texto(a.texto, a.titulo) if a.texto else pagina_tickets(a.tickets, a.titulo)
        temporal = tempfile.NamedTemporaryFile("w", suffix=".html", delete=False, encoding="utf-8")
        temporal.write(contenido)
        temporal.close()
        url = pathlib.Path(temporal.name).as_uri()

    ahora = datetime.datetime.now(ZONA)
    sello = f"{a.titulo} · Capturado: {ahora:%d/%m/%Y %H:%M:%S} (hora de Lima) · {origen()}"

    with sync_playwright() as pw:
        navegador = pw.chromium.launch()
        pagina = navegador.new_page(viewport={"width": a.ancho, "height": a.alto},
                                    locale="es-PE", timezone_id="America/Lima")
        pagina.goto(url, wait_until="domcontentloaded", timeout=60000)
        try:
            pagina.wait_for_load_state("networkidle", timeout=15000)
        except PlaywrightTimeout:
            pass
        if a.clic:
            try:
                pagina.get_by_role("button", name=re.compile(a.clic)).first.click(timeout=15000)
            except PlaywrightTimeout:
                print(f"Aviso: no se encontro el boton {a.clic!r}")
        if a.esperar_texto:
            try:
                pagina.get_by_text(re.compile(a.esperar_texto)).first.wait_for(timeout=30000)
            except PlaywrightTimeout:
                print(f"Aviso: no aparecio el texto {a.esperar_texto!r}")
        pagina.wait_for_timeout(a.esperar_ms)
        pagina.evaluate(
            """(texto) => {
                const d = document.createElement('div');
                d.textContent = texto;
                d.style.cssText = 'position:fixed;right:12px;bottom:12px;z-index:2147483647;'
                  + 'background:rgba(166,25,46,.95);color:#fff;font:600 13px Segoe UI,Arial,sans-serif;'
                  + 'padding:7px 12px;border-radius:6px;box-shadow:0 2px 8px rgba(0,0,0,.35)';
                document.body.appendChild(d);
            }""", sello)
        pathlib.Path(a.salida).parent.mkdir(parents=True, exist_ok=True)
        pagina.screenshot(path=a.salida, full_page=a.completa)
        navegador.close()
    print(f"Captura guardada: {a.salida} ({sello})")


if __name__ == "__main__":
    main()
