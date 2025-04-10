-- create default identity providers
insert into identity_providers (key,
                                metadata_identifier,
                                type,
                                name,
                                description,
                                icon_asset_key,
                                authorization_endpoint,
                                token_endpoint,
                                userinfo_endpoint,
                                end_session_endpoint,
                                client_id,
                                client_secret_key,
                                attributes,
                                default_scopes,
                                additional_params,
                                is_enabled,
                                is_test_provider)
values (gen_random_uuid(),
        'bund_id',
        2,
        'BundID',
        'BundID Produktivsystem',
        null,
        '/idp/realms/customer/protocol/openid-connect/auth',
        '/idp/realms/customer/protocol/openid-connect/token',
        '/idp/realms/customer/protocol/openid-connect/userinfo',
        '/idp/realms/customer/protocol/openid-connect/logout',
        'app',
        null,
        '[
            {
                "label": "Anrede",
                "description": "Anrede",
                "keyInData": "salutation"
            },
            {
                "label": "Akademischer Titel",
                "description": "Akademischer Titel",
                "keyInData": "title"
            },
            {
                "label": "Vorname(n)",
                "description": "Vorname(n)",
                "keyInData": "given_name"
            },
            {
                "label": "Nachname",
                "description": "Nachname",
                "keyInData": "family_name"
            },
            {
                "label": "Vollständiger Name",
                "description": "Vollständiger Name",
                "keyInData": "name"
            },
            {
                "label": "Geburtsdatum",
                "description": "Geburtsdatum",
                "keyInData": "date_of_birth"
            },
            {
                "label": "Geburtsort",
                "description": "Geburtsort",
                "keyInData": "place_of_birth"
            },
            {
                "label": "Geburtsname",
                "description": "Geburtsname",
                "keyInData": "birth_name"
            },
            {
                "label": "Staatsangehörigkeit",
                "description": "Staatsangehörigkeit",
                "keyInData": "nationality"
            },
            {
                "label": "Straße und Hausnummer",
                "description": "Straße und Hausnummer",
                "keyInData": "street_and_building"
            },
            {
                "label": "Postleitzahl",
                "description": "Postleitzahl",
                "keyInData": "zip_code"
            },
            {
                "label": "Ort",
                "description": "Ort",
                "keyInData": "city"
            },
            {
                "label": "Land",
                "description": "Land",
                "keyInData": "country"
            },
            {
                "label": "E-Mail-Adresse",
                "description": "E-Mail-Adresse",
                "keyInData": "email"
            },
            {
                "label": "Telefonnummer",
                "description": "Telefonnummer",
                "keyInData": "telephone"
            },
            {
                "label": "bPK2",
                "description": "bPK2",
                "keyInData": "bPK2"
            },
            {
                "label": "Postfach",
                "description": "Postfach",
                "keyInData": "legacy_postkorb_handle"
            },
            {
                "label": "Vertrauensniveau",
                "description": "Vertrauensniveau",
                "keyInData": "trust_level_authentication"
            },
            {
                "label": "Dokumententyp",
                "description": "Dokumententyp",
                "keyInData": "document_type"
            },
            {
                "label": "eIDAS-Issuing-Country",
                "description": "eIDAS-Issuing-Country",
                "keyInData": "e_idas_issuing_country"
            },
            {
                "label": "AssertionProvedBy",
                "description": "AssertionProvedBy",
                "keyInData": "assertion_proved_by"
            },
            {
                "label": "Version",
                "description": "Version",
                "keyInData": "api_version"
            }
        ]',
        '[
            "email",
            "profile",
            "openid"
        ]',
        '[
            {
                "key": "kc_idp_hint",
                "value": "bundid"
            }
        ]',
        exists(select 1 from system_configs where key = 'bundIDActive' and value = 'true'),
        false);

update forms as fms
set identity_providers = fms.identity_providers || json_build_array(json_object(
        'identityProviderKey' : (select idps.key from identity_providers as idps where idps.type = 2 limit 1),
        'additionalScopes' : json_build_array('level' || fms.bund_id_level)))::jsonb
where fms.bund_id_enabled;

