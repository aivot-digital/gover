-- form version update

create function handle_form_version_update_or_insert_to_set_update_timestamp() returns trigger
    language plpgsql as
$$
begin
    update forms set updated = now() where id = new.form_id;
    return new;
end;
$$;

create trigger on_form_version_update_or_insert
    after insert or update
    on form_versions
    for each row
execute function handle_form_version_update_or_insert_to_set_update_timestamp();

-- preset version update

create function handle_preset_version_update_or_insert_to_set_update_timestamp() returns trigger
    language plpgsql as
$$
begin
    update presets set updated = now() where key = new.preset_key;
    return new;
end;
$$;

create trigger on_preset_version_update_or_insert
    after insert or update
    on preset_versions
    for each row
execute function handle_preset_version_update_or_insert_to_set_update_timestamp();