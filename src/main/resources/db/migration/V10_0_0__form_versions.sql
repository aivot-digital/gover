-- fix payment provider key type
alter table forms
    drop constraint forms_payment_provider_fkey;
alter table payment_transactions
    drop constraint payment_transactions_payment_provider_key_fkey;

alter table payment_providers
    alter column key type uuid using key::uuid;
alter table payment_transactions
    alter column payment_provider_key type uuid using payment_provider_key::uuid,
    add foreign key (payment_provider_key) references payment_providers (key) on delete cascade;

-- create a new table
create table form_versions
(
    form_id                         int references forms (id) on delete cascade,
    version                         smallint     not null default 1,
    type                            smallint     not null default 0,
    legal_support_department_id     int          null references departments (id) on delete set null,
    technical_support_department_id int          null references departments (id) on delete set null,
    imprint_department_id           int          null references departments (id) on delete set null,
    privacy_department_id           int          null references departments (id) on delete set null,
    accessibility_department_id     int          null references departments (id) on delete set null,
    destination_id                  int          null references destinations (id) on delete set null,
    theme_id                        int          null references themes (id) on delete set null,
    pdf_template_key                uuid         null references assets (key) on delete set null,
    payment_provider_key            uuid         null references payment_providers (key) on delete set null,
    payment_purpose                 varchar(27)  null,
    payment_description             varchar(250) null,
    payment_products                jsonb        null,
    identity_providers              jsonb        null,
    identity_verification_required  boolean      not null default false,
    customer_access_hours           smallint     null     default 4,
    submission_retention_weeks      smallint     null     default 4,
    root_element                    jsonb        not null,
    created                         timestamp    not null default now(),
    updated                         timestamp    not null default now(),
    published                       timestamp    null,
    revoked                         timestamp    null,
    primary key (form_id, version)
);

-- add new version number to existing forms
alter table forms
    add column new_version smallint;

update forms as fms
set new_version = (select version_order
                   from (select _fms.version, row_number() over (order by _fms.major, _fms.minor, _fms.patch) as version_order
                         from (select version,
                                      (string_to_array(version, '.'))[1]::int as major,
                                      (string_to_array(version, '.'))[2]::int as minor,
                                      (string_to_array(version, '.'))[3]::int as patch
                               from forms as _fms
                               where _fms.slug = fms.slug) as _fms) as _fms
                   where _fms.version = fms.version
                   limit 1)::smallint;

-- move data from forms to form_versions
insert into form_versions (form_id,
                           version,
                           type,
                           legal_support_department_id,
                           technical_support_department_id,
                           imprint_department_id,
                           privacy_department_id,
                           accessibility_department_id,
                           customer_access_hours,
                           submission_retention_weeks,
                           theme_id,
                           pdf_template_key,
                           payment_provider_key,
                           payment_purpose,
                           payment_description,
                           payment_products,
                           identity_providers,
                           identity_verification_required,
                           destination_id,
                           root_element,
                           created,
                           updated,
                           published,
                           revoked)
select (select id from forms as _fms where _fms.slug = fms.slug and _fms.new_version = 1) as form_id,
       new_version,
       type,
       legal_support_department_id,
       technical_support_department_id,
       imprint_department_id,
       privacy_department_id,
       accessibility_department_id,
       customer_access_hours,
       submission_deletion_weeks,
       theme_id,
       pdf_body_template_key::uuid,
       payment_provider::uuid,
       payment_purpose,
       payment_description,
       products,
       identity_providers,
       identity_required,
       destination_id,
       root,
       created,
       now(),
       case
           when status = 2 then created
           else null
           end                                                                            as published,
       case
           when status = 3 then created
           else null
           end                                                                            as revoked
from forms as fms;

-- drop existing views that depend on forms table
drop view forms_with_memberships;
drop view submissions_with_memberships;

-- add form version to submissions
alter table submissions
    drop constraint submissions_application_id_fkey,
    add column form_version int;

update submissions as subs
set form_version = (select new_version from forms as fms where subs.form_id = fms.id);

update submissions as subs
set form_id = (select id
               from forms _fms
               where _fms.slug = (select slug from forms as _fms where _fms.id = subs.form_id)
                 and _fms.new_version = 1
               limit 1);

alter table submissions
    alter column form_version set not null,
    add foreign key (form_id, form_version) references form_versions (form_id, version) on delete cascade;

-- add form version to form revisions
alter table form_revisions
    drop constraint form_revisions_form_id_fkey,
    add column form_version int;

update form_revisions as revs
set form_version = (select new_version from forms as fms where revs.form_id = fms.id);

update form_revisions as revs
set form_id = (select id
               from forms _fms
               where _fms.slug = (select slug from forms as _fms where _fms.id = revs.form_id)
                 and _fms.new_version = 1
               limit 1);

alter table form_revisions
    alter column form_version set not null,
    add foreign key (form_id, form_version) references form_versions (form_id, version) on delete cascade;

-- remove all forms with a version number > 1 because they are no longer needed
delete
from forms
where new_version > 1;

-- make slug column unique
alter table forms
    add constraint forms_slug_key unique (slug);

-- create and prefill headline
alter table forms
    add column public_title text not null default '';

update forms as fms
set public_title = fms.root ->> 'headline';

alter table forms
    rename column title to internal_title;

-- create a column with the current published and draft version for forms
alter table forms
    add column published_version smallint null,
    add column drafted_version   smallint null;

update forms as fms
set published_version = (select version
                         from form_versions as vers
                         where vers.form_id = fms.id
                           and vers.published is not null
                         order by vers.published desc
                         limit 1),
    drafted_version   = (select version
                         from form_versions as vers
                         where vers.form_id = fms.id
                           and vers.published is null
                         order by vers.created desc
                         limit 1);

-- drop old columns from forms table
alter table forms
    drop column version,
    drop column new_version,
    drop column status,
    drop column type,
    drop column legal_support_department_id,
    drop column technical_support_department_id,
    drop column imprint_department_id,
    drop column privacy_department_id,
    drop column accessibility_department_id,
    drop column customer_access_hours,
    drop column submission_deletion_weeks,
    drop column theme_id,
    drop column pdf_body_template_key,
    drop column payment_provider,
    drop column payment_purpose,
    drop column payment_description,
    drop column products,
    drop column identity_providers,
    drop column identity_required,
    drop column destination_id,
    drop column root
;

-- create view form_version_with_details
create view form_version_with_details as
select fms.id,
       fms.slug,
       fms.internal_title,
       fms.public_title,
       fms.developing_department_id,
       fms.managing_department_id,
       fms.responsible_department_id,
       fms.published_version,
       fms.drafted_version,
       vers.*,
       vers.version = fms.published_version as is_currently_published_version,
       vers.version = fms.drafted_version   as is_currently_drafted_version
from form_versions vers
         inner join forms as fms on fms.id = vers.form_id;

-- create table with user form permissions
create view user_form_version_permissions as
select fms.id                                                      as form_id,
       fms.version                                                 as form_version,
       mems.user_id                                                as user_id,
       bool_or(mems.department_id = fms.developing_department_id)  as user_is_developer,
       bool_or(mems.department_id = fms.managing_department_id)    as user_is_manager,
       bool_or(mems.department_id = fms.responsible_department_id) as user_is_responsible
from form_version_with_details as fms
         inner join department_memberships as mems
                    on fms.developing_department_id = mems.department_id
                        or fms.managing_department_id = mems.department_id
                        or fms.responsible_department_id = mems.department_id
group by fms.id, fms.version, mems.user_id;

-- recreate forms with memberships
create view form_versions_with_memberships as
select fms.*,

       usrs.id             as user_id,
       usrs.email          as user_email,
       usrs.first_name     as user_first_name,
       usrs.last_name      as user_last_name,
       usrs.full_name      as user_full_name,
       usrs.enabled        as user_enabled,
       usrs.verified       as user_verified,
       usrs.global_admin   as user_global_admin,
       usrs.deleted_in_idp as user_deleted_in_idp,

       perm.user_is_developer,
       perm.user_is_manager,
       perm.user_is_responsible
from form_version_with_details as fms
         inner join user_form_version_permissions as perm
                    on fms.id = perm.form_id and fms.version = perm.form_version
         inner join users as usrs
                    on usrs.id = perm.user_id;

-- recreate submissions with memberships
create view submissions_with_memberships as
select subs.*,

       fms.slug                            as form_slug,
       fms.internal_title                  as form_internal_title,
       fms.public_title                    as form_public_title,
       fms.developing_department_id        as form_developing_department_id,
       fms.managing_department_id          as form_managing_department_id,
       fms.responsible_department_id       as form_responsible_department_id,
       fms.created                         as form_created,
       fms.updated                         as form_updated,
       fms.published_version               as form_published_version,
       fms.drafted_version                 as form_drafted_version,
       fms.type                            as form_type,
       fms.legal_support_department_id     as form_legal_support_department_id,
       fms.technical_support_department_id as form_technical_support_department_id,
       fms.imprint_department_id           as form_imprint_department_id,
       fms.privacy_department_id           as form_privacy_department_id,
       fms.accessibility_department_id     as form_accessibility_department_id,
       fms.customer_access_hours           as form_customer_access_hours,
       fms.submission_retention_weeks      as form_submission_retention_weeks,
       fms.theme_id                        as form_theme_id,
       fms.pdf_template_key                as form_pdf_template_key,
       fms.payment_provider_key            as form_payment_provider_key,
       fms.payment_purpose                 as form_payment_purpose,
       fms.payment_description             as form_payment_description,
       fms.payment_products                as form_payment_products,
       fms.identity_providers              as form_identity_providers,
       fms.identity_verification_required  as form_identity_verification_required,
       fms.destination_id                  as form_destination_id,
       fms.root_element                    as form_root_element,
       fms.published                       as form_version_published,
       fms.revoked                         as form_version_revoked,
       fms.is_currently_published_version  as form_is_currently_published_version,
       fms.is_currently_drafted_version    as form_is_currently_drafted_version,

       fms.user_id,
       fms.user_email,
       fms.user_first_name,
       fms.user_last_name,
       fms.user_full_name,
       fms.user_enabled,
       fms.user_verified,
       fms.user_global_admin,
       fms.user_deleted_in_idp,
       fms.user_is_developer,
       fms.user_is_manager,
       fms.user_is_responsible
from submissions as subs
         inner join form_versions_with_memberships as fms
                    on subs.form_id = fms.id and subs.form_version = fms.version;

-- create table for form slug history

create table form_slug_history
(
    slug    varchar(255) primary key,
    form_id int not null references forms (id) on delete cascade
);