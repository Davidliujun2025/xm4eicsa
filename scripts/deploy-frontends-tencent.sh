#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
STAGE_DIR="${ROOT_DIR}/.deploy-stage/web"
ARCHIVE_PATH="${ROOT_DIR}/.deploy-stage/web.tar.gz"

SSH_USER="${TENCENT_CLOUD_SSH_USER:-ubuntu}"
SSH_HOST="${TENCENT_CLOUD_HOST:-42.193.201.236}"
SSH_PORT="${TENCENT_CLOUD_SSH_PORT:-22}"
REMOTE_ROOT="${TENCENT_WEB_ROOT:-/var/www/xm4eicsa}"
REMOTE_STAGE="${TENCENT_REMOTE_STAGE:-/home/${SSH_USER}/xm4eicsa-web-upload}"
REMOTE_ARCHIVE="${TENCENT_REMOTE_ARCHIVE:-/home/${SSH_USER}/xm4eicsa-web-upload.tar.gz}"
MODE="${1:-full}"

if [[ "${MODE}" != "full" && "${MODE}" != "build-only" ]]; then
  echo "Usage: $0 [full|build-only]"
  exit 1
fi

if ! command -v npm >/dev/null 2>&1; then
  echo "npm is required."
  exit 1
fi

if ! command -v rsync >/dev/null 2>&1; then
  echo "rsync is required."
  exit 1
fi

build_vite_app() {
  local app_dir="$1"
  local base_path="$2"
  local install_mode="${3:-normal}"

  echo "[build] ${app_dir} (base=${base_path})"
  if [[ "${install_mode}" == "force-platform" ]]; then
    # The original token-quota package pins a macOS binding. Force installation
    # so npm can also select the native binding for the deployment host.
    (cd "${app_dir}" && npm install --force --no-audit --no-fund && npx vite build --base "${base_path}")
  else
    (cd "${app_dir}" && npm install --no-audit --no-fund && npx vite build --base "${base_path}")
  fi
}

copy_dir_contents() {
  local src="$1"
  local dst="$2"
  mkdir -p "${dst}"
  rsync -a --delete "${src}/" "${dst}/"
}

echo "[1/4] Build frontends"
build_vite_app "${ROOT_DIR}/features/login/ui" "/"
build_vite_app "${ROOT_DIR}/features/forgot-password/ui" "/forgot-password/"
build_vite_app "${ROOT_DIR}/features/create-agent-account/ui" "/admin/users/"
build_vite_app "${ROOT_DIR}/features/token-quota-management/token-quota-adminvue" "/admin/tokens/" "force-platform"
build_vite_app "${ROOT_DIR}/features/ai-chat-workbench/ui/client" "/workbench/"
build_vite_app "${ROOT_DIR}/features/favorite-script-library/UI" "/favorite-script-library/"

echo "[2/4] Assemble deployment stage"
rm -rf "${STAGE_DIR}"
mkdir -p "${STAGE_DIR}"

copy_dir_contents "${ROOT_DIR}/features/login/ui/dist" "${STAGE_DIR}"
copy_dir_contents "${ROOT_DIR}/features/forgot-password/ui/dist" "${STAGE_DIR}/forgot-password"
copy_dir_contents "${ROOT_DIR}/features/create-agent-account/ui/dist" "${STAGE_DIR}/admin-users"
copy_dir_contents "${ROOT_DIR}/features/token-quota-management/token-quota-adminvue/dist" "${STAGE_DIR}/admin-tokens"
copy_dir_contents "${ROOT_DIR}/features/ai-chat-workbench/ui/client/dist" "${STAGE_DIR}/workbench"
copy_dir_contents "${ROOT_DIR}/features/forbidden-words-management/ui" "${STAGE_DIR}/admin-forbidden-words"
copy_dir_contents "${ROOT_DIR}/features/token-usage-view/ui" "${STAGE_DIR}/token-usage"

copy_dir_contents "${ROOT_DIR}/features/favorite-script-library/UI/dist" "${STAGE_DIR}/favorite-script-library"

if [[ "${MODE}" == "build-only" ]]; then
  echo "Build-only mode complete. Staged files: ${STAGE_DIR}"
  exit 0
fi

echo "[3/4] Upload to Tencent Cloud"
rm -f "${ARCHIVE_PATH}"
tar -C "${STAGE_DIR}" -czf "${ARCHIVE_PATH}" .
scp -P "${SSH_PORT}" "${ARCHIVE_PATH}" "${SSH_USER}@${SSH_HOST}:${REMOTE_ARCHIVE}"
ssh -p "${SSH_PORT}" "${SSH_USER}@${SSH_HOST}" "rm -rf '${REMOTE_STAGE}' && mkdir -p '${REMOTE_STAGE}' && tar -xzf '${REMOTE_ARCHIVE}' -C '${REMOTE_STAGE}' && rm -f '${REMOTE_ARCHIVE}'"

echo "[3.5/4] Promote staged files to web root (sudo required on server)"
ssh -t -p "${SSH_PORT}" "${SSH_USER}@${SSH_HOST}" "sudo mkdir -p '${REMOTE_ROOT}' && sudo rsync -a --delete '${REMOTE_STAGE}/' '${REMOTE_ROOT}/'"

echo "[4/4] Done"
echo "Uploaded latest frontend assets to ${SSH_USER}@${SSH_HOST}:${REMOTE_ROOT}"
echo "Next on server: sudo cp deploy/nginx/xm4eicsa.conf.example /etc/nginx/conf.d/xm4eicsa.conf && sudo nginx -t && sudo systemctl reload nginx"
