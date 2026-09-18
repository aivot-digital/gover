-- create example themes
insert into themes (id,
                    name,
                    primary_color,
                    secondary_color,
                    primary_color_dark,
                    secondary_color_dark)
values (1,
        'Verwaltungsgemeinschaft Bergwald',
        '#235A45',
        '#58729D',
        '#78B69A',
        '#9FB5D6'),
       (2,
        'EDV',
        '#355F8A',
        '#6A5A9B',
        '#83ACD4',
        '#ACA0D4'),
       (3,
        'Kämmerei',
        '#536A3D',
        '#86634E',
        '#9CB781',
        '#BE9A82'),
       (4,
        'Liegenschaften',
        '#6C5B3C',
        '#3F7080',
        '#B7A47E',
        '#82B4C0'),
       (5,
        'Ordnungsamt',
        '#614A66',
        '#87662D',
        '#A98EB0',
        '#C8AA6B'),
       (6,
        'Stadt Grünberg',
        '#3D6F3F',
        '#986039',
        '#86BD88',
        '#D6A077'),
       (8,
        'Gemeinde Blauheim',
        '#275F8C',
        '#976027',
        '#78ACD2',
        '#D1A269'),
       (10,
        'Kommune Rotfeld',
        '#9A3E49',
        '#3E6779',
        '#D9858D',
        '#80A7B8')
on conflict (id) do update
    set name                 = excluded.name,
        primary_color        = excluded.primary_color,
        secondary_color      = excluded.secondary_color,
        primary_color_dark   = excluded.primary_color_dark,
        secondary_color_dark = excluded.secondary_color_dark;

-- fix id sequence for themes
select setval('themes_id_seq',
              (select max(id) from themes));

-- assign the default system theme
insert into system_configs (key,
                            value)
values ('SystemTheme', '1')
on conflict (key) do update
    set value = excluded.value;
