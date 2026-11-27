#!/usr/bin/env bash
# Prueba de restauracion (Test Restore) en una BD limpia de sandbox (Sesion 32)
#
#   restaurar.sh <carpeta_del_respaldo>
#
# 1. Verifica la huella SHA-256 del respaldo cifrado.
# 2. Crea desde cero la BD de sandbox (DB_SANDBOX, por defecto lalucha_sandbox).
# 3. Descifra (AES-256 con BACKUP_PASS), descomprime y restaura con psql.
# 4. Compara las filas de cada tabla con el manifiesto del respaldo.
# Termina con error si algo no coincide (regla "0 errores" de 3-2-1-1-0).
#
# Conexion al servidor de sandbox: PGHOST, PGPORT, PGUSER, PGPASSWORD.
set -euo pipefail

: "${BACKUP_PASS:?Define BACKUP_PASS (clave de cifrado) como variable de entorno}"
CARPETA="${1:?Uso: restaurar.sh <carpeta_del_respaldo>}"
SANDBOX="${DB_SANDBOX:-lalucha_sandbox}"

ARCHIVO=$(ls -1t "$CARPETA"/backup_lalucha_*.sql.gz.enc | head -1)
BASE="${ARCHIVO%.sql.gz.enc}"
MANIFIESTO="$BASE.manifiesto.txt"
INICIO=$(date +%s.%N)
hora() { TZ=America/Lima date '+%H:%M:%S'; }

echo "[$(hora)] Respaldo: $(basename "$ARCHIVO")"
echo "[$(hora)] 1. Verificando huella SHA-256"
(cd "$(dirname "$ARCHIVO")" && sha256sum -c "$(basename "$ARCHIVO").sha256")

echo "[$(hora)] 2. Creando BD limpia de sandbox: $SANDBOX"
dropdb --if-exists "$SANDBOX"
createdb "$SANDBOX"

echo "[$(hora)] 3. Descifrando, descomprimiendo y restaurando"
openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 -pass env:BACKUP_PASS -in "$ARCHIVO" \
  | gunzip \
  | psql -v ON_ERROR_STOP=1 -q -o /dev/null -d "$SANDBOX"
FIN_RESTORE=$(date +%s.%N)

echo "[$(hora)] 4. Verificando integridad (filas por tabla contra el manifiesto)"
ERRORES=0
TOTAL=0
printf '%-38s %10s %10s  %s\n' "tabla" "respaldo" "sandbox" "estado"
while read -r TABLA ESPERADAS; do
  [ "$TABLA" = "FIN" ] && continue
  REALES=$(psql -At -d "$SANDBOX" -c "SELECT count(*) FROM $TABLA")
  if [ "$REALES" = "$ESPERADAS" ]; then ESTADO=OK; else ESTADO=ERROR; ERRORES=$((ERRORES + 1)); fi
  TOTAL=$((TOTAL + REALES))
  printf '%-38s %10s %10s  %s\n' "$TABLA" "$ESPERADAS" "$REALES" "$ESTADO"
done < "$MANIFIESTO"

FIN=$(date +%s.%N)
echo
echo "Filas restauradas:      $TOTAL"
echo "Tablas con diferencias: $ERRORES"
echo "Tiempo de restauracion: $(awk -v a="$INICIO" -v b="$FIN_RESTORE" 'BEGIN {printf "%.1f s", b - a}') (verificacion incluida: $(awk -v a="$INICIO" -v b="$FIN" 'BEGIN {printf "%.1f s", b - a}'))"
[ "$ERRORES" -eq 0 ] || { echo "RESULTADO: ERROR - el respaldo no se restauro completo" >&2; exit 1; }
echo "RESULTADO: OK - 0 errores"
