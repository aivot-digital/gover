alter table users
    drop
        column full_name;

alter table users
    add column
        full_name text generated always as
            (first_name || ' ' || last_name) stored;

-- create a view with details for users and their deputies
create view v_user_deputy_with_details as
select udep.id                                                                                        as id,
       udep.from_timestamp                                                                            as from_timestamp,
       udep.until_timestamp                                                                           as until_timestamp,
       udep.until_timestamp is null or (udep.from_timestamp < now() and udep.until_timestamp > now()) as active,

       ou.id                                                                                          as original_user_id,
       ou.email                                                                                       as original_user_email,
       ou.first_name                                                                                  as original_user_first_name,
       ou.last_name                                                                                   as original_user_last_name,
       ou.enabled                                                                                     as original_user_enabled,
       ou.verified                                                                                    as original_user_verified,
       ou.deleted_in_idp                                                                              as original_user_deleted_in_idp,
       ou.system_role_id                                                                              as original_user_system_role_id,
       ou.full_name                                                                                   as original_user_full_name,

       du.id                                                                                          as deputy_user_id,
       du.email                                                                                       as deputy_user_email,
       du.first_name                                                                                  as deputy_user_first_name,
       du.last_name                                                                                   as deputy_user_last_name,
       du.enabled                                                                                     as deputy_user_enabled,
       du.verified                                                                                    as deputy_user_verified,
       du.deleted_in_idp                                                                              as deputy_user_deleted_in_idp,
       du.system_role_id                                                                              as deputy_user_system_role_id,
       du.full_name                                                                                   as deputy_user_full_name
from user_deputies udep
         join users ou on udep.original_user_id = ou.id
         join users du on udep.deputy_user_id = du.id;