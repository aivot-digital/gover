-- create data object schemas
insert into data_object_schemas (key, name, description, id_gen, schema, display_fields)
values ('kita_kinder',
        'Kita Kinder',
        'Datenobjekt für Kinder in Kindertagesstätten',
        '__SERIAL__',
        '{
          "id": "kita_kind",
          "type": 3,
          "children": [
            {
              "id": "name",
              "type": 15,
              "label": "Name",
              "required": true,
              "weight": 6
            },
            {
              "id": "geburtsdatum",
              "type": 5,
              "label": "Geburtsdatum",
              "required": true,
              "weight": 6
            },
            {
              "id": "kita_name",
              "type": 15,
              "label": "Kita",
              "required": true,
              "weight": 12
            },
            {
              "id": "name_erziehungsberechtigte",
              "type": 15,
              "label": "Name des Erziehungsberechtigten",
              "required": true,
              "weight": 12
            },
            {
              "id": "adresse_erziehungsberechtigte",
              "type": 15,
              "label": "Adresse des Erziehungsberechtigten",
              "required": true,
              "weight": 12,
              "isMultiline": true
            }
          ]
        }',
        '{"name", "geburtsdatum", "kita_name"}')
on conflict (key) do update
    set name           = excluded.name,
        description    = excluded.description,
        id_gen         = excluded.id_gen,
        schema         = excluded.schema,
        display_fields = excluded.display_fields;

-- create data object items
insert into data_object_items (schema_key, id, data)
values ('kita_kinder',
        '1',
        '{
          "name": "Leon Lorbeere",
          "geburtsdatum": "2018-12-11",
          "kita_name": "Kita Blaubeeren",
          "name_erziehungsberechtigte": "Lara Lorbeere",
          "adresse_erziehungsberechtigte": "Karottenstraße 4\n54321 Blauheim"
        }'),
       ('kita_kinder',
        '2',
        '{
          "name": "Merve Mandel",
          "geburtsdatum": "2019-01-09",
          "kita_name": "Kita Blaubeeren",
          "name_erziehungsberechtigte": "Miraç Mandel",
          "adresse_erziehungsberechtigte": "Bahnhofsstraße 2\n54321 Blauheim"
        }'),
       ('kita_kinder',
        '3',
        '{
          "name": "Raphael Rübe",
          "geburtsdatum": "2019-05-24",
          "kita_name": "Kita Grüngras",
          "name_erziehungsberechtigte": "Robert Rübe",
          "adresse_erziehungsberechtigte": "Backweg 19\n54321 Grünberg"
        }')
on conflict (schema_key, id) do update
    set data = excluded.data;