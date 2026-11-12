-- Task views flag the stored attachments of a task that its upload fields no longer reference. The flagged attachments
-- are hidden and are due for deletion when the task ends.
alter table process_instance_attachments
    add column removed_at timestamp with time zone;

-- Due attachments keep their row until their file is deleted from storage, so a failed deletion is retried.
alter table process_instance_attachments
    add column deletion_due_at timestamp with time zone;

-- Every end of a task looks up the flagged attachments of the task.
create index idx_process_instance_attachments_removed_task
    on process_instance_attachments (process_instance_task_id)
    where removed_at is not null;

-- The retry looks up the attachments whose deletion is overdue.
create index idx_process_instance_attachments_deletion_due
    on process_instance_attachments (deletion_due_at)
    where deletion_due_at is not null;
