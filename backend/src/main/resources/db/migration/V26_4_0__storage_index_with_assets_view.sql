-- Shift asset read model to storage-index-backed fields.
-- Keep key for public access, but use provider/path reference as canonical identity for staff access.

alter table assets
    drop column filename,
    drop column content_type,
    drop column created;

create view v_storage_index_items_with_assets as
SELECT sii.storage_provider_id,
       sii.path_from_root,
       sii.directory,
       sii.filename,
       sii.mime_type,
       sii.size_in_bytes,
       sii.missing,
       sii.metadata,
       sii.created,
       sii.updated,
       key         as asset_key,
       uploader_id as asset_uploader_id,
       is_private  as asset_is_private
from storage_index_items as sii
         left join assets as ass
                   on sii.storage_provider_id = ass.storage_provider_id and
                      sii.path_from_root = ass.storage_path_from_root
         join storage_providers stp
              on sii.storage_provider_id = stp.id
where stp.type = 0; -- type 0 is an asset storage provider
