-- delete all assets with no uploader_id
delete from assets where uploader_id = '';

-- delete all department memberships with no user_id
delete from department_memberships where user_id = '';

-- delete all form_revisions with no user_id
delete from form_revisions where user_id = '';

-- nullify all assignee_id in applications with no assignee_id
update submissions set assignee_id = null where assignee_id = '';

-- delete all user_configs with no user_id
delete from user_configs where user_id = '';

-- delete all users with no id
delete from users where id = '';