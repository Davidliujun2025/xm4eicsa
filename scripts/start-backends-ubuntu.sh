#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

if [[ ! -f /etc/xm4eicsa/xm4.env ]]; then
  echo "Missing /etc/xm4eicsa/xm4.env"
  exit 1
fi

# shellcheck disable=SC1091
source /etc/xm4eicsa/xm4.env
mkdir -p logs

pkill -f 'xm4eicsa-application-1.0.0-SNAPSHOT.jar' || true

nohup env \
  SERVER_PORT=8080 \
  DB_URL="$XM4_MYSQL_URL" \
  DB_USERNAME="$XM4_MYSQL_USERNAME" \
  DB_PASSWORD="$XM4_MYSQL_PASSWORD" \
  java ${JAVA_OPTS:-} -jar application/target/xm4eicsa-application-1.0.0-SNAPSHOT.jar \
  > logs/xm4eicsa.out 2>&1 &

sleep 2
ss -lntp | grep ':8080' || true

echo "XM4EICSA single application started. Log: $ROOT_DIR/logs/xm4eicsa.out"
