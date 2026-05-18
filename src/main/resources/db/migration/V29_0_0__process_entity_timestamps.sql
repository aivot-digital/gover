alter table process_nodes
    add column created  timestamp with time zone not null default now(),
    add column updated timestamp with time zone not null default now();

create or replace function update_parent_process_entities_timestamps() returns trigger as
$$
begin
    update processes
    set updated = now()
    where id = NEW.process_id;

    update process_versions
    set updated = now()
    where process_id = NEW.process_id
      and process_version = NEW.process_version;

    return NEW;
end;
$$ language plpgsql;

create trigger update_parent_process_entities_timestamps_trigger
    after update or insert or delete
    on process_nodes
    for each row
execute procedure update_parent_process_entities_timestamps();

create trigger update_parent_process_entities_timestamps_trigger
    after update or insert or delete
    on process_edges
    for each row
execute procedure update_parent_process_entities_timestamps();
