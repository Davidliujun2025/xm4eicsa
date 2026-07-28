#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-https://42.193.201.236}"

echo "=== API Connectivity ==="
for url in \
  "$BASE_URL/api/v1/public/login-config" \
  "$BASE_URL/api/v1/auth/me" \
  "$BASE_URL/api/admin/customer-service-users/statistics" \
  "$BASE_URL/api/forbidden-words?platform=ALL&page=1&size=1" \
  "$BASE_URL/api/chat-audit/logs?page=1&size=1"; do
  code=$(curl -k -sS -o /tmp/xm4_api_check.out -w "%{http_code}" "$url" || true)
  echo "$url => $code"
  head -c 200 /tmp/xm4_api_check.out; echo
  echo "---"
done

echo "=== Static Asset Check (no HTML fallback) ==="
asset_code=$(curl -k -sS -o /tmp/xm4_asset.out -w "%{http_code}" "$BASE_URL/login-bg.png" || true)
asset_type=$(curl -k -sSI "$BASE_URL/login-bg.png" | awk -F': ' 'BEGIN{IGNORECASE=1}/^Content-Type/{print $2}' | tr -d '\r')
echo "login-bg.png => $asset_code, Content-Type: ${asset_type:-unknown}"
head -c 100 /tmp/xm4_asset.out; echo

echo "=== Local Port Listeners ==="
ss -lntp | grep -E ':8080|:8082|:8083|:8084|:8085' || true

echo "=== DB Quick Check (requires mysql client and credentials) ==="
echo "Run manually if needed:"
echo "mysql -uroot -p -e \"SHOW DATABASES LIKE 'xm4';\""
echo "mysql -uroot -p -e \"USE xm4; SHOW TABLES;\""
