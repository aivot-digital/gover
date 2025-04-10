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

# Example Server Setup

The following setup instructions are primarily designed for server installations.
This setup requires a server with Docker and Docker Compose installed.
The server must be reachable via a given domain name from the internet.

## Prerequisites

- Ubuntu 20.04 LTS
- Docker

## Setup

### Docker-Compose File

Create a new file named `docker-compose.yml` with the content of the example [docker-compose file](./docker-compose.yml) in the root directory of the Gover repository.

After you have created the Docker Compose file, you can start the Traefik and Keycloak application by running the following command:

```bash
docker compose up -d traefik keycloak
```

### Setup Keycloak

After Keycloak has started, you can import the required realm templates by importing them into the Keycloak administration console. 
The administration console can be accessed at `https://<YOUR_DOMAIN>/idp/`.
You can log in with the credentials you have set in the `docker-compose.yml` file.

The realm templates can be found in the [realm-templates directory](./realm-templates) of the Gover repository.
To import the realm templates, click on the Realm dropdown in the left drawer and click on the `Create realm` button. 
Then click on the `Browse...` button and select the realm template file you want to import.
When the template has loaded click `Create` to import the realm.

#### Setup Master-Realm

The Master-Realm comes pre-configured with every keycloak installation and cannot be removed. Since we do not rely heavily on anything in the Master-Realm just go to the `Realm settings` section in the drawer on the left and select the `Themes` tab. Set the login, account and admin theme to `gover` and the email theme to `keycloak`.

#### Setup Staff-Realm

After importing the Staff-Realm go to the newly created realm.
First you need to set the E-Mail-Credentials in for the Staff-Realm to support sending E-Mail-Verifications.
Navigate to the `Realm Settings` section in the drawer on the left and select the `Email` tab.
Fill in the SMTP settings for your mail server and save the settings. Hint: Switch the `Authentication` off and back on to reveal the username and password.

After setting up the E-Mail-Credentials for the Staff-Realm, you can add your first user to the realm.
Navigate to the `Users` section in the drawer on the left and click on the `Add user` button.
Fill in the user details and click `Save`.
After saving the user, you can set the user's password by clicking on the `Credentials` tab and setting a new password.
Make sure to add the role `admin` via the `Role Mappings` tab to the user to be able to access the Gover application as an admin.

Then you need to generate a new secret for the `backend` client.
Select the `Clients` section in the drawer on the left and navigate to the `backend` client.
Go to the tab `Credentials` and regenerate the `Client Secret` via the button `Regenerate`.
Copy the new secret and insert it into the `docker-compose.yml` file for the `KEYCLOAK_BACKEND_CLIENT_SECRET` environment variable.

#### Setup Customer-Realm

After importing the Customer-Realm go to the newly created realm.
Select the `Identity Providers` section in the drawer on the left and update the `Service provider entity ID` for all identity providers via the settings to match your Gover instance domain.

Then you need to set the correct redirect URIs as well as web origins for the app client.
To set this up, select the `Clients` section in the drawer on the left. Choose the tab `Clients list`. In the list choose the client id `app`. Update the `Valid redirect URIs` as well as the `Web origins` to reflect the correct URL of your gover instance.

### Start Gover

You can then start the Gover application by running the following command:

```bash
docker compose up -d gover
```

After this process is complete, you can access the Gover application at `https://<YOUR_DOMAIN>/staff`.
You can log in with the user you have created in the Staff-Realm.

# Development Setup
Refer to the [development setup instructions](./dev/README.md) for setting up Gover for development.

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
