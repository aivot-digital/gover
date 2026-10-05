alter sequence provider_links_id_seq rename to custom_links_id_seq;
alter table provider_links rename to custom_links;

alter table custom_links rename column text to label;
alter table custom_links rename column link to url;

-- Custom links are deliberately not part of the global search, so the legacy label index is no longer needed.
drop index if exists idx_provider_links_text_full_text;

alter table custom_links
    alter column url type varchar(500),
    add column description varchar(255) null,
    add column icon varchar(64) null,
    -- 0 = Dashboard. Values are mapped explicitly by CustomLinkTypeConverter.
    add column type smallint not null default 0,
    add column position integer not null default 0,
    add column enabled boolean not null default true;

update custom_links
set position = id;

-- Future link contexts must be selected explicitly instead of silently becoming dashboard links.
alter table custom_links
    alter column type drop default;

create index idx_custom_links_type_position on custom_links (type, enabled, position, id);
