-- alter existing tables

alter table department_memberships
    alter column role type smallint using role::smallint;

alter table destinations
    alter column type type smallint using type::smallint;

alter table forms
    alter column status type smallint using status::smallint,
    alter column bund_id_level type smallint using bund_id_level::smallint,
    alter column bayern_id_level type smallint using bund_id_level::smallint,
    alter column sh_id_level type smallint using bund_id_level::smallint,
    alter column muk_level type smallint using bund_id_level::smallint;
