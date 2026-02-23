#!/usr/bin/env bash
set -euo pipefail

CASTLE_HOST="castle.local"
CASTLE_USER="tushar"
CASTLE_SSH="${CASTLE_USER}@${CASTLE_HOST}"

APP_NAME="portfolio-auth"
REMOTE_DIR="/opt/castle"
REMOTE_JAR="${REMOTE_DIR}/${APP_NAME}.jar"
LOG_FILE="${REMOTE_DIR}/${APP_NAME}.log"
PID_FILE="${REMOTE_DIR}/${APP_NAME}.pid"

SPRING_PROFILE="castle"
PORT="18080"

JAVA_BIN="/opt/homebrew/opt/openjdk@21/bin/java"

echo "==> Building locally (${APP_NAME})..."
./mvnw -q -DskipTests clean package

JAR_PATH="$(ls -1 target/*.jar | head -n 1)"
if [[ -z "${JAR_PATH}" ]]; then
  echo "ERROR: No jar found in target/*.jar"
  exit 1
fi

echo "==> Ensuring remote dir exists: ${REMOTE_DIR}"
ssh "${CASTLE_SSH}" "bash -lc 'mkdir -p \"${REMOTE_DIR}\"'"

echo "==> Uploading jar to castle: ${REMOTE_JAR}"
scp "${JAR_PATH}" "${CASTLE_SSH}:${REMOTE_JAR}"

echo "==> Stopping any process on port ${PORT}..."
ssh "${CASTLE_SSH}" "bash -lc '
  PID=\$(lsof -ti :${PORT} || true)
  if [[ -n \"\$PID\" ]]; then
    echo \"Killing process on ${PORT} (PID=\$PID)\"
    kill -9 \$PID
  else
    echo \"Port ${PORT} is free.\"
  fi
'"

echo "==> Starting ${APP_NAME} (profile=${SPRING_PROFILE}) on port ${PORT}..."
ssh "${CASTLE_SSH}" "bash -lc '
  set -e
  cd \"${REMOTE_DIR}\"

  if [[ ! -x \"${JAVA_BIN}\" ]]; then
    echo \"ERROR: Java not found at ${JAVA_BIN}\"
    exit 1
  fi

  echo \"Using JAVA_BIN=${JAVA_BIN}\"
  \"${JAVA_BIN}\" -version

  nohup \"${JAVA_BIN}\" \
    -jar \"${APP_NAME}.jar\" \
    --server.port=${PORT} \
    --server.ssl.enabled=false \
    --spring.profiles.active=\"${SPRING_PROFILE}\" \
    > \"${LOG_FILE}\" 2>&1 &

  echo \$! > \"${PID_FILE}\"
  echo \"Started ${APP_NAME} with PID \$(cat ${PID_FILE})\"
'"

echo "==> Deployment complete."
echo "Tail logs with:"
echo "  ssh ${CASTLE_SSH} \"tail -f ${LOG_FILE}\""