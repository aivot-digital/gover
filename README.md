<p align="center">
  <a href="https://aivot.de" target="_blank">
    <img width="150" src="https://aivot.de/img/aivot-logo.svg" alt="Aivot logo">
  </a>
</p>

<h1 align="center">
  Prosuna
</h1>
<h3 align="center">
  The open source platform for fully digital, end-to-end application processes
</h3>

<p>
  Prosuna is purpose-built to help public authorities digitize administrative processes efficiently, transparently, and in full legal compliance.
  As an open source low-/no-code platform, it empowers governments to deliver modern digital services at scale — without needing deep technical expertise.
</p>

[![GitHub release](https://img.shields.io/github/v/release/aivot-digital/gover)](https://github.com/aivot-digital/gover/releases)
[![Build Status](https://img.shields.io/github/actions/workflow/status/aivot-digital/gover/build-and-push.yml)](https://github.com/aivot-digital/gover/actions)
[![Docker image on GHCR](https://img.shields.io/badge/image-prosuna-blue?logo=docker)](https://github.com/aivot-digital/gover/pkgs/container/gover)
[![Helm Chart](https://img.shields.io/badge/helm-chart-blue)](https://github.com/aivot-digital/gover-chart)
[![License: Sustainable Use License](https://img.shields.io/badge/license-Sustainable%20Use%20License-blue)](https://github.com/aivot-digital/gover/blob/main/LICENSE)

## What is Prosuna?
Prosuna is a specialized software platform designed to digitize and manage public sector application processes from start to finish.
Unlike general-purpose software for forms and automation, Prosuna is tailored exclusively to the workflows and requirements of public administration.

It enables administrative teams to:
* Easily create user-centric online applications/forms and complete workflows
* Digitally model and manage professional processes in a structured way
* Integrate existing systems and registers without friction
* Ensure traceability through reliable documentation and change tracking
* Stay fully compliant with all relevant legal requirements in Germany

By combining powerful functionality with an intuitive user experience, Prosuna allows administrations to implement digital services quickly and at scale — benefiting both staff and citizens.

For more information visit <https://prosuna.de>

## Who uses Prosuna?
Prosuna is primarily used by public administrations at the local level – cities, municipalities, and counties.
It supports authorities that aim to fully digitize their application processes end-to-end, making them more efficient and accessible for both citizens and staff.

Prosuna is especially valued by administrations that want to reduce reliance on overburdened IT departments by enabling subject-matter experts in individual departments to build and manage digital processes themselves.
It’s also chosen by those who care about digital sovereignty and seek to avoid vendor lock-in by using a solution that puts control back in the hands of the public sector.

Want to learn more? Feel free to contact us anytime at <https://aivot.de/kontakt>.

## Deployment Examples
The Docker Compose configurations in the `examples` directory illustrate two possible deployment shapes. They are examples only, not universal installation instructions.

For an on-premises installation, only the system administrators responsible for the target environment know its infrastructure and operational requirements.
They must review and adapt the networking, TLS, storage, backups, monitoring, availability, secret management, and security hardening before deploying Prosuna.

- [Local development and evaluation example](./examples/local/README.md): [Docker Compose configuration](./examples/local/compose.yml)
- [Single-server example](./examples/server/README.md): [Docker Compose configuration](./examples/server/compose.yml)

## Development Setup
Refer to the [development setup instructions](./development/README.md) for setting up Prosuna for development.

## Documentation
If you are looking for code documentation as well as end user documentation visit our [documentation overview](https://docs.prosuna.de) and select
the respective project.

## Contributing
Anyone can support us. There are many different ways to contribute to Prosuna. There is certainly one for you as well.

| Support opportunity               | Remark                                                                                                                                                                                                                                                                 |
|-----------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Spread the word                   | Share your thoughts on this project on social media. Feel free to link to our website or this GitHub repository.                                                                                                                                                       |
| Share your ideas or give feedback | Share your ideas with us or report a bug. With GitHub Issues and our templates, you can easily bring something up for discussion. Ideally you should read the [contributing guideline](https://github.com/aivot-digital/.github/blob/main/docs/CONTRIBUTING.md) first. |
| Develop                           | Develop together with us on the project. Contributions are managed via GitHub. Please read the [contributing guideline](https://github.com/aivot-digital/.github/blob/main/docs/CONTRIBUTING.md) first.                                                                |
| Write out a Bounty                | "Share your ideas" on steroids. If you have a business critical idea and want to see it implemented, you have the chance to set a bounty and accelerate a possible development.                                                                                        |

❤ Thank you for contributing! ❤

## Changelog
Please refer to the [changelog](./CHANGELOG.md) for details of what has changed.

## Roadmap
Future functionalities and improvements in prioritized order can be found in the project's [roadmap](https://aivot.de/roadmaps).

## License
This project is licensed under the terms of the [Sustainable Use License](./LICENSE.md).

## Services used
These great services build Aivot's core infrastructure for this project:

[<img loading="lazy" alt="GitHub" src="https://github.githubassets.com/images/modules/logos_page/GitHub-Logo.png" height="25">](https://github.com/)

GitHub allows us to host the Git repository and coordinate contributions.
