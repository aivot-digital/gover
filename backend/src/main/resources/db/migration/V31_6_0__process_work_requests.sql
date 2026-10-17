-- Durable outbox for pending process work. PostgreSQL is the only record of work that
-- still has to run; RabbitMQ only carries the IDs of these rows as wake-up signals, so
-- the broker holds no state that would have to be persisted or backed up.
-- A row is committed together with the state change that requires the work, such as a
-- completed task, a started or restarted process instance, or a payment status change.
-- The row ID is published after the commit and again by a periodic dispatcher if the
-- row is still pending after the redispatch interval, e.g. after a lost message. Rows
-- of paused or failed instances are held and published again once the instance runs.
-- The consumer locks the owning process instance and rechecks the row and any task the
-- work would create before running it and deleting the row in one transaction. This
-- makes duplicate deliveries safe. Rows are also discarded once the process instance
-- has completed or been aborted.
create table process_work_requests
(
    id                  bigserial primary key,
    -- 0 Start, 1 Transition, 2 Restart, 3 Resume
    kind                smallint not null,
    process_instance_id bigint not null references process_instances (id) on delete cascade,
    -- Transition: the completed predecessor task; Restart: the failed task to run again;
    -- Resume: the waiting task. Start requests are not tied to a task.
    task_id             bigint null references process_instance_tasks (id) on delete cascade,
    -- The outgoing port and target node of a transition.
    via_port            varchar(96) null,
    to_node_id          int null,
    created             timestamptz not null,
    -- The last publication to RabbitMQ, or its scheduled after-commit publication.
    last_dispatched     timestamptz null,
    constraint process_work_requests_kind_check check (kind in (0, 1, 2, 3)),
    constraint process_work_requests_task_check check ((kind = 0) = (task_id is null)),
    constraint process_work_requests_port_check check ((kind = 1) = (via_port is not null)),
    constraint process_work_requests_node_check check ((kind = 1) = (to_node_id is not null))
);

-- At most one pending start per instance and one pending transition or restart per task.
-- Resume requests may repeat, because resuming re-reads the current state.
create unique index process_work_requests_start_idx
    on process_work_requests (process_instance_id)
    where kind = 0;
create unique index process_work_requests_task_idx
    on process_work_requests (task_id, kind)
    where kind in (1, 2);
create index process_work_requests_instance_idx
    on process_work_requests (process_instance_id);

-- The consumer checks whether a transition has already created its successor task.
create index process_instance_tasks_previous_task_idx
    on process_instance_tasks (previous_process_instance_task_id);

-- 10 = DeadlineExceeded. Existing status values retain their meaning.
comment on column process_instance_tasks.status is
    '0 Running, 1 Paused, 2 Completed, 3 Aborted, 4 Failed, 5 Restarted, 6 AwaitingPayment, 7 AwaitingCustomer, 8 InProgress, 9 AwaitingStaff, 10 DeadlineExceeded';

-- Only hard deadlines are scanned; tasks past a soft deadline never become due for the worker.
create index process_instance_tasks_due_deadline_idx
    on process_instance_tasks (deadline, id)
    where status in (0, 6, 7, 8, 9) and deadline_strict;
