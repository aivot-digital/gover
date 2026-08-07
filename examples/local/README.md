# Local Gover Setup

## 1. Prepare Secrets

Create a file called `.env` in the directory of the `compose.yml` file and add the following secrets to it.
Make sure to replace the values with your own secure secrets.

```dotenv
# Gover Passwords and Secrets
PROSUNA_DB_PASSWORD=password
PROSUNA_SECRETS_KEY=Super-secret-key-with-at-least-32-characters
PROSUNA_CAPTCHA_KEY=Super-secret-key-with-at-least-32-characters

# Keycloak Passwords and Secrets
PROSUNA_KEYCLOAK_OIDC_BACKEND_CLIENT_SECRET=Super-secret-key-with-at-least-32-characters
KEYCLOAK_DB_PASSWORD=password
KEYCLOAK_ADMIN_USERNAME=admin
KEYCLOAK_ADMIN_PASSWORD=Super-secret-password-with-at-least-12-characters1!
KEYCLOAK_DEPLOYMENT_CLIENT_SECRET=Super-secret-key-with-at-least-32-characters

# RabbitMQ Password
RABBITMQ_PASSWORD=password
```

### Keycloak Password Policy

Keycloak user passwords, including staff user passwords, must be at least 12 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character.
They must not match the username or e-mail address, must not contain the username, and must not appear in the `100k_passwords.txt` password blacklist.

## 2. Start the Services

Run the following command to start the services defined in the `compose.yml` file:

```bash
docker compose up -d
```

## 3. Access the Services

Once the services are up and running, you can access them using the following URLs:

- Keycloak Admin Console: [http://localhost:9595/idp](http://localhost:9595/idp)
- Gover Staff App: [http://localhost:9595/staff](http://localhost:9595/staff)
- Mailpit: [http://localhost:9025](http://localhost:9025)

## 4. Next Steps

After all services are running, you need to create a new staff user in the staff realm.
Log into the Keycloak Admin Console with the credentials from the `.env`-File and open the "Staff" realm.
Then, create a new user in the "Users" section and assign them a password.
You can then log into the Gover Staff App using the newly created staff user credentials.

You can use the email address `admin@example.com` for the staff user to automatically register this user as an admin.
