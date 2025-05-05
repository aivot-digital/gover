update forms as fms
set sh_id_enabled = true
where (select exists(select 1
                     from jsonb_array_elements(fms.identity_providers) as item
                     where item ->> 'identityProviderKey' = (select key
                                                             from identity_providers as idps
                                                             where idps.type = 3 and idps.is_test_provider is false)::text));
