-- add two new columns to the assets table to reference the storage provider and path from the root.
alter table assets
    add column if not exists storage_provider_id    integer null,
    add column if not exists storage_path_from_root text    null;

-- link the two columns to the storage_index_items table to reference the real stored files.
alter table assets
    add column storage_provider_id    integer null,
    add column storage_path_from_root text    null,
    alter column uploader_id drop not null;

-- add foreign key constraint for the storage provider index item.
alter table assets
    add foreign key (storage_provider_id, storage_path_from_root) references storage_index_items (storage_provider_id, path_from_root);

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
       sp.configuration -> 'root' ->> 'inputValue' || '/' || a.key || '.' || substring(a.filename from '\.([^.]+)$'),
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
        (select sp.configuration -> 'root' ->> 'inputValue'
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