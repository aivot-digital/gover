-- Optional soft processing deadline in calendar days, counted from the creation of each instance.
-- The bounds match the task deadline configuration of process nodes.
alter table process_versions
    add column instance_deadline_days integer null,
    add constraint process_versions_instance_deadline_days_check check (
        instance_deadline_days is null or instance_deadline_days between 1 and 3652
    );

-- Fixed when the instance is created, so later version changes do not move existing deadlines.
-- Exceeding it has no effect on the process flow; it is only highlighted to staff.
alter table process_instances
    add column deadline timestamptz null;

-- Covers assignment filtering and the deadline-first, oldest-instance ordering used by the dashboard preview.
create index idx_process_instances_dashboard_assignment
    on process_instances (assigned_user_id, status, deadline asc nulls last, started asc, id asc)
    where assigned_user_id is not null;
