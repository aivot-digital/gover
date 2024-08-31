-- create new tables

CREATE TABLE assets
(
    key         uuid PRIMARY KEY,
    filename    VARCHAR(255) NOT NULL,
    created     TIMESTAMP    NOT NULL,
    uploader_id VARCHAR(64) NOT NULL
);

-- drop referenced views

DROP VIEW IF EXISTS accessible_applications;
DROP VIEW IF EXISTS accessible_departments;

-- alter existing tables

ALTER TABLE department_memberships
    DROP CONSTRAINT department_memberships_user_id_fkey,
    ALTER COLUMN user_id TYPE VARCHAR(255);

ALTER TABLE submissions
    DROP CONSTRAINT submissions_assignee_id_fkey,
    ALTER COLUMN assignee_id TYPE VARCHAR(255);

-- remove obsolete tables

DROP TABLE IF EXISTS api_keys;
DROP TABLE IF EXISTS users;

-- rename applications table into forms

ALTER TABLE applications RENAME TO forms;

-- recreate views

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
FROM forms apps
         INNER JOIN departments deps
                    ON apps.developing_department_id = deps.id OR apps.managing_department_id = deps.id OR
                       apps.responsible_department_id = deps.id
         INNER JOIN department_memberships mems
                    ON deps.id = mems.department_id;
