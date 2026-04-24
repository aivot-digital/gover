#!/bin/sh

echo "Starting Gover version ${BUILD_VERSION} build ${BUILD_NUMBER}"

if [ "$1" = "serve" ]; then
  echo "serve" > /app/runtime-mode
  echo "Waiting for IDP to be available at ${GOVER_KEYCLOAK_OIDC_HOSTNAME}/realms/staff…"

  until curl --output /dev/null --silent --head --fail "${GOVER_KEYCLOAK_OIDC_HOSTNAME}/realms/staff/"; do
      sleep 5
  done

  echo "IDP is available, starting api…"

  java \
    -cp /app/gover.jar \
    -Dloader.path=/app/plugins/ \
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
