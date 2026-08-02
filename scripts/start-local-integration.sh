#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
LOCAL_DIR="${PROJECT_ROOT}/.local"

source "${SCRIPT_DIR}/load-dotenv.sh"
load_dotenv "${PROJECT_ROOT}/.env"

mkdir -p "${LOCAL_DIR}"

BACKEND_PORT="${SERVER_PORT:-8080}"
LOGIN_PORT="${LOGIN_PORT:-5173}"
WORKBENCH_PORT="${WORKBENCH_PORT:-5174}"
FORGOT_PASSWORD_PORT="${FORGOT_PASSWORD_PORT:-5175}"
ADMIN_USERS_PORT="${ADMIN_USERS_PORT:-5176}"
ADMIN_TOKENS_PORT="${ADMIN_TOKENS_PORT:-5177}"
FAVORITE_SCRIPT_PORT="${FAVORITE_SCRIPT_PORT:-5178}"
ADMIN_FORBIDDEN_WORDS_PORT="${ADMIN_FORBIDDEN_WORDS_PORT:-5179}"
TOKEN_USAGE_PORT="${TOKEN_USAGE_PORT:-5180}"

ensure_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "$1 not found." >&2
    exit 1
  fi
}

ensure_command npm
ensure_command node
ensure_command curl

prepare_node_runtime() {
  if [[ -s "${HOME}/.nvm/nvm.sh" ]]; then
    # Prefer the user's managed Node version so newer Vite apps can stay up.
    # shellcheck disable=SC1090
    source "${HOME}/.nvm/nvm.sh"
    nvm use --silent default >/dev/null 2>&1 || true
  fi
}

ensure_node_modules() {
  local app_dir="$1"
  if [[ ! -d "${app_dir}/node_modules" ]]; then
    echo "Installing frontend dependencies in ${app_dir}"
    (cd "${app_dir}" && npm ci --no-audit --no-fund)
  fi
}

app_url() {
  local port="$1"
  echo "http://127.0.0.1:${port}/"
}

start_frontend() {
  local name="$1"
  local app_dir="$2"
  local port="$3"
  local pid_file="$4"
  local log_file="${LOCAL_DIR}/${name}.log"

  if [[ -f "${pid_file}" ]]; then
    local existing_pid
    existing_pid="$(cat "${pid_file}")"
    if kill -0 "${existing_pid}" >/dev/null 2>&1; then
      echo "${name} already running on $(app_url "${port}")"
      return 0
    fi
    rm -f "${pid_file}"
  fi

  ensure_node_modules "${app_dir}"
  (
    cd "${app_dir}"
    prepare_node_runtime
    nohup env \
      VITE_API_PROXY_TARGET="http://127.0.0.1:${BACKEND_PORT}" \
      VITE_WORKBENCH_URL="$(app_url "${WORKBENCH_PORT}")" \
      VITE_FORGOT_PASSWORD_URL="$(app_url "${FORGOT_PASSWORD_PORT}")" \
      VITE_ADMIN_USERS_URL="$(app_url "${ADMIN_USERS_PORT}")" \
      VITE_ADMIN_TOKENS_URL="$(app_url "${ADMIN_TOKENS_PORT}")" \
      VITE_ADMIN_FORBIDDEN_WORDS_URL="$(app_url "${ADMIN_FORBIDDEN_WORDS_PORT}")" \
      VITE_FAVORITE_SCRIPT_URL="$(app_url "${FAVORITE_SCRIPT_PORT}")" \
      VITE_TOKEN_USAGE_URL="$(app_url "${TOKEN_USAGE_PORT}")" \
      npm run dev -- --host 127.0.0.1 --port "${port}" --strictPort \
      >"${log_file}" 2>&1 &
    echo $! > "${pid_file}"
  )
  echo "Started ${name} on $(app_url "${port}")"
}

start_static_app() {
  local name="$1"
  local app_dir="$2"
  local port="$3"
  local pid_file="$4"
  local log_file="${LOCAL_DIR}/${name}.log"

  if [[ -f "${pid_file}" ]]; then
    local existing_pid
    existing_pid="$(cat "${pid_file}")"
    if kill -0 "${existing_pid}" >/dev/null 2>&1; then
      echo "${name} already running on $(app_url "${port}")"
      return 0
    fi
    rm -f "${pid_file}"
  fi

  (
    cd "${PROJECT_ROOT}"
    prepare_node_runtime
    nohup node "${SCRIPT_DIR}/static-dev-server.mjs" \
      --root "${app_dir}" \
      --port "${port}" \
      --proxyBase "http://127.0.0.1:${BACKEND_PORT}" \
      >"${log_file}" 2>&1 &
    echo $! > "${pid_file}"
  )
  echo "Started ${name} on $(app_url "${port}")"
}

wait_for_url() {
  local url="$1"
  local label="$2"
  local attempts="${3:-60}"

  for ((i = 1; i <= attempts; i += 1)); do
    if curl -fsS "${url}" >/dev/null 2>&1; then
      echo "${label} ready: ${url}"
      return 0
    fi
    sleep 1
  done

  echo "${label} did not become ready: ${url}" >&2
  return 1
}

echo "Starting local backend..."
bash "${SCRIPT_DIR}/start-local-backend.sh" --daemon
wait_for_url "http://127.0.0.1:${BACKEND_PORT}/actuator/health" "Backend"

start_frontend "login" "${PROJECT_ROOT}/features/login/ui" "${LOGIN_PORT}" "${LOCAL_DIR}/login.pid"
start_frontend "workbench" "${PROJECT_ROOT}/features/ai-chat-workbench/ui/client" "${WORKBENCH_PORT}" "${LOCAL_DIR}/workbench.pid"
start_frontend "forgot-password" "${PROJECT_ROOT}/features/forgot-password/ui" "${FORGOT_PASSWORD_PORT}" "${LOCAL_DIR}/forgot-password.pid"
start_frontend "admin-users" "${PROJECT_ROOT}/features/create-agent-account/ui" "${ADMIN_USERS_PORT}" "${LOCAL_DIR}/admin-users.pid"
start_frontend "admin-tokens" "${PROJECT_ROOT}/features/token-quota-management/token-quota-adminvue" "${ADMIN_TOKENS_PORT}" "${LOCAL_DIR}/admin-tokens.pid"
start_frontend "favorite-script-library" "${PROJECT_ROOT}/features/favorite-script-library/UI" "${FAVORITE_SCRIPT_PORT}" "${LOCAL_DIR}/favorite-script-library.pid"
start_static_app "admin-forbidden-words" "${PROJECT_ROOT}/features/forbidden-words-management/ui" "${ADMIN_FORBIDDEN_WORDS_PORT}" "${LOCAL_DIR}/admin-forbidden-words.pid"
start_static_app "token-usage" "${PROJECT_ROOT}/features/token-usage-view/ui" "${TOKEN_USAGE_PORT}" "${LOCAL_DIR}/token-usage.pid"

wait_for_url "$(app_url "${LOGIN_PORT}")" "Login"
wait_for_url "$(app_url "${WORKBENCH_PORT}")" "Workbench"
wait_for_url "$(app_url "${FORGOT_PASSWORD_PORT}")" "Forgot password"
wait_for_url "$(app_url "${ADMIN_USERS_PORT}")" "Admin users"
wait_for_url "$(app_url "${ADMIN_TOKENS_PORT}")" "Admin tokens"
wait_for_url "$(app_url "${FAVORITE_SCRIPT_PORT}")" "Favorite script library"
wait_for_url "$(app_url "${ADMIN_FORBIDDEN_WORDS_PORT}")" "Admin forbidden words"
wait_for_url "$(app_url "${TOKEN_USAGE_PORT}")" "Token usage"

cat <<EOF
Local pages are ready:
  Login:                $(app_url "${LOGIN_PORT}")
  Workbench:            $(app_url "${WORKBENCH_PORT}")
  Forgot password:      $(app_url "${FORGOT_PASSWORD_PORT}")
  Admin users:          $(app_url "${ADMIN_USERS_PORT}")
  Admin tokens:         $(app_url "${ADMIN_TOKENS_PORT}")
  Favorite scripts:     $(app_url "${FAVORITE_SCRIPT_PORT}")
  Admin forbidden:      $(app_url "${ADMIN_FORBIDDEN_WORDS_PORT}")
  Token usage:          $(app_url "${TOKEN_USAGE_PORT}")
EOF
