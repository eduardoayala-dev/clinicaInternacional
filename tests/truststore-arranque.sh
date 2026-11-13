#!/usr/bin/env bash
set -euo pipefail

raiz=$(cd "$(dirname "$0")/.." && pwd)
temporal=$(mktemp -d)
trap 'rm -rf "$temporal"' EXIT
unset DB_CA_PEM DB_TRUSTSTORE_PATH DB_TRUSTSTORE_PASSWORD DB_TRUSTSTORE_TYPE
arranque="$raiz/scripts/Iniciar-Tomcat.sh"

sh -n "$arranque"
test "$(sh "$arranque" printf ARRANQUE_NORMAL)" = ARRANQUE_NORMAL
MSYS2_ARG_CONV_EXCL='/CN=' openssl req -x509 -newkey rsa:2048 -nodes -days 1 -subj /CN=PruebaLocal \
    -keyout "$temporal/ca-key.pem" -out "$temporal/ca.pem" > /dev/null 2>&1
export DB_CA_PEM="$(cat "$temporal/ca.pem")"
if sh "$arranque" true > "$temporal/ausente.log" 2>&1; then exit 1; fi
export DB_TRUSTSTORE_PASSWORD=solo-prueba-local
sh "$arranque" sh -c '
    test -r "$DB_TRUSTSTORE_PATH"
    test "$DB_TRUSTSTORE_TYPE" = PKCS12
    test -z "${DB_CA_PEM:-}"
    keytool -list -keystore "$DB_TRUSTSTORE_PATH" -storetype PKCS12 \
        -storepass:env DB_TRUSTSTORE_PASSWORD -alias mysql-ca >/dev/null
    rm "$DB_TRUSTSTORE_PATH"
    rmdir "$(dirname "$DB_TRUSTSTORE_PATH")"
'
export DB_TRUSTSTORE_PATH=/almacen-existente.p12
if sh "$arranque" true > "$temporal/ambiguo.log" 2>&1; then exit 1; fi
unset DB_TRUSTSTORE_PATH
export DB_CA_PEM=certificado-invalido
if sh "$arranque" true > "$temporal/invalido.log" 2>&1; then exit 1; fi
printf '%s\n' 'OK: arranque normal, CA válida, contraseña ausente, configuración ambigua y CA inválida.'
