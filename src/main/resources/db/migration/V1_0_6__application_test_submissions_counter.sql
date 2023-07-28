-- update tigger functions

CREATE OR REPLACE FUNCTION func_submission_added() RETURNS TRIGGER
    LANGUAGE plpgsql AS
$$
BEGIN
    IF NOT NEW.is_test_submission THEN
        UPDATE applications SET total_submissions = applications.total_submissions + 1 WHERE id = NEW.application_id;

        IF NEW.archived IS NULL THEN
            UPDATE applications SET open_submissions = applications.open_submissions + 1 WHERE id = NEW.application_id;
        END IF;

        IF NEW.assignee_id IS NOT NULL THEN
            UPDATE applications
            SET in_progress_submissions = applications.in_progress_submissions + 1
            WHERE id = NEW.application_id;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION func_submission_archived() RETURNS TRIGGER
    LANGUAGE plpgsql AS
$$
BEGIN
    IF NOT NEW.is_test_submission THEN
        IF OLD.archived IS NULL AND NEW.archived IS NOT NULL THEN
            UPDATE applications SET open_submissions = applications.open_submissions - 1 WHERE id = NEW.application_id;
            IF NEW.assignee_id IS NOT NULL THEN
                UPDATE applications
                SET in_progress_submissions = applications.in_progress_submissions - 1
                WHERE id = NEW.application_id;
            END IF;
        END IF;

        IF OLD.assignee_id IS NULL AND NEW.assignee_id IS NOT NULL THEN
            UPDATE applications
            SET in_progress_submissions = applications.in_progress_submissions + 1
            WHERE id = NEW.application_id;
        END IF;

        IF OLD.assignee_id IS NOT NULL AND NEW.assignee_id IS NULL THEN
            UPDATE applications
            SET in_progress_submissions = applications.in_progress_submissions - 1
            WHERE id = NEW.application_id;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;
