-- drop data

DROP TRIGGER IF EXISTS trigger_submission_added ON submissions;
DROP TRIGGER IF EXISTS trigger_submission_archived ON submissions;
DROP FUNCTION IF EXISTS func_submission_added();
DROP FUNCTION IF EXISTS func_submission_archived();

-- drop columns

ALTER TABLE forms
    DROP COLUMN IF EXISTS open_submissions,
    DROP COLUMN IF EXISTS in_progress_submissions,
    DROP COLUMN IF EXISTS total_submissions;