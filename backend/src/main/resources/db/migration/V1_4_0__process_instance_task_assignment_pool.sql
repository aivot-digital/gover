-- The Personenkreis a process element evaluated when it assigned the task. Manual reassignments are limited to it,
-- because it can depend on process data. Tasks without a value keep the unrestricted selection.
alter table process_instance_tasks
    add column if not exists assignment_pool jsonb;
