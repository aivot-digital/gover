-- create version count for forms
alter table forms
    add column version_count integer default 0 not null;

update forms
set version_count = (select count(*) from form_versions where form_id = forms.id);

create function handle_new_form_version() returns trigger
    language plpgsql as
$$
begin
    update forms
    set version_count = (select count(*) from form_versions where form_id = new.form_id)
    where id = new.form_id;
    return new;
end;
$$;

create trigger new_form_version
    after insert
    on form_versions
    for each row
execute function handle_new_form_version();

-- create version count for presets
alter table presets
    add column version_count integer default 0 not null;

update presets
set version_count = (select count(*) from preset_versions where preset_key = presets.key);

create function handle_new_preset_version() returns trigger
    language plpgsql as
$$
begin
    update presets
    set version_count = (select count(*) from preset_versions where preset_key = new.preset_id)
    where key = new.preset_id;
    return new;
end;
$$;

create trigger new_preset_version
    after insert
    on preset_versions
    for each row
execute function handle_new_preset_version();
