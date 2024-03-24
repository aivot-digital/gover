<p align="center">
  <a href="https://aivot.de" target="_blank">
    <img width="150" src="https://aivot.de/img/aivot-logo.svg" alt="Aivot logo">
  </a>
</p>

<h1 align="center">
  Gover
</h1>
<h3 align="center">
  The foundation for modern digital e-government services
</h3>

<p>
  Our mission is to build the digital bridge between administration and citizens. 
  There are countries with an extreme need to catch up in the area of digitalization. 
  They are too rigid and slow to digitize on their own. 
  We want to change that and offer a way for administrations to catapult themselves to the spearhead of digitalization, especially at the touchpoint with citizens.
</p>

<!-- Badges go here -->

# What is Gover?
Gover is an efficient low-code e-government platform for creating and managing user-centric online applications.  
We enable administrations to:
- build and manage visually stunning online applications fast
- document changes reliably and automatically
- ensure quality
- comply with all legislation (at least in Germany 😅)

All this while putting user experience (for citizens and administration staff) first.

For more information visit us at <https://aivot.de/gover>

# Setup

The following setup instructions, especially those for Docker, are primarily designed for local and/or small server installations.

## Docker Installation

Installing Gover on Ubuntu with Docker is a straightforward process ensured by careful planning and configuration. 
The procedure involves creating a Docker Compose file, configuring the necessary services, and finally starting the system in an orderly manner.

### Preparing Docker Compose

Alter your hosts file to include the following line:

```bash
127.0.0.1 gover.local
```

Create a new file named docker-compose.yml.

Copy the following configuration into the file and adjust the appropriate variables within the angle brackets.

```yaml
version: "3"

services:
   # All Gover data is stored in this database
   gover_database:
      image: postgres:15.6
      restart: always
      environment:
         POSTGRES_PASSWORD: GOVER_DB_PASSWORD # Password for the Gover database. Change this in production!
         POSTGRES_USER: GOVER_DB_USERNAME # Username for the Gover database. Change this in production!
         POSTGRES_DB: gover
         PGDATA: /var/lib/postgresql/data/pgdata
      volumes:
         - gover_database_data:/var/lib/postgresql/data
      networks:
         - gover

   # All Keycloak data is stored in this database
   keycloak_database:
      image: postgres:15.6
      restart: always
      environment:
         POSTGRES_PASSWORD: KEYCLOAK_DB_PASSWORD # Password for the Keycloak database. Change this in production!
         POSTGRES_USER: KEYCLOAK_DB_USERNAME # Username for the Keycloak database. Change this in production!
         POSTGRES_DB: keycloak
         PGDATA: /var/lib/postgresql/data/pgdata
      volumes:
         - keycloak_database_data:/var/lib/postgresql/data
      networks:
         - keycloak

   # Antivirus
   clamav:
      image: clamav/clamav-debian:1.3.0
      restart: always
      networks:
         - gover

   # In-memory storage for internal caching
   redis:
      image: redis:7.2.4
      restart: always
      networks:
         - gover

   # Keycloak setup
   keycloak:
      image: ghcr.io/aivot-digital/keycloak-egov-plugins:24.0.1.1
      restart: always
      depends_on:
         - keycloak_database
      command: start
      environment:
         KC_PROXY_HEADERS: xforwarded
         # KC_HOSTNAME: gover.local # Replace gover.local to domain under which Gover is available in production
         # KC_HOSTNAME_PATH: /idp # Activate this in production
         KC_HTTP_RELATIVE_PATH: /idp
         KC_HOSTNAME_STRICT: false # Disable this in production
         KC_HTTP_ENABLED: true # Disable this in production
         KC_HOSTNAME_DEBUG: true

         KC_HEALTH_ENABLED: true

         KEYCLOAK_ADMIN: admin # Username for the Keycloak admin. Change this in production!
         KEYCLOAK_ADMIN_PASSWORD: admin # Password for the Keycloak admin. Change this in production!

         KC_DB: postgres
         KC_DB_URL: jdbc:postgresql://keycloak_database:5432/keycloak
         KC_DB_USERNAME: KEYCLOAK_DB_USERNAME # Username for the Keycloak database (see above). Change this in production!
         KC_DB_PASSWORD: KEYCLOAK_DB_PASSWORD # Password for the Keycloak database (see above). Change this in production!
         KC_FEATURES: transient-users,update-email

         KC_SPI_EGOV_BUNDID_ENABLED: true
         KC_SPI_EGOV_BUNDID_SP_NAME: Online-Antrags-Management
         KC_SPI_EGOV_BUNDID_SP_DESCRIPTION: Online-Antrags-Management
         KC_SPI_EGOV_BUNDID_ORG_NAME: <ORG_NAME> # Name of your organization. Maximum 50 characters.
         KC_SPI_EGOV_BUNDID_ORG_DESCRIPTION: <ORG_DESCRIPTION> # Description of your organization. Maximum 50 characters.
         KC_SPI_EGOV_BUNDID_ORG_URL: <ORG_WEBSITE> # Website of your organization.
         KC_SPI_EGOV_BUNDID_TECHNICAL_CONTACT_NAME: <TECHNICAL_CONTACT_NAME> # Name of the contact for technical questions in your organization. Maximum 50 characters.
         KC_SPI_EGOV_BUNDID_TECHNICAL_CONTACT_EMAIL: <TECHNICAL_CONTACT_EMAIL> # E-Mail of the contact for technical questions in your organization.
         KC_SPI_EGOV_BUNDID_SUPPORT_CONTACT_NAME: <SUPPORT_CONTACT_NAME> # Name of the contact for general questions in your organization. Maximum 50 characters.
         KC_SPI_EGOV_BUNDID_SUPPORT_CONTACT_EMAIL: <SUPPORT_CONTACT_EMAIL> # E-Mail of the contact for general questions in your organization.

         KC_SPI_EGOV_BAYERNID_ENABLED: true
         KC_SPI_EGOV_BAYERNID_SP_NAME: Online-Antrags-Management
         KC_SPI_EGOV_BAYERNID_SP_DESCRIPTION: Online-Antrags-Management
         KC_SPI_EGOV_BAYERNID_ORG_NAME: <ORG_NAME> # Name of your organization. Maximum 50 characters.
         KC_SPI_EGOV_BAYERNID_ORG_DESCRIPTION: <ORG_DESCRIPTION> # Description of your organization. Maximum 50 characters.
         KC_SPI_EGOV_BAYERNID_ORG_URL: <ORG_WEBSITE> # Website of your organization.
         KC_SPI_EGOV_BAYERNID_TECHNICAL_CONTACT_NAME: <TECHNICAL_CONTACT_NAME> # Name of the contact for technical questions in your organization. Maximum 50 characters.
         KC_SPI_EGOV_BAYERNID_TECHNICAL_CONTACT_EMAIL: <TECHNICAL_CONTACT_EMAIL> # E-Mail of the contact for technical questions in your organization.
         KC_SPI_EGOV_BAYERNID_SUPPORT_CONTACT_NAME: <SUPPORT_CONTACT_NAME> # Name of the contact for general questions in your organization. Maximum 50 characters.
         KC_SPI_EGOV_BAYERNID_SUPPORT_CONTACT_EMAIL: <SUPPORT_CONTACT_EMAIL> # E-Mail of the contact for general questions in your organization.
         KC_SPI_EGOV_BAYERNID_DISPLAY_NAME: <ORG_NAME> # Name of your organization. Is displayed when the BayernID website is accessed. Maximum 50 characters.
         KC_SPI_EGOV_BAYERNID_DISPLAY_DESCRIPTION: <ORG_DESCRIPTION> # Description of your organization. Is displayed when the BayernID website is accessed. Maximum 50 characters.

         KC_SPI_EGOV_DATAPORT_ENABLED: true
      networks:
         - keycloak
         - common
      labels:
         - "traefik.http.routers.keycloak.rule=Host(`gover.local`) && PathPrefix(`/idp`)" # Replace gover.local to domain under which Gover is available (see above) in production
         - "traefik.http.services.keycloak.loadbalancer.server.port=8080"
         # - "traefik.http.routers.keycloak.tls=true" # Activate this in production
         # - "traefik.http.routers.keycloak.tls.certresolver=ssl_resolver" # Activate this in production

   # Gover application server
   gover:
      image: ghcr.io/aivot-digital/gover:3.1.4-alpha-51
      depends_on:
         - gover_database
         - clamav
         - redis
         - keycloak
         - gover_customer_app
         - gover_staff_app
      restart: always
      environment:
         GOVER_DB_HOST: gover_database
         GOVER_DB_PORT: 5432
         GOVER_DB_DATABASE: gover
         GOVER_DB_USERNAME: GOVER_DB_USERNAME # Username for the Gover database. Change this in production!
         GOVER_DB_PASSWORD: GOVER_DB_PASSWORD # Password for the Gover database. Change this in production!

         GOVER_CLAM_HOST: clamav
         GOVER_CLAM_PORT: 3310

         GOVER_SMTP_HOST: "" # Host name or IP address of the mail server
         GOVER_SMTP_PORT: "" # Port of the mail server
         GOVER_SMTP_USERNAME: <SMTP_USERNAME> # Username of the mail server
         GOVER_SMTP_PASSWORD: <SMTP_PASSWORD> # Password of the mail server

         GOVER_LOG_LEVEL: "ERROR"

         GOVER_FROM_MAIL: '"<ENV_NAME>" <noreply@<HOSTNAME>>' # Name and mail address for outgoing mails
         GOVER_REPORT_MAIL: <FALLBACK_E_MAIL> # Fallback mail address to which error reports should be sent
         # GOVER_SENTRY_SERVER: <SERVER_SENTRY_DSN> # Sentry DSN (Data Source Name) for the Gover backend server. Remove to deactivate Sentry.
         # GOVER_SENTRY_WEB_APP: <WEB_APP_SENTRY_DSN> # Sentry DSN (Data Source Name) for the Gover frontend application. Remove to deactivate Sentry.
         GOVER_ENVIRONMENT: <ENV_NAME> # Unique name for the Gover environment for identification purposes
         GOVER_HOSTNAME: https://gover.local # Domain under which Gover is available
         GOVER_DEVELOPMENT_MODE: false
         GOVER_FILE_EXTENSIONS: pdf, png, jpg, jpeg, doc, docx, xls, xlsx, ppt, pptx, odt, fodt, ods, fods, odp, fodp, odg, fodg, odf
         GOVER_CONTENT_TYPES: application/pdf, image/png, image/jpeg, application/msword, application/vnd.openxmlformats-officedocument.wordprocessingml.document, application/msexcel, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, application/mspowerpoint, application/vnd.openxmlformats-officedocument.presentationml.presentation, application/vnd.oasis.opendocument.text, application/vnd.oasis.opendocument.spreadsheet, application/vnd.oasis.opendocument.presentation, application/vnd.oasis.opendocument.graphics, application/vnd.oasis.opendocument.formula

         GOVER_KEYCLOAK_OIDC_FRONTEND_CLIENT_ID: app # The name of the frontend client in Keycloak. Cannot be changed (yet).
         GOVER_KEYCLOAK_OIDC_BACKEND_CLIENT_ID: backend # The name of the backend client in Keycloak. Cannot be changed (yet).
         GOVER_KEYCLOAK_OIDC_BACKEND_CLIENT_SECRET: 1dhJiNxlO5iDrV1lSCeg2lq1Iqvcn6b9 # Backend client secret in Keycloak
         GOVER_KEYCLOAK_OIDC_HOSTNAME: http://gover.local/idp # Replace with protocol and host under which Keycloak is available in production
         GOVER_KEYCLOAK_OIDC_REALM: staff # The name of the realm in Keycloak. Cannot be changed (yet).

         GOVER_KEYCLOAK_ID_CLIENT: app # The name of the service account client in Keycloak. Cannot be changed (yet).
         GOVER_KEYCLOAK_ID_HOSTNAME: http://gover.local/idp # Replace with protocol and host under which Keycloak is available in production
         GOVER_KEYCLOAK_ID_REALM: customer # The name of the realm in Keycloak. Cannot be changed (yet).

         GOVER_REDIS_HOST: redis
         GOVER_REDIS_PORT: 6379
         GOVER_REDIS_DATABASE: 0
      volumes:
         - gover_data:/app/data
      networks:
         - gover
         - common
      links:
         - 'traefik:gover.local'
      labels:
         - "traefik.http.routers.gover_api.rule=Host(`gover.local`) && PathPrefix(`/api`)" # Replace gover.local with domain under which Gover is available in prodiction
         - "traefik.http.services.gover_api.loadbalancer.server.port=8080"
         # - "traefik.http.routers.gover_api.tls=true" # Activate this in production
         # - "traefik.http.routers.gover_api.tls.certresolver=ssl_resolver" # Activate this in production

   # Gover web application for employees
   gover_staff_app:
      image: ghcr.io/aivot-digital/gover-staff:3.1.4-alpha-51
      restart: always
      networks:
         - common
      labels:
         - "traefik.http.routers.gover_staff_app.rule=Host(`gover.local`) && PathPrefix(`/staff`)" # Replace gover.local with domain under which Gover is available
         - "traefik.http.services.gover_staff_app.loadbalancer.server.port=80"
         # - "traefik.http.routers.gover_staff_app.tls=true" # Activate this in production
         # - "traefik.http.routers.gover_staff_app.tls.certresolver=ssl_resolver" # Activate this in production

   # Gover web application for citizens
   gover_customer_app:
      image: ghcr.io/aivot-digital/gover-customer:3.1.4-alpha-51
      restart: always
      networks:
         - common
      labels:
         - "traefik.http.routers.gover_customer_app.rule=Host(`gover.local`)" # Replace gover.local with domain under which Gover is available
         - "traefik.http.services.gover_customer_app.loadbalancer.server.port=80"
         # - "traefik.http.routers.gover_customer_app.tls=true" # Activate this in production
         # - "traefik.http.routers.gover_customer_app.tls.certresolver=ssl_resolver" # Activate this in production

   # Reverse Proxy
   traefik:
      image: traefik:2.11.0
      restart: always
      command:
         - --providers.docker
         - --api.dashboard=false
         # - --entrypoints.websecure.address=:443 # Activate this in production
         # - --entrypoints.web.http.redirections.entryPoint.to=websecure # Activate this in production
         # - --entrypoints.web.http.redirections.entryPoint.scheme=https # Activate this in production
         # - --entrypoints.web.http.redirections.entrypoint.permanent=true # Activate this in production
         # - --certificatesresolvers.ssl_resolver.acme.email=<TECHNICAL_CONTACT_ADDRESS> # E-Mail-Address of the techncial contact person, activate this in production
         # - --certificatesresolvers.ssl_resolver.acme.storage=/var/traefik/acme.json # Activate this in production
         # - --certificatesresolvers.ssl_resolver.acme.httpchallenge.entrypoint=web # Activate this in production
      volumes:
         - /var/run/docker.sock:/var/run/docker.sock
         - traefik_data:/var/traefik
      networks:
         - common
      ports:
         - 80:80 # Disable this in production
         - 8080:8080 # Disable this in production
         # - 80:80 # Activate this in production
         # - 443:443 # Activate this in production
volumes:
   gover_database_data:
   keycloak_database_data:
   gover_data:
   traefik_data:

networks:
   keycloak:
   gover:
   common:
```

### Starting the System
To start the system, navigate to the directory containing the docker-compose.yml file and run the following command:

```bash
docker-compose up -d traefik keycloak
```

After the system has started, you can set up the keycloak staff realm as described in the official documentation under <http://gover.local/idp>.
You need to add two clients to the staff realm (app and backend) and configure the backend client with authentication and authorization.
You also need to set the realm frontend url in the realms settings to `http://gover.local/idp`.
Copy the secret of the backend client to the docker compose file and start the gover service with the following command:

```bash
docker-compose up -d gover
```

## Native Installation

This guide assumes you are setting up the Gover application, a React single-page application (SPA) for both staff and customers, and a custom Keycloak instance with specific plugins for a non-Dockerized environment.

### Prerequisites
* Java Development Kit (JDK) 21 or newer for the Spring Boot server
* Node.js and npm for the React SPA
* PostgreSQL for the databases up and running
* A suitable web server such as Apache or Nginx for serving the SPA
* Access to the Keycloak custom plugins from Aivot Digital

### Setting up the Gover Spring Boot Application
1. Ensure JDK 21 or newer is installed on your server. 
2. Clone the Gover application repository. 
3. Configure application properties according to your environment, database, and security requirements. 
4. Build the application using Maven: `mvn clean install`. 
5. Run the application using `java -jar target/gover-4.0.0.jar`.
6. Proxy the application using a web server such as Apache or Nginx under the path `/api`.

### Setting up the React Single Page Applications (SPAs)

#### Gover Staff App
1. Clone the repository of the Gover and navigate to the `app` directory.
2. Install dependencies using npm install.
3. Build the SPA using `REACT_APP_BUILD_TARGET=staff PORT=3001 PUBLIC_URL=/staff npm run build`.
4. Serve the built files using a web server such as Apache or Nginx unter the path `/staff`.

#### Gover Customer App
Follow the same steps as for the Gover Staff App, ensuring you use the specific repository or download for the customer application.

### Setting up Keycloak
1. Download and install Keycloak from the official website.
2. Deploy the custom Keycloak plugins from Aivot:
    1. Clone or download the plugins from here: <https://github.com/aivot-digital/keycloak-egov-plugins>.
    2. Build the plugins and copy the JAR files to the Keycloak deployment directory.
3. Proxy the application using a web server such as Apache or Nginx under the path `/idp`.

### Final Steps
After all services are set up and running, you can access the Gover application at the specified domain. 
The staff application is available under `/staff`, and the customer application is available at the root path.
Make sure to follow the setup guide in the official documentation for further configuration and customization.

# Who uses Gover?
Currently top secret. If you want to get an introduction into using Gover contact us at <https://aivot.de/kontakt>.

# Documentation
If you are looking for code documentation as well as end user documentation visit our [documentation overview](https://aivot.de/docs) and select
the respective project.

# Contributing
Anyone can support us. There are many different ways to contribute to Gover. There is certainly one for you as well.

| Support opportunity               | Remark                                                                                                                                                                                                                                                                 |
|-----------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Spread the word                   | Share your thoughts on this project on social media. Feel free to link to our website or this GitHub repository.                                                                                                                                                       |
| Share your ideas or give feedback | Share your ideas with us or report a bug. With GitHub Issues and our templates, you can easily bring something up for discussion. Ideally you should read the [contributing guideline](https://github.com/aivot-digital/.github/blob/main/docs/CONTRIBUTING.md) first. |
| Develop                           | Develop together with us on the project. Contributions are managed via GitHub. Please read the [contributing guideline](https://github.com/aivot-digital/.github/blob/main/docs/CONTRIBUTING.md) first.                                                                |
| Write out a Bounty                | "Share your ideas" on steroids. If you have a business critical idea and want to see it implemented, you have the chance to set a bounty and accelerate a possible development.                                                                                        |

❤ Thank you for contributing! ❤

# Changelog
Please refer to the [changelog](./CHANGELOG.md) for details of what has changed.

# Roadmap
Future functionalities and improvements in prioritized order can be found in the project's [roadmap](https://aivot.de/roadmaps).

# License
This project is licensed under the terms of the [Sustainable Use License](./LICENSE.md).

# Services used
These great services build Aivot's core infrastructure for this project:

[<img loading="lazy" alt="GitHub" src="https://github.githubassets.com/images/modules/logos_page/GitHub-Logo.png" height="25">](https://github.com/)

GitHub allows us to host the Git repository and coordinate contributions.
