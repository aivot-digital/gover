# Changelog

## [4.2.3](https://github.com/aivot-digital/gover/compare/v4.2.2...v4.2.3) (2024-07-28)

### Features

* **App:** Add submission id to destination data
* **App:** Add date, place and signature option to pdf print

### Bug Fixes

* **App:** Fix missing error message when uploading assets
* **App:** Fix broken links when referencing assets with special characters in their name
* **App:** Fix broken error page when the clamav service is not available
* **App:** Removed payment details from public form endpoints
* **App:** Removed unnecessary console log when navigating between segments
* **App:** Fix naming of children of duplicated store elements
* **App:** Fix length limits for title and slug when creating forms
* **App:** Fix broken imports when theme is not present in target system
* **App:** Fix title changing in form editor

## [4.2.2](https://github.com/aivot-digital/gover/compare/v4.2.1...v4.2.2) (2024-06-17)

### Features

* **Server:** Add flag for smtp tls requirement
* **Server:** Updated README.md

### Bug Fixes

* **Project:** Fix broken authorization settings in staff realm export
* **Project:** Updated ClamAV version in docker-compose.yml
* **Project:** Fix ePayBL certificate name in docker-compose.yml
* **Project:** Fix Keycloak terminating proxy ssl settings in docker-compose.yml
* **Project:** Fix Traefik setup in docker-compose.yml
* **Project:** Updated Quarkus payload size for Keycloak to support MUK payloads
* **Project:** Removed MinIO setup from docker-compose.yml

## [4.2.1](https://github.com/aivot-digital/gover/compare/v4.2.0...v4.2.1) (2024-06-15)

### Features

* **Server:** Add option for base64 certificate for payment provider ePayBL

## [4.2.0](https://github.com/aivot-digital/gover/compare/v4.1.1...v4.2.0) (2024-06-15)

### Features

* **Server:** giropay is integrated
* **Server:** S3 storage compatibility
* **App:** Autocomplete attributes for browsers can be selected in elements (e.g. the browser can automatically fill in the first name)
* **App:** Improvement of the accessibility of Gover forms and support for compliance with accessibility, e.g. through contrast checkers in the themes, references to alt attributes, etc.

## [4.1.1](https://github.com/aivot-digital/gover/compare/v4.1.0...v4.1.1) (2024-05-15)

### Bug Fixes

* **App:** Removed unnecessary feature toggles
* **App:** Fix logo dimensions

## [4.1.0](https://github.com/aivot-digital/gover/compare/v4.0.0...v4.1.0) (2024-05-13)

### Features

* **App:** Add support payment provider ePayBL (XBezahldienste)
* **App:** Add support payment provider pmPayment (XBezahldienste)
* **App:** Extended pdf templating
* **App:** Add support for form printing as pdf for offline submissions
* **App:** Add generic url to load the latest version of a form

### Bug Fixes

* **App:** Added several bug fixes

## [4.0.0](https://github.com/aivot-digital/gover/compare/v3.1.0...v4.0.0) (2024-03-21)

### Features

* **App:** Add Keycloak as the major identity provider
* **App:** Add support for BundID
* **App:** Add support for BayernID
* **App:** Add support for MUK
* **App:** Add support for Servicekonto Schleswig-Holstein
* **App:** Improved asset management
* **App:** Major UI/UX improvements
* **App:** Add dedicated view for submission processing
* **App:** Improved mail reporting
* **Server:** Removed support for API keys

## [3.1.0](https://github.com/aivot-digital/gover/compare/v3.0.1...v3.1.0) (2023-09-22)

### Features

* **App:** Add api key support for the backend endpoints
* **App:** Add versioning to presets


## [3.0.1](https://github.com/aivot-digital/gover/compare/v3.0.0...v3.0.1) (2023-08-02)

### Bug Fixes

* **App:** Fix adding of patch functions in element editor
* **App:** Fix number fields loosing values when revisiting step and blurring input
* **App:** Fix min and max value of number field element

## [3.0.0](https://github.com/aivot-digital/gover/compare/v2.1.8...v3.0.0) (2023-07-29)

### Features

* **App:** Add departments and user roles
* **App:** Add asset management
* **App:** Add theming support
* **App:** Add submission lists for forms

### Bug Fixes

* **App:** Various bug fixes

## [2.1.8](https://github.com/aivot-digital/gover/compare/v2.1.7...v2.1.8) (2023-05-10)

### Bug Fixes

* **App:** Fix timeout for smtp server connection test

## [2.1.7](https://github.com/aivot-digital/gover/compare/v2.1.6...v2.1.7) (2023-05-07)

### Features

* **App:** Add function to test smtp server connection

## [2.1.6](https://github.com/aivot-digital/gover/compare/v2.1.5...v2.1.6) (2023-04-24)

### Bug Fixes

* **App:** Fix users password setting in users list as an admin

## [2.1.5](https://github.com/aivot-digital/gover/compare/v2.1.4...v2.1.5) (2023-04-23)

### Bug Fixes

* **App:** Add missing function for max file size

## [2.1.4](https://github.com/aivot-digital/gover/compare/v2.1.3...v2.1.4) (2023-04-23)

### Features

* **App:** Added destination max size property and file size check

## [2.1.3](https://github.com/aivot-digital/gover/compare/v2.1.2...v2.1.3) (2023-04-10)

### Bug Fixes

* **App:** Fix submit of applications without attachments
* **App:** Removed Hessen favicon
* **App:** Fix email validation on submit page
* **App:** Fix email sending to destination after renaming of destination field
* **App:** Fix missing styling when dragging files over fiel upload element
* **App:** Fix min/max file counter in file upload element
* **App:** Fix min/max counter resetting when disabling multiple files in file input element editor

## [2.1.2](https://github.com/aivot-digital/gover/compare/v2.1.1...v2.1.2) (2023-04-09)

### Bug Fixes

* **App:** Removed unnecessary Gover from dashboard tab title


## [2.1.1](https://github.com/aivot-digital/gover/compare/v2.1.0...v2.1.1) (2023-04-08)

### Features

* **App:** Updated file upload element with new styling and validations


## [2.1.0](https://github.com/aivot-digital/gover/compare/v2.0.3...v2.1.0) (2023-04-02)

### Features

* **Project:** Added different ports for customer and staff app in the dev environment
* **Project:** Bump the version for the gover jar file
* **App:** Add new element for file uploads

### Bug Fixes

* **App:** Fix auth state handling and prevent incorrect reset on wrong email/password
* **Server:** Exclude ResponseStatusExceptions and AccessDeniedExceptions from admin logging


## [2.0.3](https://github.com/aivot-digital/gover/compare/v2.0.2...v2.0.3) (2023-01-13)

### Bug Fixes

* **Project:** Remove unused dependency minio and fix outdated imports
* **App:** Fix summary displaying invisible container elements (container, replicating container)

## [2.0.2](https://github.com/aivot-digital/gover/compare/v2.0.1...v2.0.2) (2023-01-12)

### Features

* **Server:** Patches for elements are now handled by the sever

### Bug Fixes

* **Server:** Fix warnings during the server startup

## [2.0.1](https://github.com/aivot-digital/gover/compare/v2.0.0...v2.0.1) (2023-01-10)

### Features

* **Project:** Add auto redirect for the admin app. Appending index.html is no longer necessary
* **Project:** Migrate @mui DatePicker from @mui/lab to @mui/x-date-pickers
* **Project:** Remove unused library @mui/lab
* **Project:** Update README.md with new path for the admin app

### Bug Fixes

* **Project:** Fix CORS settings for local development


## [2.0.0](https://github.com/aivot-digital/gover/compare/v1.0.6...v2.0.0) (2023-01-05)

### Features

* **Project:** Unify frontend, backend and central repositories into one
* **Project:** Improve Dockerfile
* **Project:** Improve Maven-Setup
* **Project:** Adjust README.md for new project structure, build process and deployment process
* **Server:** Internalize app hosting
* **Server:** Internalize media file hosting
* **App:** Open edit view when adding new departments, presets, links and destinations
* **App:** Add code refresh button to admin tools dialog
* **App:** Remove dysfunctional buttons from admin tools dialog
* **App:** Remove broken expand all button from element tree header
* **App:** Hide FadingPaper-Element on Submit-Page when no data is present

## [1.0.6](https://github.com/aivot-digital/gover/compare/v1.0.5...v1.0.6) (2022-12-27)

### Features

* **Project:** Update the version of dependent repositories

## [1.0.5](https://github.com/aivot-digital/gover/compare/v1.0.4...v1.0.5) (2022-12-27)

### Features

* **Project:** Update the version of dependent repositories

## [1.0.4](https://github.com/aivot-digital/gover/compare/v1.0.3...v1.0.4) (2022-12-16)

### Bug Fixes

* **Project:** Fix Dockerfile

## [1.0.3](https://github.com/aivot-digital/gover/compare/v1.0.2...v1.0.3) (2022-12-16)

### Features

* **Project:** Improve README.md and build files

## [1.0.2](https://github.com/aivot-digital/gover/compare/v1.0.1...v1.0.2) (2022-12-05)

### Features

* **Project:** Upgrade version in readme

## [1.0.1](https://github.com/aivot-digital/gover/compare/v1.0.0...v1.0.1) (2022-11-19)

### Bug Fixes

* **Project:** Fix theming example syntax in README.md

## 1.0.0 (2022-11-19)

### Features

* Initial Version
