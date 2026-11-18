-- Counts the tasks that are still open for the metrics without scanning the finished tasks.
-- Statuses: Running, Paused, AwaitingPayment, AwaitingCustomer, InProgress, AwaitingStaff.
create index if not exists idx_process_instance_tasks_open_status
    on process_instance_tasks (status)
    where (status = ANY (ARRAY[0, 1, 6, 7, 8, 9]));
