create view v_departments_shadowed as
with recursive departments_cte as (
    -- Anchor member: Select all organizational units of type 'organization' with depth 0
    select aou.id,
           aou.name,
           aou.address,
           aou.imprint,
           aou.common_privacy,
           aou.common_accessibility,
           aou.technical_support_address,
           aou.special_support_address,
           aou.created,
           aou.updated,
           aou.department_mail,
           aou.theme_id,
           aou.technical_support_phone,
           aou.technical_support_info,
           aou.special_support_phone,
           aou.special_support_info,
           aou.additional_info,
           aou.depth,
           aou.parent_department_id,
           '{}'::varchar[] as parent_names,
           '{}'::int[]     as parent_ids
    from departments aou
    where parent_department_id is null

    union all

    -- Recursive member: Join organizational units to find children and increment depth
    select rou.id,
           rou.name,
           coalesce(rou.address, p.address)                                     as address,
           coalesce(rou.imprint, p.imprint)                                     as imprint,
           coalesce(rou.common_privacy, p.common_privacy)                       as common_privacy,
           coalesce(rou.common_accessibility, p.common_accessibility)           as common_accessibility,
           coalesce(rou.technical_support_address, p.technical_support_address) as technical_support_address,
           coalesce(rou.special_support_address, p.special_support_address)     as special_support_address,
           rou.created,
           rou.updated,
           coalesce(rou.department_mail, p.department_mail)                     as department_mail,
           coalesce(rou.theme_id, p.theme_id)                                   as theme_id,
           coalesce(rou.technical_support_phone, p.technical_support_phone)     as technical_support_phone,
           coalesce(rou.technical_support_info, p.technical_support_info)       as technical_support_info,
           coalesce(rou.special_support_phone, p.special_support_phone)         as special_support_phone,
           coalesce(rou.special_support_info, p.special_support_info)           as special_support_info,
           coalesce(rou.additional_info, p.additional_info)                     as additional_info,
           rou.depth,
           rou.parent_department_id,
           p.parent_names || p.name                                             as parent_names,
           p.parent_ids || p.id                                                 as parent_ids
    from departments rou
             join departments_cte p on p.id = rou.parent_department_id)
select distinct on (id) *
from departments_cte
-- where id = 5
order by id, depth DESC;