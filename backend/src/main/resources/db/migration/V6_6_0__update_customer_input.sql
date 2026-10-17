update submissions
set customer_input = jsonb_set(customer_input, '{__id_data__}', customer_input -> '__id_data__' || jsonb_build_object(
        'identityProviderKey', (select key from identity_providers where type = 1 limit 1),
        'metadataIdentifier', (select metadata_identifier from identity_providers where type = 1 limit 1)))
where customer_input -> '__id_data__' is not null
  and customer_input -> '__id_data__' ->> 'idp' = 'bayern_id';

update submissions
set customer_input = jsonb_set(customer_input, '{__id_data__}', customer_input -> '__id_data__' || jsonb_build_object(
        'identityProviderKey', (select key from identity_providers where type = 2 limit 1),
        'metadataIdentifier', (select metadata_identifier from identity_providers where type = 2 limit 1)))
where customer_input -> '__id_data__' is not null
  and customer_input -> '__id_data__' ->> 'idp' = 'bund_id';

update submissions
set customer_input = jsonb_set(customer_input, '{__id_data__}', customer_input -> '__id_data__' || jsonb_build_object(
        'identityProviderKey', (select key from identity_providers where type = 3 limit 1),
        'metadataIdentifier', (select metadata_identifier from identity_providers where type = 3 limit 1)))
where customer_input -> '__id_data__' is not null
  and customer_input -> '__id_data__' ->> 'idp' = 'sh_id';

update submissions
set customer_input = jsonb_set(customer_input, '{__id_data__}', customer_input -> '__id_data__' || jsonb_build_object(
        'identityProviderKey', (select key from identity_providers where type = 4 limit 1),
        'metadataIdentifier', (select metadata_identifier from identity_providers where type = 4 limit 1)))
where customer_input -> '__id_data__' is not null
  and customer_input -> '__id_data__' ->> 'idp' = 'muk';
