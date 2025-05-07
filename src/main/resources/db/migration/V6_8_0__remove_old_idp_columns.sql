-- remove idp data columns from forms table
alter table forms
    drop column bayern_id_enabled,
    drop column bayern_id_level,
    drop column bund_id_enabled,
    drop column bund_id_level,
    drop column sh_id_enabled,
    drop column sh_id_level,
    drop column muk_enabled,
    drop column muk_level;