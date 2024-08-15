-- alter existing tables

ALTER TABLE applications
    ADD COLUMN open_submissions  INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN in_progress_submissions  INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN total_submissions INTEGER NOT NULL DEFAULT 0;

-- create tigger functions

CREATE FUNCTION func_submission_added() RETURNS TRIGGER
    LANGUAGE plpgsql AS
$$
BEGIN
    UPDATE applications SET total_submissions = applications.total_submissions + 1 WHERE id = NEW.application_id;

    IF NEW.archived IS NULL THEN
        UPDATE applications SET open_submissions = applications.open_submissions + 1 WHERE id = NEW.application_id;
    END IF;

    IF NEW.assignee_id IS NOT NULL THEN
        UPDATE applications SET in_progress_submissions = applications.in_progress_submissions + 1 WHERE id = NEW.application_id;
    END IF;

    RETURN NEW;
END;
$$;

CREATE FUNCTION func_submission_archived() RETURNS TRIGGER
    LANGUAGE plpgsql AS
$$
BEGIN
    IF OLD.archived IS NULL AND NEW.archived IS NOT NULL THEN
        UPDATE applications SET open_submissions = applications.open_submissions - 1 WHERE id = NEW.application_id;
        UPDATE applications SET in_progress_submissions = applications.in_progress_submissions - 1 WHERE id = NEW.application_id;
    END IF;

    IF OLD.assignee_id IS NULL AND NEW.assignee_id IS NOT NULL THEN
        UPDATE applications SET in_progress_submissions = applications.in_progress_submissions + 1 WHERE id = NEW.application_id;
    END IF;

    IF OLD.assignee_id IS NOT NULL AND NEW.assignee_id IS NULL THEN
        UPDATE applications SET in_progress_submissions = applications.in_progress_submissions - 1 WHERE id = NEW.application_id;
    END IF;

    RETURN NEW;
END;
$$;

-- create triggers

CREATE TRIGGER trigger_submission_added
    AFTER INSERT
    ON submissions
    FOR EACH ROW
EXECUTE PROCEDURE
    func_submission_added();


CREATE TRIGGER trigger_submission_archived
    AFTER UPDATE
    ON submissions
    FOR EACH ROW
EXECUTE PROCEDURE
    func_submission_archived();
