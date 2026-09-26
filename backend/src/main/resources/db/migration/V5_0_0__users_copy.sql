-- create a table to contain the imported users from keycloak
create table users
(
    id             varchar(36)  not null,
    email          varchar(255) not null default '',
    first_name     varchar(255) not null default '',
    last_name      varchar(255) not null default '',
    full_name      varchar(255) not null default '',
    enabled        boolean      not null default false,
    verified       boolean      not null default false,
    global_admin   boolean      not null default false,
    deleted_in_idp boolean      not null default false,
    primary key (id)
);

-- transfer the users from the user configs table to the new users table
insert into users (id)
select distinct user_id
from user_configs
on conflict do nothing;

-- update user configs user ids to reference the new user table
alter table user_configs
    add constraint user_configs_user_id_fkey foreign key (user_id) references users (id) on delete cascade;

-- transfer the users from the department memberships table to the new users table
insert into users (id)
select distinct user_id
from department_memberships
on conflict do nothing;

-- update department memberships user ids to reference the new user table
alter table department_memberships
    add constraint department_memberships_user_id_fkey foreign key (user_id) references users (id) on delete cascade;

-- transfer the users from the assets table to the new users table
insert into users (id)
select distinct uploader_id
from assets
on conflict do nothing;

-- update assets uploader ids to reference the new user table
alter table assets
    add constraint assets_uploader_id_fkey foreign key (uploader_id) references users (id) on delete cascade;

-- transfer the users from the submissions table to the new users table
insert into users (id)
select distinct assignee_id
from submissions where submissions.assignee_id is not null
on conflict do nothing;

-- update submissions assignee ids to reference the new user table
alter table submissions
    add constraint submissions_assignee_id_fkey foreign key (assignee_id) references users (id) on delete cascade;

-- transfer the users from the form revisions table to the new users table
insert into users (id)
select distinct user_id
from form_revisions
on conflict do nothing;

-- update form revisions user ids to reference the new user table
alter table form_revisions
    add constraint form_revisions_user_id_fkey foreign key (user_id) references users (id) on delete cascade;
