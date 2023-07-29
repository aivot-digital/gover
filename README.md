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
  Our mission is to build the digital bridge between administration and citizens. There are countries with an extreme need to catch up in the area of digitalization. They are too rigid and slow to digitize on their own. We want to change that and offer a way for administrations to catapult themselves to the spearhead of digitalization, especially at the touchpoint with citizens.
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




# Who uses Gover?
Currently top secret. If you want to get an introduction into using Gover contact us at <https://aivot.de/kontakt>.




# Setup
Gover was developed as a cloud native application and works best with Docker.

Gover depends on a few other services to fully function:

* [PostgreSQL](https://www.postgresql.org/)
* [ClamAV](https://www.clamav.net/)
* [InfluxDB](https://www.influxdata.com/)

## Development Setup
To develop with the Gover application, follow these steps:

Clone this repository and enter it via:

```bash
git clone https://github.com/aivot-digital/gover.git ./gover
cd ./gover
```

Start the development environment with:

```bash
docker compose -f docker-compose.dev.yml up
```

Start the customer frontend with:

```bash
cd ./app
npm run start:customer
# or for windows
npm run start:customer:win
```

The customer frontend is now avaiable at <http://localhost:3000>.

Start the admin frontend with:

```bash
cd ./app
npm run start:staff
# or for windows
npm run start:staff:win
```

The admin frontend is now avaiable at <http://localhost:3001>.

Make sure, that the environment variable for `JAVA_HOME` is set to the correct JDK path and start the backend with:

```bash
mvnw spring-boot:run
```

The Gover backend is now available at <http://localhost:8080>.

## Docker Setup

To run the prebuilt docker container, follow these steps:

### Prerequisites

Make sure docker and the docker compose plugin is installed.

### Configuration

Create a docker-compose file `docker-compose.yml` with the following content:

```yaml
# docker-compose.yml
version: '3.1'

services:

  database:
    image: postgres:14
    restart: always
    environment:
      POSTGRES_USER: gover
      POSTGRES_PASSWORD: gover
      POSTGRES_DB: gover
      PGDATA: /var/lib/postgresql/data/pgdata
    volumes:
      - ./pg:/var/lib/postgresql/data

  clamav:
    image: clamav/clamav:1.1
    restart: always

  influx:
    image: influxdb:2.7
    restart: 'always'
    environment:
      DOCKER_INFLUXDB_INIT_MODE: 'setup'
      DOCKER_INFLUXDB_INIT_USERNAME: 'gover'
      DOCKER_INFLUXDB_INIT_PASSWORD: 'gover-password'
      DOCKER_INFLUXDB_INIT_ORG: 'gover'
      DOCKER_INFLUXDB_INIT_BUCKET: 'gover'
      DOCKER_INFLUXDB_INIT_ADMIN_TOKEN: 'gover'

  gover:
    image: ghcr.io/aivot-digital/gover:3.0.0
    depends_on:
      - database
      - clamav
      - influx
    restart: always
    volumes:
      - ./gv_data:/app/data
    environment:
      GOVER_DB_HOST: database
      GOVER_DB_PORT: 5432
      GOVER_DB_DATABASE: gover
      GOVER_DB_USERNAME: gover
      GOVER_DB_PASSWORD: gover

      GOVER_CLAM_HOST: clamav
      GOVER_CLAM_PORT: 3310

      GOVER_INFLUX_HOST: influx
      GOVER_INFLUX_PORT: 8086
      GOVER_INFLUX_ORG: gover
      GOVER_INFLUX_BUCKET: gover
      GOVER_INFLUX_TOKEN: gover

      GOVER_SMTP_HOST: localhost
      GOVER_SMTP_PORT: 25
      GOVER_SMTP_USERNAME: gover
      GOVER_SMTP_PASSWORD: gover

      GOVER_LOG_LEVEL: WARN
      GOVER_JWT_SECRET: geheimnis-fuer-lokales-testen
      GOVER_FROM_MAIL: '"Lokal" <noreply@localhost>'
      GOVER_REPORT_MAIL: noreply@localhost
      GOVER_SENTRY_SERVER: ""
      GOVER_SENTRY_WEB_APP: ""
      GOVER_ENVIRONMENT: Lokal
      GOVER_HOSTNAME: "http://localhost:8080"
      GOVER_FILE_EXTENSIONS: pdf, png, jpg, jpeg, doc, docx, xls, xlsx, ppt, pptx, odt, fodt, ods, fods, odp, fodp, odg, fodg, odf
      GOVER_CONTENT_TYPES: application/pdf, image/png, image/jpeg, application/msword, application/vnd.openxmlformats-officedocument.wordprocessingml.document, application/msexcel, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, application/mspowerpoint, application/vnd.openxmlformats-officedocument.presentationml.presentation, application/vnd.oasis.opendocument.text, application/vnd.oasis.opendocument.spreadsheet, application/vnd.oasis.opendocument.presentation, application/vnd.oasis.opendocument.graphics, application/vnd.oasis.opendocument.formula
    ports:
      - "8080:8080/tcp"
```

### Running Gover

Start the base containers with
```bash
docker compose up -d database clamav influx
```

Wait for the containers to come up and initialize.
Start the gover with `docker compose up -d gover` and monitor the startup with `docker compose logs -f gover`.
Gover is now available at <http://localhost:8080> and <http://localhost:8080/admin>.

**Please note**, that the Gover application prints initial login data for an admin user to the console. 
Use these credentials for your first login.
If you have the smtp correctly configured, an email with the credentials is also sent to the report mail address.

```bash
... Created default admin with email "admin@gover.aivot.de" and password "dkmWySOPpQAr"
```


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
