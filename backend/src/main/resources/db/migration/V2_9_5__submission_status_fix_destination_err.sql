-- update existing table

UPDATE submissions
SET status = 501
WHERE archived IS NOT NULL
  and destination_id is not null
  and destination_success is false;