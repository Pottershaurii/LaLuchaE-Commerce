#!/usr/bin/env bash
# Respaldo logico cifrado de la BD de La Lucha (Sesion 32, regla 3-2-1-1-0)
#
#   pg_dump (foto consistente) | gzip -9 | openssl AES-256-CBC (PBKDF2, 200 000 iteraciones)
#
# Conexion: variables estandar de PostgreSQL (PGHOST, PGPORT, PGUSER, PGPASSWORD,
# PGDATABASE, PGSSLMODE). Clave de cifrado: BACKUP_PASS (nunca escrita en el repositorio).
# Opcionales: DESTINO (carpeta, por defecto respaldos), BACKUP_KEEP_DAYS (retencion, 14) y
# PG_DUMP (comando de pg_dump; debe ser de la misma version mayor que el servidor, porque un
# pg_dump mas nuevo escribe opciones que PostgreSQL 16 no reconoce al restaurar).
#
# Genera en DESTINO:
#   backup_lalucha_AAAAMMDD_HHMMSS.sql.gz.enc   respaldo cifrado
#   backup_lalucha_...sql.gz.enc.sha256          huella para verificar que llego intacto
#   backup_lalucha_...manifiesto.txt             filas por tabla dentro del respaldo
#   backup_lalucha_...info.txt                   resumen (tamano, duracion, versiones)
set -euo pipefail

: "${BACKUP_PASS:?Define BACKUP_PASS (clave de cifrado) como variable de entorno}"
: "${PGDATABASE:?Define PGDATABASE}"
DESTINO="${DESTINO:-respaldos}"
KEEP="${BACKUP_KEEP_DAYS:-14}"
PG_DUMP="${PG_DUMP:-pg_dump}"
DIR_SCRIPT="$(cd "$(dirname "$0")" && pwd)"

mkdir -p "$DESTINO"
TS=$(TZ=America/Lima date +%Y%m%d_%H%M%S)
BASE="backup_lalucha_${TS}"
ARCHIVO="$DESTINO/${BASE}.sql.gz.enc"
MANIFIESTO="$DESTINO/${BASE}.manifiesto.txt"
INFO="$DESTINO/${BASE}.info.txt"

INICIO=$(date +%s.%N)
INICIO_LIMA=$(TZ=America/Lima date '+%d/%m/%Y %H:%M:%S')

# El manifiesto se calcula sobre el mismo flujo que se cifra: cuenta las filas de cada
# bloque COPY del respaldo, asi la verificacion del restore compara contra lo que de
# verdad se guardo (aunque la BD de produccion siga recibiendo pedidos).
$PG_DUMP --no-owner --no-privileges --format=plain "$PGDATABASE" \
  | tee >(awk -f "$DIR_SCRIPT/contar_filas.awk" > "$MANIFIESTO") \
  | gzip -9 \
  | openssl enc -aes-256-cbc -salt -pbkdf2 -iter 200000 -pass env:BACKUP_PASS -out "$ARCHIVO"

for _ in $(seq 1 100); do grep -q '^FIN$' "$MANIFIESTO" 2>/dev/null && break; sleep 0.1; done
grep -q '^FIN$' "$MANIFIESTO" || { echo "ERROR: el manifiesto no se completo" >&2; exit 1; }

(cd "$DESTINO" && sha256sum "$(basename "$ARCHIVO")" > "$(basename "$ARCHIVO").sha256")

FIN=$(date +%s.%N)
TABLAS=$(grep -vc '^FIN$' "$MANIFIESTO")
FILAS=$(awk '$1 != "FIN" {s += $2} END {print s + 0}' "$MANIFIESTO")
BYTES=$(stat -c %s "$ARCHIVO")
{
  echo "archivo:          $(basename "$ARCHIVO")"
  echo "inicio (Lima):    $INICIO_LIMA"
  echo "duracion:         $(awk -v a="$INICIO" -v b="$FIN" 'BEGIN {printf "%.1f s", b - a}')"
  echo "tamano cifrado:   $BYTES bytes ($(numfmt --to=iec --suffix=B "$BYTES"))"
  echo "sha256:           $(cut -d' ' -f1 "$ARCHIVO.sha256")"
  echo "cifrado:          AES-256-CBC, sal aleatoria, clave derivada con PBKDF2 (200 000 iteraciones)"
  echo "contenido:        $TABLAS tablas, $FILAS filas"
  echo "pg_dump:          $($PG_DUMP --version)"
  echo "servidor:         $(psql -At -c 'SHOW server_version' "$PGDATABASE")"
  echo "retencion:        $KEEP dias"
} > "$INFO"

# Rotacion: borra respaldos locales con mas dias que la retencion
find "$DESTINO" -maxdepth 1 -name 'backup_lalucha_*' -mtime +"$KEEP" -print -delete

cat "$INFO"
