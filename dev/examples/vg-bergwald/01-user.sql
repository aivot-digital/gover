insert into users (id,
                   email,
                   first_name,
                   last_name,
                   system_role_id,
                   verified,
                   enabled,
                   deleted_in_idp)
select '00000000-0000-0000-0000-' || lpad((row_number() over ())::text, 12, '0')  as id,
       (lower(u.column1) || '.' || lower(u.column2) || '@' || u.column3 || '.de') as email,
       u.column1                                                                  as first_name,
       u.column2                                                                  as last_name,
       3                                                                          as system_role_id,
       true                                                                       as verified,
       true                                                                       as enabled,
       false                                                                      as deleted_in_idp
from (values ('Nino', 'Niehda', 'vg-bergwald'),
             ('Vadim', 'Vertreter', 'vg-bergwald'),
             ('Michael', 'Maus', 'vg-bergwald'),
             ('Lisa', 'Laus', 'vg-bergwald'),
             ('Murat', 'Mango', 'vg-bergwald'),
             ('Karla', 'Kiwi', 'vg-bergwald'),
             ('Antje', 'Apfel', 'gruenberg'),
             ('Peter', 'Pfirsich', 'gruenberg'),
             ('Sophie', 'Sonne', 'gruenberg'),
             ('Aylin', 'Agave', 'gruenberg'),
             ('René', 'Rhabarber', 'blauheim'),
             ('Lena', 'Litschi', 'blauheim'),
             ('Tina', 'Tanne', 'blauheim'),
             ('Miraç', 'Mandel', 'rotfeld'),
             ('Håkon', 'Himbeere', 'rotfeld'),
             ('Clara', 'Clementine', 'rotfeld'),
             ('Amina', 'Amaranth', 'rotfeld'),
             ('Jana-Lena', 'Jujube', 'rotfeld')) as u
on conflict (id) do update
    set email          = excluded.email,
        first_name     = excluded.first_name,
        last_name      = excluded.last_name,
        system_role_id = excluded.system_role_id,
        verified       = excluded.verified,
        enabled        = excluded.enabled,
        deleted_in_idp = excluded.deleted_in_idp;

-- create deputies
insert into user_deputies (id, original_user_id, deputy_user_id, from_timestamp, until_timestamp)
values
    -- indefinite deputy
    (1,
     '00000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000002',
     now(), null),
    -- deputy in the past
    (2,
     '00000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000002',
     now() - interval '10 days',
     now() - interval '5 days'),
    -- deputy in the future
    (3,
     '00000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000002',
     now() + interval '5 days',
     now() + interval '10 days'),
    -- current deputy
    (4,
     '00000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000002',
     now() - interval '1 day',
     now() + interval '1 day')
on conflict (id) do update
    set original_user_id = excluded.original_user_id,
        deputy_user_id   = excluded.deputy_user_id,
        from_timestamp   = excluded.from_timestamp,
        until_timestamp  = excluded.until_timestamp;

-- fix id sequence for user_deputies
select setval('user_deputies_id_seq',
              (select max(id) from user_deputies));