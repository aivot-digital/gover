# Server Gover Setup

This example starts Gover, Keycloak, PostgreSQL, Redis, RabbitMQ, ClamAV, Gotenberg, and Caddy on one production server.
Caddy terminates HTTPS and requests Let's Encrypt certificates automatically.

## 1. Prepare the Server

Before starting the stack, make sure that:

- Docker Engine with the Docker Compose plugin is installed.
- The public DNS record for your Gover hostname points to this server.
- Ports `80` and `443` are reachable from the internet.
- No other service is already listening on ports `80` or `443`.
- You have SMTP credentials for outbound mail.

Use a real HTTPS hostname. Do not use `localhost` or an IP address for this setup.

## 2. Create `.env`

Create a file named `.env` next to `compose.yml`.
Replace every example value before starting the stack.

```dotenv
# Public URL
GOVER_HOSTNAME=https://gover.example.org
GOVER_HOSTNAME_REGEX=gover\\.example\\.org

# Gover database and application secrets
GOVER_DB_PASSWORD=change-me
GOVER_SECRETS_KEY=change-me-with-at-least-32-characters
GOVER_CAPTCHA_KEY=change-me-with-at-least-32-characters
GOVER_KEYCLOAK_OIDC_BACKEND_CLIENT_SECRET=change-me-with-at-least-32-characters

# Mail
GOVER_SMTP_HOST=smtp.example.org
GOVER_SMTP_PORT=587
GOVER_SMTP_AUTH=true
GOVER_SMTP_TLS=true
GOVER_SMTP_USERNAME=gover@example.org
GOVER_SMTP_PASSWORD=change-me
GOVER_FROM_MAIL=gover@example.org
GOVER_REPORT_MAIL=admin@example.org

# Initial Gover administrator
GOVER_BOOTSTRAP_ADMIN_MAIL=admin@example.org

# Keycloak
KEYCLOAK_DB_PASSWORD=change-me
KEYCLOAK_BOOTSTRAP_ADMIN_USERNAME=bootstrap-admin
KEYCLOAK_BOOTSTRAP_ADMIN_PASSWORD=change-me
KEYCLOAK_ADMIN_EMAIL=admin@example.org
KEYCLOAK_ADMIN_USERNAME=admin
KEYCLOAK_ADMIN_PASSWORD=change-me
KEYCLOAK_DEPLOYMENT_CLIENT_SECRET=change-me-with-at-least-32-characters

# RabbitMQ
RABBITMQ_PASSWORD=change-me
```

`GOVER_HOSTNAME` is the full public URL including `https://`, without a trailing slash.
`GOVER_HOSTNAME_REGEX` is the same hostname escaped for a regular expression, without the protocol.

Generate strong secrets, for example:

```bash
openssl rand -hex 32
```

## 3. Start the Stack

Run the commands from this directory so Docker Compose loads the `.env` file:

```bash
cd examples/server
docker compose config --quiet
docker compose up -d
```

First startup can take several minutes while PostgreSQL initializes, Keycloak starts, Caddy requests certificates, and `keycloak_setup` creates the required realms and clients.

Check the startup logs with:

```bash
docker compose logs -f caddy keycloak keycloak_setup gover_api
```

## 4. Access Gover

After startup, use these URLs:

- Gover staff app: `${GOVER_HOSTNAME}/staff`
- Keycloak admin console: `${GOVER_HOSTNAME}/idp/admin/`

Sign in to the Keycloak admin console with `KEYCLOAK_ADMIN_USERNAME` and `KEYCLOAK_ADMIN_PASSWORD`.
The `KEYCLOAK_BOOTSTRAP_ADMIN_*` account is only used to bootstrap Keycloak.

## 5. Create the First Gover Admin

Gover staff users are managed in the Keycloak `staff` realm.
The Keycloak admin account is not automatically a Gover administrator.

To create the initial Gover administrator:

1. Open the Keycloak admin console.
2. Switch to the `staff` realm.
3. Create a new user.
4. Set the user's e-mail address to `GOVER_BOOTSTRAP_ADMIN_MAIL`.
5. Set a password on the user's "Credentials" tab.
6. Disable "Temporary" if the password should not expire after first login.
7. Log into `${GOVER_HOSTNAME}/staff` with that staff user.

When this user logs into Gover for the first time, Gover imports the user.
If no Gover super administrator exists yet and the e-mail address matches `GOVER_BOOTSTRAP_ADMIN_MAIL`, the user receives the `Superadministrator:in` system role.

## 6. Backups

Back up these Docker volumes regularly:

- `gover_database_data`
- `keycloak_database_data`
- `gover_data`
- `rabbitmq_data`
- `caddy_data`
- `caddy_config`

The database volumes and `gover_data` are the critical application data.
`caddy_data` contains the TLS certificates and ACME account data.

**Attention:** Make sure to stop the stack before backing up the volumes to avoid data corruption:

```bash
docker compose down
```

## 7. Updates

Back up the volumes first, then update the stack with:

```bash
docker compose pull
docker compose up -d
docker compose logs -f gover_api keycloak_setup
```
