#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
LOCAL_DIR="${PROJECT_ROOT}/.local"
PID_FILE="${LOCAL_DIR}/backend.pid"
LOG_FILE="${LOCAL_DIR}/backend.log"
DAEMON_MODE="false"

source "${SCRIPT_DIR}/load-dotenv.sh"
load_dotenv "${PROJECT_ROOT}/.env"

for arg in "$@"; do
  case "$arg" in
    --daemon)
      DAEMON_MODE="true"
      ;;
    *)
      echo "Unknown argument: $arg" >&2
      exit 1
      ;;
  esac
done

mkdir -p "${LOCAL_DIR}"

SERVER_PORT="${SERVER_PORT:-8080}"
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-mysql}"
if [[ "${SPRING_PROFILES_ACTIVE}" == "local" ]]; then
  SPRING_PROFILES_ACTIVE="mysql"
fi
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
DB_URL="${DB_URL:-jdbc:mysql://127.0.0.1:3307/xm4?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai}"
DB_USERNAME="${DB_USERNAME:-root}"
DB_PASSWORD="${DB_PASSWORD:-}"
JWT_SECRET_BASE64="${JWT_SECRET_BASE64:-MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=}"
SPRING_FLYWAY_ENABLED="${SPRING_FLYWAY_ENABLED:-false}"
SPRING_SQL_INIT_MODE="${SPRING_SQL_INIT_MODE:-never}"
SPRING_JPA_HIBERNATE_DDL_AUTO="${SPRING_JPA_HIBERNATE_DDL_AUTO:-validate}"

if ! command -v mvn >/dev/null 2>&1; then
  echo "mvn not found. Install Maven 3.9+ first." >&2
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "java not found. Install Java 21 first." >&2
  exit 1
fi

if [[ "$DB_URL" == *"127.0.0.1:3307"* ]] && ! lsof -nP -iTCP:3307 -sTCP:LISTEN >/dev/null 2>&1; then
  echo "MySQL tunnel on 127.0.0.1:3307 is not listening. Start scripts/start-mysql-tunnel.sh first." >&2
  exit 1
fi

echo "Building backend modules..."
(cd "${PROJECT_ROOT}" && mvn -q -DskipTests package)

if [[ "${DAEMON_MODE}" == "true" ]]; then
  if [[ -f "${PID_FILE}" ]]; then
    existing_pid="$(cat "${PID_FILE}")"
    if kill -0 "${existing_pid}" >/dev/null 2>&1; then
      echo "Backend already running on http://127.0.0.1:${SERVER_PORT}"
      exit 0
    fi
    rm -f "${PID_FILE}"
  fi

  (
    cd "${PROJECT_ROOT}"
    nohup env \
      SERVER_PORT="${SERVER_PORT}" \
      SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE}" \
      SPRING_FLYWAY_ENABLED="${SPRING_FLYWAY_ENABLED}" \
      SPRING_SQL_INIT_MODE="${SPRING_SQL_INIT_MODE}" \
      SPRING_JPA_HIBERNATE_DDL_AUTO="${SPRING_JPA_HIBERNATE_DDL_AUTO}" \
      REDIS_HOST="${REDIS_HOST}" \
      REDIS_PORT="${REDIS_PORT}" \
      DB_URL="${DB_URL}" \
      DB_USERNAME="${DB_USERNAME}" \
      DB_PASSWORD="${DB_PASSWORD}" \
      JWT_SECRET_BASE64="${JWT_SECRET_BASE64}" \
      java -jar application/target/xm4eicsa-application-1.0.0-SNAPSHOT.jar \
      >"${LOG_FILE}" 2>&1 &
    echo $! > "${PID_FILE}"
  )
  echo "Backend starting on http://127.0.0.1:${SERVER_PORT}"
  echo "Log: ${LOG_FILE}"
  exit 0
fi

cd "${PROJECT_ROOT}"
exec env \
  SERVER_PORT="${SERVER_PORT}" \
  SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE}" \
  SPRING_FLYWAY_ENABLED="${SPRING_FLYWAY_ENABLED}" \
  SPRING_SQL_INIT_MODE="${SPRING_SQL_INIT_MODE}" \
  SPRING_JPA_HIBERNATE_DDL_AUTO="${SPRING_JPA_HIBERNATE_DDL_AUTO}" \
  REDIS_HOST="${REDIS_HOST}" \
  REDIS_PORT="${REDIS_PORT}" \
  DB_URL="${DB_URL}" \
  DB_USERNAME="${DB_USERNAME}" \
  DB_PASSWORD="${DB_PASSWORD}" \
  JWT_SECRET_BASE64="${JWT_SECRET_BASE64}" \
  java -jar application/target/xm4eicsa-application-1.0.0-SNAPSHOT.jar
