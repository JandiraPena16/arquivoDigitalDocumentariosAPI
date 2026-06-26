#!/usr/bin/env bash
# =====================================================================
#  PKI — Arquivo Digital de Documentários
#  Fase 1: cria a Autoridade Certificadora (CA) + certificado do servidor
#  Uso:  bash gerar_pki.sh
# =====================================================================
set -e
cd "$(dirname "$0")"
export MSYS_NO_PATHCONV=1   # evita o Git Bash converter "/C=AO/..." em caminho Windows

SERVER_IP="${SERVER_IP:-192.168.100.171}"
PASS="${PASS:-changeit}"
DIAS_CA=3650          # 10 anos
DIAS_CERT=825         # ~2 anos (máximo aceite por muitos clientes)

mkdir -p ca server devices
echo "==> IP do servidor: $SERVER_IP"

# ---------------------------------------------------------------------
# 1) ROOT CA  (a raiz de confiança — a chave ca.key é a JOIA DA COROA)
# ---------------------------------------------------------------------
if [ ! -f ca/ca.key ]; then
  echo "==> A criar a Root CA..."
  openssl genrsa -out ca/ca.key 4096
  openssl req -x509 -new -nodes -key ca/ca.key -sha256 -days $DIAS_CA \
    -out ca/ca.crt \
    -subj "/C=AO/ST=Luanda/O=Arquivo Digital de Documentarios/OU=PKI/CN=Arquivo Digital Root CA"
  echo "   ca/ca.crt + ca/ca.key criados."
else
  echo "==> Root CA já existe (a reutilizar ca/ca.key)."
fi

# ---------------------------------------------------------------------
# 2) CERTIFICADO DO SERVIDOR (TLS)  -> previne man-in-the-middle
# ---------------------------------------------------------------------
echo "==> A criar o certificado do servidor (SAN: $SERVER_IP, 127.0.0.1, localhost)..."
cat > server/server_ext.cnf <<EOF
basicConstraints=CA:FALSE
keyUsage=digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth
subjectAltName=@alt
[alt]
IP.1=$SERVER_IP
IP.2=127.0.0.1
DNS.1=localhost
EOF

openssl genrsa -out server/server.key 2048
openssl req -new -key server/server.key -out server/server.csr \
  -subj "/C=AO/O=Arquivo Digital de Documentarios/CN=$SERVER_IP"
openssl x509 -req -in server/server.csr -CA ca/ca.crt -CAkey ca/ca.key \
  -CAcreateserial -out server/server.crt -days $DIAS_CERT -sha256 \
  -extfile server/server_ext.cnf

# Keystore (PKCS12) que o Spring Boot usa para servir HTTPS
openssl pkcs12 -export -in server/server.crt -inkey server/server.key \
  -certfile ca/ca.crt -name arquivo -out server/server-keystore.p12 \
  -password pass:$PASS

# Truststore (PKCS12) com a CA — TEM de ser feito com keytool para o Java o
# reconhecer como 'trustedCertEntry' (o openssl -nokeys deixa trustAnchors vazio).
KEYTOOL="keytool"
[ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/keytool" ] && KEYTOOL="$JAVA_HOME/bin/keytool"
[ -x "/c/Program Files/Java/jdk-25.0.3/bin/keytool.exe" ] && KEYTOOL="/c/Program Files/Java/jdk-25.0.3/bin/keytool.exe"
rm -f server/truststore.p12
"$KEYTOOL" -importcert -noprompt -trustcacerts -alias arquivo-ca \
  -file ca/ca.crt -keystore server/truststore.p12 \
  -storetype PKCS12 -storepass "$PASS"

echo ""
echo "============================================================"
echo " PKI Fase 1 concluida."
echo "   CA:          ca/ca.crt   (instalar nos dispositivos)"
echo "   Servidor:    server/server-keystore.p12   (Spring Boot)"
echo "   Truststore:  server/truststore.p12        (validar dispositivos)"
echo "   Password:    $PASS"
echo "============================================================"