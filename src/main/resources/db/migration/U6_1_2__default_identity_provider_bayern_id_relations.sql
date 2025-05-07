update forms as fms
set bayern_id_enabled = true
where (select exists(select 1
                     from jsonb_array_elements(fms.identity_providers) as item
                     where item ->> 'identityProviderKey' = (select key
                                                             from identity_providers as idps
                                                             where idps.type = 1 and idps.is_test_provider is false)::text));
