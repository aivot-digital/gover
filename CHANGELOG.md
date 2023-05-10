# Changelog

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
