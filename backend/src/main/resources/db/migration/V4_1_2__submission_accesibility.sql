-- make submission accessibility accessing via department and make developing departments access
-- them only if they are test submission
create or replace view submissions_with_memberships as
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
              on (fms.developing_department_id = mems.department_id and subs.is_test_submission = true)
                  or fms.managing_department_id = mems.department_id
                  or fms.responsible_department_id = mems.department_id
group by subs.id, fms.id, mems.user_id;