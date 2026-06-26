-- This migration should rework the form version triggers and functions to fix issues with setting the correct versions and version counts.
-- The following behaviour should be achieved:
--   1. When a new form version is inserted, the parent form version counter should be incremented.
--   2. When a form version is inserted or updated the parent form must be updated if the status of the form version implies that this is the new published or drafted version.
--   3. When a form version is deleted, the parent form version counter should be decremented and the parent form must be updated if the deleted version was the published or drafted version.

-- Remove existing triggers
DROP TRIGGER IF EXISTS on_form_version_insert ON form_versions;
DROP TRIGGER IF EXISTS on_form_version_update ON form_versions;

-- Remove existing form versions functions
DROP FUNCTION IF EXISTS handle_form_version_insert;
DROP FUNCTION IF EXISTS handle_form_version_update;

-- Create new form versions functions
CREATE FUNCTION handle_form_version_insert()
    RETURNS TRIGGER
    LANGUAGE plpgsql AS
$$
BEGIN
    -- Increment the version count of the parent form
    UPDATE forms
    SET version_count = version_count + 1,
        updated       = now()
    WHERE id = NEW.form_id;

    IF NEW.status = 0 THEN -- drafted
    -- Check if there is already a drafted version and raise an exception if so
        IF exists(SELECT 1 FROM form_versions WHERE form_id = NEW.form_id AND status = 0 AND version <> NEW.version) THEN
            RAISE EXCEPTION 'A drafted version already exists for this form.';
        END IF;

        -- Set the drafted version for the form
        UPDATE forms
        SET drafted_version = NEW.version
        WHERE id = NEW.form_id;
    ELSIF NEW.status = 1 THEN -- published
    -- Set the published timestamp for the form version
        NEW.published = now();

        -- Set all other versions (published and drafted) to revoked
        UPDATE form_versions
        SET status  = 2,
            revoked = now()
        WHERE form_id = NEW.form_id
          AND version <> NEW.version
          AND status IN (0, 1);

        -- Set the published version for the form
        UPDATE forms
        SET published_version = NEW.version
        WHERE id = NEW.form_id;
    ELSEIF NEW.status = 2 THEN -- revoked
    -- Raise an exception if trying to insert a revoked version
        RAISE EXCEPTION 'Cannot insert a form version with revoked status.';
    END IF;

    RETURN NEW;
END;
$$;

CREATE FUNCTION handle_form_version_update()
    RETURNS TRIGGER
    LANGUAGE plpgsql AS
$$
BEGIN
    IF NEW.status = OLD.status THEN
        -- No status change, just update the updated timestamp of the parent form
        UPDATE forms
        SET updated = now()
        WHERE id = NEW.form_id;

        RETURN NEW;
    END IF;

    IF NEW.status = 0 THEN -- drafted
        -- Check if the previous status was not drafted
        IF OLD.status <> 0 THEN
            RAISE EXCEPTION 'Cannot change status to drafted from a different status.';
        END IF;

        -- Set the drafted version for the form
        UPDATE forms
        SET drafted_version = NEW.version,
            updated         = now()
        WHERE id = NEW.form_id;
    ELSIF NEW.status = 1 THEN -- published
        -- Set the published timestamp for the form version
        NEW.published = now();

        -- Revoke all other published versions
        UPDATE form_versions
        SET status  = 2,
            revoked = now()
        WHERE form_id = NEW.form_id
          AND version <> NEW.version
          AND status = 1;

        -- Set the published version for the form
        UPDATE forms
        SET published_version = NEW.version,
            updated           = now()
        WHERE id = NEW.form_id;

        -- Set the drafted version to null if it was the drafted version
        UPDATE forms
        SET drafted_version = null,
            updated         = now()
        WHERE id = NEW.form_id
          AND drafted_version = NEW.version;
    ELSEIF NEW.status = 2 THEN -- revoked
        -- Set the revoked timestamp for the form version
        NEW.revoked = now();

        -- Remove the published reference if necessary
        UPDATE forms
        SET published_version = null,
            updated           = now()
        WHERE id = NEW.form_id
          AND published_version = NEW.version;

        -- Remove the drafted reference if necessary
        UPDATE forms
        SET drafted_version = null,
            updated         = now()
        WHERE id = NEW.form_id
          AND drafted_version = NEW.version;
    END IF;

    RETURN NEW;
END;
$$;

-- Create new triggers
CREATE TRIGGER on_form_version_insert
    AFTER INSERT
    ON form_versions
    FOR EACH ROW
EXECUTE FUNCTION handle_form_version_insert();

CREATE TRIGGER on_form_version_update
    BEFORE UPDATE
    ON form_versions
    FOR EACH ROW
EXECUTE FUNCTION handle_form_version_update();
