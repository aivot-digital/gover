-- create new tables

CREATE TABLE api_keys
(
    key            uuid PRIMARY KEY,
    title          VARCHAR(32)  NOT NULL,
    description    VARCHAR(255) NOT NULL,

    created        TIMESTAMP    NOT NULL,
    expires        TIMESTAMP    NOT NULL,

    api_keys       INTEGER      NOT NULL DEFAULT 0,
    applications   INTEGER      NOT NULL DEFAULT 0,
    assets         INTEGER      NOT NULL DEFAULT 0,
    departments    INTEGER      NOT NULL DEFAULT 0,
    destinations   INTEGER      NOT NULL DEFAULT 0,
    presets        INTEGER      NOT NULL DEFAULT 0,
    provider_links INTEGER      NOT NULL DEFAULT 0,
    submissions    INTEGER      NOT NULL DEFAULT 0,
    system_configs INTEGER      NOT NULL DEFAULT 0,
    themes         INTEGER      NOT NULL DEFAULT 0,
    users          INTEGER      NOT NULL DEFAULT 0
);
