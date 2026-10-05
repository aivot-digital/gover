alter table process_versions
    add column retention_time_value integer null,
    add column retention_time_unit varchar(16) null,
    add constraint process_versions_retention_time_pair_check check (
        (retention_time_value is null and retention_time_unit is null)
        or (retention_time_value is not null and retention_time_value > 0
            and retention_time_unit is not null and retention_time_unit in ('Days', 'Weeks', 'Months', 'Years'))
    );

create index process_instances_retention_cleanup_idx
    on process_instances (status, keep_until, id);
