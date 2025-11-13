DROP VIEW form_with_memberships;

-- Create a view to get forms with user memberships
CREATE VIEW form_with_memberships AS
SELECT fms.*,

       usrs.id                        AS user_id,
       usrs.email                     AS user_email,
       usrs.first_name                AS user_first_name,
       usrs.last_name                 AS user_last_name,
       usrs.full_name                 AS user_full_name,
       usrs.enabled                   AS user_enabled,
       usrs.verified                  AS user_verified,
       usrs.global_admin              AS user_global_admin,
       usrs.deleted_in_idp            AS user_deleted_in_idp,

       bool_or(access.is_developer)   AS user_is_developer,
       bool_or(access.is_manager)     AS user_is_manager,
       bool_or(access.is_responsible) AS user_is_responsible
FROM user_form_access AS access
         INNER JOIN forms as fms
                    ON fms.id = access.form_id
         INNER JOIN users as usrs
                    ON usrs.id = access.user_id
GROUP BY fms.id, usrs.id;
