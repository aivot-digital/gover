import {ElementAttributeMappingOption} from '../components/element-editor-metadata-tab/element-editor-metadata-tab';
import {ElementType} from './element-type/element-type';
import {parse} from 'date-fns';

export enum ShIdAttribute {
    DataportIdentitaetstyp = 'dataport_identitaetstyp',
    Salutation = 'salutation',
    Title = 'title',
    GivenName = 'given_name',
    FamilyName = 'family_name',
    Name = 'name',
    DateOfBirth = 'date_of_birth',
    PlaceOfBirth = 'place_of_birth',
    BirthName = 'birth_name',
    Gender = 'gender',
    StreetAndBuilding = 'street_and_building',
    ZipCode = 'zip_code',
    City = 'city',
    Country = 'country',
    Email = 'email',
    Telephone = 'telephone',
    Mobilephone = 'mobilephone',
    PreferredLanguage = 'preferred_language',
    OrgCompanyName = 'org_company_name',
    OrgOrganizationUnit = 'org_organization_unit',
    OrgRegisterNumber = 'org_register_number',
    OrgRegisterCourt = 'org_register_court',
    OrgContactMail = 'org_contact_mail',
    OrgContactPhone = 'org_contact_phone',
    OrgStreetAndBuilding = 'org_street_and_building',
    OrgZipCode = 'org_zip_code',
    OrgCity = 'org_city',
    OrgCountry = 'org_country',
    OrgPostboxNumber = 'org_postbox_number',
    OrgPostboxZipCode = 'org_postbox_zip_code',
    OrgPostboxCountry = 'org_postbox_country',
    ElsterDatenuebermittler = 'elster_datenuebermittler',
    TrustLevelAuthentication = 'trust_level_authentication',
    PreferredUsername = 'preferred_username',
    DataportInboxId = 'dataport_inbox_id',
    DataportServicekontoType = 'dataport_servicekonto_type',
}

export const ShIdAttributes: ElementAttributeMappingOption[] = [
    {
        label: 'Identitätstyp',
        value: ShIdAttribute.DataportIdentitaetstyp,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Anrede',
        value: ShIdAttribute.Salutation,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Akademischer Titel',
        value: ShIdAttribute.Title,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Vorname(n)',
        value: ShIdAttribute.GivenName,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Nachname',
        value: ShIdAttribute.FamilyName,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Vollständiger Name',
        value: ShIdAttribute.Name,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Geburtsdatum',
        value: ShIdAttribute.DateOfBirth,
        limit: [
            ElementType.Date,
        ],
    },
    {
        label: 'Geburtsort',
        value: ShIdAttribute.PlaceOfBirth,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Geburtsname',
        value: ShIdAttribute.BirthName,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Geschlecht',
        value: ShIdAttribute.Gender,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Anschrift Privatperson: Straße und Hausnummer ',
        value: ShIdAttribute.StreetAndBuilding,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Anschrift Privatperson: Postleitzahl',
        value: ShIdAttribute.ZipCode,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Anschrift Privatperson: Ort',
        value: ShIdAttribute.City,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Anschrift Privatperson: Land',
        value: ShIdAttribute.Country,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'E-Mail-Adresse',
        value: ShIdAttribute.Email,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Telefonnummer',
        value: ShIdAttribute.Telephone,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Mobiltelefonnummer',
        value: ShIdAttribute.Mobilephone,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Favorisierte Sprache',
        value: ShIdAttribute.PreferredLanguage,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Firmenname',
        value: ShIdAttribute.OrgCompanyName,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Unternehmenseinheit',
        value: ShIdAttribute.OrgOrganizationUnit,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Registernummer',
        value: ShIdAttribute.OrgRegisterNumber,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Registergericht',
        value: ShIdAttribute.OrgRegisterCourt,
        limit: [
            ElementType.Text,
            ElementType.Radio,
            ElementType.Select,
        ],
    },
    {
        label: 'Unternehmen: E-Mail-Adresse',
        value: ShIdAttribute.OrgContactMail,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Unternehmen: Telefonnummer',
        value: ShIdAttribute.OrgContactPhone,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Unternehmensanschrift: Straße und Hausnummer',
        value: ShIdAttribute.OrgStreetAndBuilding,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Unternehmensanschrift: Postleitzahl',
        value: ShIdAttribute.OrgZipCode,
        limit: [
            ElementType.Text,
            ElementType.Radio,
            ElementType.Select,
        ],
    },
    {
        label: 'Unternehmensanschrift: Ort',
        value: ShIdAttribute.OrgCity,
        limit: [
            ElementType.Text,
            ElementType.Radio,
            ElementType.Select,
        ],
    },
    {
        label: 'Unternehmensanschrift: Land',
        value: ShIdAttribute.OrgCountry,
        limit: [
            ElementType.Text,
            ElementType.Radio,
            ElementType.Select,
        ],
    },
    {
        label: 'Unternehmensanschrift: Postfach-Nummer',
        value: ShIdAttribute.OrgPostboxNumber,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Unternehmensanschrift: Postfach-Postleitzahl',
        value: ShIdAttribute.OrgPostboxZipCode,
        limit: [
            ElementType.Text,
            ElementType.Radio,
            ElementType.Select,
        ],
    },
    {
        label: 'Unternehmensanschrift: Postfach-Land',
        value: ShIdAttribute.OrgPostboxCountry,
        limit: [
            ElementType.Text,
            ElementType.Radio,
            ElementType.Select,
        ],
    },
    {
        label: 'Datenübermittler Pseudonym ID',
        value: ShIdAttribute.ElsterDatenuebermittler,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Vertrauensniveau',
        value: ShIdAttribute.TrustLevelAuthentication,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Servicekonto ID',
        value: ShIdAttribute.PreferredUsername,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Postfach',
        value: ShIdAttribute.DataportInboxId,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Servicekontotyp',
        value: ShIdAttribute.DataportServicekontoType,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
];

export function transformShIdAttribute(key: string, value: string): any {
    switch (key) {
        case 'date_of_birth':
            return parse(value, 'yyyy-MM-dd', new Date()).toISOString();
        default:
            return value;
    }
}

