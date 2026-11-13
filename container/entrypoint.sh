#!/bin/sh

export PROSUNA_TIMEZONE="${PROSUNA_TIMEZONE:-Europe/Berlin}"
export TZ="${PROSUNA_TIMEZONE}"

export PROSUNA_PLUGINS_DIR="${PROSUNA_PLUGINS_DIR:-/app/plugins}"

echo "Starting Prosuna version ${BUILD_VERSION} build ${BUILD_NUMBER}"
echo "Using runtime timezone ${TZ}"

if [ "$1" = "serve" ]; then
  echo "serve" > /app/runtime-mode
  # Fail fast instead of polling /realms// forever; the backend requires this variable as well.
  idp_realm_url="${PROSUNA_KEYCLOAK_OIDC_INTERNAL_HOSTNAME:-${PROSUNA_KEYCLOAK_OIDC_HOSTNAME}}/realms/${PROSUNA_KEYCLOAK_OIDC_REALM:?PROSUNA_KEYCLOAK_OIDC_REALM must be set}"
  echo "Waiting for IdP to be available at ${idp_realm_url}…"

  until curl --output /dev/null --silent --head --fail "${idp_realm_url}/"; do
      sleep 5
  done

  echo "IdP is available, starting api…"

  java \
    -cp /app/prosuna.jar \
    -Dloader.path="${PROSUNA_PLUGINS_DIR}" \
    -Duser.timezone="${TZ}" \
    org.springframework.boot.loader.launch.PropertiesLauncher
else
  echo "app" > /app/runtime-mode

  # The actuator only listens on the internal management port of the API, so wait for a public endpoint instead.
  echo "Waiting for the API to be available at ${PROSUNA_HOSTNAME}/api/public/system/app-config.js…"
  until curl --output /dev/null --silent --head --fail "${PROSUNA_HOSTNAME}/api/public/system/app-config.js"; do
    sleep 5
  done

  echo "Starting app…"
  nginx -g "daemon off;"
fi
