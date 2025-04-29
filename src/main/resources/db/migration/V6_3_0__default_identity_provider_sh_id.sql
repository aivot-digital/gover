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
                "description": "Gibt an, ob es sich um eine natürliche Person, eine Organisation oder ein Funktionskonto handelt.",
                "keyInData": "dataport_identitaetstyp",
                "displayAttribute": true
            },
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
                "label": "Geschlecht",
                "description": "Im Ausweisdokument angegebenes Geschlecht.",
                "keyInData": "gender",
                "displayAttribute": false
            },
            {
                "label": "Staatsangehörigkeit",
                "description": "Aktuelle Staatsbürgerschaft, i. d. R. als Ländercode (z. B. DE).",
                "keyInData": "nationality",
                "displayAttribute": false
            },
            {
                "label": "Anschrift Privatperson: Straße und Hausnummer",
                "description": "Wohnanschrift: Straßenname und Hausnummer.",
                "keyInData": "street_and_building",
                "displayAttribute": false
            },
            {
                "label": "Anschrift Privatperson: Postleitzahl",
                "description": "Postleitzahl der Wohnanschrift.",
                "keyInData": "zip_code",
                "displayAttribute": false
            },
            {
                "label": "Anschrift Privatperson: Ort",
                "description": "Wohnort bzw. Stadt.",
                "keyInData": "city",
                "displayAttribute": false
            },
            {
                "label": "Anschrift Privatperson: Land",
                "description": "Ländercode oder -name der Wohnanschrift.",
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
                "description": "Festnetznummer der Person.",
                "keyInData": "telephone",
                "displayAttribute": false
            },
            {
                "label": "Mobiltelefonnummer",
                "description": "Handy-Nummer der Person.",
                "keyInData": "mobilephone",
                "displayAttribute": false
            },
            {
                "label": "Favorisierte Sprache",
                "description": "Bevorzugte Sprache für Kommunikation und Benachrichtigungen.",
                "keyInData": "preferred_language",
                "displayAttribute": false
            },
            {
                "label": "Firmenname",
                "description": "Offizieller Name des Unternehmens.",
                "keyInData": "org_company_name",
                "displayAttribute": false
            },
            {
                "label": "Unternehmenseinheit",
                "description": "Abteilung oder Organisationseinheit innerhalb des Unternehmens.",
                "keyInData": "org_company_unit",
                "displayAttribute": false
            },
            {
                "label": "Registernummer",
                "description": "Handels- oder Vereinsregisternummer des Unternehmens.",
                "keyInData": "org_register_number",
                "displayAttribute": false
            },
            {
                "label": "Registergericht",
                "description": "Amtsgericht, bei dem das Unternehmen registriert ist.",
                "keyInData": "org_register_court",
                "displayAttribute": false
            },
            {
                "label": "Unternehmen: E-Mail-Adresse",
                "description": "Zentrale E-Mail des Unternehmens.",
                "keyInData": "org_contact_mail",
                "displayAttribute": false
            },
            {
                "label": "Unternehmen: Telefonnummer",
                "description": "Zentrale Telefonnummer des Unternehmens.",
                "keyInData": "org_contact_phone",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Straße und Hausnummer",
                "description": "Straße inkl. Hausnummer der Firmenanschrift.",
                "keyInData": "org_street_and_building",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Postleitzahl",
                "description": "Postleitzahl der Firmenanschrift.",
                "keyInData": "org_zip_code",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Ort",
                "description": "Ort der Firmenanschrift.",
                "keyInData": "org_city",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Land",
                "description": "Land der Firmenanschrift.",
                "keyInData": "org_country",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Postfach-Nummer",
                "description": "Postfachnummer der Firmenanschrift.",
                "keyInData": "org_postbox_number",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Postfach-Postleitzahl",
                "description": "Postleitzahl des Firmenpostfachs.",
                "keyInData": "org_postbox_zip_code",
                "displayAttribute": false
            },
            {
                "label": "Unternehmensanschrift: Postfach-Land",
                "description": "Land des Firmenpostfachs.",
                "keyInData": "org_postbox_country",
                "displayAttribute": false
            },
            {
                "label": "Datenübermittler Pseudonym ID",
                "description": "Pseudonyme technische Kennung des Datenübermittlers.",
                "keyInData": "elster_datenuebermittler",
                "displayAttribute": true
            },
            {
                "label": "Vertrauensniveau",
                "description": "Authentifizierungsstufe nach eIDAS (Low, Substantial, High).",
                "keyInData": "trust_level_authentication",
                "displayAttribute": true
            },
            {
                "label": "Servicekonto ID",
                "description": "Eindeutige Kennung des Servicekontos (Login-Name).",
                "keyInData": "preferred_username",
                "displayAttribute": false
            },
            {
                "label": "Postfach",
                "description": "Interne Postfach-ID im Dataport-Postfachsystem.",
                "keyInData": "dataport_inbox_id",
                "displayAttribute": true
            },
            {
                "label": "Servicekontotyp",
                "description": "Kennzeichnet, ob es sich um ein Bürger- oder Organisationskonto handelt.",
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

