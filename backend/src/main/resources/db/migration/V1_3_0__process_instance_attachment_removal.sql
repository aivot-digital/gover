-- Task views flag the stored attachments of a task that its upload fields no longer reference. The flagged attachments
-- are hidden and are deleted when the task ends.
alter table process_instance_attachments
    add column removed_at timestamp with time zone;

-- Every end of a task looks up the flagged attachments of the task.
create index idx_process_instance_attachments_removed_task
    on process_instance_attachments (process_instance_task_id)
    where removed_at is not null;
