-- create a secret for the local s3 storage secret key
insert into secrets (key, name, description, value, salt)
values ('113356ef-8bab-4844-9f3e-6779c4117294'::uuid,
        'Secret Key Lokaler S3-Speicher',
        'Der S3-Secret-Key für den lokalen S3-Speicher.',
        'ZIXuBvyMcFpyGqXP8u0Hrki12flJWgUdk+2P7pkfRpk7AMziQKesl9mtZvc=',
        'iYowO01D6scpyc2SzlE0qg==')
on conflict (key) do update
    set name        = excluded.name,
        description = excluded.description,
        value       = excluded.value,
        salt        = excluded.salt;

-- create the local s3 storage provider
insert into storage_providers (id,
                               storage_provider_definition_key,
                               storage_provider_definition_version,
                               name,
                               description,
                               type,
                               status,
                               status_message,
                               read_only_storage,
                               configuration,
                               max_file_size_in_bytes,
                               system_provider,
                               test_provider,
                               metadata_attributes,
                               last_sync,
                               created,
                               updated)
values (4,
        'de.aivot.core.s3_storage',
        1,
        'Lokaler S3-Speicher',
        'Der lokale S3-Speicher zum Testen.',
        0, -- Assets
        0, -- Sync Pending
        null,
        false,
        '{
          "bucket": "gover",
          "endpoint": "http://localhost:9000",
          "access_key": "super-access-key",
          "secret_key_secret": "113356ef-8bab-4844-9f3e-6779c4117294"
        }',
        10000000,
        false,
        true,
        '[
          {
            "key": "x-amz-meta-farbe",
            "label": "Farbe",
            "description": "Die Farbe dieser Datei"
          }
        ]',
        null,
        now(),
        now())
on conflict (id) do update
    set storage_provider_definition_key     = excluded.storage_provider_definition_key,
        storage_provider_definition_version = excluded.storage_provider_definition_version,
        name                                = excluded.name,
        description                         = excluded.description,
        type                                = excluded.type,
        status                              = excluded.status,
        status_message                      = excluded.status_message,
        read_only_storage                   = excluded.read_only_storage,
        configuration                       = excluded.configuration,
        max_file_size_in_bytes              = excluded.max_file_size_in_bytes,
        system_provider                     = excluded.system_provider,
        test_provider                       = excluded.test_provider,
        metadata_attributes                 = excluded.metadata_attributes,
        last_sync                           = excluded.last_sync,
        created                             = excluded.created,
        updated                             = excluded.updated;

-- fix id sequence for the storage providers
select setval('storage_providers_id_seq',
              (select max(id) from storage_providers));