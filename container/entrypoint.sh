#!/bin/sh

export GOVER_TIMEZONE="${GOVER_TIMEZONE:-Europe/Berlin}"
export TZ="${GOVER_TIMEZONE}"

export GOVER_PLUGINS_DIR="${GOVER_PLUGINS_DIR:-/app/plugins}"

echo "Starting Gover version ${BUILD_VERSION} build ${BUILD_NUMBER}"
echo "Using runtime timezone ${TZ}"

if [ "$1" = "serve" ]; then
  echo "serve" > /app/runtime-mode
  echo "Waiting for IDP to be available at ${GOVER_KEYCLOAK_OIDC_INTERNAL_HOSTNAME}/realms/staff…"

  until curl --output /dev/null --silent --head --fail "${GOVER_KEYCLOAK_OIDC_INTERNAL_HOSTNAME}/realms/staff/"; do
      sleep 5
  done

  echo "IDP is available, starting api…"

  java \
    -cp /app/gover.jar \
    -Dloader.path="${GOVER_PLUGINS_DIR}" \
    -Duser.timezone="${TZ}" \
    org.springframework.boot.loader.launch.PropertiesLauncher
else
  echo "app" > /app/runtime-mode

  echo "Waiting for the API to be available at ${GOVER_HOSTNAME}/api/actuator/health…"
  until curl --output /dev/null --silent --head --fail "${GOVER_HOSTNAME}/api/actuator/health"; do
    sleep 5
  done

  echo "Starting app…"
  nginx -g "daemon off;"
fi
