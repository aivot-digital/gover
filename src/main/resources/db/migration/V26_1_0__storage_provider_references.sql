-- create a table for process instance attachments
create table process_instance_attachments
(
    -- The unique key of this attachment
    key                      uuid primary key,

    -- The name of the attachment, e.g. the filename
    file_name                varchar(255) not null,

    -- The process instance this attachment belongs to. Attachments need to be deleted before deleting the process instance
    process_instance_id      bigint       not null references process_instances (id) on delete restrict,
    -- The process instance task this attachment belongs to, if any
    process_instance_task_id bigint       null references process_instance_tasks (id) on delete restrict,

    -- Reference to the storage provider item
    storage_provider_id      integer      not null,
    storage_path_from_root   text         not null,

    -- The id of the user, who uploaded this attachment, if it was explicitly uploaded by a user
    -- If this is null, the attachment was added automatically by the system
    uploaded_by_user_id      varchar(36)  null references users (id) on delete restrict,

    foreign key (storage_provider_id, storage_path_from_root) references storage_index_items (storage_provider_id, path_from_root)
);
