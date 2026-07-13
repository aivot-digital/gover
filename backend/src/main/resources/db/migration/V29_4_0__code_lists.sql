create table code_lists
(
    id                 serial primary key,
    -- The type of the source of this code list
    --   0: Manually Created
    --   1: Automatic Created by Plugin
    --   2: XRepository
    --   3: Asset
    source_type        int         not null default 0,
    -- The reference to the source of this code list
    --   if source_type = 0: No ref
    --   if source_type = 1: The unique key of the plugin
    --   if source_type = 2: The urn of the XRepository entry
    --   if source_type = 3: The key of the asset
    source_ref         varchar(96) not null default '',
    name               varchar(96) not null,
    description        text        not null,
    columns            text[]      not null,
    value_column_index int         not null default 0,
    label_column_index int         not null default 0,
    status             smallint    not null default 0,
    status_message     text        null,
    last_sync          timestamptz null,
    created            timestamptz not null default current_timestamp,
    updated            timestamptz not null default current_timestamp
);

create table code_list_items
(
    id           bigserial primary key,
    code_list_id int         not null references code_lists (id) on delete cascade,
    columns      text[]      not null,
    created      timestamptz not null default current_timestamp,
    updated      timestamptz not null default current_timestamp
);

create view v_code_list_items as
select cli.*,
       cli.columns[cl.value_column_index + 1] as value,
       cli.columns[cl.label_column_index + 1] as label
from code_list_items as cli
         join code_lists as cl on cli.code_list_id = cl.id;
