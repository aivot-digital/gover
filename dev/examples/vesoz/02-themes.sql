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
        'VeSoz',
        '#04378b',
        '#142d56',
        '#8B5704',
        '#28a745',
        '#17a2b8',
        '#ffc107',
        '#dc3545'),
       (2,
        'Flön',
        '#005a9a',
        '#004475',
        '#e91b23',
        '#28a745',
        '#17a2b8',
        '#ffc107',
        '#dc3545'),
       (3,
        'Westbringstein',
        '#003f8f',
        '#003375',
        '#565656',
        '#28a745',
        '#17a2b8',
        '#ffc107',
        '#dc3545'),
       (4,
        'Altkirchen',
        '#73b129',
        '#87bd3c',
        '#f1f7e9',
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