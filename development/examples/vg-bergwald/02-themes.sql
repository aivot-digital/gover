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
        '#05378B',
        '#142D56',
        '#D2E260',
        '#378550',
        '#1F7894',
        '#B55E06',
        '#CD362D'),
       (2,
        'Rotfeld',
        '#005a9a',
        '#004475',
        '#e91b23',
        '#378550',
        '#1F7894',
        '#B55E06',
        '#CD362D'),
       (3,
        'Blauheim',
        '#003F8F',
        '#00316D',
        '#F0AC5F',
        '#378550',
        '#1F7894',
        '#B55E06',
        '#CD362D'),
       (4,
        'Grünberg',
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