#!/bin/sh

echo "Starting Gover version ${BUILD_VERSION} build ${BUILD_NUMBER}"

if [ "$1" = "serve" ]; then
  echo "Waiting for IDP to be available at ${GOVER_HOSTNAME}/idp/realms/staff…"

  until curl --output /dev/null --silent --head --fail "${GOVER_HOSTNAME}/idp/realms/staff/"; do
      sleep 5
  done

  echo "IDP is available, starting api…"

  java -jar /app/gover.jar
else
  cat > /app/app-config.js <<EOF
window.AppConfig = {
    oidc: {
        realm: '$GOVER_KEYCLOAK_OIDC_REALM',
        client: '$GOVER_KEYCLOAK_OIDC_FRONTEND_CLIENT_ID',
        hostname: '$GOVER_KEYCLOAK_OIDC_HOSTNAME',
        idp_hint: '$GOVER_KEYCLOAK_OIDC_IDP_HINT',
    },
    api: {
        hostname: '$GOVER_HOSTNAME',
    },
    sentry: {
        dsn: '$GOVER_SENTRY_WEB_APP',
    },
};
EOF

  cp /app/app-config.js /app/www/app-config.js
  cp /app/app-config.js /app/www/staff/app-config.js

  echo "Waiting for the API to be available at ${GOVER_HOSTNAME}/api/public/actuator/health…"
  until curl --output /dev/null --silent --head --fail "${GOVER_HOSTNAME}/api/public/actuator/health"; do
    sleep 5
  done

  echo "Starting app…"
  nginx -g "daemon off;"
fi
