#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
LOCAL_DIR="${PROJECT_ROOT}/.local"

stop_from_pid_file() {
  local name="$1"
  local pid_file="$2"

  if [[ ! -f "${pid_file}" ]]; then
    return 0
  fi

  local pid
  pid="$(cat "${pid_file}")"
  if kill -0 "${pid}" >/dev/null 2>&1; then
    kill "${pid}" >/dev/null 2>&1 || true
    echo "Stopped ${name} (${pid})"
  fi
  rm -f "${pid_file}"
}

stop_from_pid_file "login" "${LOCAL_DIR}/login.pid"
stop_from_pid_file "workbench" "${LOCAL_DIR}/workbench.pid"
stop_from_pid_file "forgot-password" "${LOCAL_DIR}/forgot-password.pid"
stop_from_pid_file "admin-users" "${LOCAL_DIR}/admin-users.pid"
stop_from_pid_file "admin-tokens" "${LOCAL_DIR}/admin-tokens.pid"
stop_from_pid_file "favorite-script-library" "${LOCAL_DIR}/favorite-script-library.pid"
stop_from_pid_file "admin-forbidden-words" "${LOCAL_DIR}/admin-forbidden-words.pid"
stop_from_pid_file "token-usage" "${LOCAL_DIR}/token-usage.pid"
stop_from_pid_file "backend" "${LOCAL_DIR}/backend.pid"
