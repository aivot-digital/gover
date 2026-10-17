alter table process_instances
    add column inbound_reference varchar(128) null;

create unique index process_instances_inbound_reference_unique
    on process_instances (inbound_reference)
    where inbound_reference is not null;
