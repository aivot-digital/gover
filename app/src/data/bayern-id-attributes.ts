import {ElementType} from './element-type/element-type';
import {ElementAttributeMappingOption} from '../components/element-editor-metadata-tab/element-editor-metadata-tab';
import {parse} from 'date-fns';

export enum BayernIdAttribute {
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

export const BayernIdAttributes: ElementAttributeMappingOption[] = [
    {
        label: 'Anrede',
        value: BayernIdAttribute.Salutation,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Akademischer Titel',
        value: BayernIdAttribute.Title,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Vorname(n)',
        value: BayernIdAttribute.GivenName,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Nachname',
        value: BayernIdAttribute.FamilyName,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Vollständiger Name',
        value: BayernIdAttribute.Name,
        limit: [
            ElementType.Text,
        ],
    },

    {
        label: 'Geburtsdatum',
        value: BayernIdAttribute.DateOfBirth,
        limit: [
            ElementType.Date,
        ],
    },
    {
        label: 'Geburtsort',
        value: BayernIdAttribute.PlaceOfBirth,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Geburtsname',
        value: BayernIdAttribute.BirthName,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Staatsangehörigkeit',
        value: BayernIdAttribute.Nationality,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },

    {
        label: 'Straße und Hausnummer',
        value: BayernIdAttribute.StreetAndBuilding,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Postleitzahl',
        value: BayernIdAttribute.ZipCode,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Ort',
        value: BayernIdAttribute.City,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Land',
        value: BayernIdAttribute.Country,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },

    {
        label: 'E-Mail-Adresse',
        value: BayernIdAttribute.Email,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Telefonnummer',
        value: BayernIdAttribute.Telephone,
        limit: [
            ElementType.Text,
        ],
    },

    {
        label: 'bPK2',
        value: BayernIdAttribute.BPk2,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Postfach',
        value: BayernIdAttribute.LegacyPostkorbHandle,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Vertrauensniveau',
        value: BayernIdAttribute.TrustLevelAuthentication,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Dokumententyp',
        value: BayernIdAttribute.DocumentType,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'eIDAS-Issuing-Country',
        value: BayernIdAttribute.EIdasIssuingCountry,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'AssertionProvedBy',
        value: BayernIdAttribute.AssertionProvedBy,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Version',
        value: BayernIdAttribute.ApiVersion,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
];

export function transformBayernIdAttribute(key: string, value: string): any {
    switch (key) {
        case BayernIdAttribute.DateOfBirth:
            return parse(value, 'yyyy-MM-dd', new Date()).toISOString();
        default:
            return value;
    }
}