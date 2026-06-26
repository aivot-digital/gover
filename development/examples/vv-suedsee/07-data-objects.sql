-- create data object schemas
insert into data_object_schemas (key, name, description, id_gen, schema, display_fields)
values ('verkehrsunternehmen',
        'Verkehrsunternehmen',
        'Datenobjekt für Verkehrsunternehmen im Verkehrsverbund Südsee',
        '__UUID__',
        '{
          "id": "verkehrsunternehmen",
          "type": 3,
          "children": [
            {
              "id": "name",
              "type": 15,
              "label": "Name",
              "required": true,
              "weight": 8
            },
            {
              "id": "kuerzel",
              "type": 15,
              "label": "Kürzel",
              "required": true,
              "weight": 4
            },
            {
              "id": "betriebsgebiet",
              "type": 15,
              "label": "Betriebsgebiet",
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
              "id": "ansprechperson",
              "type": 3,
              "weight": 12,
              "children": [
                {
                  "id": "name_ansprechperson",
                  "type": 15,
                  "label": "Name der Ansprechperson",
                  "required": true,
                  "weight": 6
                },
                {
                  "id": "kontakt_ansprechperson",
                  "type": 15,
                  "label": "Kontakt der Ansprechperson",
                  "required": true,
                  "weight": 6
                }
              ]
            }
          ]
        }',
        '{"name", "kuerzel", "betriebsgebiet"}'),
       ('haltestellen',
        'Haltestellen',
        'Datenobjekt für Haltestellen im Verkehrsverbund Südsee',
        '__SERIAL__',
        '{
          "id": "haltestelle",
          "type": 3,
          "children": [
            {
              "id": "verkehrsunternehmen_id",
              "type": 41,
              "label": "Betreibendes Verkehrsunternehmen",
              "required": true,
              "weight": 12,
              "dataModelKey": "verkehrsunternehmen",
              "dataLabelAttributeKey": "name"
            },
            {
              "id": "bezeichner",
              "type": 15,
              "label": "Haltestellenname",
              "required": true,
              "weight": 8
            },
            {
              "id": "tarifzone",
              "type": 15,
              "label": "Tarifzone",
              "required": true,
              "weight": 4
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
              "id": "linien",
              "type": 14,
              "label": "Bediente Linien",
              "required": true,
              "weight": 12,
              "fields": [
                {
                  "key": "linie",
                  "label": "Linie",
                  "datatype": "string"
                },
                {
                  "key": "richtung",
                  "label": "Richtung",
                  "datatype": "string"
                },
                {
                  "key": "takt_min",
                  "label": "Takt (Minuten)",
                  "datatype": "number"
                }
              ]
            }
          ]
        }',
        '{"verkehrsunternehmen_id", "bezeichner", "tarifzone"}')
on conflict (key) do update
    set name           = excluded.name,
        description    = excluded.description,
        id_gen         = excluded.id_gen,
        schema         = excluded.schema,
        display_fields = excluded.display_fields;

-- create data object items
insert into data_object_items (schema_key, id, data)
values ('verkehrsunternehmen',
        '00000000-0000-0000-0000-000000000001',
        '{
          "name": "SüdseeBus GmbH",
          "kuerzel": "SSB",
          "betriebsgebiet": "Flön, Bad Musterstadt und Oideen",
          "strasse": "Betriebshofstraße",
          "hausnummer": "1",
          "plz": "12345",
          "ort": "Bad Musterstadt",
          "name_ansprechperson": "Udo Urlaub",
          "kontakt_ansprechperson": "udo.urlaub@example.com"
        }'),
       ('verkehrsunternehmen',
        '00000000-0000-0000-0000-000000000002',
        '{
          "name": "Inselfähre Südsee AG",
          "kuerzel": "IFS",
          "betriebsgebiet": "Altkirchen, Westbringstein und die Südsee-Inseln",
          "strasse": "Hafenallee",
          "hausnummer": "4",
          "plz": "54321",
          "ort": "Oideen",
          "name_ansprechperson": "Verona Vertreter",
          "kontakt_ansprechperson": "verona.vertreter@example.com"
        }'),
       ('haltestellen',
        '1',
        '{
          "verkehrsunternehmen_id": "00000000-0000-0000-0000-000000000001",
          "bezeichner": "Bad Musterstadt ZOB",
          "tarifzone": "100",
          "strasse": "Seestraße",
          "hausnummer": "1a",
          "plz": "12345",
          "ort": "Bad Musterstadt",
          "linien": [
            {
              "linie": "S1",
              "richtung": "Flön Markt",
              "takt_min": 15
            },
            {
              "linie": "B12",
              "richtung": "Oideen Hafen",
              "takt_min": 30
            }
          ]
        }'),
       ('haltestellen',
        '2',
        '{
          "verkehrsunternehmen_id": "00000000-0000-0000-0000-000000000002",
          "bezeichner": "Oideen Hafen",
          "tarifzone": "220",
          "strasse": "Am Meer",
          "hausnummer": "4",
          "plz": "54321",
          "ort": "Oideen",
          "linien": [
            {
              "linie": "F2",
              "richtung": "Altkirchen Anleger",
              "takt_min": 60
            },
            {
              "linie": "B12",
              "richtung": "Bad Musterstadt ZOB",
              "takt_min": 30
            }
          ]
        }')
on conflict (schema_key, id) do update
    set data = excluded.data;
