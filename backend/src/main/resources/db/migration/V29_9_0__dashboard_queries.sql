-- Support the activity window without scanning historical or test process instances.
create index idx_process_instances_dashboard_started
    on process_instances (started)
    where created_for_test_claim_id is null;

create index idx_process_instances_dashboard_finished
    on process_instances (finished)
    where created_for_test_claim_id is null
      and finished is not null;

create index idx_process_instances_dashboard_status
    on process_instances (status)
    where created_for_test_claim_id is null;

-- Covers assignment filtering and the deadline-first ordering used by the task preview.
create index idx_process_instance_tasks_dashboard_assignment
    on process_instance_tasks (assigned_user_id, status, deadline asc nulls last, started desc)
    where assigned_user_id is not null;

create index idx_processes_dashboard_updated
    on processes (updated desc);
