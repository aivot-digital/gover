# Prerequisites
* Java JDK 21
* Docker v27.3.1+ and Docker Compose v2.29.7+
* Apache Maven v3.9.9+
* Node.js v22.0.0+
* Git v2.46.2+

# Setting up a development environment
The following setup instructions are primarily designed for development installations under unix systems like Linux or macOS.
Running the development setup on Windows is possible but may require additional steps, especially for setting up the environment variables.
We recommend using the Windows Subsystem for Linux (WSL) for a more a better development experience.

1. Clone this repository with `git clone https://github.com/aivot-digital/gover.git ./gover`
2. Change into the `gover` directory with `cd gover`
3. Install the dependencies by running `mvn clean install -Dmaven.test.skip`
4. Install the dependencies and build the mail templates for the module `mails` by running `npm install --prefix ./mails && npm run build:dev --prefix ./mails`
5. Install the dependencies for the module `app` by running `npm install --prefix ./app`
6. Start the dependent services by running `docker compose -f ./dev/docker-compose.yml up -d`
7. Log into the Keycloak administration console at <http://localhost:8888/idp> with the credentials in the `docker-compose.yml` file
    * Default username is `admin` and default password is `admin`
    * Import the [staff realm template](../realm-templates/staff-realm.json) to create the staff realm
      * Visit the client `backend` in the staff realm and regenerate the secret in the credentials tab
      * Copy this secret into the [Gover environment file](./gover.env) for the variable `KEYCLOAK_BACKEND_CLIENT_SECRET`
      * Create a new user in the staff realm and assign the realm role `admin` under the `Role mapping` tab
    * If you want to test the service accounts like BundID or BayernID also import the [customer realm template](../realm-templates/customer-realm.json)
8. Adjust the variables in the [Gover environment file](./gover.env) to match your local setup if necessary
9. Load your environment file by running `export $(cat ./dev/gover.env | xargs)` 
10. Start the Gover application by running `mvn spring-boot:run -Dmaven.test.skip`
11. Start the staff and customer apps by running `npm run --prefix ./app start:staff` and `npm run --prefix ./app start:customer` in separate terminals respectively
12. Your local instance of Gover is now available at <http://localhost:8888/> and <http://localhost:8888/staff>

# Development
Changes to the app will be hot-reloaded automatically.
Changes to the mails module require a rebuild by running `npm run build:dev --prefix ./mails` again.
Changes to the backend require a restart of the Gover application.

# View API Documentation
Run
```bash
npx open-swagger-ui --open http://localhost:8080/api/public/openapi.yml
```