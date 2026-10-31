package de.aivot.prosuna.backend.seeding.data;

import de.aivot.prosuna.backend.identity.models.IdentityAttributeMapping;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Attribute mappings of the system identity providers. Stage and production variants of a provider share the same
 * attributes, so the identity provider seed uses one list per provider type.
 */
public class SystemIdentityProviderAttributes {
    public static final List<IdentityAttributeMapping> BAYERN_ID = List.of(
            attribute("Anrede", "Anrede, wie sie vom Identitätsanbieter übermittelt wird, zum Beispiel Herr oder Frau.", "salutation", false),
            attribute("Akademischer Titel", "Akademischer Grad wie Dr. oder Prof.", "title", false),
            attribute("Vorname(n)", "Ruf- und ggf. weitere Vornamen der Person.", "given_name", false),
            attribute("Nachname", "Familien- bzw. Nachname der Person.", "family_name", false),
            attribute("Vollständiger Name", "Zusammengesetzter kompletter Name aus Vor- und Nachnamen.", "name", false),
            attribute("Geburtsdatum", "Datum, an welchem die Person geboren wurde.", "date_of_birth", false),
            attribute("Geburtsort", "Ort, an dem die Person geboren wurde.", "place_of_birth", false),
            attribute("Geburtsname", "Familienname zum Zeitpunkt der Geburt (falls abweichend).", "birth_name", false),
            attribute("Staatsangehörigkeit", "Aktuelle Staatsbürgerschaft, i. d. R. als Ländercode (z. B. DE).", "nationality", false),
            attribute("Straße und Hausnummer", "Wohnanschrift: Straßenname und Hausnummer.", "street_and_building", false),
            attribute("Postleitzahl", "Postleitzahl der Wohnanschrift.", "zip_code", false),
            attribute("Ort", "Wohnort bzw. Stadt.", "city", false),
            attribute("Wohnort-ID", "Amtlicher Schlüssel zur eindeutigen Identifikation des Wohnortes (ARS).", "community_id", false),
            attribute("Land", "Land der Wohnanschrift (ISO 3166-1 alpha-2).", "country", false),
            attribute("E-Mail-Adresse", "Vom Nutzenden hinterlegte E-Mail-Adresse zur Kontaktaufnahme.", "email", false),
            attribute("Telefonnummer", "Telefon- oder Mobilnummer im internationalen Format.", "telephone", false),
            attribute("bPK2", "Bereichsspezifisches Personenkennzeichen – pseudonymisierte, pro Service eindeutige ID der Person.", "bPK2", true),
            attribute("Postfach", "Technischer Postkorb-Handle (UUID) zum Zustellen von BayernID-Nachrichten.", "legacy_postkorb_handle", true),
            attribute("Vertrauensniveau", "Qualitätsstufe der Authentifizierung (z. B. STORK-QAA-Level 1–4).", "trust_level_authentication", true),
            attribute("Dokumententyp", "Art des verwendeten Ausweisdokuments, z. B. ID (Personalausweis) oder P (Reisepass).", "document_type", false),
            attribute("eIDAS-Issuing-Country", "Land, das das eID-/eIDAS-Identitätsmittel ausgestellt hat (ISO 3166-1 alpha-2).", "e_idas_issuing_country", false),
            attribute("Pseudonym", "Dienste‑ bzw. kartenspezifisches Kennzeichen (DKK) aus eID oder anderen Quellen, um eine Person systemübergreifend pseudonym zuzuordnen; zur eindeutigen Zuordnung mit AssertionProvedBy kombinieren.", "pseudonym", true),
            attribute("AssertionProvedBy", "Quelle, die die Identität geprüft hat (z. B. eIDAS, eID, ELSTER).", "assertion_proved_by", true),
            attribute("Version", "Version der BayernID-Schnittstelle (Calendar Versioning, z. B. 2025.4.17).", "api_version", false),
            attribute("Antrags-ID", "Technische Vorgangs‑ID (UUID), die alle Nachrichten, Dokumente und Statusmeldungen zu einem Antrag bündelt.", "application_id", false)
    );

    public static final List<IdentityAttributeMapping> BUND_ID = List.of(
            attribute("Anrede", "Anrede, wie sie vom Identitätsanbieter übermittelt wird, zum Beispiel Herr oder Frau.", "salutation", false),
            attribute("Akademischer Titel", "Akademischer Grad wie Dr. oder Prof.", "title", false),
            attribute("Vorname(n)", "Ruf- und ggf. weitere Vornamen der Person.", "given_name", false),
            attribute("Nachname", "Familien- bzw. Nachname der Person.", "family_name", false),
            attribute("Vollständiger Name", "Zusammengesetzter kompletter Name aus Vor- und Nachnamen.", "name", false),
            attribute("Geburtsdatum", "Datum, an welchem die Person geboren wurde.", "date_of_birth", false),
            attribute("Geburtsort", "Ort, an dem die Person geboren wurde.", "place_of_birth", false),
            attribute("Geburtsname", "Familienname zum Zeitpunkt der Geburt (falls abweichend).", "birth_name", false),
            attribute("Staatsangehörigkeit", "Aktuelle Staatsbürgerschaft, i. d. R. als Ländercode (z. B. DE).", "nationality", false),
            attribute("Straße und Hausnummer", "Wohnanschrift: Straßenname und Hausnummer.", "street_and_building", false),
            attribute("Postleitzahl", "Postleitzahl der Wohnanschrift.", "zip_code", false),
            attribute("Ort", "Wohnort bzw. Stadt.", "city", false),
            attribute("Wohnort-ID", "Amtlicher Schlüssel zur eindeutigen Identifikation des Wohnortes (ARS).", "community_id", false),
            attribute("Land", "Land der Wohnanschrift (ISO 3166-1 alpha-2).", "country", false),
            attribute("E-Mail-Adresse", "Vom Nutzenden hinterlegte E-Mail-Adresse zur Kontaktaufnahme.", "email", false),
            attribute("Telefonnummer", "Telefon- oder Mobilnummer im internationalen Format.", "telephone", false),
            attribute("bPK2", "Bereichsspezifisches Personenkennzeichen – pseudonymisierte, pro Service eindeutige ID der Person.", "bPK2", true),
            attribute("Postfach", "Technischer Postkorb-Handle (UUID) zum Zustellen von BundID-Nachrichten.", "legacy_postkorb_handle", true),
            attribute("Vertrauensniveau", "Qualitätsstufe der Authentifizierung (z. B. STORK-QAA-Level 1–4).", "trust_level_authentication", true),
            attribute("Dokumententyp", "Art des verwendeten Ausweisdokuments, z. B. ID (Personalausweis) oder P (Reisepass).", "document_type", false),
            attribute("eIDAS-Issuing-Country", "Land, das das eID-/eIDAS-Identitätsmittel ausgestellt hat (ISO 3166-1 alpha-2).", "e_idas_issuing_country", false),
            attribute("Pseudonym", "Dienste‑ bzw. kartenspezifisches Kennzeichen (DKK) aus eID oder anderen Quellen, um eine Person systemübergreifend pseudonym zuzuordnen; zur eindeutigen Zuordnung mit AssertionProvedBy kombinieren.", "pseudonym", true),
            attribute("AssertionProvedBy", "Quelle, die die Identität geprüft hat (z. B. eIDAS, eID, ELSTER).", "assertion_proved_by", true),
            attribute("Version", "Version der BundID-Schnittstelle (Calendar Versioning, z. B. 2025.4.17).", "api_version", false),
            attribute("Antrags-ID", "Technische Vorgangs‑ID (UUID), die alle Nachrichten, Dokumente und Statusmeldungen zu einem Antrag bündelt.", "application_id", false)
    );

    public static final List<IdentityAttributeMapping> SH_ID = List.of(
            attribute("Identitätstyp", "Gibt an, ob es sich um eine natürliche Person, eine Organisation oder ein Funktionskonto handelt.", "dataport_identitaetstyp", true),
            attribute("Anrede", "Anrede, wie sie vom Identitätsanbieter übermittelt wird, zum Beispiel Herr oder Frau.", "salutation", false),
            attribute("Akademischer Titel", "Akademischer Grad wie Dr. oder Prof.", "title", false),
            attribute("Vorname(n)", "Ruf- und ggf. weitere Vornamen der Person.", "given_name", false),
            attribute("Nachname", "Familien- bzw. Nachname der Person.", "family_name", false),
            attribute("Vollständiger Name", "Zusammengesetzter kompletter Name aus Vor- und Nachnamen.", "name", false),
            attribute("Geburtsdatum", "Datum, an welchem die Person geboren wurde.", "date_of_birth", false),
            attribute("Geburtsort", "Ort, an dem die Person geboren wurde.", "place_of_birth", false),
            attribute("Geburtsname", "Familienname zum Zeitpunkt der Geburt (falls abweichend).", "birth_name", false),
            attribute("Geschlecht", "Im Ausweisdokument angegebenes Geschlecht.", "gender", false),
            attribute("Staatsangehörigkeit", "Aktuelle Staatsbürgerschaft, i. d. R. als Ländercode (z. B. DE).", "nationality", false),
            attribute("Anschrift Privatperson: Straße und Hausnummer", "Wohnanschrift: Straßenname und Hausnummer.", "street_and_building", false),
            attribute("Anschrift Privatperson: Postleitzahl", "Postleitzahl der Wohnanschrift.", "zip_code", false),
            attribute("Anschrift Privatperson: Ort", "Wohnort bzw. Stadt.", "city", false),
            attribute("Anschrift Privatperson: Land", "Ländercode oder -name der Wohnanschrift.", "country", false),
            attribute("E-Mail-Adresse", "Vom Nutzenden hinterlegte E-Mail-Adresse zur Kontaktaufnahme.", "email", false),
            attribute("Telefonnummer", "Festnetznummer der Person.", "telephone", false),
            attribute("Mobiltelefonnummer", "Handy-Nummer der Person.", "mobilephone", false),
            attribute("Favorisierte Sprache", "Bevorzugte Sprache für Kommunikation und Benachrichtigungen.", "preferred_language", false),
            attribute("Firmenname", "Offizieller Name des Unternehmens.", "org_company_name", false),
            attribute("Unternehmenseinheit", "Abteilung oder Organisationseinheit innerhalb des Unternehmens.", "org_company_unit", false),
            attribute("Registernummer", "Handels- oder Vereinsregisternummer des Unternehmens.", "org_register_number", false),
            attribute("Registergericht", "Amtsgericht, bei dem das Unternehmen registriert ist.", "org_register_court", false),
            attribute("Unternehmen: E-Mail-Adresse", "Zentrale E-Mail des Unternehmens.", "org_contact_mail", false),
            attribute("Unternehmen: Telefonnummer", "Zentrale Telefonnummer des Unternehmens.", "org_contact_phone", false),
            attribute("Unternehmensanschrift: Straße und Hausnummer", "Straße inkl. Hausnummer der Firmenanschrift.", "org_street_and_building", false),
            attribute("Unternehmensanschrift: Postleitzahl", "Postleitzahl der Firmenanschrift.", "org_zip_code", false),
            attribute("Unternehmensanschrift: Ort", "Ort der Firmenanschrift.", "org_city", false),
            attribute("Unternehmensanschrift: Land", "Land der Firmenanschrift.", "org_country", false),
            attribute("Unternehmensanschrift: Postfach-Nummer", "Postfachnummer der Firmenanschrift.", "org_postbox_number", false),
            attribute("Unternehmensanschrift: Postfach-Postleitzahl", "Postleitzahl des Firmenpostfachs.", "org_postbox_zip_code", false),
            attribute("Unternehmensanschrift: Postfach-Land", "Land des Firmenpostfachs.", "org_postbox_country", false),
            attribute("Datenübermittler Pseudonym ID", "Pseudonyme technische Kennung des Datenübermittlers.", "elster_datenuebermittler", true),
            attribute("Vertrauensniveau", "Authentifizierungsstufe nach eIDAS (Low, Substantial, High).", "trust_level_authentication", true),
            attribute("Servicekonto ID", "Eindeutige Kennung des Servicekontos (Login-Name).", "preferred_username", false),
            attribute("Postfach", "Interne Postfach-ID im Dataport-Postfachsystem.", "dataport_inbox_id", true),
            attribute("Servicekontotyp", "Kennzeichnet, ob es sich um ein Bürger- oder Organisationskonto handelt.", "dataport_servicekonto_type", false)
    );

    public static final List<IdentityAttributeMapping> MUK = List.of(
            attribute("Identitätstyp (PersTyp)", "Kennzeichnet, ob es sich um eine natürliche Person (NatPers) oder um eine juristische/nicht-natürliche Person (NNatPers) handelt.", "elster_identitaetstyp", false),
            attribute("Firmenname", "Offiziell im Steuerkonto hinterlegter Name der Organisation (bzw. zusammengeführter Vor- und Nachname bei Einzelunternehmern).", "org_company_name", false),
            attribute("Rechtsform (RechtsformText)", "Klartext-Bezeichnung der Rechtsform, z. B. „GmbH“, „e. V.“.", "org_legal_form_text", false),
            attribute("Rechtsform Code (Rechtsform)", "Dreistelliger Code für die Rechtsform laut ELSTER-Werteliste.", "org_legal_form_code", false),
            attribute("Tätigkeit (TaetigkeitText)", "Beschreibung der betrieblichen Tätigkeit bei natürlichen Personen, z. B. „Handwerklicher Betrieb“.", "org_occupation_text", false),
            attribute("Tätigkeit Code (Taetigkeit)", "Dreistelliger Code der Tätigkeit (siehe ELSTER-Liste).", "org_occupation_code", false),
            attribute("Registernummer", "Eintragungs-/Handelsregisternummer der Organisation (max. 11 alphanum. Zeichen).", "org_register_number", false),
            attribute("Registerart", "Art des Registers, z. B. HRA, HRB, VR, GR, PR.", "org_register_type", false),
            attribute("Registergericht", "Name des Registergerichts, das die Organisation führt.", "org_register_court", false),
            attribute("Handelnde Person: Namensvorsatz", "Anredezusatz vor dem Vor-/Nachnamen, z. B. „von“, „van“.", "salutation", false),
            attribute("Handelnde Person: Akademischer Grad", "Akademischer Grad der handelnden Person, z. B. „Dr.“, „Dipl.-Ing.“.", "title", false),
            attribute("Handelnde Person: Vorname(n)", "Alle im Melderegister hinterlegten Vornamen der handelnden Person.", "given_name", false),
            attribute("Handelnde Person: Nachname", "Familienname der handelnden Person.", "family_name", false),
            attribute("Handelnde Person: Vollständiger Name", "Vor- und Nachname in einem Feld; dient v. a. zur Anzeige.", "name", false),
            attribute("Handelnde Person: Namenszusatz", "Namensbestandteil nach dem Nachnamen, z. B. „MBA“, „jun.“.", "zusatz_name", false),
            attribute("Handelnde Person: Geburtsdatum", "Geburtstag der handelnden Person.", "date_of_birth", false),
            attribute("Unternehmensanschrift: Typ", "Kennzeichnet die Adresse als INLAND oder AUSLAND.", "org_address_type", false),
            attribute("Unternehmensanschrift: Straße", "Straßenname der Unternehmensanschrift.", "org_street", false),
            attribute("Unternehmensanschrift: Hausnummer", "Hausnummer (+ Zusatz) der Unternehmensanschrift.", "org_building", false),
            attribute("Unternehmensanschrift: Postleitzahl", "Postleitzahl (bei Auslandsadressen frei formatiert, max. 12 Zeichen).", "org_zip_code", false),
            attribute("Unternehmensanschrift: Ort", "Ort der Unternehmensanschrift.", "org_city", false),
            attribute("Unternehmensanschrift: Ortsteil", "Ortsteil/Bezirksangabe, falls vorhanden.", "org_city_district", false),
            attribute("Unternehmensanschrift: Adressergänzung", "Zusätzliche Adressinformation (z. B. Bauabschnitt, Gebäudeteil).", "org_address_addition", false),
            attribute("Unternehmensanschrift: Land", "Zwei-Buchstaben-ISO-Code des Landes (z. B. DE, AT).", "org_country", false),
            attribute("ELSTER ID", "Eindeutiger Login-Name des Kontos („preferred username“).", "preferred_username", true),
            attribute("Datenübermittler Pseudonym ID", "Für jeden Service-Provider pseudonymisierte Organisations-ID (Präfix du-).", "elster_datenuebermittler", true),
            attribute("Datenkranz Typ", "Gibt an, ob die Daten aus einem StNr-, IdNr- oder BZStNr-Datenkranz stammen.", "elster_datenkranz_typ", true),
            attribute("Vertrauensniveau Identifizierung (ElsterVertrauensniveauIdentifizierung)", "Sicherheitsniveau der einmaligen Identitätsprüfung (z. B. SUBSTANZIELL, HOCH).", "trust_level_identification", false),
            attribute("Vertrauensniveau Authentifizierung (ElsterVertrauensniveauAuthentifizierung)", "Sicherheitsniveau der aktuellen Anmeldung (bei NEZO-Token immer SUBSTANZIELL).", "trust_level_authentication", true)
    );

    @Nonnull
    private static IdentityAttributeMapping attribute(@Nonnull String label,
                                                      @Nonnull String description,
                                                      @Nonnull String keyInData,
                                                      boolean displayAttribute) {
        return new IdentityAttributeMapping()
                .setLabel(label)
                .setDescription(description)
                .setKeyInData(keyInData)
                .setDisplayAttribute(displayAttribute);
    }
}
