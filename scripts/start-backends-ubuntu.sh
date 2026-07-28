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

pkill -f 'ai-customer-service-login-0.0.1-SNAPSHOT.jar' || true
pkill -f 'password-reset-backend-0.1.0-SNAPSHOT.jar' || true
pkill -f 'create-agent-account-server-0.0.1-SNAPSHOT.jar' || true
pkill -f 'forbidden-words-server-0.0.1-SNAPSHOT.jar' || true
pkill -f 'ai-chat-workbench-server-0.0.1-SNAPSHOT.jar' || true

nohup env SERVER_PORT=8080 XM4_MYSQL_URL="$XM4_MYSQL_URL" XM4_MYSQL_USERNAME="$XM4_MYSQL_USERNAME" XM4_MYSQL_PASSWORD="$XM4_MYSQL_PASSWORD" \
  java ${JAVA_OPTS:-} -jar features/login/server/target/ai-customer-service-login-0.0.1-SNAPSHOT.jar \
  > logs/login.out 2>&1 &

nohup env SERVER_PORT=8082 XM4_MYSQL_URL="$XM4_MYSQL_URL" XM4_MYSQL_USERNAME="$XM4_MYSQL_USERNAME" XM4_MYSQL_PASSWORD="$XM4_MYSQL_PASSWORD" \
  java ${JAVA_OPTS:-} -jar features/forgot-password/server/target/password-reset-backend-0.1.0-SNAPSHOT.jar \
  > logs/forgot-password.out 2>&1 &

nohup env SERVER_PORT=8083 XM4_MYSQL_URL="$XM4_MYSQL_URL" XM4_MYSQL_USERNAME="$XM4_MYSQL_USERNAME" XM4_MYSQL_PASSWORD="$XM4_MYSQL_PASSWORD" \
  java ${JAVA_OPTS:-} -jar features/create-agent-account/server/target/create-agent-account-server-0.0.1-SNAPSHOT.jar \
  > logs/create-agent-account.out 2>&1 &

nohup env FORBIDDEN_WORDS_SERVER_PORT=8084 XM4_MYSQL_URL="$XM4_MYSQL_URL" XM4_MYSQL_USERNAME="$XM4_MYSQL_USERNAME" XM4_MYSQL_PASSWORD="$XM4_MYSQL_PASSWORD" \
  java ${JAVA_OPTS:-} -jar features/forbidden-words-management/server/target/forbidden-words-server-0.0.1-SNAPSHOT.jar \
  > logs/forbidden-words.out 2>&1 &

nohup env SERVER_PORT=8085 XM4_MYSQL_URL="$XM4_MYSQL_URL" XM4_MYSQL_USERNAME="$XM4_MYSQL_USERNAME" XM4_MYSQL_PASSWORD="$XM4_MYSQL_PASSWORD" \
  java ${JAVA_OPTS:-} -jar features/ai-chat-workbench/server/target/ai-chat-workbench-server-0.0.1-SNAPSHOT.jar \
  > logs/ai-chat-workbench.out 2>&1 &

sleep 2
ss -lntp | grep -E ':8080|:8082|:8083|:8084|:8085' || true

echo "Backends started. Logs under $ROOT_DIR/logs"
