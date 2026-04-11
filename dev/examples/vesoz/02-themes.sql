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
        '#05378B',
        '#142D56',
        '#F8D27C',
        '#378550',
        '#1F7894',
        '#B55E06',
        '#CD362D'),
       (2,
        'Flön',
        '#005a9a',
        '#004475',
        '#e91b23',
        '#378550',
        '#1F7894',
        '#B55E06',
        '#CD362D'),
       (3,
        'Westbringstein',
        '#003F8F',
        '#00316D',
        '#F0AC5F',
        '#378550',
        '#1F7894',
        '#B55E06',
        '#CD362D'),
       (4,
        'Altkirchen',
        '#507819',
        '#35701F',
        '#E6F8CC',
        '#378550',
        '#1F7894',
        '#B55E06',
        '#CD362D')
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