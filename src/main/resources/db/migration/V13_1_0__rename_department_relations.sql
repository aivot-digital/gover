-- Rename department relations to organizational unit relations

alter table forms
    rename column developing_department_id to developing_organizational_unit_id;

alter table form_versions
    rename column managing_department_id to managing_organizational_unit_id;
alter table form_versions
    rename column responsible_department_id to responsible_organizational_unit_id;
alter table form_versions
    rename column legal_support_department_id to legal_support_organizational_unit_id;
alter table form_versions
    rename column technical_support_department_id to technical_support_organizational_unit_id;
alter table form_versions
    rename column imprint_department_id to imprint_organizational_unit_id;
alter table form_versions
    rename column privacy_department_id to privacy_organizational_unit_id;
alter table form_versions
    rename column accessibility_department_id to accessibility_organizational_unit_id;

alter table department_memberships
    rename to organizational_unit_memberships;
alter table organizational_unit_memberships
    rename column department_id to organizational_unit_id;
