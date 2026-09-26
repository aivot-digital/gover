# Single-Server Prosuna Setup Example

This example shows one possible way to run Prosuna, Keycloak, PostgreSQL, Redis, RabbitMQ, ClamAV, Gotenberg, and Caddy on a single server.
Caddy terminates HTTPS and requests Let's Encrypt certificates automatically.

For an on-premises installation, the responsible system administrators must review and adapt this example to the actual infrastructure, security, availability, backup, and operating requirements. The included [Docker Compose configuration](./compose.yml) and [Caddyfile](./Caddyfile) are examples, not universal production installation instructions.

## 1. Prepare the Server

### Prerequisites

- Ubuntu 26.04 LTS or newer.
- Docker Engine with the Docker Compose plugin.

Before starting the stack, make sure that:

- The public DNS record for your Prosuna hostname points to this server.
- Ports `80` and `443` are reachable from the internet.
- No other service is already listening on ports `80` or `443`.
- You have SMTP credentials for outbound mail.

Use a real HTTPS hostname. Do not use `localhost` or an IP address for this setup.

## 2. Create `.env`

Create a file named `.env` next to `compose.yml`.
Replace every example value before starting the stack.

```dotenv
# Public URL
PROSUNA_HOSTNAME=https://prosuna.example.org
PROSUNA_HOSTNAME_REGEX=prosuna\\.example\\.org

# Prosuna database and application secrets
PROSUNA_DB_PASSWORD=change-me
PROSUNA_SECRETS_KEY=change-me-with-at-least-32-characters
PROSUNA_CAPTCHA_KEY=change-me-with-at-least-32-characters
PROSUNA_KEYCLOAK_OIDC_BACKEND_CLIENT_SECRET=change-me-with-at-least-32-characters

# Mail
PROSUNA_SMTP_HOST=smtp.example.org
PROSUNA_SMTP_PORT=587
PROSUNA_SMTP_AUTH=true
PROSUNA_SMTP_TLS=true
PROSUNA_SMTP_USERNAME=prosuna@example.org
PROSUNA_SMTP_PASSWORD=change-me
PROSUNA_FROM_MAIL=prosuna@example.org
PROSUNA_REPORT_MAIL=admin@example.org

# Initial Prosuna administrator
PROSUNA_BOOTSTRAP_ADMIN_MAIL=admin@example.org

# Keycloak
KEYCLOAK_DB_PASSWORD=change-me
KEYCLOAK_BOOTSTRAP_ADMIN_USERNAME=bootstrap-admin
KEYCLOAK_BOOTSTRAP_ADMIN_PASSWORD=Change-me-with-12-characters1!
KEYCLOAK_ADMIN_EMAIL=admin@example.org
KEYCLOAK_ADMIN_USERNAME=admin
KEYCLOAK_ADMIN_PASSWORD=Change-me-with-12-characters1!
KEYCLOAK_DEPLOYMENT_CLIENT_SECRET=change-me-with-at-least-32-characters

# RabbitMQ
RABBITMQ_PASSWORD=change-me
```

`PROSUNA_HOSTNAME` is the full public URL including `https://`, without a trailing slash.
`PROSUNA_HOSTNAME_REGEX` is the same hostname escaped for a regular expression, without the protocol.

Generate strong secrets, for example:

```bash
openssl rand -hex 32
```

### Keycloak Password Policy

Keycloak user passwords, including staff user passwords, must be at least 12 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character.
They must not match the username or e-mail address, must not contain the username, and must not appear in the `100k_passwords.txt` password blacklist.

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
docker compose logs -f caddy keycloak keycloak_setup prosuna_api
```

## 4. Access Prosuna

After startup, use these URLs:

- Prosuna staff app: `${PROSUNA_HOSTNAME}/staff`
- Keycloak admin console: `${PROSUNA_HOSTNAME}/idp/admin/`

Sign in to the Keycloak admin console with `KEYCLOAK_ADMIN_USERNAME` and `KEYCLOAK_ADMIN_PASSWORD`.
The `KEYCLOAK_BOOTSTRAP_ADMIN_*` account is only used to bootstrap Keycloak.

## 5. Create the First Prosuna Admin

Prosuna staff users are managed in the Keycloak `staff` realm.
The Keycloak admin account is not automatically a Prosuna administrator.

To create the initial Prosuna administrator:

1. Open the Keycloak admin console.
2. Switch to the `staff` realm.
3. Create a new user.
4. Set the user's e-mail address to `PROSUNA_BOOTSTRAP_ADMIN_MAIL`.
5. Set a password on the user's "Credentials" tab.
6. Disable "Temporary" if the password should not expire after first login.
7. Log into `${PROSUNA_HOSTNAME}/staff` with that staff user.

When this user logs into Prosuna for the first time, Prosuna imports the user.
If no active Prosuna user holds the configured system role with the highest permission level and the e-mail address matches `PROSUNA_BOOTSTRAP_ADMIN_MAIL`, the user receives that role.
The role defaults to `Superadministrator:in` and can later be changed under the general application settings.

## 6. Backups

Back up these Docker volumes regularly:

- `prosuna_database_data`
- `keycloak_database_data`
- `prosuna_data`
- `rabbitmq_data`
- `caddy_data`
- `caddy_config`

The database volumes and `prosuna_data` are the critical application data.
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
docker compose logs -f prosuna_api keycloak_setup
```
