-- create data object schemas
insert into data_object_schemas (key, name, description, id_gen, schema, display_fields)
values ('leistungserbringer',
        'Leistungserbringer',
        'Datenobjekt für Leistungserbringer',
        '__UUID__',
        '{
          "id": "leistungserbringer",
          "type": 3,
          "children": [
            {
              "id": "name",
              "type": 15,
              "label": "Name",
              "required": true,
              "weight": 12
            },
            {
              "id": "anschrift",
              "type": 3,
              "weight": 12,
              "children": [
                {
                  "id": "strasse",
                  "type": 15,
                  "label": "Straße",
                  "required": true,
                  "weight": 6
                },
                {
                  "id": "hausnummer",
                  "type": 15,
                  "label": "Hausnummer",
                  "required": true,
                  "weight": 6
                },
                {
                  "id": "plz",
                  "type": 15,
                  "label": "Postleitzahl",
                  "required": true,
                  "weight": 4
                },
                {
                  "id": "ort",
                  "type": 15,
                  "label": "Ort",
                  "required": true,
                  "weight": 8
                }
              ]
            },
            {
              "id": "gescheaftsfuehrung",
              "type": 3,
              "weight": 12,
              "children": [
                {
                  "id": "name_gescheaftsfuehrung",
                  "type": 15,
                  "label": "Name der Geschäftsführung",
                  "required": true,
                  "weight": 12
                },
                {
                  "id": "kontakt_gescheaftsfuehrung",
                  "type": 15,
                  "label": "Kontakt der Geschäftsführung",
                  "required": true,
                  "weight": 12
                }
              ]
            }
          ]
        }',
        '{"name", "name_gescheaftsfuehrung"}'),
       ('standorte',
        'Standorte',
        'Datenobjekt für EGH Standorte',
        '__SERIAL__',
        '{
          "id": "standort",
          "type": 3,
          "children": [
            {
              "id": "leistungserbringer_id",
              "type": 15,
              "label": "Leistungserbringer",
              "required": true,
              "weight": 12
            },
            {
              "id": "bezeichner",
              "type": 15,
              "label": "Bezeichner",
              "required": true,
              "weight": 6
            },
            {
              "id": "anschrift",
              "type": 3,
              "weight": 12,
              "children": [
                {
                  "id": "strasse",
                  "type": 15,
                  "label": "Straße",
                  "required": true,
                  "weight": 6
                },
                {
                  "id": "hausnummer",
                  "type": 15,
                  "label": "Hausnummer",
                  "required": true,
                  "weight": 6
                },
                {
                  "id": "plz",
                  "type": 15,
                  "label": "Postleitzahl",
                  "required": true,
                  "weight": 4
                },
                {
                  "id": "ort",
                  "type": 15,
                  "label": "Ort",
                  "required": true,
                  "weight": 8
                }
              ]
            },
            {
              "id": "raeume",
              "type": 14,
              "label": "Räume",
              "required": true,
              "weight": 12,
              "fields": [
                {
                  "key": "bezeichner",
                  "label": "Bezeichner",
                  "datatype": "string"
                },
                {
                  "key": "stockwerk",
                  "label": "Stockwerk",
                  "datatype": "string"
                },
                {
                  "key": "flaeche_qm",
                  "label": "Fläche (m²)",
                  "datatype": "number",
                  "decimalPlaces": 2
                }
              ]
            }
          ]
        }',
        '{"leistungserbringer_id", "bezeichner"}')
on conflict (key) do update
    set name           = excluded.name,
        description    = excluded.description,
        id_gen         = excluded.id_gen,
        schema         = excluded.schema,
        display_fields = excluded.display_fields;

-- create data object items
insert into data_object_items (schema_key, id, data)
values ('leistungserbringer',
        '00000000-0000-0000-0000-000000000001',
        '{
          "name": "Hilfswerk e.V.",
          "strasse": "Hilfsweg",
          "hausnummer": "1",
          "plz": "12345",
          "ort": "Flön",
          "name_gescheaftsfuehrung": "Maria Musterfrau",
          "kontakt_gescheaftsfuehrung": "+49 123 4567890"
        }'),
       ('leistungserbringer',
        '00000000-0000-0000-0000-000000000002',
        '{
          "name": "Werkshilfe e.V.",
          "strasse": "Werksstraße",
          "hausnummer": "15",
          "plz": "54321",
          "ort": "Altenkirchen",
          "name_gescheaftsfuehrung": "Demre Musterfrau",
          "kontakt_gescheaftsfuehrung": "+49 123 4567890"
        }'),
    ('standorte',
     '1',
     '{
       "leistungserbringer_id": "00000000-0000-0000-0000-000000000001",
       "bezeichner": "Werksvilla",
       "strasse": "Hilfsweg",
       "hausnummer": "1",
       "plz": "12345",
       "ort": "Flön",
       "raeume": [
         {
           "bezeichner": "Küche",
           "stockwerk": "EG",
           "flaeche_qm": "28"
         },
         {
           "bezeichner": "Gemeinschaftsraum",
           "stockwerk": "1. OG",
           "flaeche_qm": "13"
         }
       ]
     }'),
       ('standorte',
        '2',
        '{
          "leistungserbringer_id": "00000000-0000-0000-0000-000000000001",
          "bezeichner": "Holzmanufaktur",
          "strasse": "Hilfsweg",
          "hausnummer": "3",
          "plz": "12345",
          "ort": "Flön",
          "raeume": [
            {
              "bezeichner": "Werkraum",
              "stockwerk": "EG",
              "flaeche_qm": "59"
            },
            {
              "bezeichner": "Waschküche",
              "stockwerk": "EG",
              "flaeche_qm": "9"
            }
          ]
        }')
on conflict (schema_key, id) do update
    set data = excluded.data;