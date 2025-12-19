-- create example themes
insert into themes (id,
                    name,
                    main,
                    main_dark,
                    accent,
                    success,
                    info,
                    warning,
                    error)
values (1,
        'VG Bergwald',
        '#004777',
        '#00253D',
        '#A30000',
        '#28a745',
        '#17a2b8',
        '#ffc107',
        '#dc3545'),
       (2,
        'Grünberg',
        '#F34213',
        '#9B2808',
        '#B09D1C',
        '#28a745',
        '#17a2b8',
        '#ffc107',
        '#dc3545'),
       (3,
        'Blauheim',
        '#F34213',
        '#9B2808',
        '#B09D1C',
        '#28a745',
        '#17a2b8',
        '#ffc107',
        '#dc3545'),
       (4,
        'Rotfeld',
        '#F34213',
        '#9B2808',
        '#B09D1C',
        '#28a745',
        '#17a2b8',
        '#ffc107',
        '#dc3545')
on conflict (id) do update
    set name      = excluded.name,
        main      = excluded.main,
        main_dark = excluded.main_dark,
        accent    = excluded.accent,
        success   = excluded.success,
        info      = excluded.info,
        warning   = excluded.warning,
        error     = excluded.error;

-- fix id sequence for themes
select setval('themes_id_seq',
              (select max(id) from themes));

-- assign the default system theme
insert into system_configs (key,
                            value)
values ('SystemTheme', '1')
on conflict (key) do update
    set value = excluded.value;