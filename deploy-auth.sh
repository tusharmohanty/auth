#!/usr/bin/env bash
set -euo pipefail

CASTLE_HOST="castle"
CASTLE_USER="tushar"
CASTLE_SSH="${CASTLE_USER}@${CASTLE_HOST}"

APP_NAME="portfolio-auth"
REMOTE_DIR="/home/tushar/portfolio-auth"
REMOTE_JAR="${REMOTE_DIR}/target/portfolio-auth.jar"
REMOTE_LOG_FILE="/home/tushar/logs/portfolio-auth.log"

JAVA_BIN="/usr/bin/java"
SYSTEMD_SERVICE="portfolio-auth"

echo "==> Building locally (${APP_NAME})..."
./mvnw -q -DskipTests clean package

JAR_PATH="$(ls -1 target/*.jar | head -n 1)"
if [[ -z "${JAR_PATH}" ]]; then
  echo "ERROR: Could not find built jar under target/"
  exit 1
fi

echo "==> Built jar: ${JAR_PATH}"

echo "==> Copying jar to ${CASTLE_HOST}:${REMOTE_JAR} ..."
scp "${JAR_PATH}" "${CASTLE_SSH}:${REMOTE_JAR}"

echo "==> Restarting systemd service ${SYSTEMD_SERVICE} on ${CASTLE_HOST}..."
ssh "${CASTLE_SSH}" "
  set -e
  if [[ ! -x '${JAVA_BIN}' ]]; then
    echo 'ERROR: Java not found at ${JAVA_BIN}'
    exit 1
  fi

  if [[ ! -f '${REMOTE_JAR}' ]]; then
    echo 'ERROR: Deployed jar not found at ${REMOTE_JAR}'
    exit 1
  fi

  sudo -n systemctl restart '${SYSTEMD_SERVICE}'
  sleep 3
  sudo -n systemctl is-active --quiet '${SYSTEMD_SERVICE}'
"

echo "==> Deployment complete."
echo
echo "Useful commands:"
echo "  ssh ${CASTLE_SSH} \"sudo -n systemctl status ${SYSTEMD_SERVICE} --no-pager -l\""
echo "  ssh ${CASTLE_SSH} \"sudo -n journalctl -u ${SYSTEMD_SERVICE} -f\""
echo "  ssh ${CASTLE_SSH} \"tail -f ${REMOTE_LOG_FILE}\""
echo "  ssh ${CASTLE_SSH} \"ss -ltnp | grep 18080 || true\""
echo "  curl -k https://castle.local/auth/kite/login?returnTo=%2Fholdings"