#!/bin/sh

cat > /app/app-config.js <<EOF
window.AppConfig = {
    oidc: {
        realm: '$GOVER_KEYCLOAK_OIDC_REALM',
        client: '$GOVER_KEYCLOAK_OIDC_FRONTEND_CLIENT_ID',
        hostname: '$GOVER_KEYCLOAK_OIDC_HOSTNAME',
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

if [ "$1" = "serve" ]; then
    echo "Starting server…"
    java -jar /app/gover.jar
else
  echo "Starting frontend…"
  nginx -g "daemon off;"
fi
