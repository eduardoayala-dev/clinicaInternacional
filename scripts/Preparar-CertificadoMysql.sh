#!/usr/bin/env bash
set -euo pipefail
umask 077

datos=/var/lib/mysql
for archivo in ca.pem ca-key.pem server-key.pem server-cert.pem; do
    test -r "$datos/$archivo" || { printf 'No se puede leer %s\n' "$archivo" >&2; exit 1; }
done

destino=$(mktemp -d "$datos/tls-preparacion.XXXXXXXX")
cat > "$destino/extensiones.cnf" <<'EOF'
basicConstraints=critical,CA:FALSE
keyUsage=critical,digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth
subjectAltName=DNS:mysql.railway.internal
EOF

openssl req -new -sha256 -key "$datos/server-key.pem" \
    -subj '/CN=mysql.railway.internal' -out "$destino/servidor.csr"
openssl x509 -req -sha256 -days 365 -in "$destino/servidor.csr" \
    -CA "$datos/ca.pem" -CAkey "$datos/ca-key.pem" \
    -set_serial "0x$(openssl rand -hex 16)" \
    -extfile "$destino/extensiones.cnf" -out "$destino/server-cert.pem"
openssl verify -CAfile "$datos/ca.pem" -purpose sslserver \
    -verify_hostname mysql.railway.internal "$destino/server-cert.pem"
openssl x509 -in "$destino/server-cert.pem" -noout -subject -issuer -dates -ext subjectAltName
printf '\nCertificado preparado en: %s/server-cert.pem\n' "$destino"
printf 'El certificado activo no ha sido reemplazado. No se han cambiado datos ni claves.\n'
