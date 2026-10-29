#!/bin/sh
set -eu

if [ -n "${DB_CA_PEM:-}" ]; then
    if [ -n "${DB_TRUSTSTORE_PATH:-}" ]; then
        printf '%s\n' 'Configura DB_CA_PEM o DB_TRUSTSTORE_PATH, no ambos.' >&2
        exit 1
    fi
    if [ -z "${DB_TRUSTSTORE_PASSWORD:-}" ]; then
        printf '%s\n' 'DB_CA_PEM requiere DB_TRUSTSTORE_PASSWORD.' >&2
        exit 1
    fi
    umask 077
    directorio=$(mktemp -d)
    trap 'rm -rf "$directorio"' EXIT HUP INT TERM
    printf '%s\n' "$DB_CA_PEM" > "$directorio/mysql-ca.pem"
    if ! keytool -importcert -noprompt -alias mysql-ca \
        -file "$directorio/mysql-ca.pem" -keystore "$directorio/mysql-truststore.p12" \
        -storetype PKCS12 -storepass:env DB_TRUSTSTORE_PASSWORD > "$directorio/importacion.log" 2>&1; then
        printf '%s\n' 'No se pudo importar DB_CA_PEM: revisa el certificado PEM y la contraseña del truststore (mínimo 6 caracteres).' >&2
        exit 1
    fi
    export DB_TRUSTSTORE_PATH="$directorio/mysql-truststore.p12"
    export DB_TRUSTSTORE_TYPE=PKCS12
    rm "$directorio/mysql-ca.pem" "$directorio/importacion.log"
    unset DB_CA_PEM
    trap - EXIT HUP INT TERM
    printf '%s\n' 'Truststore MySQL preparado con la CA configurada.'
fi

exec "$@"
