-- This migration should rework the form version triggers and functions to fix issues with setting the correct versions and version counts.
-- The following behaviour should be achieved:
--   1. When a new form version is inserted, the parent form version counter should be incremented.
--   2. When a form version is inserted or updated the parent form must be updated if the status of the form version implies that this is the new published or drafted version.
--   3. When a form version is deleted, the parent form version counter should be decremented and the parent form must be updated if the deleted version was the published or drafted version.

-- remove existing triggers
DROP TRIGGER IF EXISTS new_form_version ON form_versions;
DROP TRIGGER IF EXISTS on_form_version_insert ON form_versions;
DROP TRIGGER IF EXISTS on_form_version_update ON form_versions;
DROP TRIGGER IF EXISTS on_form_version_update_or_insert ON form_versions;

-- remove existing form versions functions
DROP FUNCTION IF EXISTS handle_form_version_insert_or_update;
DROP FUNCTION IF EXISTS handle_form_version_update_or_insert_to_set_update_timestamp;
DROP FUNCTION IF EXISTS handle_new_form_version;

-- create new form versions functions
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

    -- Update the drafted or published version of the parent form based on the status of the new form version
    IF NEW.status = 0 THEN -- drafted
        UPDATE forms
        SET drafted_version = NEW.version
        WHERE id = NEW.form_id;
    ELSIF NEW.status = 1 THEN -- published
        UPDATE forms
        SET published_version = NEW.version
        WHERE id = NEW.form_id;

        -- Set the published timestamp for the form version
        NEW.published = now();
    END IF;

    RETURN NEW;
END;
$$;

CREATE FUNCTION handle_form_version_update()
    RETURNS TRIGGER
    LANGUAGE plpgsql AS
$$
BEGIN
    -- Update the drafted or published version of the parent form based on the status of the updated form version
    IF NEW.status = 0 THEN -- drafted
        UPDATE forms
        SET drafted_version = NEW.version,
            updated         = now()
        WHERE id = NEW.form_id;
    ELSIF NEW.status = 1 THEN -- published
        UPDATE forms
        SET published_version = NEW.version,
            updated           = now()
        WHERE id = NEW.form_id;

        -- Set the published timestamp for the form version
        NEW.published = now();
    ELSEIF NEW.status = 2 THEN -- revoked
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

        -- Set the revoked timestamp for the form version
        NEW.published = now();
    END IF;

    RETURN NEW;
END;
$$;

CREATE FUNCTION handle_form_version_delete()
    RETURNS TRIGGER
    LANGUAGE plpgsql AS
$$
BEGIN
    -- Decrement the version count of the parent form
    UPDATE forms
    SET version_count = version_count - 1,
        updated       = now()
    WHERE id = OLD.form_id;

    -- Remove the published reference if necessary
    UPDATE forms
    SET published_version = null
    WHERE id = OLD.form_id
      AND published_version = OLD.version;

    -- Remove the drafted reference if necessary
    UPDATE forms
    SET drafted_version = null
    WHERE id = OLD.form_id
      AND drafted_version = OLD.version;

    RETURN OLD;
END;
$$;

-- create new triggers
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

CREATE TRIGGER on_form_version_delete
    AFTER DELETE
    ON form_versions
    FOR EACH ROW
EXECUTE FUNCTION handle_form_version_delete();
