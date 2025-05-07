-- add idp data columns to forms table
alter table forms
    add column bayern_id_enabled boolean default false,
    add column bayern_id_level integer default 0,
    add column bund_id_enabled boolean default false,
    add column bund_id_level integer default 0,
    add column sh_id_enabled boolean default false,
    add column sh_id_level integer default 0,
    add column muk_enabled boolean default false,
    add column muk_level integer default 0;
