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
        'Mein Unternehmenskonto Produktivsystem',
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
                "keyInData": "elster_identitaetstyp"
            },
            {
                "label": "Firmenname",
                "description": "Firmenname",
                "keyInData": "org_company_name"
            },
            {
                "label": "Rechtsform (RechtsformText)",
                "description": "Rechtsform (RechtsformText)",
                "keyInData": "org_legal_form_text"
            },
            {
                "label": "Rechtsform Code (Rechtsform)",
                "description": "Rechtsform Code (Rechtsform)",
                "keyInData": "org_legal_form_code"
            },
            {
                "label": "Tätigkeit (TaetigkeitText)",
                "description": "Tätigkeit (TaetigkeitText)",
                "keyInData": "org_occupation_text"
            },
            {
                "label": "Tätigkeit Code (Taetigkeit)",
                "description": "Tätigkeit Code (Taetigkeit)",
                "keyInData": "org_occupation_code"
            },
            {
                "label": "Registernummer",
                "description": "Registernummer",
                "keyInData": "org_register_number"
            },
            {
                "label": "Registerart",
                "description": "Registerart",
                "keyInData": "org_register_type"
            },
            {
                "label": "Registergericht",
                "description": "Registergericht",
                "keyInData": "org_register_court"
            },
            {
                "label": "Handelnde Person: Namensvorsatz",
                "description": "Handelnde Person: Namensvorsatz",
                "keyInData": "salutation"
            },
            {
                "label": "Handelnde Person: Akademischer Grad",
                "description": "Handelnde Person: Akademischer Grad",
                "keyInData": "title"
            },
            {
                "label": "Handelnde Person: Vorname(n)",
                "description": "Handelnde Person: Vorname(n)",
                "keyInData": "given_name"
            },
            {
                "label": "Handelnde Person: Nachname",
                "description": "Handelnde Person: Nachname",
                "keyInData": "family_name"
            },
            {
                "label": "Handelnde Person: Vollständiger Name",
                "description": "Handelnde Person: Vollständiger Name",
                "keyInData": "name"
            },
            {
                "label": "Handelnde Person: Namenszusatz",
                "description": "Handelnde Person: Namenszusatz",
                "keyInData": "zusatz_name"
            },
            {
                "label": "Handelnde Person: Geburtsdatum",
                "description": "Handelnde Person: Geburtsdatum",
                "keyInData": "date_of_birth"
            },
            {
                "label": "Unternehmensanschrift: Typ",
                "description": "Unternehmensanschrift: Typ",
                "keyInData": "org_address_type"
            },
            {
                "label": "Unternehmensanschrift: Straße",
                "description": "Unternehmensanschrift: Straße",
                "keyInData": "org_street"
            },
            {
                "label": "Unternehmensanschrift: Hausnummer",
                "description": "Unternehmensanschrift: Hausnummer",
                "keyInData": "org_building"
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
                "label": "Unternehmensanschrift: Ortsteil",
                "description": "Unternehmensanschrift: Ortsteil",
                "keyInData": "org_city_district"
            },
            {
                "label": "Unternehmensanschrift: Adressergänzung",
                "description": "Unternehmensanschrift: Adressergänzung",
                "keyInData": "org_address_addition"
            },
            {
                "label": "Unternehmensanschrift: Land",
                "description": "Unternehmensanschrift: Land",
                "keyInData": "org_country"
            },
            {
                "label": "ELSTER ID",
                "description": "ELSTER ID",
                "keyInData": "preferred_username"
            },
            {
                "label": "Datenübermittler Pseudonym ID",
                "description": "Datenübermittler Pseudonym ID",
                "keyInData": "elster_datenuebermittler"
            },
            {
                "label": "Datenkranz Typ",
                "description": "Datenkranz Typ",
                "keyInData": "elster_datenkranz_typ"
            },
            {
                "label": "Vertrauensniveau Identifizierung (ElsterVertrauensniveauIdentifizierung)",
                "description": "Vertrauensniveau Identifizierung (ElsterVertrauensniveauIdentifizierung)",
                "keyInData": "trust_level_identification"
            },
            {
                "label": "Vertrauensniveau Authentifizierung (ElsterVertrauensniveauAuthentifizierung)",
                "description": "Vertrauensniveau Authentifizierung (ElsterVertrauensniveauAuthentifizierung)",
                "keyInData": "trust_level_authentication"
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
