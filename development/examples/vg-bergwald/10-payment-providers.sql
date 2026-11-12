-- create an example ePayBL payment provider for the ePayBL staging environment
-- The client certificate (asset) and its password (secret) cannot be created via SQL. Select them in the
-- payment provider settings and enter the originator and endpoint IDs issued by ePayBL before enabling it.
insert into payment_providers (key,
                               payment_provider_definition_key,
                               payment_provider_definition_version,
                               name,
                               description,
                               config,
                               is_test_provider,
                               is_enabled)
values ('5c6f2a8e-3d41-4b7a-9e0c-1f2d3a4b5c6d',
        'epaybl',
        1,
        'ePayBL (Vorproduktion)',
        'Zahlungsabwicklung über die Vorproduktionsumgebung von ePayBL',
        '{
          "paymentTransactionUrl": {
            "type": "Literal",
            "value": "https://epayment-stage.dataport.de/konnektor/epayment/"
          }
        }'::jsonb,
        true,
        false)
on conflict (key) do update
    set payment_provider_definition_key     = excluded.payment_provider_definition_key,
        payment_provider_definition_version = excluded.payment_provider_definition_version,
        name                                = excluded.name,
        description                         = excluded.description,
        config                              = excluded.config,
        is_test_provider                    = excluded.is_test_provider,
        is_enabled                          = excluded.is_enabled;
