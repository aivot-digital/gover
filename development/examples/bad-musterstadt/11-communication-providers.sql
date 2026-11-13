-- create example communication providers for e-mail and the central citizen mailbox (ZBP) via FIT-Connect
-- The ZBP provider stays disabled: its destination IDs, FIT-Connect client credentials (secret) and certificates
-- (assets) cannot be created via SQL and must be configured in the communication provider settings.
insert into communication_providers (id,
                                     communication_provider_definition_key,
                                     communication_provider_definition_version,
                                     name,
                                     description,
                                     configuration,
                                     is_enabled,
                                     is_test_provider)
values (1,
        'mail_communication_provider',
        1,
        'E-Mail',
        'Versand von Nachrichten über den globalen Standardabsender',
        '{
          "senderMode": {
            "type": "Literal",
            "value": "default"
          }
        }'::jsonb,
        true,
        false),
       (2,
        'fit_connect_zbp_communication_provider',
        1,
        'Zentrales Bürgerpostfach (Vorproduktion)',
        'Versand von Nachrichten an das BundID-Postfach über FIT-Connect',
        '{}'::jsonb,
        false,
        true)
on conflict (id) do update
    set communication_provider_definition_key     = excluded.communication_provider_definition_key,
        communication_provider_definition_version = excluded.communication_provider_definition_version,
        name                                      = excluded.name,
        description                               = excluded.description,
        configuration                             = excluded.configuration,
        is_enabled                                = excluded.is_enabled,
        is_test_provider                          = excluded.is_test_provider;

-- fix id sequence for communication providers
select setval('communication_providers_id_seq',
              (select max(id) from communication_providers));

-- bind the providers to every system identity provider (production and Vorproduktion) seeded by the backend on
-- startup that offers the attribute the provider needs. Identity provider types: 1 = BayernID, 2 = BundID,
-- 3 = Servicekonto Schleswig-Holstein. Mein Unternehmenskonto (4) provides no e-mail address and is not bound.
-- The identity provider keys are random, so existing bindings are matched by identity and communication provider.
insert into communication_provider_bindings (identity_provider_key,
                                             communication_provider_id,
                                             name,
                                             description,
                                             is_enabled,
                                             position,
                                             configuration)
select identity_providers.key,
       binding.communication_provider_id,
       binding.name,
       binding.description,
       binding.is_enabled,
       binding.position,
       binding.configuration
from (values (array [1, 2, 3],
              1,
              'E-Mail',
              'Nachrichten an die im Konto hinterlegte E-Mail-Adresse',
              true,
              1,
              '{
                "emailAttribute": {
                  "type": "Literal",
                  "value": "email"
                }
              }'::jsonb),
             -- the ZBP is the mailbox of the BundID; other identity providers use their own mailbox systems
             (array [2],
              2,
              'BundID-Postfach',
              'Nachrichten an das Postfach des BundID-Kontos',
              false,
              0,
              '{
                "bpk2Attribute": {
                  "type": "Literal",
                  "value": "legacy_postkorb_handle"
                },
                "storkQaaLevel": {
                  "type": "Literal",
                  "value": "trust_level_authentication"
                }
              }'::jsonb)) as binding (identity_provider_types,
                                      communication_provider_id,
                                      name,
                                      description,
                                      is_enabled,
                                      position,
                                      configuration)
         join identity_providers
              on identity_providers.type = any (binding.identity_provider_types)
where not exists (select 1
                  from communication_provider_bindings existing
                  where existing.identity_provider_key = identity_providers.key
                    and existing.communication_provider_id = binding.communication_provider_id);
