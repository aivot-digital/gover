-- revoke all form_versions which are currently not drafted and not published

UPDATE form_versions as vers
SET status  = 2,
    revoked = now()
WHERE status <> 2
  AND NOT EXISTS (select id FROM forms as fms WHERE fms.id = vers.form_id AND (fms.drafted_version = vers.version OR fms.published_version = vers.version));