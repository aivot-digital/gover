alter table data_object_schemas
add column display_fields varchar(64)[] default array[]::varchar(64)[];