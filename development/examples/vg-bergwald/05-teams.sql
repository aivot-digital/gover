-- create example teams
insert into teams (id, name)
values (1, 'Digitalisierung'),
       (2, 'Weihnachtsfeier')
on conflict (id) do update
    set name = excluded.name,
        updated = now();

-- fix id sequence for teams
select setval('teams_id_seq',
              (select max(id) from teams));
