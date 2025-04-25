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
        'sh_id',
        3,
        'Servicekonto Schleswig-Holstein',
        'Konfiguration für die Produktionsumgebung des Servicekonto Schleswig-Holstein.',
        null,
        '/idp/realms/customer/protocol/openid-connect/auth',
        '/idp/realms/customer/protocol/openid-connect/token',
        '/idp/realms/customer/protocol/openid-connect/userinfo',
        '/idp/realms/customer/protocol/openid-connect/logout',
        'app',
        null,
        '[
            {
                "label": "Identitätstyp",
                "description": "Identitätstyp",
                "keyInData": "dataport_identitaetstyp",
                "displayAttribute": true
            },
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
                "label": "Geschlecht",
                "description": "Geschlecht",
                "keyInData": "gender",
                "displayAttribute": false
            },
            {
                "label": "Staatsangehörigkeit",
                "description": "Staatsangehörigkeit",
                "keyInData": "nationality",
                "displayAttribute": false
            },
            {
                "label": "Anschrift Privatperson: Straße und Hausnummer",
                "description": "Straße und Hausnummer",
                "keyInData": "street_and_building",
                "displayAttribute": false
            },
            {
                "label": "Anschrift Privatperson: Postleitzahl",
                "description": "Postleitzahl",
                "keyInData": "zip_code",
                "displayAttribute": false
            },
            {
                "label": "Anschrift Privatperson: Ort",
                "description": "Ort",
                "keyInData": "city",
                "displayAttribute": false
            },
            {
                "label": "Anschrift Privatperson: Land",
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
                "label": "Mobiltelefonnummer",
                "description": "Mobiltelefonnummer",
                "keyInData": "mobilephone",
                "displayAttribute": false
            },
            {
                "label": "Favorisierte Sprache",
                "description": "Favorisierte Sprache",
                "keyInData": "preferred_language",
                "displayAttribute": false
            },
            {
                "label": "Firmenname",
                "description": "Firmenname",
                "keyInData": "org_company_name",
                "displayAttribute": false
            },
            {
                "label": "Unternehmenseinheit",
                "description": "Unternehmenseinheit",
                "keyInData": "org_company_unit",
                "displayAttribute": false
            },
            {
                "label": "Registernummer",
                "description": "Registernummer",
                "keyInData": "org_register_number",
                "displayAttribute": false
            },
            {
                "label": "Registergericht",
                "description": "Registergericht",
                "keyInData": "org_register_court",
                "displayAttribute": false
            },
            {
                "label": "Unternehmen: E-Mail-Adresse",
                "description": "Unternehmen: E-Mail-Adresse",
                "keyInData": "org_contact_mail",
                "displayAttribute": false
            },
            {
                "label": "Unternehmen: Telefonnummer",
                "description": "Unternehmen: Telefonnummer",
                "keyInData": "org_contact_phone",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Straße und Hausnummer",
                "description": "Unternehmensanschrift: Straße und Hausnummer",
                "keyInData": "org_street_and_building",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Postleitzahl",
                "description": "Unternehmensanschrift: Postleitzahl",
                "keyInData": "org_zip_code",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Ort",
                "description": "Unternehmensanschrift: Ort",
                "keyInData": "org_city",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Land",
                "description": "Unternehmensanschrift: Land",
                "keyInData": "org_country",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Postfach-Nummer",
                "description": "Unternehmensanschrift: Postfach-Nummer",
                "keyInData": "org_postbox_number",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Postfach-Postleitzahl",
                "description": "Unternehmensanschrift: Postfach-Postleitzahl",
                "keyInData": "org_postbox_zip_code",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Postfach-Land",
                "description": "Unternehmensanschrift: Postfach-Land",
                "keyInData": "org_postbox_country",
                "displayAttribute": false
            },
            {
                "label": "Datenübermittler Pseudonym ID",
                "description": "Datenübermittler Pseudonym ID",
                "keyInData": "elster_datenuebermittler",
                "displayAttribute": true
            },
            {
                "label": "Vertrauensniveau",
                "description": "Vertrauensniveau",
                "keyInData": "trust_level_authentication",
                "displayAttribute": true
            },
            {
                "label": "Servicekonto ID",
                "description": "Servicekonto ID",
                "keyInData": "preferred_username",
                "displayAttribute": false
            },
            {
                "label": "Postfach",
                "description": "Postfach",
                "keyInData": "dataport_inbox_id",
                "displayAttribute": true
            },
            {
                "label": "Servicekontotyp",
                "description": "Servicekontotyp",
                "keyInData": "dataport_servicekonto_type",
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
                "value": "schleswigholstein"
            }
        ]',
        exists(select 1 from system_configs where key = 'SHActive' and value = 'true'),
        false);

update forms as fms
set identity_providers = fms.identity_providers || json_build_array(json_build_object(
        'identityProviderKey', (select idps.key from identity_providers as idps where idps.type = 3 limit 1),
        'additionalScopes', json_build_array('level' || fms.sh_id_level)))::jsonb
where fms.sh_id_enabled;

