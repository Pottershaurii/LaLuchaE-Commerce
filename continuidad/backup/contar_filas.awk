# Cuenta las filas de cada tabla dentro de un respaldo plano de pg_dump.
# Cada bloque "COPY esquema.tabla (...) FROM stdin;" termina en una linea "\."
# y tiene una fila por linea (los saltos de linea de los datos van escapados).
/^COPY [^ ]+ .* FROM stdin;$/ { tabla = $2; n = 0; dentro = 1; next }
dentro && /^\\\.$/            { print tabla, n; dentro = 0; next }
dentro                         { n++ }
END                            { print "FIN" }
