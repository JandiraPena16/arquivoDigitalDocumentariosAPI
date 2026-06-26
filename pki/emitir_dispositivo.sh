#!/usr/bin/env bash
# =====================================================================
#  Emite um certificado de CLIENTE para um dispositivo (mTLS).
#  O CN identifica o dispositivo/utilizador -> rastreabilidade.
#  Uso:  bash emitir_dispositivo.sh <id_dispositivo> [email_ou_nome]
#  Ex.:  bash emitir_dispositivo.sh dispositivo-001 joao@exemplo.com
# =====================================================================
set -e
cd "$(dirname "$0")"
export MSYS_NO_PATHCONV=1   # evita o Git Bash converter "/C=AO/..." em caminho Windows

ID="${1:?Indica o id do dispositivo. Ex: bash emitir_dispositivo.sh dispositivo-001}"
DONO="${2:-}"
PASS="${PASS:-changeit}"
DIAS_CERT=825

if [ ! -f ca/ca.key ]; then
  echo "ERRO: a CA ainda nao existe. Corre primeiro: bash gerar_pki.sh"
  exit 1
fi

mkdir -p devices
OUT="devices/$ID"
mkdir -p "$OUT"

cat > "$OUT/ext.cnf" <<EOF
basicConstraints=CA:FALSE
keyUsage=digitalSignature
extendedKeyUsage=clientAuth
EOF

echo "==> A emitir certificado para o dispositivo: $ID  (dono: ${DONO:-n/d})"
openssl genrsa -out "$OUT/$ID.key" 2048
openssl req -new -key "$OUT/$ID.key" -out "$OUT/$ID.csr" \
  -subj "/C=AO/O=Arquivo Digital de Documentarios/OU=Dispositivos/CN=$ID${DONO:+/emailAddress=$DONO}"
openssl x509 -req -in "$OUT/$ID.csr" -CA ca/ca.crt -CAkey ca/ca.key \
  -CAcreateserial -out "$OUT/$ID.crt" -days $DIAS_CERT -sha256 \
  -extfile "$OUT/ext.cnf"

# Pacote PKCS12 para instalar no Android (KeyChain) — contém a chave + cert + CA
openssl pkcs12 -export -in "$OUT/$ID.crt" -inkey "$OUT/$ID.key" \
  -certfile ca/ca.crt -name "$ID" -out "$OUT/$ID.p12" -password pass:$PASS

SERIAL=$(openssl x509 -in "$OUT/$ID.crt" -noout -serial | cut -d= -f2)
FPR=$(openssl x509 -in "$OUT/$ID.crt" -noout -fingerprint -sha256 | cut -d= -f2)
VALIDADE=$(openssl x509 -in "$OUT/$ID.crt" -noout -enddate | cut -d= -f2)

echo ""
echo "============================================================"
echo " Certificado emitido: $OUT/$ID.p12  (password: $PASS)"
echo "   CN (id):     $ID"
echo "   Dono:        ${DONO:-n/d}"
echo "   Serial:      $SERIAL"
echo "   Validade:    $VALIDADE"
echo "   SHA-256:     $FPR"
echo "------------------------------------------------------------"
echo " >> Regista estes dados no backoffice (entidade CertificadoDispositivo)."
echo "============================================================"