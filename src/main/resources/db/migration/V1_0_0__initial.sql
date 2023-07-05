-- create tables

CREATE TABLE system_configs
(
    key           VARCHAR(64) PRIMARY KEY,
    value         VARCHAR(96) NOT NULL,
    public_config BOOLEAN     NOT NULL DEFAULT false,
    created       TIMESTAMP   NOT NULL,
    updated       TIMESTAMP   NOT NULL
);

CREATE TABLE users
(
    id       SERIAL PRIMARY KEY,
    name     VARCHAR(96)  NOT NULL,
    email    VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(60)  NOT NULL,
    active   BOOLEAN      NOT NULL DEFAULT TRUE,
    admin    BOOLEAN      NOT NULL DEFAULT FALSE,
    created  TIMESTAMP    NOT NULL,
    updated  TIMESTAMP    NOT NULL
);

CREATE TABLE departments
(
    id                        SERIAL PRIMARY KEY,
    name                      VARCHAR(96)  NOT NULL,
    address                   VARCHAR(255) NOT NULL,
    imprint                   TEXT         NOT NULL,
    privacy                   TEXT         NOT NULL,
    accessibility             TEXT         NOT NULL,
    technical_support_address VARCHAR(255) NOT NULL,
    special_support_address   VARCHAR(255) NOT NULL,
    created                   TIMESTAMP    NOT NULL,
    updated                   TIMESTAMP    NOT NULL
);

CREATE TABLE department_memberships
(
    id            SERIAL PRIMARY KEY,
    department_id INTEGER NOT NULL REFERENCES departments ON DELETE CASCADE,
    user_id       INTEGER NOT NULL REFERENCES users,
    role          INTEGER NOT NULL,

    UNIQUE (department_id, user_id)
);

CREATE TABLE provider_links
(
    id      SERIAL PRIMARY KEY,
    text    VARCHAR(128) NOT NULL,
    link    VARCHAR(128) NOT NULL,
    created TIMESTAMP    NOT NULL,
    updated TIMESTAMP    NOT NULL
);

CREATE TABLE destinations
(
    id                        SERIAL PRIMARY KEY,
    name                      VARCHAR(96)  NOT NULL,
    type                      INTEGER      NOT NULL,
    created                   TIMESTAMP    NOT NULL,
    updated                   TIMESTAMP    NOT NULL,

    mail_to                   VARCHAR(255) NULL,
    mailCC                    VARCHAR(255) NULL,
    mailBCC                   VARCHAR(255) NULL,

    api_address               VARCHAR(255) NULL,
    authorization_header      VARCHAR(255) NULL,

    max_attachment_mega_bytes INTEGER      NULL
);

CREATE TABLE presets
(
    id      SERIAL PRIMARY KEY,
    root    jsonb     NOT NULL DEFAULT '{}',

    created TIMESTAMP NOT NULL,
    updated TIMESTAMP NOT NULL
);

CREATE TABLE applications
(
    id                              SERIAL PRIMARY KEY,
    slug                            VARCHAR(255) NOT NULL,
    version                         VARCHAR(11)  NOT NULL,
    title                           VARCHAR(96)  NOT NULL,
    status                          INTEGER      NOT NULL,
    root                            jsonb        NOT NULL DEFAULT '{}',

    destination_id                  INTEGER      NULL REFERENCES destinations ON DELETE SET NULL,

    legal_support_department_id     INTEGER      NULL REFERENCES departments ON DELETE SET NULL,
    technical_support_department_id INTEGER      NULL REFERENCES departments ON DELETE SET NULL,

    imprint_department_id           INTEGER      NULL REFERENCES departments ON DELETE SET NULL,
    privacy_department_id           INTEGER      NULL REFERENCES departments ON DELETE SET NULL,
    accessibility_department_id     INTEGER      NULL REFERENCES departments ON DELETE SET NULL,


    developing_department_id        INTEGER      NOT NULL REFERENCES departments ON DELETE CASCADE,
    managing_department_id          INTEGER      NULL REFERENCES departments ON DELETE SET NULL,
    responsible_department_id       INTEGER      NULL REFERENCES departments ON DELETE SET NULL,

    customer_access_hours           INTEGER      NULL,
    submission_deletion_weeks       INTEGER      NULL,

    created                         TIMESTAMP    NOT NULL,
    updated                         TIMESTAMP    NOT NULL,

    UNIQUE (slug, version)
);

CREATE TABLE submissions
(
    id                  VARCHAR(36) PRIMARY KEY,
    application_id      INTEGER     NOT NULL REFERENCES applications ON DELETE CASCADE,
    created             TIMESTAMP   NOT NULL,
    assignee_id         INTEGER     NULL REFERENCES users,
    archived            TIMESTAMP   NULL,
    file_number         VARCHAR(96) NULL,
    destination_id      INTEGER     NULL REFERENCES destinations (id) ON DELETE SET NULL,
    customer_input      jsonb       NOT NULL DEFAULT '{}',
    destination_success BOOLEAN     NULL
);

CREATE TABLE submission_attachments
(
    id            VARCHAR(36) PRIMARY KEY,
    submission_id VARCHAR(36)  NOT NULL REFERENCES submissions ON DELETE CASCADE,
    filename      VARCHAR(255) NOT NULL
);

-- create views

CREATE VIEW accessible_departments AS
SELECT mems.id as membership_id,
       deps.id as department_id,
       user_id,
       role
FROM departments deps
         INNER JOIN department_memberships mems
                    ON deps.id = mems.department_id;

CREATE VIEW accessible_applications AS
SELECT mems.id      as membership_id,
       apps.id      as application_id,
       deps.id      as department_id,
       mems.user_id as user_id,
       mems.role    as role
FROM applications apps
         INNER JOIN departments deps
                    ON apps.developing_department_id = deps.id OR apps.managing_department_id = deps.id OR
                       apps.responsible_department_id = deps.id
         INNER JOIN department_memberships mems
                    ON deps.id = mems.department_id;