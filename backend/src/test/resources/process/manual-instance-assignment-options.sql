-- Explicit fixture IDs leave the database's sequences untouched.
insert into system_roles (id, name, permissions) values
    (-1, 'Assignment test staff', '{}'),
    (-2, 'Assignment test global reader', '{process_instance.read}');
insert into domain_roles (id, name, permissions) values
    (-1, 'Assignment test reader', '{process_instance.read}');

insert into users (id, first_name, last_name, email, enabled, deleted_in_idp, system_role_id) values
    ('assignment-original', 'Ada', 'Beispiel', 'ada@example.org', true, false, -1),
    ('assignment-deputy', 'Bea', 'Beispiel', 'bea@example.org', true, false, -1),
    ('assignment-global', 'Carla', 'Beispiel', 'carla@example.org', true, false, -2),
    ('assignment-team', 'Dora', 'Beispiel', 'dora@example.org', true, false, -1),
    ('assignment-disabled', 'Eva', 'Beispiel', 'eva@example.org', false, false, -1),
    ('assignment-deleted', 'Frida', 'Beispiel', 'frida@example.org', true, true, -1);
insert into user_deputies (id, original_user_id, deputy_user_id, from_date) values
    (-1, 'assignment-original', 'assignment-deputy', current_date);

insert into departments (id, name, created, updated) values
    (-1, 'Assignment test managing department', now(), now()),
    (-2, 'Assignment test unrelated department', now(), now());
insert into department_memberships (id, department_id, user_id) values
    (-1, -1, 'assignment-original'),
    (-2, -2, 'assignment-deputy'),
    (-3, -1, 'assignment-disabled'),
    (-4, -1, 'assignment-deleted');
insert into domain_role_assignments (id, department_membership_id, domain_role_id) values
    (-1, -1, -1), (-2, -2, -1), (-3, -3, -1), (-4, -4, -1);

insert into teams (id, name) values (-1, 'Assignment test team');
insert into team_memberships (id, team_id, user_id) values
    (-1, -1, 'assignment-original'),
    (-2, -1, 'assignment-global'),
    (-3, -1, 'assignment-team');
insert into domain_role_assignments (id, team_membership_id, domain_role_id) values
    (-5, -3, -1), (-6, -1, -1);

insert into processes (id, internal_title, department_id, access_key, slug)
values (-1, 'Assignment test', -1, gen_random_uuid(), 'manual-assignment-options-test');
insert into process_versions (process_id, process_version, public_title) values (-1, 1, 'Assignment test');
insert into process_nodes (id, process_id, process_version, data_key, process_node_definition_key, process_node_definition_version)
values (-1, -1, 1, 'assignment', 'de.aivot.core.assign_instance', 1);
insert into process_instances (id, case_number, access_key, process_id, initial_process_version, initial_node_id)
values (-1, 'manual-assignment-options-test', 'manual-assignment-options-test', -1, 1, -1);
