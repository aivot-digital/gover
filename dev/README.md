# Gover Development Setup

## 1 Prerequisites

* Java JDK 21
* Docker v27.3.1+ and Docker Compose v2.29.7+
* Apache Maven v3.9.9+
* Node.js v22.0.0+
* Git v2.46.2+

## 2 Setting up a development environment

The following setup instructions are primarily designed for development installations on Unix-like systems such as
Linux or macOS.
Running the development setup on Windows is possible but may require additional steps, especially for setting up the
environment variables.
We recommend using the Windows Subsystem for Linux (WSL) for a better development experience.

The project contains the following modules and directories:

- `./app`: The frontend application for both staff and customers
- `./`: The project root of the Spring Boot backend application, including `pom.xml`
- `./src`: The source code of the Spring Boot backend application
- `./mails`: The module that contains the mail templates
- `./dev`: The module that contains the development setup
    - `./dev/docker-compose.yml`: The docker compose file for all local development services
    - `./dev/gover.env`: An environment file containing the environment variables for the Gover application
    - `./dev/examples`: A directory with example datasets to populate the local database with
- `./default-assets`: The directory that contains the default assets for the Gover service, such as default templates
  for PDFs
- `./docker`: The directory contains additional files for the container image

### 2.1 Getting started

For working on this project, you need to set up a local development environment.
The following steps will guide you through the process of setting up the development environment for the Gover service:

1. Clone this repository with

   ```bash
   git clone https://github.com/aivot-digital/gover.git ./gover
   ```

2. Change into the `gover` directory

   ```bash
   cd gover
   ```

After cloning the repository and changing into the project directory, you can follow the steps below to set up the development environment and start working on the project.
Make sure to follow the steps in the given order, as some steps depend on the successful completion of previous steps.

### 2.2 Start the development services

Gover needs several dependent services to run locally for development, such as a database and a Keycloak instance for
authentication.
You can run these services with docker compose, which will pull the necessary images and start the containers for you.

```bash
docker compose -f ./dev/docker-compose.yml up -d
```

This command will spin up all necessary services in detached mode, allowing you to continue working in your terminal.

**Service Setup:**

The docker compose file includes the following services:

- `gover-database`: A PostgreSQL database for storing the Gover application data
- `antivirus`: A ClamAV antivirus service for scanning uploaded files for viruses
- `cache`: A Redis instance for caching and session management
- `pdf-printer`: A Gotenberg instance for converting documents to PDF format
- `s3-object-storage`: A MinIO instance for storing uploaded files
- `s3-object-storage-setup`: A helper service for setting up the MinIO instance with the necessary buckets and keys
- `smtp`: A Mailpit instance for capturing outgoing emails from the application and providing a web interface to view
  them
- `keycloak-database`: A PostgreSQL database for storing the keycloak application data
- `keycloak`: A Keycloak instance for handling authentication and authorization
- `keycloak-setup`: A helper service for setting up the Keycloak instance with the necessary realms, clients, and users
- `message-broker`: A RabbitMQ instance for handling asynchronous tasks in the application
- `reverse-proxy`: A Caddy instance for reverse proxying the running services all under a single domain `http://localhost:9595`

You can find more information about accessing the services in section 3 below.

**Checking the service status:**

After running the above command, you can check the status of the services with the following command:

```bash
docker compose -f ./dev/docker-compose.yml ps
```

The services for setting up the Keycloak and MinIO instance may take a few minutes to complete.
They will stop automatically after they have completed their setup.
You can check the logs of these services to see when they have completed their setup with the following command:

```bash
docker compose -f ./dev/docker-compose.yml logs -f keycloak-setup 
```

```bash
docker compose -f ./dev/docker-compose.yml logs -f s3-object-storage-setup
```

### 2.3 Compile the mail templates

The backend application uses mail templates for sending emails to users.
These templates are located in the `./mails` module and need to be compiled before starting the backend application.
To compile the mail templates, you need to have Node.js and npm installed on your machine.

```bash
npm install --prefix ./mails 
npm run build:dev --prefix ./mails
```

### 2.4 Start the backend application

The backend application is a Spring Boot application that serves as the API for the Gover service.
To start the backend application, you need to have Java JDK 21 and Apache Maven installed on your machine.
The following commands work in Linux, macOS terminal shells, and Windows WSL:

```bash
set -a
. ./dev/gover.env
set +a
mvn spring-boot:run
```

These commands load the environment variables from `./dev/gover.env` into your current shell session and then start the Spring Boot application.

### 2.5 Start the frontend applications

The frontend consists of two separate React applications for staff and customers, which are located in the `./app`
module.
To start the frontend applications, you need to have Node.js and npm installed on your machine.

```bash
npm install --prefix ./app
```

For starting the staff frontend application, run:

```bash
npm run --prefix ./app start:staff
```

For starting the customer frontend application, run:

```bash
npm run --prefix ./app start:customer
```

After the frontends are started, you can access the staff application at [http://localhost:9595/staff](http://localhost:9595/staff) and the customer application at [http://localhost:9595/](http://localhost:9595/).

## 3 Accessing the services

### 3.1 Gover Database (PostgreSQL)

The Gover Database exposes its port `5432` to the host machine.
You can connect to the database with the following credentials:

| Parameter | Value       |
|-----------|-------------|
| Host      | `localhost` |
| Port      | `5432`      |
| Database  | `gover`     |
| Username  | `gover`     |
| Password  | `gover`     |

You can find these credentials in the `./dev/docker-compose.yml` file under `services.gover-database.environment`.

### 3.2 S3 Object Storage (MinIO)

MinIO exposes two ports to the host machine.

- Port `9000` is for accessing the MinIO API.
- Port `9001` is for accessing the MinIO Web Console.

You can access the MinIO web interface at [http://localhost:9001](http://localhost:9001) with the following credentials:

| Parameter | Value           |
|-----------|-----------------|
| Username  | `admin`         |
| Password  | `adminpassword` |

You can find these credentials in the `./dev/docker-compose.yml` file under `services.s3-object-storage.environment`.

The MinIO Setup Helper automatically creates a bucket named `gover` for storing the uploaded files and configures an access key and secret key for the application to access the MinIO instance.
You can use the following Access Key and Secret Key to access the MinIO API:

| Parameter  | Value              |
|------------|--------------------|
| Access Key | `super-access-key` |
| Secret Key | `super-secret-key` |

You can find these credentials in the `./dev/docker-compose.yml` file under `services.s3-object-storage-setup.entrypoint`.

### 3.3 SMTP (Mailpit)

The Mailpit service exposes its SMTP port `2025` to the host machine.
Port `9025` is exposed to the host machine for accessing the Mailpit web interface.
You can access the Mailpit web interface at [http://localhost:9025](http://localhost:9025) to view the captured emails sent by the application.
No authentication is required to access the Mailpit web interface or the SMTP port.

### 3.4 Keycloak Database (PostgreSQL)

The Keycloak Database exposes its port `5433` to the host machine.
You can connect to the database with the following credentials:

| Parameter | Value       |
|-----------|-------------|
| Host      | `localhost` |
| Port      | `5433`      |
| Database  | `keycloak`  |
| Username  | `keycloak`  |
| Password  | `keycloak`  |

You can find these credentials in the `./dev/docker-compose.yml` file under `services.keycloak-database.environment`.

### 3.5 Keycloak

The Keycloak service is proxied by the Caddy service and can be accessed at [http://localhost:9595/idp](http://localhost:9595/idp).
The Keycloak Setup Helper automatically configures the Keycloak instance with all necessary realms, clients, and users.

Keycloak is initialized with a **Bootstrap Admin** user.
This user is used by the Keycloak Setup Helper to configure the Keycloak instance.
After the successful setup, the **Bootstrap Admin** user is disabled and cannot be used to log in anymore.

For future configurations of the Keycloak instance, a deployment client is created.
This client can be used to configure Keycloak via the Keycloak Admin REST API, for example with the **Keycloak Admin CLI**.
You can find the credentials for the deployment client in the `./dev/docker-compose.yml` file under `services.keycloak-setup.environment`.

During the setup a new user with the username `superuser` and the e-mail `mail@example.com` is created.
You can use this user to log in to the Keycloak admin console and manage the Keycloak instance.

| Parameter | Value                           |
|-----------|---------------------------------|
| Username  | `superuser`                     |
| E-mail    | `mail@example.com`              |
| Password  | `My-Super-Secret-Password-No.1` |

You can find these credentials in the `./dev/docker-compose.yml` file under `services.keycloak-setup.environment`.

On your first login, you will be prompted to change the password for the `superuser` account.
Additionally, you need to set up an OTP for the account, as it is required by the security settings of the Keycloak instance.
Please follow the instructions in the Keycloak admin console to complete the setup of the `superuser` account.
Make sure to save the new password and the OTP secret in a secure place, as you will need them to log in to the Keycloak admin console in the future.

**Important distinction:**

- `superuser` is a Keycloak administration user.
- `superuser` is used for the Keycloak admin console only. It is not automatically a Gover administrator.
- Gover authenticates its own users against the Keycloak realm configured by `GOVER_KEYCLOAK_OIDC_REALM`.
- In the default development setup, `GOVER_KEYCLOAK_OIDC_REALM` is set to `staff`.
- Because of this, only users from the `staff` realm can log into the Gover staff app directly and be imported as Gover users.
- The Gover bootstrap super admin is therefore a Gover application user from the `staff` realm, not a special Keycloak admin-console account.
- The `customer` realm is used for customer identity provider integration and is not used for direct Gover staff login.
- A user is promoted to the Gover system role `Superadministrator:in` only when all of the following are true:
  - the user is imported into Gover by user synchronization or by logging into Gover
  - the user comes from the `staff` realm
  - the user's e-mail address matches `GOVER_BOOTSTRAP_ADMIN_MAIL`
  - no Gover super admin exists yet
- In the default development setup, these values are different:
  - the Keycloak admin user `superuser` has the e-mail `mail@example.com`
  - `GOVER_BOOTSTRAP_ADMIN_MAIL` in `./dev/gover.env` is set to `admin@example.com`

Because of this, the default Keycloak user `superuser` does not become the bootstrap Gover super admin.

The Keycloak Setup Helper creates the following realms:

- `staff`: This realm is for the staff users of the Gover service.
- `customer`: This realm is for the integration of customer identity providers such as the **BundID** or **Mein Unternehmenskonto**.

#### 3.5.1 Creating a Staff User

To create a new staff user, log into your Keycloak admin console with the `superuser` account and navigate to the `staff` realm.
Then, go to the "Users" section and click on "Add user".
Fill in the required fields such as username and email, and click "Save".
After saving the user, go to the "Credentials" tab and set a password for the user.
Make sure to disable the "Temporary" option, so the password does not expire after the first login.
The user is now available for logging into the staff frontend application.

If this staff user should become the first Gover super admin, set the user's e-mail address to the value of `GOVER_BOOTSTRAP_ADMIN_MAIL` before the user logs into Gover for the first time.
In the default development setup, that means using the e-mail address `admin@example.com`, or changing `GOVER_BOOTSTRAP_ADMIN_MAIL` in `./dev/gover.env` before the first import/login.

**Bootstrap Gover Admin User:**

When Keycloak users are synced into Gover, or when a user logs into Gover and is imported on demand, Gover checks whether that user should become the bootstrap super admin.
Gover does this by comparing the imported user's e-mail address with the environment variable `GOVER_BOOTSTRAP_ADMIN_MAIL` in `./dev/gover.env`.
If the e-mail address matches and no Gover super admin exists yet, that user receives the Gover system role `Superadministrator:in`, which grants full access to the Gover application.

### 3.6 Message Broker (RabbitMQ)

The RabbitMQ service exposes port `5672` to the host machine for AMQP connections.
The RabbitMQ management web interface is exposed on port `15672`.
You can access the RabbitMQ management web interface at [http://localhost:15672](http://localhost:15672) with the following credentials:

| Parameter | Value           |
|-----------|-----------------|
| Host      | `localhost`     |
| Port      | `15672`         |
| Username  | `admin`         |
| Password  | `adminpassword` |

You can find these credentials in the `./dev/docker-compose.yml` file under `services.message-broker.environment`.

## 4 Development

Changes to the app will be hot-reloaded automatically.
Changes to the mails module require a rebuild by running `npm run build:dev --prefix ./mails` again.
Changes to the backend require a restart of the Gover application.

## 5 View API Documentation

You can view the API documentation for the backend application at [http://localhost:9595/api/public/docs/swagger.html](http://localhost:9595/api/public/docs/swagger.html) after starting the backend application.
