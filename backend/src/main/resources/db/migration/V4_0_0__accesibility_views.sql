-- create a view of all accessible departments for a user
create view departments_with_memberships as
select deps.*,
       mems.id      as membership_id,
       mems.user_id as user_id,
       mems.role    as user_role
from departments as deps
         join department_memberships as mems
              on deps.id = mems.department_id;

-- create a view of all accessible forms for a user
create view forms_with_memberships as
select fms.*,
       mems.user_id                                                as user_id,
       bool_or(mems.department_id = fms.developing_department_id)  as is_developer,
       bool_or(mems.department_id = fms.managing_department_id)    as is_manager,
       bool_or(mems.department_id = fms.responsible_department_id) as is_responsible
from forms as fms
         join department_memberships as mems
              on fms.developing_department_id = mems.department_id
                  or fms.managing_department_id = mems.department_id
                  or fms.responsible_department_id = mems.department_id
group by fms.id, mems.user_id;

-- create a view of all accessible submissions for a user
create view submissions_with_memberships as
select subs.*,
       mems.user_id                                                as user_id,
       fms.developing_department_id                                as developing_department_id,
       fms.managing_department_id                                  as managing_department_id,
       fms.responsible_department_id                               as responsible_department_id,
       bool_or(mems.department_id = fms.developing_department_id)  as is_developer,
       bool_or(mems.department_id = fms.managing_department_id)    as is_manager,
       bool_or(mems.department_id = fms.responsible_department_id) as is_responsible
from submissions as subs
         join forms as fms
              on subs.form_id = fms.id
         join department_memberships as mems
              on fms.developing_department_id = mems.department_id
                  or fms.managing_department_id = mems.department_id
                  or fms.responsible_department_id = mems.department_id
group by subs.id, fms.id, mems.user_id;
