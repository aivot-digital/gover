-- create example code lists
with updated_code_list as (
    update code_lists
        set key = 'kita-betreuungsumfang',
            source_type = 0,
            source_ref = '',
            name = 'Kita-Betreuungsumfang',
            description = 'Beispielhafte manuelle Codeliste für Betreuungsmodelle in Kindertagesstätten.',
            columns = array ['Beschriftung', 'Wert']::text[],
            value_column_index = 1,
            label_column_index = 0,
            status = 2,
            status_message = null,
            last_sync = null,
            updated = current_timestamp
        where key = 'kita-betreuungsumfang'
        returning id
),
inserted_code_list as (
    insert into code_lists (
        key,
        source_type,
        source_ref,
        name,
        description,
        columns,
        value_column_index,
        label_column_index,
        status,
        status_message,
        last_sync
    )
    select 'kita-betreuungsumfang',
           0,
           '',
           'Kita-Betreuungsumfang',
           'Beispielhafte manuelle Codeliste für Betreuungsmodelle in Kindertagesstätten.',
           array ['Beschriftung', 'Wert']::text[],
           1,
           0,
           2,
           null,
           null
    where not exists (select 1 from updated_code_list)
    returning id
),
example_code_list as (
    select id from updated_code_list
    union all
    select id from inserted_code_list
),
deleted_items as (
    delete from code_list_items
        where code_list_id in (select id from example_code_list)
)
insert into code_list_items (code_list_id, columns)
select example_code_list.id,
       item.columns
from example_code_list
         cross join (values
                         (array ['Ganztagsbetreuung', 'ganztags']::text[]),
                         (array ['Halbtagsbetreuung', 'halbtags']::text[]),
                         (array ['Vormittagsbetreuung', 'vormittags']::text[]),
                         (array ['Nachmittagsbetreuung', 'nachmittags']::text[])
                    ) as item(columns);

with updated_code_list as (
    update code_lists
        set key = 'familienstand',
            source_type = 2,
            source_ref = 'urn:de:xauslaender:codelist:familienstand_2',
            name = 'Familienstand',
            description = 'Beschreibt den Familienstand einer Person.',
            columns = array ['Schluessel', 'Wert']::text[],
            value_column_index = 0,
            label_column_index = 1,
            status = 2,
            status_message = null,
            last_sync = timestamp with time zone '2026-07-19T00:00:00+02:00',
            updated = current_timestamp
        where key = 'familienstand'
        returning id
),
inserted_code_list as (
    insert into code_lists (
        key,
        source_type,
        source_ref,
        name,
        description,
        columns,
        value_column_index,
        label_column_index,
        status,
        status_message,
        last_sync
    )
    select 'familienstand',
           2,
           'urn:de:xauslaender:codelist:familienstand_2',
           'Familienstand',
           'Beschreibt den Familienstand einer Person.',
           array ['Schluessel', 'Wert']::text[],
           0,
           1,
           2,
           null,
           timestamp with time zone '2026-07-19T00:00:00+02:00'
    where not exists (select 1 from updated_code_list)
    returning id
),
example_code_list as (
    select id from updated_code_list
    union all
    select id from inserted_code_list
),
deleted_items as (
    delete from code_list_items
        where code_list_id in (select id from example_code_list)
)
insert into code_list_items (code_list_id, columns)
select example_code_list.id,
       item.columns
from example_code_list
         cross join (values
                         (array ['EA', 'Ehe aufgehoben']::text[]),
                         (array ['GS', 'geschieden']::text[]),
                         (array ['LA', 'Lebenspartnerschaft aufgehoben']::text[]),
                         (array ['LD', 'ledig']::text[]),
                         (array ['LE', 'durch Todeserklärung aufgelöste Lebenspartnerschaft']::text[]),
                         (array ['LP', 'Lebenspartnerschaft']::text[]),
                         (array ['LV', 'Lebenspartner verstorben']::text[]),
                         (array ['NB', 'unbekannt']::text[]),
                         (array ['VH', 'verheiratet']::text[]),
                         (array ['VW', 'verwitwet']::text[])
                    ) as item(columns);
