-- drop the not null constraint on the uploader_id column in the assets table.
-- add two new columns to the assets table to reference the storage provider and path from the root.
-- link the new columns to the storage_index_items table.
alter table assets
    alter column uploader_id drop not null,
    add column storage_provider_id    integer null,
    add column storage_path_from_root text    null,
    add foreign key (storage_provider_id, storage_path_from_root) references storage_index_items (storage_provider_id, path_from_root) on delete cascade on update cascade;

-- copy all assets to the default asset storage provider and mark them as missing.
insert into storage_index_items
(storage_provider_id,
 storage_provider_type,
 path_from_root,
 directory,
 filename,
 mime_type,
 size_in_bytes,
 missing,
 metadata,
 created,
 updated)
select sp.id,
       sp.type,
       sp.configuration ->> 'root' || '/' || a.key || '.' || substring(a.filename from '\.([^.]+)$'),
       false,
       a.filename,
       a.content_type,
       0,
       true,
       '{}'::jsonb,
       a.created,
       a.created
from assets as a
         cross join (select *
                     from storage_providers
                     where id = (select value
                                 from system_configs
                                 where key = 'storage.assets.default_storage_provider'
                                 limit 1)::integer) as sp;

-- set the default storage provider and path for all assets.
update assets as a
set storage_provider_id    = (select value
                              from system_configs
                              where key = 'storage.assets.default_storage_provider'
                              limit 1)::integer,
    storage_path_from_root =
        (select sp.configuration ->> 'root'
         from public.storage_providers sp
         where id = (select value
                     from system_configs
                     where key = 'storage.assets.default_storage_provider'
                     limit 1)::integer) || '/' || a.key || '.' ||
        substring(a.filename from '\.([^.]+)$')
;

-- make the storage_provider_id and storage_path_from_root columns not null to ensure all assets have a valid reference to the storage provider and path.
alter table assets
    alter column storage_provider_id set not null,
    alter column storage_path_from_root set not null;

-- add a unique constraint to the assets table to ensure that each combination of storage_provider_id and storage_path_from_root is unique.
alter table assets
    add unique (storage_provider_id, storage_path_from_root);