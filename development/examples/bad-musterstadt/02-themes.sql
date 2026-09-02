-- create example themes
insert into themes (id,
                    name,
                    primary_color,
                    secondary_color,
                    primary_color_dark,
                    secondary_color_dark)
values (1,
        'Stadt Bad Musterstadt',
        '#1B4D7A',
        '#8A641F',
        '#76A9D4',
        '#E7C06D'),
       (2,
        'FB 1 - Bürgerservice',
        '#006B73',
        '#9A5B16',
        '#5DB8BD',
        '#E4AD68'),
       (3,
        'FB 2 - Umwelt und Bauen',
        '#3F6B3A',
        '#9A5528',
        '#8FBD88',
        '#D79A72'),
       (4,
        'FB 3 - Bildung, Familie und Kultur',
        '#80558C',
        '#2F7A78',
        '#C59ACF',
        '#73B9B5'),
       (5,
        'FB 4 - Innerer Service',
        '#4E5968',
        '#9B6039',
        '#9CA8B7',
        '#D6A07A')
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
