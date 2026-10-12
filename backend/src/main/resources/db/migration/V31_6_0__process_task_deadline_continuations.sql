-- Durable outbox for process continuations after a hard task deadline expires.
-- A pending row is committed together with the task's DeadlineExceeded status and
-- history event, so a broker outage or application restart cannot lose the next step.
-- Rows remain pending while a worker repeatedly publishes their IDs to RabbitMQ.
-- The consumer locks the owning process instance and rechecks the pending row and
-- existing child task before creating the next task and deleting the row in one
-- transaction. This makes duplicate deliveries safe. Pending rows are also discarded
-- when the process instance has already completed or been aborted.
create table process_task_deadline_continuations
(
    id                  bigserial primary key,
    process_instance_id bigint not null references process_instances (id) on delete cascade,
    task_id             bigint not null unique references process_instance_tasks (id) on delete cascade,
    from_node_id        int not null,
    to_node_id          int not null,
    via_port            varchar(96) not null,
    created             timestamptz not null
);

-- 10 = DeadlineExceeded. Existing status values retain their meaning.
comment on column process_instance_tasks.status is
    '0 Running, 1 Paused, 2 Completed, 3 Aborted, 4 Failed, 5 Restarted, 6 AwaitingPayment, 7 AwaitingCustomer, 8 InProgress, 9 AwaitingStaff, 10 DeadlineExceeded';

-- Only hard deadlines are scanned; tasks past a soft deadline never become due for the worker.
create index process_instance_tasks_due_deadline_idx
    on process_instance_tasks (deadline, id)
    where status in (0, 6, 7, 8, 9) and deadline_strict;
