-- Remove views depending on forms or form_versions
DROP VIEW IF EXISTS search_items;
DROP VIEW IF EXISTS submissions_with_memberships;
DROP VIEW IF EXISTS form_with_memberships;
DROP VIEW IF EXISTS form_versions_with_memberships;
DROP VIEW IF EXISTS user_form_version_permissions;
DROP VIEW IF EXISTS form_version_with_details;

-- Add a new column to form_versions
ALTER TABLE form_versions
    ADD COLUMN public_title              TEXT,
    ADD COLUMN managing_department_id    INTEGER,
    ADD COLUMN responsible_department_id INTEGER;

-- Copy the public title from forms to form_versions
UPDATE form_versions fv
SET public_title              = f.public_title,
    managing_department_id    = f.managing_department_id,
    responsible_department_id = f.responsible_department_id
FROM forms f
WHERE fv.form_id = f.id;

-- Add a NOT NULL constraint to the new column
ALTER TABLE form_versions
    ALTER COLUMN public_title SET NOT NULL;

-- Remove the public title column from forms
ALTER TABLE forms
    DROP COLUMN public_title,
    DROP COLUMN managing_department_id,
    DROP COLUMN responsible_department_id;
