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
        'Servicekonto Schleswig-Holstein Produktivsystem',
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
                "keyInData": "dataport_identitaetstyp"
            },
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
                "label": "Geschlecht",
                "description": "Geschlecht",
                "keyInData": "gender"
            },
            {
                "label": "Staatsangehörigkeit",
                "description": "Staatsangehörigkeit",
                "keyInData": "nationality"
            },
            {
                "label": "Anschrift Privatperson: Straße und Hausnummer",
                "description": "Straße und Hausnummer",
                "keyInData": "street_and_building"
            },
            {
                "label": "Anschrift Privatperson: Postleitzahl",
                "description": "Postleitzahl",
                "keyInData": "zip_code"
            },
            {
                "label": "Anschrift Privatperson: Ort",
                "description": "Ort",
                "keyInData": "city"
            },
            {
                "label": "Anschrift Privatperson: Land",
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
                "label": "Mobiltelefonnummer",
                "description": "Mobiltelefonnummer",
                "keyInData": "mobilephone"
            },
            {
                "label": "Favorisierte Sprache",
                "description": "Favorisierte Sprache",
                "keyInData": "preferred_language"
            },
            {
                "label": "Firmenname",
                "description": "Firmenname",
                "keyInData": "org_company_name"
            },
            {
                "label": "Unternehmenseinheit",
                "description": "Unternehmenseinheit",
                "keyInData": "org_company_unit"
            },
            {
                "label": "Registernummer",
                "description": "Registernummer",
                "keyInData": "org_register_number"
            },
            {
                "label": "Registergericht",
                "description": "Registergericht",
                "keyInData": "org_register_court"
            },
            {
                "label": "Unternehmen: E-Mail-Adresse",
                "description": "Unternehmen: E-Mail-Adresse",
                "keyInData": "org_contact_mail"
            },
            {
                "label": "Unternehmen: Telefonnummer",
                "description": "Unternehmen: Telefonnummer",
                "keyInData": "org_contact_phone"
            },
            {
                "label": "Unternehmensanschrift: Straße und Hausnummer",
                "description": "Unternehmensanschrift: Straße und Hausnummer",
                "keyInData": "org_street_and_building"
            },
            {
                "label": "Unternehmensanschrift: Postleitzahl",
                "description": "Unternehmensanschrift: Postleitzahl",
                "keyInData": "org_zip_code"
            },
            {
                "label": "Unternehmensanschrift: Ort",
                "description": "Unternehmensanschrift: Ort",
                "keyInData": "org_city"
            },
            {
                "label": "Unternehmensanschrift: Land",
                "description": "Unternehmensanschrift: Land",
                "keyInData": "org_country"
            },
            {
                "label": "Unternehmensanschrift: Postfach-Nummer",
                "description": "Unternehmensanschrift: Postfach-Nummer",
                "keyInData": "org_postbox_number"
            },
            {
                "label": "Unternehmensanschrift: Postfach-Postleitzahl",
                "description": "Unternehmensanschrift: Postfach-Postleitzahl",
                "keyInData": "org_postbox_zip_code"
            },
            {
                "label": "Unternehmensanschrift: Postfach-Land",
                "description": "Unternehmensanschrift: Postfach-Land",
                "keyInData": "org_postbox_country"
            },
            {
                "label": "Datenübermittler Pseudonym ID",
                "description": "Datenübermittler Pseudonym ID",
                "keyInData": "elster_datenuebermittler"
            },
            {
                "label": "Vertrauensniveau",
                "description": "Vertrauensniveau",
                "keyInData": "trust_level_authentication"
            },
            {
                "label": "Servicekonto ID",
                "description": "Servicekonto ID",
                "keyInData": "preferred_username"
            },
            {
                "label": "Postfach",
                "description": "Postfach",
                "keyInData": "dataport_inbox_id"
            },
            {
                "label": "Servicekontotyp",
                "description": "Servicekontotyp",
                "keyInData": "dataport_servicekonto_type"
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
set identity_providers = fms.identity_providers || json_build_array(json_object(
        array['identityProviderKey', 'additionalScopes'],
        array[(select idps.key from identity_providers as idps where idps.type = 3 limit 1)::varchar, 'level' || fms.sh_id_level]))::jsonb
where fms.sh_id_enabled;

