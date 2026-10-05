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
        'Mein Unternehmenskonto (Vorproduktion)',
        'Konfiguration für die Vorproduktionsumgebung des Mein Unternehmenskonto (MUK).',
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
                "description": "Kennzeichnet, ob es sich um eine natürliche Person (NatPers) oder um eine juristische/nicht-natürliche Person (NNatPers) handelt.",
                "keyInData": "elster_identitaetstyp",
                "displayAttribute": false
            },
            {
                "label": "Firmenname",
                "description": "Offiziell im Steuerkonto hinterlegter Name der Organisation (bzw. zusammengeführter Vor- und Nachname bei Einzelunternehmern).",
                "keyInData": "org_company_name",
                "displayAttribute": false
            },
            {
                "label": "Rechtsform (RechtsformText)",
                "description": "Klartext-Bezeichnung der Rechtsform, z. B. „GmbH“, „e. V.“.",
                "keyInData": "org_legal_form_text",
                "displayAttribute": false
            },
            {
                "label": "Rechtsform Code (Rechtsform)",
                "description": "Dreistelliger Code für die Rechtsform laut ELSTER-Werteliste.",
                "keyInData": "org_legal_form_code",
                "displayAttribute": false
            },
            {
                "label": "Tätigkeit (TaetigkeitText)",
                "description": "Beschreibung der betrieblichen Tätigkeit bei natürlichen Personen, z. B. „Handwerklicher Betrieb“.",
                "keyInData": "org_occupation_text",
                "displayAttribute": false
            },
            {
                "label": "Tätigkeit Code (Taetigkeit)",
                "description": "Dreistelliger Code der Tätigkeit (siehe ELSTER-Liste).",
                "keyInData": "org_occupation_code",
                "displayAttribute": false
            },
            {
                "label": "Registernummer",
                "description": "Eintragungs-/Handelsregisternummer der Organisation (max. 11 alphanum. Zeichen).",
                "keyInData": "org_register_number",
                "displayAttribute": false
            },
            {
                "label": "Registerart",
                "description": "Art des Registers, z. B. HRA, HRB, VR, GR, PR.",
                "keyInData": "org_register_type",
                "displayAttribute": false
            },
            {
                "label": "Registergericht",
                "description": "Name des Registergerichts, das die Organisation führt.",
                "keyInData": "org_register_court",
                "displayAttribute": false
            },
            {
                "label": "Handelnde Person: Namensvorsatz",
                "description": "Anredezusatz vor dem Vor-/Nachnamen, z. B. „von“, „van“.",
                "keyInData": "salutation",
                "displayAttribute": false
            },
            {
                "label": "Handelnde Person: Akademischer Grad",
                "description": "Akademischer Grad der handelnden Person, z. B. „Dr.“, „Dipl.-Ing.“.",
                "keyInData": "title",
                "displayAttribute": false
            },
            {
                "label": "Handelnde Person: Vorname(n)",
                "description": "Alle im Melderegister hinterlegten Vornamen der handelnden Person.",
                "keyInData": "given_name",
                "displayAttribute": false
            },
            {
                "label": "Handelnde Person: Nachname",
                "description": "Familienname der handelnden Person.",
                "keyInData": "family_name",
                "displayAttribute": false
            },
            {
                "label": "Handelnde Person: Vollständiger Name",
                "description": "Vor- und Nachname in einem Feld; dient v. a. zur Anzeige.",
                "keyInData": "name",
                "displayAttribute": false
            },
            {
                "label": "Handelnde Person: Namenszusatz",
                "description": "Namensbestandteil nach dem Nachnamen, z. B. „MBA“, „jun.“.",
                "keyInData": "zusatz_name",
                "displayAttribute": false
            },
            {
                "label": "Handelnde Person: Geburtsdatum",
                "description": "Geburtstag der handelnden Person.",
                "keyInData": "date_of_birth",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Typ",
                "description": "Kennzeichnet die Adresse als INLAND oder AUSLAND.",
                "keyInData": "org_address_type",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Straße",
                "description": "Straßenname der Unternehmensanschrift.",
                "keyInData": "org_street",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Hausnummer",
                "description": "Hausnummer (+ Zusatz) der Unternehmensanschrift.",
                "keyInData": "org_building",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Postleitzahl",
                "description": "Postleitzahl (bei Auslandsadressen frei formatiert, max. 12 Zeichen).",
                "keyInData": "org_zip_code",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Ort",
                "description": "Ort der Unternehmensanschrift.",
                "keyInData": "org_city",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Ortsteil",
                "description": "Ortsteil/Bezirksangabe, falls vorhanden.",
                "keyInData": "org_city_district",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Adressergänzung",
                "description": "Zusätzliche Adressinformation (z. B. Bauabschnitt, Gebäudeteil).",
                "keyInData": "org_address_addition",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Land",
                "description": "Zwei-Buchstaben-ISO-Code des Landes (z. B. DE, AT).",
                "keyInData": "org_country",
                "displayAttribute": false
            },
            {
                "label": "ELSTER ID",
                "description": "Eindeutiger Login-Name des Kontos („preferred username“).",
                "keyInData": "preferred_username",
                "displayAttribute": true
            },
            {
                "label": "Datenübermittler Pseudonym ID",
                "description": "Für jeden Service-Provider pseudonymisierte Organisations-ID (Präfix du-).",
                "keyInData": "elster_datenuebermittler",
                "displayAttribute": true
            },
            {
                "label": "Datenkranz Typ",
                "description": "Gibt an, ob die Daten aus einem StNr-, IdNr- oder BZStNr-Datenkranz stammen.",
                "keyInData": "elster_datenkranz_typ",
                "displayAttribute": true
            },
            {
                "label": "Vertrauensniveau Identifizierung (ElsterVertrauensniveauIdentifizierung)",
                "description": "Sicherheitsniveau der einmaligen Identitätsprüfung (z. B. SUBSTANZIELL, HOCH).",
                "keyInData": "trust_level_identification",
                "displayAttribute": false
            },
            {
                "label": "Vertrauensniveau Authentifizierung (ElsterVertrauensniveauAuthentifizierung)",
                "description": "Sicherheitsniveau der aktuellen Anmeldung (bei NEZO-Token immer SUBSTANZIELL).",
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
                "value": "muk-stage"
            }
        ]',
        exists(select 1 from system_configs where key = 'MukActive' and value = 'true'),
        true);
