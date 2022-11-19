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
If you want to deploy MultiSpace without docker, read more at the [native setup section](./README.md#Native-Setup).

Gover depends on a few other services to fully function:

* [PostgreSQL](https://www.postgresql.org/)
* [MinIO](https://min.io/)


## Clone Dependencies
Clone the denpendend repositories:

```
git clone --branch v1.0.1 https://github.com/aivot-digital/gover-frontend.git ./gover-frontend
git clone --branch v1.0.0 https://github.com/aivot-digital/gover-backend.git ./gover-backend
```


## Docker Setup

### Prerequisites
For configuration, create a folder named `./config` in your current working directory and a file `./config/application.properties`.
The `application.properties` contains all configs for the Gover application.

Copy and paste the following content into the `application.properties`.

```
spring.datasource.url=jdbc:postgresql://db:5432/gover
spring.datasource.username=gover
spring.datasource.password=gover

minio.url=http://minio:9000
minio.access=gover
minio.secret=gover_secret_key
minio.bucket=gover

smtp.host=<SMTP_HOST>
smtp.port=<SMTP_PORT>
smtp.username=<SMTP_USERNAME>
smtp.password=<SMTP_PASSWORD>
smtp.useTls=true

gover.reportMail=<YOUR_ADMIN_MAIL>
```

If you want to use the mail feature, insert the credentials to your smtp server into the `application.properties`.

### Running Gover
If you have docker-compose installed, simply clone this repository with `git clone --recurse-submodules git@github.com:aivot-digital/gover.git` and copy the contents of a docker-compose file below into `docker-compose.yml` and get started by running `docker-compose up`.
Gover is now available at <http://localhost:8080>.

```yaml
# docker-compose.yml

version: '3.1'

services:

  db:
    image: postgres
    restart: always
    environment:
      POSTGRES_PASSWORD: gover
      POSTGRES_USER: gover
      POSTGRES_DB: gover
    ports:
      - "5432:5432"
  
  minio:
    image: minio/minio
    restart: always
    environment:
      MINIO_ACCESS_KEY: gover
      MINIO_SECRET_KEY: gover_secret_key
    ports:
      - "9000:9000"
      - "9001:9001"
    command: server /data --console-address ":9001"
  
  gover:
    build:
      context: .
      dockerfile: Dockerfile
    depends_on:
      - db
      - minio
    restart: always
    volumes:
      - ./config:/app/config:ro
    ports:
      - "8080:80"
```

Please note, that you might need to adjust the path to the `config` folder for the Gover service in the `docker-compose.yml`.

## Native Setup
**The native setup guide was made for unix platforms only.**

### Preqrequisites

* Working [PostgreSQL](https://www.postgresql.org/)
* Working [MinIO](https://min.io/)
* Working [nginx](https://www.nginx.com/)
* Working [wkhtmltopdf](https://wkhtmltopdf.org/)
* [node.js 17](https://nodejs.org/en/)
* [openjdk17](https://openjdk.org/projects/jdk/17/)
* [make 3.81](https://www.gnu.org/software/make/)

Clone this repository with its submodules anywhere `git clone --recurse-submodules git@github.com:aivot-digital/gover.git` and change into the cloned repository.

### Adjusting backend config
Edit the file `./gover-backend/src/main/resources/application.properties` and insert the credentials for PostgreSQL, MinIO and your SMTP.

### Build
Run the following command: `make all`.
This will create a folder `./out` in the root of your project.

### Configure nginx
Configure your nginx as follows and reload the service with `service nginx reload`.

```nginx
upstream backend {
  server localhost:8080;
}

server {
  listen 80      default_server;
  listen [::]:80 default_server;

  location /admin {
    alias     /<YOUR_CURRENT_PATH>/out/html/admin;
    try_files $uri $uri/ /index.html;
  }

  location ^~/api/ {
    proxy_pass       http://backend/;
    proxy_set_header Host      $host;
    proxy_set_header X-Real-IP $remote_addr;
  }

  location / {
    root      /<YOUR_CURRENT_PATH>/out/html/app;
    try_files $uri $uri/ /index.html;
  }
}
```

You may setup your nginx to https. Please refere to [this guide](http://nginx.org/en/docs/http/configuring_https_servers.html).

### Run Gover
After everything is set up, you can now run the Gover instance.
Simply run `java -jar ./out/gover.jar` or `nohup java -jar ./out/gover.jar`.


### Customization

You can customize some parts of the gover application.
All customizations must be applied, before the gover application is built.

### Customize Theming

To customize the theming, edit the `custom-themes.json` at `./gover-frontend/src/custom-themes.json`.
This file contains a list of custom themes, which will be compiled into the Gover frontend application. A config for a new theme called `Candy` looks as follows.

```json
[
  {
    "name": "Candy",
    "primary": "#FF8DC7",
    "primaryDark": "#BF6B9C";
    "accent": "#FFDDD2;
  }
]
```

### Customize E-Mail-Templates

The E-Mail-Templates used for sending Mails to the customer and staff, can be edited.
All files are located at `./gover-backend/src/main/resources/templates/mail/`.
Please note, that the templates must follow the rules for the [Thymeleaf](https://www.thymeleaf.org/) templating engine.



# Documentation
Code documentation is stored in the project's [GitHub wiki](../../wiki) so that it is as close to the code as possible.

If you are looking for end user documentation visit our [documentation overview](https://aivot.de/docs) and select
the respective project.




# Contributing
Anyone can support us. There are many ways to contribute to Gover. There is certainly one for you as well.

| Support opportunity               | Remark                                                                                                                                                                                                                           |
| --------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Spread the word                   | Share your thoughts on this project on social media. Feel free to link to our website or this GitHub repository.                                                                                                                 |
| Share your ideas or give feedback | Share your ideas with us or report a bug. With GitHub Issues and our templates, you can easily bring something up for discussion. Ideally you should read the [contributing guideline](./CONTRIBUTING.md) first.                 |
| Develop                           | Develop together with us on the project. Contributions are managed via GitHub. Please read the [contributing guideline](./CONTRIBUTING.md) first.                                                                                |
| Write out a Bounty                | "Share your ideas" on steroids. If you have a business critical idea and want to see it implemented, you have the chance to set a bounty and accelerate a possible development.                                                  |
| Support us financially            | Donate via [GitHub Sponsors](https://github.com/sponsors/aivot-digital) or [open collective](https://opencollective.com/aivot-digital). All funds are managed transparently and go directly into the development of the project. |

❤ Thank you for contributing! ❤




# Changelog
Please refer to the [changelog](./CHANGELOG.md) for details of what has changed.




# Roadmap
Future functionalities and improvements in prioritized order can be found in the project's [roadmap](https://aivot.de/roadmaps).




# License
This project is licensed under the terms of the [Business Source License](./LICENSE.md).




# Sponsoring services
These great services sponsor Aivot's core infrastructure:

[<img loading="lazy" alt="GitHub" src="https://github.githubassets.com/images/modules/logos_page/GitHub-Logo.png" height="25">](https://github.com/)

GitHub allows us to host the Git repository and coordinate contributions.