-- create example teams
insert into teams (id, name)
values (1, 'Digitalisierung'),
       (2, 'Gleichstellungsbeauftragte'),
       (3, 'Büro der Bürgermeisterin'),
       (4, 'Weihnachtsfeier')
on conflict (id) do update
    set name = excluded.name,
        updated = now();

-- fix id sequence for teams
select setval('teams_id_seq',
              (select max(id) from teams));
