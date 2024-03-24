import {ElementAttributeMappingOption} from '../components/element-editor-metadata-tab/element-editor-metadata-tab';
import {ElementType} from './element-type/element-type';
import {formatISO, parse} from 'date-fns';

export enum MukAttribute {
    ElsterIdentitaetstyp = 'elster_identitaetstyp',
    OrgCompanyName = 'org_company_name',
    OrgLegalFormText = 'org_legal_form_text',
    OrgLegalFormCode = 'org_legal_form_code',
    OrgOccupationText = 'org_occupation_text',
    OrgOccupationCode = 'org_occupation_code',
    OrgRegisterNumber = 'org_register_number',
    OrgRegisterType = 'org_register_type',
    OrgRegisterCourt = 'org_register_court',
    Salutation = 'salutation',
    Title = 'title',
    GivenName = 'given_name',
    FamilyName = 'family_name',
    Name = 'name',
    ZusatzName = 'zusatz_name',
    DateOfBirth = 'date_of_birth',
    OrgAddressType = 'org_address_type',
    OrgStreet = 'org_street',
    OrgBuilding = 'org_building',
    OrgZipCode = 'org_zip_code',
    OrgCity = 'org_city',
    OrgCityDistrict = 'org_city_district',
    OrgAddressAddition = 'org_address_addition',
    OrgCountry = 'org_country',
    PreferredUsername = 'preferred_username',
    ElsterDatenuebermittler = 'elster_datenuebermittler',
    ElsterDatenkranzTyp = 'elster_datenkranz_typ',
    TrustLevelIdentification = 'trust_level_identification',
    TrustLevelAuthentication = 'trust_level_authentication',
}

export const MukAttributes: ElementAttributeMappingOption[] = [
    {
        label: 'Identitätstyp (PersTyp)',
        value: MukAttribute.ElsterIdentitaetstyp,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Firmenname',
        value: MukAttribute.OrgCompanyName,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Rechtsform (RechtsformText)',
        value: MukAttribute.OrgLegalFormText,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Rechtsform Code (Rechtsform)',
        value: MukAttribute.OrgLegalFormCode,
        limit: [
            ElementType.Text,
            ElementType.Number,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Tätigkeit (TaetigkeitText)',
        value: MukAttribute.OrgOccupationText,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Tätigkeit Code (Taetigkeit)',
        value: MukAttribute.OrgOccupationCode,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Registernummer',
        value: MukAttribute.OrgRegisterNumber,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Registerart',
        value: MukAttribute.OrgRegisterType,
        limit: [
            ElementType.Text,
            ElementType.Radio,
            ElementType.Select,
        ],
    },
    {
        label: 'Registergericht',
        value: MukAttribute.OrgRegisterCourt,
        limit: [
            ElementType.Text,
            ElementType.Radio,
            ElementType.Select,
        ],
    },
    {
        label: 'Handelnde Person: Namensvorsatz',
        value: MukAttribute.Salutation,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Handelnde Person: Akademischer Grad',
        value: MukAttribute.Title,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Handelnde Person: Vorname',
        value: MukAttribute.GivenName,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Handelnde Person: Nachname',
        value: MukAttribute.FamilyName,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Handelnde Person: Vollständiger Name',
        value: MukAttribute.Name,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Handelnde Person: Namenszusatz',
        value: MukAttribute.ZusatzName,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Handelnde Person: Geburtsdatum',
        value: MukAttribute.DateOfBirth,
        limit: [
            ElementType.Date,
        ],
    },
    {
        label: 'Unternehmensanschrift: Typ',
        value: MukAttribute.OrgAddressType,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Unternehmensanschrift: Straße',
        value: MukAttribute.OrgStreet,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Unternehmensanschrift: Hausnummer',
        value: MukAttribute.OrgBuilding,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Unternehmensanschrift: Postleitzahl',
        value: MukAttribute.OrgZipCode,
        limit: [
            ElementType.Text,
            ElementType.Radio,
            ElementType.Select,
        ],
    },
    {
        label: 'Unternehmensanschrift: Ort',
        value: MukAttribute.OrgCity,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Unternehmensanschrift: Ortsteil',
        value: MukAttribute.OrgCityDistrict,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Unternehmensanschrift: Adressergänzung',
        value: MukAttribute.OrgAddressAddition,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Unternehmensanschrift: Land',
        value: MukAttribute.OrgCountry,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'ELSTER ID',
        value: MukAttribute.PreferredUsername,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Datenübermittler Pseudonym ID',
        value: MukAttribute.ElsterDatenuebermittler,
        limit: [
            ElementType.Text,
        ],
    },
    {
        label: 'Datenkranz Typ',
        value: MukAttribute.ElsterDatenkranzTyp,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Vertrauensniveau Identifizierung (ElsterVertrauensniveauIdentifizierung)',
        value: MukAttribute.TrustLevelIdentification,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
    {
        label: 'Vertrauensniveau Authentifizierung (ElsterVertrauensniveauAuthentifizierung)',
        value: MukAttribute.TrustLevelAuthentication,
        limit: [
            ElementType.Text,
            ElementType.Select,
            ElementType.Radio,
        ],
    },
];

export function transformMukAttribute(key: string, value: string): any {
    switch (key) {
        case 'date_of_birth':
            return parse(value, 'dd.MM.yyyy', new Date()).toISOString();
        default:
            return value;
    }
}