#!/bin/sh

export PROSUNA_TIMEZONE="${PROSUNA_TIMEZONE:-Europe/Berlin}"
export TZ="${PROSUNA_TIMEZONE}"

export PROSUNA_PLUGINS_DIR="${PROSUNA_PLUGINS_DIR:-/app/plugins}"

echo "Starting Prosuna version ${BUILD_VERSION} build ${BUILD_NUMBER}"
echo "Using runtime timezone ${TZ}"

if [ "$1" = "serve" ]; then
  echo "serve" > /app/runtime-mode
  echo "Waiting for IDP to be available at ${PROSUNA_KEYCLOAK_OIDC_INTERNAL_HOSTNAME:-${PROSUNA_KEYCLOAK_OIDC_HOSTNAME}}/realms/staff…"

  until curl --output /dev/null --silent --head --fail "${PROSUNA_KEYCLOAK_OIDC_INTERNAL_HOSTNAME:-${PROSUNA_KEYCLOAK_OIDC_HOSTNAME}}/realms/staff/"; do
      sleep 5
  done

  echo "IDP is available, starting api…"

  java \
    -cp /app/prosuna.jar \
    -Dloader.path="${PROSUNA_PLUGINS_DIR}" \
    -Duser.timezone="${TZ}" \
    org.springframework.boot.loader.launch.PropertiesLauncher
else
  echo "app" > /app/runtime-mode

  echo "Waiting for the API to be available at ${PROSUNA_HOSTNAME}/api/actuator/health…"
  until curl --output /dev/null --silent --head --fail "${PROSUNA_HOSTNAME}/api/actuator/health"; do
    sleep 5
  done

  echo "Starting app…"
  nginx -g "daemon off;"
fi
