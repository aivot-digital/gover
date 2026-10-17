alter table processes
    add column slug varchar(128);

update processes
set slug = 'process-' || id
where slug is null;

alter table processes
    alter column slug set not null;

alter table processes
    add constraint processes_slug_unique unique (slug);

create table process_slug_history
(
    slug       varchar(128) not null,
    process_id int          not null,
    created    timestamp with time zone not null default now(),

    primary key (slug),
    foreign key (process_id) references processes (id) on delete cascade
);

create index process_slug_history_process_id_idx
    on process_slug_history (process_id);
