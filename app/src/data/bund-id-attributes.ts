import {ElementType} from './element-type/element-type';
import {parse} from 'date-fns';
import {ElementAttributeMappingOption} from '../components/element-editor-metadata-tab/element-editor-metadata-tab';

export enum BundIdAttribute {
    Salutation = 'salutation',
    Title = 'title',
    GivenName = 'given_name',
    FamilyName = 'family_name',
    Name = 'name',
    DateOfBirth = 'date_of_birth',
    PlaceOfBirth = 'place_of_birth',
    BirthName = 'birth_name',
    Nationality = 'nationality',
    StreetAndBuilding = 'street_and_building',
    ZipCode = 'zip_code',
    City = 'city',
    Country = 'country',
    Email = 'email',
    Telephone = 'telephone',
    BPk2 = 'bPK2',
    LegacyPostkorbHandle = 'legacy_postkorb_handle',
    TrustLevelAuthentication = 'trust_level_authentication',
    DocumentType = 'document_type',
    EIdasIssuingCountry = 'e_idas_issuing_country',
    AssertionProvedBy = 'assertion_proved_by',
    ApiVersion = 'api_version',
}


export const BundIdAttributes: ElementAttributeMappingOption[] = [
    {
        label: 'Anrede',
        value: BundIdAttribute.Salutation,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Akademischer Titel',
        value: BundIdAttribute.Title,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Vorname(n)',
        value: BundIdAttribute.GivenName,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Nachname',
        value: BundIdAttribute.FamilyName,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Vollständiger Name',
        value: BundIdAttribute.Name,
        limit: [
            ElementType.Text,
        ],
    },

    {
        label: 'Geburtsdatum',
        value: BundIdAttribute.DateOfBirth,
        limit: [
            ElementType.Date,
        ],
    },
    {
        label: 'Geburtsort',
        value: BundIdAttribute.PlaceOfBirth,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Geburtsname',
        value: BundIdAttribute.BirthName,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Staatsangehörigkeit',
        value: BundIdAttribute.Nationality,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },

    {
        label: 'Straße und Hausnummer',
        value: BundIdAttribute.StreetAndBuilding,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Postleitzahl',
        value: BundIdAttribute.ZipCode,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Ort',
        value: BundIdAttribute.City,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Land',
        value: BundIdAttribute.Country,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },

    {
        label: 'E-Mail-Adresse',
        value: BundIdAttribute.Email,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Telefonnummer',
        value: BundIdAttribute.Telephone,
        limit: [
            ElementType.Text,
        ],
    },

    {
        label: 'bPK2',
        value: BundIdAttribute.BPk2,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Postfach',
        value: BundIdAttribute.LegacyPostkorbHandle,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Vertrauensniveau',
        value: BundIdAttribute.TrustLevelAuthentication,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Dokumententyp',
        value: BundIdAttribute.DocumentType,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'eIDAS-Issuing-Country',
        value: BundIdAttribute.EIdasIssuingCountry,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'AssertionProvedBy',
        value: BundIdAttribute.AssertionProvedBy,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Version',
        value: BundIdAttribute.ApiVersion,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
];


export function transformBundIdAttribute(key: string, value: string): any {
    switch (key) {
        case BundIdAttribute.DateOfBirth:
            return parse(value, 'yyyy-MM-dd', new Date()).toISOString();
        default:
            return value;
    }
}

