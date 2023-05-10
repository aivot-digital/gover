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
If you want to deploy Gover without docker, read more at the [native setup section](./README.md#Native-Setup).

Gover depends on a few other services to fully function:

* [PostgreSQL](https://www.postgresql.org/)

## Prerequisites

**Attention:** This projects requires [FontawesomePro](https://fontawesome.com/) Version 6 or higher.

## Download
Clone this repository and enter it via:

```bash
git clone https://github.com/aivot-digital/gover.git ./gover
cd ./gover
```

## Authenticate FontawesomePro

To pull the required FontawesomPro packages, edit the file `./app/.npmrc` and insert your FontawesomePro Key.

The file should look like this:

```
@fortawesome:registry=https://npm.fontawesome.com/
//npm.fontawesome.com/:_authToken=<YOUR_FONT_AWESOME_KEY>
```

More information about the FontawesomePro setup, can be found here: <https://fontawesome.com/docs/web/setup/packages>.

## DockerCompose Setup

**This is one of two options. For the native setup, refer to the [native setup guide](./README.md#Native-Setup) below.**

### Prerequisites

Configure the gover installation by editing the file `./config/application.properties`.
The `application.properties` contains all configs for the Gover application.

If you plan to use docker-compose, you do not need to change the `application.properties` at all.

The config `application.properties` should contain the following data.

```properties
spring.datasource.url=jdbc:postgresql://<POSTGRES_HOST>:<POSTGRES_PORT>/<POSTGRES_DATABASE>
spring.datasource.username=<POSTGRES_USERNAME>
spring.datasource.password=<POSTGRES_PASSWORD>

spring.mail.host=<SMTP_HOST>
spring.mail.port=<SMTP_PORT>
spring.mail.username=<SMTP_USERNAME>
spring.mail.password=<SMTP_PASSWORD>
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

gover.fromMail=<FROM_MAIL_ADDRESS>
gover.reportMail=<REPORT_MAIL_ADDRESS>
```

Replace all data in the angle brackets with your respective data.

### Running Gover

If you have docker-compose installed get started by running `docker-compose up`.
Gover is now available at <http://localhost:8080/admin>.

**Please note**, that the Gover application prints initial login data for an admin user to the console. 
Use these credentials for your first login.
If you have the smtp correctly configured, an email with the credentials is also sent to the report mail address.

```bash
... Created default admin with email "admin@gover.aivot.de" and password "dkmWySOPpQAr"
```

## Native Setup

**The native setup guide was made for unix platforms only.**

**This is one of two options. For the docker setup, refer to the [docker setup guide](./README.md#DockerCompose-Setup) above.**

### Prerequisites

* Working [PostgreSQL](https://www.postgresql.org/)
* Working [wkhtmltopdf](https://wkhtmltopdf.org/)
* [node.js 17](https://nodejs.org/en/)
* [openjdk17](https://openjdk.org/projects/jdk/17/)

Clone this repository with its submodules anywhere and change into the cloned repository:

```bash
git clone git@github.com:aivot-digital/gover.git ./gover
cd ./gover
``` 

### Adjusting backend config

Edit the file `./config/application.properties` and insert the credentials for PostgreSQL and your SMTP.

### Build

Run the following command: `./mvnw -DskipTests install`.
This will build a JAR file at `./target/Gover-2.1.8.jar`.

### Run Gover
After everything is set up, you can now run the Gover instance.
Simply run `java -jar ./target/Gover-2.1.8.jar` or `nohup java -jar ./target/Gover-2.1.8.jar`.

## Customization

You can customize some parts of the Gover application.
All customizations must be applied, before the Gover application is built.

### Customize Theming

To customize the theming, edit the `custom-themes.json` at `./app/src/custom-themes.json`.
This file contains a list of custom themes, which will be compiled into the Gover frontend application. A config for a new theme called `Candy` looks as follows.

```json
[
  {
    "name": "Candy",
    "primary": "#FF8DC7",
    "primaryDark": "#BF6B9C",
    "accent": "#FFDDD2"
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
