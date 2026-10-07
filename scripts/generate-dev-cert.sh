#!/usr/bin/env sh
# Creates the self-signed PKCS12 keystore used by the dev profile (TLS on https://localhost:8443).
# Usage: APP_SSL_KEYSTORE_PASSWORD=<password> ./scripts/generate-dev-cert.sh
set -eu

if [ -z "${APP_SSL_KEYSTORE_PASSWORD:-}" ]; then
  echo "APP_SSL_KEYSTORE_PASSWORD is required" >&2
  exit 1
fi

KEYSTORE="${APP_SSL_KEYSTORE:-./data/dev-keystore.p12}"
mkdir -p "$(dirname "$KEYSTORE")"

keytool -genkeypair \
  -alias real-estate-dev \
  -keyalg RSA -keysize 2048 \
  -storetype PKCS12 \
  -keystore "$KEYSTORE" \
  -storepass "$APP_SSL_KEYSTORE_PASSWORD" \
  -validity 365 \
  -dname "CN=localhost, OU=dev, O=Constantino Imoveis" \
  -ext "SAN=dns:localhost,ip:127.0.0.1"

echo "Keystore written to $KEYSTORE"
