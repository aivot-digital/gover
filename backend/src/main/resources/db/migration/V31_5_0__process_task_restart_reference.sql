alter table process_instance_tasks
    add column restart_for_task_id bigint null;

alter table process_instance_tasks
    add constraint process_instance_tasks_restart_for_task_fk
        foreign key (restart_for_task_id) references process_instance_tasks (id) on delete no action;

alter table process_instance_tasks
    add constraint process_instance_tasks_restart_not_self
        check (restart_for_task_id <> id);

create index process_instance_tasks_restart_for_task_idx
    on process_instance_tasks (restart_for_task_id);
