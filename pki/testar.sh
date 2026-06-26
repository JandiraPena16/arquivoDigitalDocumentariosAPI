#!/usr/bin/env bash
# =====================================================================
#  Testa o mTLS ponta-a-ponta (sem precisar do telemóvel).
#  Pré-requisito: o backend tem de estar a correr em HTTPS (Fase 6).
#  Uso:  bash testar.sh            (usa localhost:8080)
#        HOST=192.168.100.171 bash testar.sh
# =====================================================================
cd "$(dirname "$0")"
export MSYS_NO_PATHCONV=1
HOST="${HOST:-localhost}"; PORT="${PORT:-8080}"
URL="https://$HOST:$PORT"
DEV="devices/dispositivo-001"

echo "############################################################"
echo " TESTE 1 — O servidor serve HTTPS com o NOSSO certificado"
echo "############################################################"
echo | openssl s_client -connect "$HOST:$PORT" -CAfile ca/ca.crt 2>/dev/null \
  | openssl x509 -noout -subject -issuer 2>/dev/null
echo "(esperado: subject CN=192.168.100.171 ; issuer = Arquivo Digital Root CA)"
echo ""

echo "############################################################"
echo " TESTE 2 — Anti-MITM: SEM a CA deve falhar; COM a CA funciona"
echo "############################################################"
curl -s -o /dev/null "$URL/api/categorias"
echo "  sem --cacert  -> curl exit=$?  (esperado: != 0, erro de certificado = protegido)"
curl -s --cacert ca/ca.crt -o /dev/null -w "  com --cacert  -> HTTP %{http_code}   (esperado: 200)\n" "$URL/api/categorias"
echo ""

echo "############################################################"
echo " TESTE 3 — mTLS: apresentar o certificado do DISPOSITIVO"
echo "############################################################"
curl -s --cacert ca/ca.crt \
     --cert "$DEV/dispositivo-001.crt" --key "$DEV/dispositivo-001.key" \
     -o /dev/null -w "  com certificado -> HTTP %{http_code}\n" "$URL/api/categorias"
echo "  >> Abre o backoffice (Certificados) e confirma a 'Ultima utilizacao' do dispositivo-001."
echo ""

echo "############################################################"
echo " TESTE 4 — Handshake mostra que o servidor pediu o certificado"
echo "############################################################"
echo | openssl s_client -connect "$HOST:$PORT" -CAfile ca/ca.crt \
     -cert "$DEV/dispositivo-001.crt" -key "$DEV/dispositivo-001.key" 2>&1 \
  | grep -iE "Acceptable client certificate|Verify return code|Certificate Request" | head -3
echo "(esperado: 'Verify return code: 0 (ok)' e pedido de certificado de cliente)"