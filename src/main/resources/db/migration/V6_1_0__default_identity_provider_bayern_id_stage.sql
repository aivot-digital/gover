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
        'bayern_id',
        1,
        'BayernID (Vorproduktion)',
        'Konfiguration für die Vorproduktionsumgebung der BayernID.',
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
                "description": "Formelle Anrede, z. B. Herr, Frau oder Divers.",
                "keyInData": "salutation",
                "displayAttribute": false
            },
            {
                "label": "Akademischer Titel",
                "description": "Akademischer Grad wie Dr. oder Prof.",
                "keyInData": "title",
                "displayAttribute": false
            },
            {
                "label": "Vorname(n)",
                "description": "Ruf- und ggf. weitere Vornamen der Person.",
                "keyInData": "given_name",
                "displayAttribute": false
            },
            {
                "label": "Nachname",
                "description": "Familien- bzw. Nachname der Person.",
                "keyInData": "family_name",
                "displayAttribute": false
            },
            {
                "label": "Vollständiger Name",
                "description": "Zusammengesetzter kompletter Name aus Vor- und Nachnamen.",
                "keyInData": "name",
                "displayAttribute": false
            },
            {
                "label": "Geburtsdatum",
                "description": "Datum, an welchem die Person geboren wurde.",
                "keyInData": "date_of_birth",
                "displayAttribute": false
            },
            {
                "label": "Geburtsort",
                "description": "Ort, an dem die Person geboren wurde.",
                "keyInData": "place_of_birth",
                "displayAttribute": false
            },
            {
                "label": "Geburtsname",
                "description": "Familienname zum Zeitpunkt der Geburt (falls abweichend).",
                "keyInData": "birth_name",
                "displayAttribute": false
            },
            {
                "label": "Staatsangehörigkeit",
                "description": "Aktuelle Staatsbürgerschaft, i. d. R. als Ländercode (z. B. DE).",
                "keyInData": "nationality",
                "displayAttribute": false
            },
            {
                "label": "Straße und Hausnummer",
                "description": "Wohnanschrift: Straßenname und Hausnummer.",
                "keyInData": "street_and_building",
                "displayAttribute": false
            },
            {
                "label": "Postleitzahl",
                "description": "Postleitzahl der Wohnanschrift.",
                "keyInData": "zip_code",
                "displayAttribute": false
            },
            {
                "label": "Ort",
                "description": "Wohnort bzw. Stadt.",
                "keyInData": "city",
                "displayAttribute": false
            },
            {
                "label": "Land",
                "description": "Land der Wohnanschrift (ISO 3166-1 alpha-2).",
                "keyInData": "country",
                "displayAttribute": false
            },
            {
                "label": "E-Mail-Adresse",
                "description": "Vom Nutzenden hinterlegte E-Mail-Adresse zur Kontaktaufnahme.",
                "keyInData": "email",
                "displayAttribute": false
            },
            {
                "label": "Telefonnummer",
                "description": "Telefon- oder Mobilnummer im internationalen Format.",
                "keyInData": "telephone",
                "displayAttribute": false
            },
            {
                "label": "bPK2",
                "description": "Bereichsspezifisches Personenkennzeichen – pseudonymisierte, pro Service eindeutige ID der Person.",
                "keyInData": "bPK2",
                "displayAttribute": true
            },
            {
                "label": "Postfach",
                "description": "Technischer Postkorb-Handle (UUID) zum Zustellen von BayernID-Nachrichten.",
                "keyInData": "legacy_postkorb_handle",
                "displayAttribute": true
            },
            {
                "label": "Vertrauensniveau",
                "description": "Qualitätsstufe der Authentifizierung (z. B. STORK-QAA-Level 1–4).",
                "keyInData": "trust_level_authentication",
                "displayAttribute": true
            },
            {
                "label": "Dokumententyp",
                "description": "Art des verwendeten Ausweisdokuments, z. B. ID (Personalausweis) oder P (Reisepass).",
                "keyInData": "document_type",
                "displayAttribute": false
            },
            {
                "label": "eIDAS-Issuing-Country",
                "description": "Land, das das eID-/eIDAS-Identitätsmittel ausgestellt hat (ISO 3166-1 alpha-2).",
                "keyInData": "e_idas_issuing_country",
                "displayAttribute": false
            },
            {
                "label": "AssertionProvedBy",
                "description": "Quelle, die die Identität geprüft hat (z. B. eIDAS, eID, ELSTER).",
                "keyInData": "assertion_proved_by",
                "displayAttribute": true
            },
            {
                "label": "Version",
                "description": "Version der BayernID-Schnittstelle (Calendar Versioning, z. B. 2025.4.17).",
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
                "value": "bayernid-stage"
            }
        ]',
        exists(select 1 from system_configs where key = 'BayernIDActive' and value = 'true'),
        true);
