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
        'Konfiguration für die Produktionsumgebung der BundID.',
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
                "keyInData": "salutation",
                "displayAttribute": false
            },
            {
                "label": "Akademischer Titel",
                "description": "Akademischer Titel",
                "keyInData": "title",
                "displayAttribute": false
            },
            {
                "label": "Vorname(n)",
                "description": "Vorname(n)",
                "keyInData": "given_name",
                "displayAttribute": false
            },
            {
                "label": "Nachname",
                "description": "Nachname",
                "keyInData": "family_name",
                "displayAttribute": false
            },
            {
                "label": "Vollständiger Name",
                "description": "Vollständiger Name",
                "keyInData": "name",
                "displayAttribute": false
            },
            {
                "label": "Geburtsdatum",
                "description": "Geburtsdatum",
                "keyInData": "date_of_birth",
                "displayAttribute": false
            },
            {
                "label": "Geburtsort",
                "description": "Geburtsort",
                "keyInData": "place_of_birth",
                "displayAttribute": false
            },
            {
                "label": "Geburtsname",
                "description": "Geburtsname",
                "keyInData": "birth_name",
                "displayAttribute": false
            },
            {
                "label": "Staatsangehörigkeit",
                "description": "Staatsangehörigkeit",
                "keyInData": "nationality",
                "displayAttribute": false
            },
            {
                "label": "Straße und Hausnummer",
                "description": "Straße und Hausnummer",
                "keyInData": "street_and_building",
                "displayAttribute": false
            },
            {
                "label": "Postleitzahl",
                "description": "Postleitzahl",
                "keyInData": "zip_code",
                "displayAttribute": false
            },
            {
                "label": "Ort",
                "description": "Ort",
                "keyInData": "city",
                "displayAttribute": false
            },
            {
                "label": "Land",
                "description": "Land",
                "keyInData": "country",
                "displayAttribute": false
            },
            {
                "label": "E-Mail-Adresse",
                "description": "E-Mail-Adresse",
                "keyInData": "email",
                "displayAttribute": false
            },
            {
                "label": "Telefonnummer",
                "description": "Telefonnummer",
                "keyInData": "telephone",
                "displayAttribute": false
            },
            {
                "label": "bPK2",
                "description": "bPK2",
                "keyInData": "bPK2",
                "displayAttribute": true
            },
            {
                "label": "Postfach",
                "description": "Postfach",
                "keyInData": "legacy_postkorb_handle",
                "displayAttribute": true
            },
            {
                "label": "Vertrauensniveau",
                "description": "Vertrauensniveau",
                "keyInData": "trust_level_authentication",
                "displayAttribute": true
            },
            {
                "label": "Dokumententyp",
                "description": "Dokumententyp",
                "keyInData": "document_type",
                "displayAttribute": false
            },
            {
                "label": "eIDAS-Issuing-Country",
                "description": "eIDAS-Issuing-Country",
                "keyInData": "e_idas_issuing_country",
                "displayAttribute": false
            },
            {
                "label": "AssertionProvedBy",
                "description": "AssertionProvedBy",
                "keyInData": "assertion_proved_by",
                "displayAttribute": true
            },
            {
                "label": "Version",
                "description": "Version",
                "keyInData": "api_version",
                "displayAttribute": false
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
        exists(select 1 from system_configs where key = 'BundIDActive' and value = 'true'),
        false);

update forms as fms
set identity_providers = fms.identity_providers || json_build_array(json_build_object(
        'identityProviderKey', (select idps.key from identity_providers as idps where idps.type = 2 limit 1),
        'additionalScopes', json_build_array('level' || fms.bund_id_level)))::jsonb
where fms.bund_id_enabled;