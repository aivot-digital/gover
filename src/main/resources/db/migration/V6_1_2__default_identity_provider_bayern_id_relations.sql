update forms as fms
set identity_providers = fms.identity_providers || json_build_array(json_build_object(
        'identityProviderKey', (select idps.key from identity_providers as idps where idps.type = 1 limit 1),
        'additionalScopes', json_build_array('level' || fms.bayern_id_level)))::jsonb
where fms.bayern_id_enabled;
