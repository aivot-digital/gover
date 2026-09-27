-- create secrets table
CREATE TABLE secrets
(
    key         VARCHAR(36) PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL,
    description VARCHAR(255) NOT NULL,
    value       TEXT         NOT NULL,
    salt        VARCHAR(16)  NOT NULL
);
