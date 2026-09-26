-- create revisions table

create table form_revisions
(
    id        bigserial primary key,
    form_id   integer      not null references forms (id),
    user_id   varchar(255) not null,
    timestamp timestamp    not null,
    diff      jsonb        not null
);