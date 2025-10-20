-- add new columns to preset_versions
-- int_version is an intermediate column to store the version as an integer
-- status is the status of the version (0 = draft, 1 = published, 2 = revoked)
-- published is the timestamp when the version was published
-- revoked is the timestamp when the version was revoked

alter table preset_versions
    add column int_version smallint default 1,
    add column status      smallint default 0,
    add column published   timestamp,
    add column revoked     timestamp;

-- set the int_version column based on the version column
update preset_versions as prs
set int_version = (select version_order
                   from (select __prs.version, row_number() over (order by __prs.major, __prs.minor, __prs.patch) as version_order
                         from (select version,
                                      (string_to_array(version, '.'))[1]::int as major,
                                      (string_to_array(version, '.'))[2]::int as minor,
                                      (string_to_array(version, '.'))[3]::int as patch
                               from preset_versions as __prs
                               where __prs.preset = prs.preset) as __prs) as ___prs
                   where ___prs.version = prs.version
                   limit 1)::smallint;

-- add new columns to presets
-- int_published_version is an intermediate column to store the published version as an integer
-- drafted_version is the version that is currently being drafted
alter table presets
    add column int_published_version smallint,
    add column drafted_version       smallint;

update presets
set current_published_version = (select int_version from preset_versions where preset = key and current_published_version = version);

update presets
set drafted_version = (select int_version from preset_versions where preset = key and current_version = version);


alter table preset_versions
    drop constraint preset_versions_preset_fkey;

alter table preset_versions
    add constraint preset_versions_preset_fkey
        foreign key (preset)
            references presets (key)
            on delete cascade;

alter table preset_versions
    drop column version;

alter table preset_versions
    rename column int_version to version;

alter table preset_versions
    rename column preset to preset_key;

alter table preset_versions
    add primary key (preset_key, version);

alter table preset_versions
    rename column root to root_element;

update preset_versions
set status    = case
                    when exists(select 1 from presets where int_published_version = version) then 1
                    when exists(select 1 from presets where drafted_version = version) then 0
                    else 2
    end,
    published = case
                    when exists(select 1 from presets where int_published_version = version) then now()
                    else null
        end,
    revoked   = case
                    when exists(select 1 from presets where int_published_version <> version and drafted_version <> version and key = preset_key) then now()
                    else null
        end;

alter table preset_versions
    drop column published_store_at,
    drop column published_at;

alter table presets
    drop column current_version,
    drop column current_published_version,
    drop column current_store_version,
    drop column store_id;

alter table presets
    rename
        column int_published_version to published_version;

create view preset_version_with_details as
select prs.key,
       prs.title,
       prs.published_version,
       prs.drafted_version,
       vers.*
from preset_versions vers
         inner join presets as prs on prs.key = vers.preset_key;

-- create a function to update the drafted version in the presets table
create function handle_preset_version_insert_or_update() returns trigger
    language plpgsql as
$$
begin
    if new.status = 0 then
        -- if a new draft is created all other drafts are revoked and the drafted version column for the parent preset is updated

        update preset_versions
        set status  = 2,
            revoked = now()
        where preset_key = new.preset_key
          and status = 0
          and version <> new.version;

        update presets
        set drafted_version = new.version
        where key = new.preset_key;
    end if;

    if new.status = 1 then
        -- if a new version is published all other published versions are revoked and the published and drafted version column for the parent preset is updated

        update preset_versions
        set published = now()
        where preset_key = new.preset_key
          and version = new.version;

        update preset_versions
        set revoked = now(),
            status  = 2
        where preset_key = new.preset_key
          and status = 1
          and version <> new.version;

        update presets
        set published_version = new.version,
            drafted_version   = null
        where key = new.preset_key
          and drafted_version = new.version;
    end if;

    if new.status = 2 then
        -- if a version is revoked the revoked timestamp is set and if it was the published version the published version column in the parent preset is cleared

        update preset_versions
        set revoked = now(),
            status  = 2
        where preset_key = new.preset_key
          and version = new.version;

        update presets
        set published_version = null
        where key = new.preset_key
          and published_version = new.version;
    end if;

    return new;
end;
$$;

create trigger on_preset_version_insert
    after insert
    on preset_versions
    for each row
execute function handle_preset_version_insert_or_update();

create trigger on_preset_version_update
    after update
    on preset_versions
    for each row
    when (old.status is distinct from new.status)
execute function handle_preset_version_insert_or_update();