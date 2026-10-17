create or replace function handle_new_preset_version() returns trigger
    language plpgsql as
$$
begin
    update presets
    set version_count = (select count(*) from preset_versions as vers where vers.preset_key = new.preset_key)
    where key = new.preset_key;
    return new;
end;
$$;