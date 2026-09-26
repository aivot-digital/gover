-- add column for private status to assets table and set already existing assets to public
ALTER TABLE assets
    ADD COLUMN is_private bool not null default true;

update assets set is_private = false where is_private = true;