-- This migration should create the process version triggers and functions to fix issues with setting the correct versions and version counts.
-- The following behaviour should be achieved:
--   1. When a new process version is inserted, the parent process version counter should be incremented.
--   2. When a process version is inserted or updated the parent process must be updated if the status of the process version implies that this is the new published or drafted version.
--   3. When a process version is deleted, the parent process version counter should be decremented and the parent process must be updated if the deleted version was the published or drafted version.

-- Create new process versions functions
CREATE FUNCTION handle_process_version_insert()
    RETURNS TRIGGER
    LANGUAGE plpgsql AS
$$
BEGIN
    -- Increment the version count of the parent process
    UPDATE processes
    SET version_count = version_count + 1,
        updated       = now()
    WHERE id = NEW.process_id;

    IF NEW.status = 0 THEN -- drafted
    -- Check if there is already a drafted version and raise an exception if so
        IF exists(SELECT 1 FROM process_versions WHERE process_id = NEW.process_id AND status = 0 AND process_version <> NEW.process_version) THEN
            RAISE EXCEPTION 'A drafted version already exists for this process.';
        END IF;

        -- Set the drafted version for the process
        UPDATE processes
        SET drafted_version = NEW.process_version
        WHERE id = NEW.process_id;
    ELSIF NEW.status = 1 THEN -- published
    -- Set the published timestamp for the process version
        NEW.published = now();

        -- Set all other versions (published and drafted) to revoked
        UPDATE process_versions
        SET status  = 2,
            revoked = now()
        WHERE process_id = NEW.process_id
          AND process_version <> NEW.process_version
          AND status IN (0, 1);

        -- Set the published version for the process
        UPDATE processes
        SET published_version = NEW.process_version
        WHERE id = NEW.process_id;
    ELSEIF NEW.status = 2 THEN -- revoked
    -- Raise an exception if trying to insert a revoked version
        RAISE EXCEPTION 'Cannot insert a process version with revoked status.';
    END IF;

    RETURN NEW;
END;
$$;

CREATE FUNCTION handle_process_version_update()
    RETURNS TRIGGER
    LANGUAGE plpgsql AS
$$
BEGIN
    IF NEW.status = OLD.status THEN
        -- No status change, just update the updated timestamp of the parent process
        UPDATE processes
        SET updated = now()
        WHERE id = NEW.process_id;

        RETURN NEW;
    END IF;

    IF NEW.status = 0 THEN -- drafted
        -- Check if the previous status was not drafted
        IF OLD.status <> 0 THEN
            RAISE EXCEPTION 'Cannot change status to drafted from a different status.';
        END IF;

        -- Set the drafted version for the process
        UPDATE processes
        SET drafted_version = NEW.process_version,
            updated         = now()
        WHERE id = NEW.process_id;
    ELSIF NEW.status = 1 THEN -- published
        -- Set the published timestamp for the process version
        NEW.published = now();

        -- Revoke all other published versions
        UPDATE process_versions
        SET status  = 2,
            revoked = now()
        WHERE process_id = NEW.process_id
          AND process_version <> NEW.process_version
          AND status = 1;

        -- Set the published version for the process
        UPDATE processes
        SET published_version = NEW.process_version,
            updated           = now()
        WHERE id = NEW.process_id;

        -- Set the drafted version to null if it was the drafted version
        UPDATE processes
        SET drafted_version = null,
            updated         = now()
        WHERE id = NEW.process_id
          AND drafted_version = NEW.process_version;
    ELSEIF NEW.status = 2 THEN -- revoked
        -- Set the revoked timestamp for the process version
        NEW.revoked = now();

        -- Remove the published reference if necessary
        UPDATE processes
        SET published_version = null,
            updated           = now()
        WHERE id = NEW.process_id
          AND published_version = NEW.process_version;

        -- Remove the drafted reference if necessary
        UPDATE processes
        SET drafted_version = null,
            updated         = now()
        WHERE id = NEW.process_id
          AND drafted_version = NEW.process_version;
    END IF;

    RETURN NEW;
END;
$$;

-- Create new triggers
CREATE TRIGGER on_process_version_insert
    AFTER INSERT
    ON process_versions
    FOR EACH ROW
EXECUTE FUNCTION handle_process_version_insert();

CREATE TRIGGER on_process_version_update
    BEFORE UPDATE
    ON process_versions
    FOR EACH ROW
EXECUTE FUNCTION handle_process_version_update();
