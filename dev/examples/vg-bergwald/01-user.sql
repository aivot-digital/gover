-- create example users
insert into users (id,
                   email,
                   first_name,
                   last_name,
                   system_role_id,
                   verified,
                   enabled,
                   deleted_in_idp)
select '00000000-0000-0000-0000-' || lpad((row_number() over ())::text, 12, '0') as id,
       u.column3 || '@example.com'                                               as email,
       u.column1                                                                 as first_name,
       u.column2                                                                 as last_name,
       3                                                                         as system_role_id,
       true                                                                      as verified,
       true                                                                      as enabled,
       false                                                                     as deleted_in_idp
from (values ('Nino', 'Niehda', 'nino.niedah'),
             ('Vadim', 'Vertreter', 'vadim.verteter'),
             ('Michael', 'Maus', 'michael.maus'),
             ('Lisa', 'Laus', 'lisa.laus'),
             ('Murat', 'Mango', 'murat.mango'),
             ('Karla', 'Kiwi', 'karla.kiwi'),
             ('Antje', 'Apfel', 'antje.apfel'),
             ('Peter', 'Pfirsich', 'peter.pfirsich'),
             ('Sophie', 'Sonne', 'sophie.sonne'),
             ('René', 'Rhabarber', 'rene.rhabarber'),
             ('Aylin', 'Agave', 'ayling.agave'),
             ('Lena', 'Litschi', 'lena.litschi'),
             ('Tina', 'Tanne', 'tina.tanne'),
             ('Miraç', 'Mandel', 'mirac.mandel'),
             ('Håkon', 'Himbeere', 'hakon.himbeere'),
             ('Clara', 'Clementine', 'clara.clementine'),
             ('Amina', 'Amaranth', 'anna.amaranth'),
             ('Jana-Lena', 'Jujube', 'jana-lena.jujube'),
             ('Simon', 'Suppengrün', 'simon.suppengruen'),
             ('Nura', 'Nutria', 'nura.nutria')) as u
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