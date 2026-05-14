#!/usr/bin/env bash
set -euo pipefail
umask 077

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_DIR="${OUTPUT_DIR:-${SCRIPT_DIR}/generated}"
PASSWORD="${MTLS_STORE_PASSWORD:-changeit}"
DAYS="${MTLS_CERT_VALID_DAYS:-365}"

SERVICES=(
  gateway-service
  customer-service
  account-service
  lending-service
  batch-service
)

mkdir -p "${OUTPUT_DIR}"

CA_KEY="${OUTPUT_DIR}/credito-dev-ca.key"
CA_CERT="${OUTPUT_DIR}/credito-dev-ca.crt"
TRUSTSTORE="${OUTPUT_DIR}/truststore.p12"

if [[ ! -f "${CA_KEY}" || ! -f "${CA_CERT}" ]]; then
  openssl req \
    -x509 \
    -newkey rsa:4096 \
    -sha256 \
    -days "${DAYS}" \
    -nodes \
    -subj "/CN=Credito Dev Root CA/O=Credito/OU=Development" \
    -keyout "${CA_KEY}" \
    -out "${CA_CERT}"
fi

rm -f "${TRUSTSTORE}"
keytool \
  -importcert \
  -noprompt \
  -storetype PKCS12 \
  -alias credito-dev-ca \
  -file "${CA_CERT}" \
  -keystore "${TRUSTSTORE}" \
  -storepass "${PASSWORD}"

for service in "${SERVICES[@]}"; do
  service_dir="${OUTPUT_DIR}/${service}"
  mkdir -p "${service_dir}"

  key_file="${service_dir}/${service}.key"
  csr_file="${service_dir}/${service}.csr"
  cert_file="${service_dir}/${service}.crt"
  chain_file="${service_dir}/${service}-chain.crt"
  ext_file="${service_dir}/${service}.ext"
  keystore_file="${service_dir}/${service}.p12"

  cat > "${ext_file}" <<EXT
subjectAltName=DNS:${service},DNS:localhost,IP:127.0.0.1
extendedKeyUsage=serverAuth,clientAuth
keyUsage=digitalSignature,keyEncipherment
EXT

  openssl req \
    -newkey rsa:2048 \
    -nodes \
    -subj "/CN=${service}/O=Credito/OU=Development" \
    -keyout "${key_file}" \
    -out "${csr_file}"

  openssl x509 \
    -req \
    -in "${csr_file}" \
    -CA "${CA_CERT}" \
    -CAkey "${CA_KEY}" \
    -CAcreateserial \
    -out "${cert_file}" \
    -days "${DAYS}" \
    -sha256 \
    -extfile "${ext_file}"

  cat "${cert_file}" "${CA_CERT}" > "${chain_file}"

  openssl pkcs12 \
    -export \
    -name "${service}" \
    -inkey "${key_file}" \
    -in "${cert_file}" \
    -certfile "${CA_CERT}" \
    -out "${keystore_file}" \
    -passout "pass:${PASSWORD}"
done

echo "Generated dev mTLS materials under ${OUTPUT_DIR}"
echo "TrustStore: ${TRUSTSTORE}"
echo "KeyStores:"
for service in "${SERVICES[@]}"; do
  echo "- ${OUTPUT_DIR}/${service}/${service}.p12"
done
