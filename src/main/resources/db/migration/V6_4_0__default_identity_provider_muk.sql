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
        'muk',
        4,
        'Mein Unternehmenskonto',
        'Konfiguration für die Produktionsumgebung des Mein Unternehmenskonto (MUK).',
        null,
        '/idp/realms/customer/protocol/openid-connect/auth',
        '/idp/realms/customer/protocol/openid-connect/token',
        '/idp/realms/customer/protocol/openid-connect/userinfo',
        '/idp/realms/customer/protocol/openid-connect/logout',
        'app',
        null,
        '[
            {
                "label": "Identitätstyp (PersTyp)",
                "description": "Identitätstyp (PersTyp)",
                "keyInData": "elster_identitaetstyp",
                "displayAttribute": false
            },
            {
                "label": "Firmenname",
                "description": "Firmenname",
                "keyInData": "org_company_name",
                "displayAttribute": false
            },
            {
                "label": "Rechtsform (RechtsformText)",
                "description": "Rechtsform (RechtsformText)",
                "keyInData": "org_legal_form_text",
                "displayAttribute": false
            },
            {
                "label": "Rechtsform Code (Rechtsform)",
                "description": "Rechtsform Code (Rechtsform)",
                "keyInData": "org_legal_form_code",
                "displayAttribute": false
            },
            {
                "label": "Tätigkeit (TaetigkeitText)",
                "description": "Tätigkeit (TaetigkeitText)",
                "keyInData": "org_occupation_text",
                "displayAttribute": false
            },
            {
                "label": "Tätigkeit Code (Taetigkeit)",
                "description": "Tätigkeit Code (Taetigkeit)",
                "keyInData": "org_occupation_code",
                "displayAttribute": false
            },
            {
                "label": "Registernummer",
                "description": "Registernummer",
                "keyInData": "org_register_number",
                "displayAttribute": false
            },
            {
                "label": "Registerart",
                "description": "Registerart",
                "keyInData": "org_register_type",
                "displayAttribute": false
            },
            {
                "label": "Registergericht",
                "description": "Registergericht",
                "keyInData": "org_register_court",
                "displayAttribute": false
            },
            {
                "label": "Handelnde Person: Namensvorsatz",
                "description": "Handelnde Person: Namensvorsatz",
                "keyInData": "salutation",
                "displayAttribute": false
            },
            {
                "label": "Handelnde Person: Akademischer Grad",
                "description": "Handelnde Person: Akademischer Grad",
                "keyInData": "title",
                "displayAttribute": false
            },
            {
                "label": "Handelnde Person: Vorname(n)",
                "description": "Handelnde Person: Vorname(n)",
                "keyInData": "given_name",
                "displayAttribute": false
            },
            {
                "label": "Handelnde Person: Nachname",
                "description": "Handelnde Person: Nachname",
                "keyInData": "family_name",
                "displayAttribute": false
            },
            {
                "label": "Handelnde Person: Vollständiger Name",
                "description": "Handelnde Person: Vollständiger Name",
                "keyInData": "name",
                "displayAttribute": false
            },
            {
                "label": "Handelnde Person: Namenszusatz",
                "description": "Handelnde Person: Namenszusatz",
                "keyInData": "zusatz_name",
                "displayAttribute": false
            },
            {
                "label": "Handelnde Person: Geburtsdatum",
                "description": "Handelnde Person: Geburtsdatum",
                "keyInData": "date_of_birth",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Typ",
                "description": "Unternehmensanschrift: Typ",
                "keyInData": "org_address_type",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Straße",
                "description": "Unternehmensanschrift: Straße",
                "keyInData": "org_street",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Hausnummer",
                "description": "Unternehmensanschrift: Hausnummer",
                "keyInData": "org_building",
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
                "label": "Unternehmensanschrift: Ortsteil",
                "description": "Unternehmensanschrift: Ortsteil",
                "keyInData": "org_city_district",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Adressergänzung",
                "description": "Unternehmensanschrift: Adressergänzung",
                "keyInData": "org_address_addition",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Land",
                "description": "Unternehmensanschrift: Land",
                "keyInData": "org_country",
                "displayAttribute": false
            },
            {
                "label": "ELSTER ID",
                "description": "ELSTER ID",
                "keyInData": "preferred_username",
                "displayAttribute": true
            },
            {
                "label": "Datenübermittler Pseudonym ID",
                "description": "Datenübermittler Pseudonym ID",
                "keyInData": "elster_datenuebermittler",
                "displayAttribute": true
            },
            {
                "label": "Datenkranz Typ",
                "description": "Datenkranz Typ",
                "keyInData": "elster_datenkranz_typ",
                "displayAttribute": true
            },
            {
                "label": "Vertrauensniveau Identifizierung (ElsterVertrauensniveauIdentifizierung)",
                "description": "Vertrauensniveau Identifizierung (ElsterVertrauensniveauIdentifizierung)",
                "keyInData": "trust_level_identification",
                "displayAttribute": false
            },
            {
                "label": "Vertrauensniveau Authentifizierung (ElsterVertrauensniveauAuthentifizierung)",
                "description": "Vertrauensniveau Authentifizierung (ElsterVertrauensniveauAuthentifizierung)",
                "keyInData": "trust_level_authentication",
                "displayAttribute": true
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
                "value": "muk"
            }
        ]',
        exists(select 1 from system_configs where key = 'MukActive' and value = 'true'),
        false);

update forms as fms
set identity_providers = fms.identity_providers || json_build_array(json_object(
        'identityProviderKey' : (select idps.key from identity_providers as idps where idps.type = 3 limit 1),
        'additionalScopes' : json_build_array()))::jsonb
where fms.muk_enabled;
