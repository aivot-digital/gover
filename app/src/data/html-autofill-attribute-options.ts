export interface HtmlAutofillAttributeOption {
    value: string;
    label: string;
    description: string;
    group: 'Name' | 'Kontakt' | 'Unternehmen und Beruf' | 'Anschrift' | 'Weitere Angaben' | 'Geburtsdatum';
}

// Keep each group contiguous: this catalogue defines the display order for every element type.
export const HtmlAutofillAttributeOptions: HtmlAutofillAttributeOption[] = [
    {
        value: 'given-name',
        label: 'Vorname',
        description: 'Vorname der Person.',
        group: 'Name',
    },
    {
        value: 'family-name',
        label: 'Nachname',
        description: 'Nachname bzw. Familienname der Person.',
        group: 'Name',
    },
    {
        value: 'name',
        label: 'Vollständiger Name',
        description: 'Name als zusammenhängende Angabe in einem Feld.',
        group: 'Name',
    },
    {
        value: 'honorific-prefix',
        label: 'Anrede bzw. Titel',
        description: 'Anrede oder vorangestellter Titel, zum Beispiel „Frau“, „Herr“ oder „Dr.“.',
        group: 'Name',
    },
    {
        value: 'additional-name',
        label: 'Weitere Vornamen',
        description: 'Weitere Vornamen zusätzlich zum ersten Vornamen.',
        group: 'Name',
    },
    {
        value: 'honorific-suffix',
        label: 'Nachgestellter Titel',
        description: 'Titel oder Namenszusatz hinter dem Namen, zum Beispiel „B. Sc.“ oder „Jr.“.',
        group: 'Name',
    },
    {
        value: 'email',
        label: 'E-Mail-Adresse',
        description: 'E-Mail-Adresse der Person oder des Unternehmens.',
        group: 'Kontakt',
    },
    {
        value: 'tel',
        label: 'Telefonnummer',
        description: 'Vollständige Telefonnummer einschließlich Ländervorwahl.',
        group: 'Kontakt',
    },
    {
        value: 'url',
        label: 'Webseite',
        description: 'Webadresse zur Person oder zum Unternehmen.',
        group: 'Kontakt',
    },
    {
        value: 'organization',
        label: 'Unternehmensname',
        description: 'Name des Unternehmens, zu dem die Angaben gehören.',
        group: 'Unternehmen und Beruf',
    },
    {
        value: 'organization-title',
        label: 'Berufsbezeichnung',
        description: 'Beruf oder Position, zum Beispiel „Softwareentwickler:in“.',
        group: 'Unternehmen und Beruf',
    },
    {
        value: 'street-address',
        label: 'Straßenanschrift (mehrzeilig)',
        description: 'Straßenanschrift mit mehreren Zeilen, einschließlich möglicher Adresszusätze.',
        group: 'Anschrift',
    },
    {
        value: 'address-line1',
        label: 'Adresszeile 1',
        description: 'Erste Zeile der Straßenanschrift, meist Straße und Hausnummer.',
        group: 'Anschrift',
    },
    {
        value: 'postal-code',
        label: 'Postleitzahl',
        description: 'Postleitzahl des Ortes, zum Beispiel „12345“.',
        group: 'Anschrift',
    },
    {
        value: 'address-level2',
        label: 'Ort / Stadt',
        description: 'Meist Stadt oder Gemeinde; die genaue Verwaltungsebene hängt vom Land ab.',
        group: 'Anschrift',
    },
    {
        value: 'country-name',
        label: 'Land',
        description: 'Ausgeschriebener Ländername, zum Beispiel „Deutschland“.',
        group: 'Anschrift',
    },
    {
        value: 'address-line2',
        label: 'Adresszeile 2',
        description: 'Ergänzung zur ersten Adresszeile, zum Beispiel Gebäude oder Wohnung.',
        group: 'Anschrift',
    },
    {
        value: 'address-line3',
        label: 'Adresszeile 3',
        description: 'Weitere Ergänzung zur Straßenanschrift.',
        group: 'Anschrift',
    },
    {
        value: 'address-level1',
        label: 'Bundesland / Region',
        description: 'Oberste Verwaltungsebene, zum Beispiel Bundesland, Kanton oder Provinz.',
        group: 'Anschrift',
    },
    {
        value: 'country',
        label: 'Ländercode',
        description: 'Zweistelliges Länderkennzeichen, zum Beispiel „DE“ für Deutschland.',
        group: 'Anschrift',
    },
    {
        value: 'address-level3',
        label: 'Weitere Verwaltungsebene (3)',
        description: 'Dritte Verwaltungsebene, sofern das Land diese verwendet, zum Beispiel ein Bezirk.',
        group: 'Anschrift',
    },
    {
        value: 'address-level4',
        label: 'Weitere Verwaltungsebene (4)',
        description: 'Vierte und feinste Verwaltungsebene bei entsprechend aufgebauten Adressen.',
        group: 'Anschrift',
    },
    {
        value: 'language',
        label: 'Bevorzugte Sprache',
        description: 'Bevorzugte Sprache, zum Beispiel „de“ für Deutsch.',
        group: 'Weitere Angaben',
    },
    {
        value: 'sex',
        label: 'Geschlechtsidentität',
        description: 'Angabe zum Geschlecht oder zur Geschlechtsidentität.',
        group: 'Weitere Angaben',
    },
    {
        value: 'username',
        label: 'Benutzername',
        description: 'Benutzername für ein Konto oder einen Dienst.',
        group: 'Weitere Angaben',
    },
    {
        value: 'nickname',
        label: 'Spitzname',
        description: 'Spitzname oder Anzeigename anstelle des vollständigen Namens.',
        group: 'Weitere Angaben',
    },
    {
        value: 'bday',
        label: 'Geburtsdatum',
        description: 'Tag, Monat und Jahr der Geburt gemeinsam.',
        group: 'Geburtsdatum',
    },
    {
        value: 'bday-year',
        label: 'Geburtsjahr',
        description: 'Jahr der Geburt im Format JJJJ.',
        group: 'Geburtsdatum',
    },
];
