-- create tables

CREATE TABLE themes
(
    id        SERIAL PRIMARY KEY,
    name      VARCHAR(96) NOT NULL,
    main      VARCHAR(7)  NOT NULL,
    main_dark VARCHAR(7)  NOT NULL,
    accent    VARCHAR(7)  NOT NULL,
    error     VARCHAR(7)  NOT NULL,
    warning   VARCHAR(7)  NOT NULL,
    info      VARCHAR(7)  NOT NULL,
    success   VARCHAR(7)  NOT NULL
);

-- alter existing tables

ALTER TABLE applications
    ADD COLUMN theme_id INTEGER NULL REFERENCES themes ON DELETE SET NULL;
