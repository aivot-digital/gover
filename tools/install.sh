#!/bin/bash

# -e: Exit immediately if a command exits with a non-zero status.
# -u: Treat unset variables as an error.
# -o pipefail: Prevents errors in a pipeline from being masked.
set -euo pipefail

# Initialize .env file if it doesn't exist
if [ ! -f .env ]; then
  touch .env
  chmod 600 .env
fi

# Container Images
postgres_image="docker.io/postgres:18.3-alpine3.23"
clamav_image="registry.opencode.de/open-code/oci/clamav:1.4.3"
redis_image="registry.opencode.de/open-code/oci/redis:8.2.3"
gotenberg_image="docker.io/gotenberg/gotenberg:8.30.1-chromium"
rabbitmq_image="dhi.io/rabbitmq:4.2"
keycloak_image="ghcr.io/aivot-digital/keycloak-egov-plugins:26.5.7.0"
keycloak_setup_image="ghcr.io/aivot-digital/keycloak-egov-plugins-setup:0.0.20"
caddy_image="dhi.io/caddy:2"
prosuna_image="ghcr.io/aivot-digital/gover-next:5.0.0-alpha-71"

# Generate random hex strings for secrets
rand_hex() {
  openssl rand -hex "$1"
}

# Automatically generated environment variables with random values
automatic_env=(
  "PROSUNA_POSTGRES_USERNAME $(rand_hex 4)"
  "PROSUNA_POSTGRES_PASSWORD $(rand_hex 18)"
  "KEYCLOAK_POSTGRES_USERNAME $(rand_hex 4)"
  "KEYCLOAK_POSTGRES_PASSWORD $(rand_hex 18)"
  "KEYCLOAK_BOOTSTRAP_ADMIN_USERNAME $(rand_hex 4)"
  "KEYCLOAK_BOOTSTRAP_ADMIN_PASSWORD $(rand_hex 18)"
  "KEYCLOAK_DEPLOYMENT_CLIENT_NAME $(rand_hex 4)"
  "KEYCLOAK_DEPLOYMENT_CLIENT_SECRET $(rand_hex 18)"
  "KEYCLOAK_ADMIN_EMAIL mail@example.com"
  "KEYCLOAK_ADMIN_PASSWORD $(rand_hex 18)"
  "KEYCLOAK_BACKEND_CLIENT_SECRET $(rand_hex 24)"
  "RABBITMQ_USERNAME $(rand_hex 4)"
  "RABBITMQ_PASSWORD $(rand_hex 18)"
  "PROSUNA_SECRETS_KEY $(rand_hex 24)"
  "PROSUNA_CAPTCHA_KEY $(rand_hex 24)"
)

# Process the automatically generated environment variables and write them to the .env file
for env in "${automatic_env[@]}"; do
  key="${env%% *}"
  value="${env#* }"

  # Check if the key already exists in the .env file
  if grep -q "^${key}=" .env 2>/dev/null; then
    # If the key already exists, replace its value
    sed -i "s/^${key}=.*/${key}=${value}/" .env
  else
    # If the key does not exist, append it to the file
    echo "${key}=${value}" >> .env
  fi
done

# Manually configured environment variables that require user input
ask_for_env=(
  "HOSTNAME Der Hostname unter dem Gover erreichbar ist"
)

# Process the manually configured environment variables and prompt the user for input if they are not already set in the .env file
for env in "${ask_for_env[@]}"; do
  key="${env%% *}"
  description="${env#* }"

  if ! grep -q "^${key}=" .env 2>/dev/null; then
    read -rp "Bitte geben Sie den Wert für ${description} (${key}) ein: " value
    echo "${key}=${value}" >> .env
  fi
done

# Create Caddyfile with reverse proxy configuration
cat > Caddyfile << EOF
{$HOSTNAME} {
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
        reverse_proxy prosuna-api:8080
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
        reverse_proxy prosuna-app:80
    }
}
EOF

# Create docker-compose.yml with service definitions
cat > docker-compose.yml <<EOF
services:

  prosuna-postgres:
    image: ${postgres_image}
    restart: unless-stopped
    environment:
      POSTGRES_USER: \${PROSUNA_POSTGRES_USERNAME}
      POSTGRES_PASSWORD: \${PROSUNA_POSTGRES_PASSWORD}
      POSTGRES_DB: prosuna
      PGDATA: /var/lib/postgresql/data/pgdata
      POSTGRES_INITDB_ARGS: >
        --encoding=UTF-8
        --lc-collate=de-DE-x-icu
        --lc-ctype=de-DE-x-icu
        --locale-provider=icu
        --icu-locale=de-DE
    volumes:
      - prosuna_pg_data:/var/lib/postgresql/data
    networks:
      - prosuna-network

  clamav:
    image: ${clamav_image}
    restart: unless-stopped
    networks:
      - prosuna-network

  redis:
    image: ${redis_image}
    restart: unless-stopped
    networks:
      - prosuna-network

  gotenberg:
    image: ${gotenberg_image}
    restart: unless-stopped
    environment:
      API_PORT: 9191
    networks:
      - prosuna-network

  rabbitmq:
    image: ${rabbitmq_image}
    restart: unless-stopped
    environment:
      RABBITMQ_DEFAULT_USER: \${RABBITMQ_USERNAME}
      RABBITMQ_DEFAULT_PASS: \${RABBITMQ_PASSWORD}
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    networks:
      - prosuna-network

  keycloak-postgres:
    image: ${postgres_image}
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
    image: ${keycloak_image}
    restart: unless-stopped
    depends_on:
      - keycloak-database
    command: start
    environment:
      KC_HTTP_ENABLED: true
      KC_PROXY_HEADERS: xforwarded
      KC_HOSTNAME: \${HOSTNAME}/idp
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
    image: ${keycloak_setup_image}
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
    networks:
      - keycloak-network

  prosuna-api:
    image: ${prosuna_image}
    command: serve
    restart: unless-stopped
    depends_on:
      - prosuna-postgres
      - clamav
      - redis
      - gotenberg
      - rabbitmq
      - keycloak
    environment:
      PROSUNA_DB_HOST: prosuna-postgres
      PROSUNA_DB_PORT: 5432
      PROSUNA_DB_DATABASE: prosuna
      PROSUNA_DB_USERNAME: \${PROSUNA_POSTGRES_USERNAME}
      PROSUNA_DB_PASSWORD: \${PROSUNA_POSTGRES_PASSWORD}
      PROSUNA_DB_MIN_IDLE: 4
      PROSUNA_DB_MAX_POOL_SIZE: 32
      PROSUNA_CLAM_HOST: clamav
      PROSUNA_CLAM_PORT: 3310
      PROSUNA_GOTENBERG_HOST: gotenberg
      PROSUNA_GOTENBERG_PORT: 9191
      PROSUNA_RABBITMQ_HOST: rabbitmq
      PROSUNA_RABBITMQ_PORT: 5672
      PROSUNA_RABBITMQ_USERNAME: \${RABBITMQ_USERNAME}
      PROSUNA_RABBITMQ_PASSWORD: \${RABBITMQ_PASSWORD}
      PROSUNA_REDIS_HOST: redis
      PROSUNA_REDIS_PORT: 6379
      PROSUNA_REDIS_DATABASE: 0
      PROSUNA_KEYCLOAK_OIDC_FRONTEND_CLIENT_ID: app
      PROSUNA_KEYCLOAK_OIDC_BACKEND_CLIENT_ID: backend
      PROSUNA_KEYCLOAK_OIDC_BACKEND_CLIENT_SECRET: \${KEYCLOAK_BACKEND_CLIENT_SECRET}
      PROSUNA_KEYCLOAK_OIDC_HOSTNAME: \${HOSTNAME}/idp
      PROSUNA_KEYCLOAK_OIDC_REALM: staff
      PROSUNA_SECRETS_KEY: \${PROSUNA_SECRETS_KEY}
      PROSUNA_CAPTCHA_KEY: \${PROSUNA_CAPTCHA_KEY}
      PROSUNA_BOOTSTRAP_ADMIN_MAIL: \${PROSUNA_BOOTSTRAP_ADMIN_MAIL}
      PROSUNA_LOG_LEVEL: WARN
      PROSUNA_LOG_FORMAT: ecs

      PROSUNA_REPORT_MAIL: \${REPORT_MAIL}
      PROSUNA_ENVIRONMENT: \${HOSTNAME}
      PROSUNA_HOSTNAME: \${HOSTNAME}

    volumes:
      - ./plugins:/app/plugins
      - prosuna_app_data:/app/data
    networks:
      - prosuna-network
      - keycloak-network
      - proxy-network

  prosuna-app:
    image: ${prosuna_image}
    restart: unless-stopped
    command: app
    depends_on:
      - prosuna-api
    environment:
      PROSUNA_KEYCLOAK_OIDC_FRONTEND_CLIENT_ID: app
      PROSUNA_KEYCLOAK_OIDC_REALM: staff
      PROSUNA_KEYCLOAK_OIDC_HOSTNAME: \${HOSTNAME}/idp
      PROSUNA_HOSTNAME: \${HOSTNAME}
    networks:
      - proxy-network

  caddy:
    image: ${caddy_image}
    restart: unless-stopped
    environment
      HOSTNAME: \${HOSTNAME}
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - caddy_data:/data
    networks:
      - proxy-network
    ports:
      - 80:80
      - 443:443

volumes:
  prosuna_pg_data:
  keycloak_pg_data:
  rabbitmq_data:
  prosuna_app_data:
  caddy_data:

networks:
  prosuna-network:
  keycloak-network:
  proxy-network:
EOF
