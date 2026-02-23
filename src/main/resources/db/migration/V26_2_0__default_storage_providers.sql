insert into storage_providers (id,
                               storage_provider_definition_key,
                               storage_provider_definition_version,
                               name,
                               description,
                               type,
                               status,
                               status_message,
                               read_only,
                               configuration,
                               max_file_size_in_bytes,
                               prevent_deletion,
                               metadata_attributes,
                               last_sync,
                               created,
                               updated)
values (1,
        'de.aivot.core.local_disk_storage',
        1,
        'Standard-Vorlagen',
        'Dieser Speicheranbieter beinhaltet die Standard-Vorlagen für das System. Er ist schreibgeschützt und kann nicht gelöscht werden.',
        0,
        0,
        null,
        true,
        '{
          "root": {
            "$type": 15,
            "inputValue": "./default/"
          }
        }',
        10 * 1024 * 1024,
        true,
        '[]'::jsonb,
        null,
        now(),
        now()),
       (2,
        'de.aivot.core.local_disk_storage',
        1,
        'Lokale Dokumente & Medien',
        'Dieser Speicheranbieter ermöglicht das Speichern von Dokumenten und Medien auf dem lokalen Dateisystem des Servers.',
        0,
        0,
        null,
        false,
        '{
          "root": {
            "$type": 15,
            "inputValue": "./assets/"
          }
        }',
        10 * 1024 * 1024,
        false,
        '[]'::jsonb,
        null,
        now(),
        now()),
       (3,
        'de.aivot.core.local_disk_storage',
        1,
        'Lokale Vorgangsdokumente',
        'Dieser Speicheranbieter ermöglicht das Speichern von Vorgangsdokumenten auf dem lokalen Dateisystem des Servers.',
        1,
        0,
        null,
        false,
        '{
          "root": {
            "$type": 15,
            "inputValue": "./attachments/"
          }
        }',
        10 * 1024 * 1024,
        false,
        '[]'::jsonb,
        null,
        now(),
        now());

-- fix id sequence for the storage providers
select setval('storage_providers_id_seq',
              (select max(id) from storage_providers));