update forms as fms
set identity_providers = fms.identity_providers || json_build_array(json_build_object(
        'identityProviderKey', (select idps.key from identity_providers as idps where idps.type = 3 and idps.is_test_provider = False limit 1),
        'additionalScopes', json_build_array()))::jsonb
where fms.muk_enabled;
