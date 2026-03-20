#!/bin/bash

db_version="16.12-alpine3.23"
antivirus_version="1.5.1"
cache_version="8.2.4-alpine3.22"
pdf_printer_version="8.26.0"
message_broker_version="4.2.5-alpine"
keycloak_version="26.5.4.0"
keycloak_setup_version="0.0.17"
reverse_proxy_version="2.10.2-alpine"
gover_version="5.0.0"

read -p "Bitte geben Sie den Hostnamen ein, unter dem die Gover-Anwendung erreichbar sein soll (z.B. https://example.com): " hostname

if [ ! -f .env ]; then
  cat > .env <<EOF
# --------------------------------- Manuelle Werte - bitte ausfüllen ---------------------------------

# Der Hostname, unter dem die Gover-Anwendung erreichbar sein soll (z.B. https://example.com)
HOSTNAME=${hostname}

# Smtp Server Details
SMTP_HOST=smtp.example.com
SMTP_PORT=587
SMTP_USERNAME=change-me
SMTP_PASSWORD=change-me

# E-Mail-Adresse für technische Fehlermeldungen
REPORT_MAIL=change-me

# ----------------------------------- Automatisch generierte Werte -----------------------------------

# Der Benutzer für die Gover-Postgresql-Datenbank.
GOVER_POSTGRES_USERNAME=gover_$(openssl rand -hex 4)
GOVER_POSTGRES_PASSWORD=$(openssl rand -hex 18)

# Der Benutzer für die Keycloak-Postgresql-Datenbank.
KEYCLOAK_POSTGRES_USERNAME=keycloak_$(openssl rand -hex 4)
KEYCLOAK_POSTGRES_PASSWORD=$(openssl rand -hex 18)

# Der Bootstrap-Admin für Keycloak.
# Dieser wird nur für die Ersteinrichtung von Keycloak benötigt und kann danach gelöscht werden.
KEYCLOAK_BOOTSTRAP_ADMIN_USERNAME=bootstrap_admin_$(openssl rand -hex 2)
KEYCLOAK_BOOTSTRAP_ADMIN_PASSWORD=$(openssl rand -hex 18)

# Der Client, der für die Einrichtung von Keycloak genutzt wird.
# Dieser wird für die Ersteinrichtung, sowie für Aktualisierungen der Konfiguration von Keycloak benötigt.
KEYCLOAK_DEPLOYMENT_CLIENT_NAME=deployment_$(openssl rand -hex 2)
KEYCLOAK_DEPLOYMENT_CLIENT_SECRET=$(openssl rand -hex 18)

# Der Admin-User für Keycloak, der nach der Ersteinrichtung angelegt wird.
KEYCLOAK_ADMIN_EMAIL=mail@example.com
KEYCLOAK_ADMIN_PASSWORD=$(openssl rand -hex 18)

# Das Secret, mit dem der Backend-Client auf Keycloak zugreift.
# Dieser wird für die Kommunikation zwischen Gover und Keycloak benötigt.
KEYCLOAK_BACKEND_CLIENT_SECRET=$(openssl rand -hex 32)

# Der Benutzer für RabbitMQ.
RABBITMQ_USERNAME=gover_$(openssl rand -hex 4)
RABBITMQ_PASSWORD=$(openssl rand -hex 18)

# Die geheimen Schlüssel für die Gover-Anwendung.
# Diese sollten zufällig generiert werden und mindestens 32 Zeichen lang sein.
GOVER_SECRETS_KEY=$(openssl rand -hex 48)
GOVER_CAPTCHA_KEY=$(openssl rand -hex 48)

# Weitere Smtp-Einstellungen
SMTP_AUTH=true
SMTP_TLS=true
EOF
fi

if [ ! -f Caddyfile ]; then
  cat > Caddyfile <<EOF
${hostname} {
    @app {
        path /
        path /*
    }

    @idp {
        path /idp
        path /idp/*
    }

    @api {
        path /api
        path /api/*
    }

    handle @api {
        reverse_proxy gover-api:8080
    }

    handle @idp {
        reverse_proxy keycloak:8080 {
            header_up X-Forwarded-For {header.X-Forwarded-For}
            header_up X-Forwarded-Host {header.X-Forwarded-Host}
            header_up X-Forwarded-Port {header.X-Forwarded-Port}
            header_up X-Forwarded-Proto {header.X-Forwarded-Proto}
        }
    }

    handle @app {
        reverse_proxy gover-app:80
    }
}
EOF

fi

cat > docker-compose.yml <<EOF
services:

  gover-database:
    image: docker.io/postgres:${db_version}
    restart: unless-stopped
    environment:
      POSTGRES_USER: \${GOVER_POSTGRES_USERNAME}
      POSTGRES_PASSWORD: \${GOVER_POSTGRES_PASSWORD}
      POSTGRES_DB: gover
      PGDATA: /var/lib/postgresql/data/pgdata
      POSTGRES_INITDB_ARGS: >
        --encoding=UTF-8
        --lc-collate=de-DE-x-icu
        --lc-ctype=de-DE-x-icu
        --locale-provider=icu
        --icu-locale=de-DE
    volumes:
      - gover_pg_data:/var/lib/postgresql/data
    networks:
      - gover-network

  antivirus:
    image: docker.io/clamav/clamav-debian:${antivirus_version}
    restart: unless-stopped
    networks:
      - gover-network

  cache:
    image: docker.io/redis:${cache_version}
    restart: unless-stopped
    networks:
      - gover-network

  pdf-printer:
    image: docker.io/gotenberg/gotenberg:${pdf_printer_version}
    restart: unless-stopped
    environment:
      API_PORT: 9191
    networks:
      - gover-network

  message-broker:
    image: docker.io/rabbitmq:${message_broker_version}
    restart: unless-stopped
    environment:
      RABBITMQ_DEFAULT_USER: \${RABBITMQ_USERNAME}
      RABBITMQ_DEFAULT_PASS: \${RABBITMQ_PASSWORD}
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    networks:
      - gover-network

  keycloak-database:
    image: docker.io/postgres:${db_version}
    restart: unless-stopped
    environment:
      POSTGRES_USER: \${KEYCLOAK_POSTGRES_USERNAME}
      POSTGRES_PASSWORD: \${KEYCLOAK_POSTGRES_PASSWORD}
      POSTGRES_DB: keycloak
      PGDATA: /var/lib/postgresql/data/pgdata
    volumes:
      - keycloak_pg_data:/var/lib/postgresql/data
    networks:
        - keycloak-network

  keycloak:
    image: ghcr.io/aivot-digital/keycloak-egov-plugins:${keycloak_version}
    restart: unless-stopped
    depends_on:
      - keycloak-database
    command: start
    environment:
      KC_HTTP_ENABLED: true
      KC_PROXY_HEADERS: xforwarded
      KC_HOSTNAME: \${HOSTNAME}/idp
      KC_HOSTNAME_PATH: /idp
      KC_HTTP_RELATIVE_PATH: /idp
      KC_BOOTSTRAP_ADMIN_USERNAME: \${KEYCLOAK_BOOTSTRAP_ADMIN_USERNAME}
      KC_BOOTSTRAP_ADMIN_PASSWORD: \${KEYCLOAK_BOOTSTRAP_ADMIN_PASSWORD}
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://keycloak-database:5432/keycloak
      KC_DB_USERNAME: \${KEYCLOAK_POSTGRES_USERNAME}
      KC_DB_PASSWORD: \${KEYCLOAK_POSTGRES_PASSWORD}
      KC_FEATURES: transient-users,update-email,hostname:v2
    networks:
        - keycloak-network
        - proxy-network

  keycloak-setup:
    image: ghcr.io/aivot-digital/keycloak-egov-plugins-setup:${keycloak_setup_version}
    depends_on:
      - keycloak
    environment:
      KEYCLOAK_URL: http://keycloak:8080
      HOSTNAME: \${HOSTNAME}
      KEYCLOAK_BOOTSTRAP_ADMIN_USERNAME: \${KEYCLOAK_BOOTSTRAP_ADMIN_USERNAME}
      KEYCLOAK_BOOTSTRAP_ADMIN_PASSWORD: \${KEYCLOAK_BOOTSTRAP_ADMIN_PASSWORD}
      KEYCLOAK_DEPLOYMENT_CLIENT_NAME: \${KEYCLOAK_DEPLOYMENT_CLIENT_NAME}
      KEYCLOAK_DEPLOYMENT_CLIENT_SECRET: \${KEYCLOAK_DEPLOYMENT_CLIENT_SECRET}
      KEYCLOAK_ADMIN_EMAIL: \${KEYCLOAK_ADMIN_EMAIL}
      KEYCLOAK_ADMIN_USERNAME: \${KEYCLOAK_ADMIN_EMAIL}
      KEYCLOAK_ADMIN_PASSWORD: \${KEYCLOAK_ADMIN_PASSWORD}
      KEYCLOAK_AVAILABILITYCHECK_ENABLED: "true"
      KEYCLOAK_AVAILABILITYCHECK_TIMEOUT: 120s
      LOGGING_LEVEL_HTTP: ERROR
      BACKEND_CLIENT_SECRET: \${KEYCLOAK_BACKEND_CLIENT_SECRET}
      SMTP_HOST: \${SMTP_HOST}
      SMTP_PORT: \${SMTP_PORT}
      SMTP_USERNAME: \${SMTP_USERNAME}
      SMTP_PASSWORD: \${SMTP_PASSWORD}
      SMTP_FROM: \${SMTP_FROM}
      SMTP_FROM_DISPLAY: \${SMTP_FROM_DISPLAY}

  gover-api:
    image: ghcr.io/aivot-digital/gover:${gover_version}
    command: serve
    restart: unless-stopped
    depends_on:
      - gover-database
      - antivirus
      - cache
      - pdf-printer
      - message-broker
      - keycloak
    environment:
      GOVER_DB_HOST: gover-database
      GOVER_DB_PORT: 5432
      GOVER_DB_DATABASE: gover
      GOVER_DB_USERNAME: \${GOVER_POSTGRES_USER}
      GOVER_DB_PASSWORD: \${GOVER_POSTGRES_PASSWORD}
      GOVER_CLAM_HOST: antivirus
      GOVER_CLAM_PORT: 3310
      GOVER_GOTENBERG_HOST: pdf-printer
      GOVER_GOTENBERG_PORT: 3000
      GOVER_SMTP_HOST: \${SMTP_HOST}
      GOVER_SMTP_PORT: \${SMTP_PORT}
      GOVER_SMTP_AUTH: \${SMTP_AUTH}
      GOVER_SMTP_TLS: \${SMTP_TLS}
      GOVER_SMTP_USERNAME: \${SMTP_USERNAME}
      GOVER_SMTP_PASSWORD: \${SMTP_PASSWORD}
      GOVER_LOG_LEVEL: WARN
      GOVER_FROM_MAIL: '"\${SMTP_FROM_DISPLAY}" <\${SMTP_FROM}>'
      GOVER_REPORT_MAIL: \${REPORT_MAIL}
      GOVER_SENTRY_SERVER: "https://4e653c46f8496447bd033f3c477e2ee2@app-monitoring.aivot.cloud/3"
      GOVER_SENTRY_WEB_APP: "https://da7fe01c989214420911c676813a5349@app-monitoring.aivot.cloud/2"
      GOVER_ENVIRONMENT: {{ env.name }}
      GOVER_HOSTNAME: \${HOSTNAME}
      GOVER_SECRETS_KEY: \${GOVER_SECRETS_KEY}
      GOVER_CAPTCHA_KEY: \${GOVER_CAPTCHA_KEY}
      GOVER_FILE_EXTENSIONS: "pdf, png, jpg, jpeg, doc, docx, xls, xlsx, ppt, pptx, odt, fodt, ods, fods, odp, fodp, odg, fodg, odf"
      GOVER_CONTENT_TYPES: "application/pdf, image/png, image/jpeg, application/msword, application/vnd.openxmlformats-officedocument.wordprocessingml.document, application/msexcel, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, application/mspowerpoint, application/vnd.openxmlformats-officedocument.presentationml.presentation, application/vnd.oasis.opendocument.text, application/vnd.oasis.opendocument.spreadsheet, application/vnd.oasis.opendocument.presentation, application/vnd.oasis.opendocument.graphics, application/vnd.oasis.opendocument.formula"
      GOVER_KEYCLOAK_OIDC_FRONTEND_CLIENT_ID: app
      GOVER_KEYCLOAK_OIDC_BACKEND_CLIENT_ID: backend
      GOVER_KEYCLOAK_OIDC_BACKEND_CLIENT_SECRET: \${KEYCLOAK_BACKEND_CLIENT_SECRET}
      GOVER_KEYCLOAK_OIDC_HOSTNAME: \${HOSTNAME}/idp
      GOVER_KEYCLOAK_OIDC_REALM: staff
      GOVER_REDIS_HOST: redis
      GOVER_REDIS_PORT: 6379
      GOVER_REDIS_DATABASE: 0
      GOVER_LOG_FORMAT: ecs
      GOVER_RABBITMQ_HOST: message-broker
      GOVER_RABBITMQ_PORT: 5672
      GOVER_RABBITMQ_USERNAME: \${RABBITMQ_USERNAME}
      GOVER_RABBITMQ_PASSWORD: \${RABBITMQ_PASSWORD}
    volumes:
      - ./plugins:/app/plugins
      - gover_app_data:/app/data
    networks:
      - gover-network
      - keycloak-network
      - proxy-network

  gover-app:
    image: ghcr.io/aivot-digital/gover:${gover_version}
    restart: unless-stopped
    command: app
    depends_on:
      - gover-api
    environment:
      GOVER_KEYCLOAK_OIDC_FRONTEND_CLIENT_ID: app
      GOVER_KEYCLOAK_OIDC_REALM: staff
      GOVER_KEYCLOAK_OIDC_HOSTNAME: \${HOSTNAME}/idp
      GOVER_HOSTNAME: \${HOSTNAME}
      GOVER_SENTRY_WEB_APP: "https://1677139e6e374703aaed37a824e3fb4a@sentry.aivot.cloud/3"
    networks:
      - proxy-network

  reverse-proxy:
    image: docker.io/caddy:${reverse_proxy_version}
    restart: unless-stopped
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - caddy_data:/data
    networks:
      - proxy-network
    ports:
      - 80:80
      - 443:443

volumes:
  gover_pg_data:
  keycloak_pg_data:
  rabbitmq_data:
  gover_app_data:
  caddy_data:

networks:
  gover-network:
  keycloak-network:
  proxy-network:
EOF

echo "Die folgenden Dateien wurden erstellt:"
echo "- .env: Enthält die Umgebungsvariablen für die Anwendung. Bitte überprüfen und ggf. anpassen."
echo "- Caddyfile: Konfiguration für den Reverse-Proxy Caddy. Bitte überprüfen und ggf. anpassen."
echo "- docker-compose.yml: Docker-Compose-Datei, die die Container für die Anwendung definiert. Bitte überprüfen und ggf. anpassen."
echo ""
echo "Bitte überprüfen Sie die .env-Datei und passen Sie die manuellen Konfigurationen an."
echo ""
echo "Nachdem Sie die .env-Datei überprüft und ggf. angepasst haben, können Sie die Anwendung mit dem folgenden Befehl starten:"
echo "docker-compose up -d"