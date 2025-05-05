import {ElementType} from '../../../data/element-type/element-type';
import {BayernIdAttribute, BundIdAttribute, MukAttribute, ShIdAttribute} from './system-identity-provider-attributes';

export const BayernIdAttributes: Record<BayernIdAttribute, ElementType[]> = {
    [BayernIdAttribute.Salutation]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [BayernIdAttribute.Title]: [
        ElementType.Text,
    ],
    [BayernIdAttribute.GivenName]: [
        ElementType.Text,
    ],
    [BayernIdAttribute.FamilyName]: [
        ElementType.Text,
    ],
    [BayernIdAttribute.Name]: [
        ElementType.Text,
    ],
    [BayernIdAttribute.DateOfBirth]: [
        ElementType.Date,
    ],
    [BayernIdAttribute.PlaceOfBirth]: [
        ElementType.Text,
    ],
    [BayernIdAttribute.BirthName]: [
        ElementType.Text,
    ],
    [BayernIdAttribute.Nationality]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [BayernIdAttribute.StreetAndBuilding]: [
        ElementType.Text,
    ],
    [BayernIdAttribute.ZipCode]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [BayernIdAttribute.City]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [BayernIdAttribute.CommunityId]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [BayernIdAttribute.Country]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [BayernIdAttribute.Email]: [
        ElementType.Text,
    ],
    [BayernIdAttribute.Telephone]: [
        ElementType.Text,
    ],
    [BayernIdAttribute.BPk2]: [
        ElementType.Text,
    ],
    [BayernIdAttribute.LegacyPostkorbHandle]: [
        ElementType.Text,
    ],
    [BayernIdAttribute.TrustLevelAuthentication]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [BayernIdAttribute.DocumentType]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [BayernIdAttribute.EIdasIssuingCountry]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [BayernIdAttribute.Pseudonym]: [
        ElementType.Text,
    ],
    [BayernIdAttribute.AssertionProvedBy]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [BayernIdAttribute.ApiVersion]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [BayernIdAttribute.ApplicationId]: [
        ElementType.Text,
    ],
};

export const BundIdAttributes: Record<BundIdAttribute, ElementType[]> = {
    [BundIdAttribute.Salutation]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [BundIdAttribute.Title]: [
        ElementType.Text,
    ],
    [BundIdAttribute.GivenName]: [
        ElementType.Text,
    ],
    [BundIdAttribute.FamilyName]: [
        ElementType.Text,
    ],
    [BundIdAttribute.Name]: [
        ElementType.Text,
    ],
    [BundIdAttribute.DateOfBirth]: [
        ElementType.Date,
    ],
    [BundIdAttribute.PlaceOfBirth]: [
        ElementType.Text,
    ],
    [BundIdAttribute.BirthName]: [
        ElementType.Text,
    ],
    [BundIdAttribute.Nationality]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [BundIdAttribute.StreetAndBuilding]: [
        ElementType.Text,
    ],
    [BundIdAttribute.ZipCode]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [BundIdAttribute.City]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [BundIdAttribute.CommunityId]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [BundIdAttribute.Country]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [BundIdAttribute.Email]: [
        ElementType.Text,
    ],
    [BundIdAttribute.Telephone]: [
        ElementType.Text,
    ],
    [BundIdAttribute.BPk2]: [
        ElementType.Text,
    ],
    [BundIdAttribute.LegacyPostkorbHandle]: [
        ElementType.Text,
    ],
    [BundIdAttribute.TrustLevelAuthentication]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [BundIdAttribute.DocumentType]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [BundIdAttribute.EIdasIssuingCountry]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [BundIdAttribute.Pseudonym]: [
        ElementType.Text,
    ],
    [BundIdAttribute.AssertionProvedBy]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [BundIdAttribute.ApiVersion]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [BundIdAttribute.ApplicationId]: [
        ElementType.Text,
    ],
};

export const MukAttributes: Record<MukAttribute, ElementType[]> = {
    [MukAttribute.ElsterIdentitaetstyp]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [MukAttribute.OrgCompanyName]: [
        ElementType.Text,
    ],
    [MukAttribute.OrgLegalFormText]: [
        ElementType.Text,
    ],
    [MukAttribute.OrgLegalFormCode]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [MukAttribute.OrgOccupationText]: [
        ElementType.Text,
    ],
    [MukAttribute.OrgOccupationCode]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [MukAttribute.OrgRegisterNumber]: [
        ElementType.Text,
    ],
    [MukAttribute.OrgRegisterType]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [MukAttribute.OrgRegisterCourt]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [MukAttribute.Salutation]: [
        ElementType.Text,
    ],
    [MukAttribute.Title]: [
        ElementType.Text,
    ],
    [MukAttribute.GivenName]: [
        ElementType.Text,
    ],
    [MukAttribute.FamilyName]: [
        ElementType.Text,
    ],
    [MukAttribute.Name]: [
        ElementType.Text,
    ],
    [MukAttribute.ZusatzName]: [
        ElementType.Text,
    ],
    [MukAttribute.DateOfBirth]: [
        ElementType.Date,
    ],
    [MukAttribute.OrgAddressType]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [MukAttribute.OrgStreet]: [
        ElementType.Text,
    ],
    [MukAttribute.OrgBuilding]: [
        ElementType.Text,
    ],
    [MukAttribute.OrgZipCode]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [MukAttribute.OrgCity]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [MukAttribute.OrgCityDistrict]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [MukAttribute.OrgAddressAddition]: [
        ElementType.Text,
    ],
    [MukAttribute.OrgCountry]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [MukAttribute.PreferredUsername]: [
        ElementType.Text,
    ],
    [MukAttribute.ElsterDatenuebermittler]: [
        ElementType.Text,
    ],
    [MukAttribute.ElsterDatenkranzTyp]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [MukAttribute.TrustLevelIdentification]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [MukAttribute.TrustLevelAuthentication]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
};

export const ShIdAttributes: Record<ShIdAttribute, ElementType[]> = {
    [ShIdAttribute.DataportIdentitaetstyp]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [ShIdAttribute.Salutation]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [ShIdAttribute.Title]: [
        ElementType.Text,
    ],
    [ShIdAttribute.GivenName]: [
        ElementType.Text,
    ],
    [ShIdAttribute.FamilyName]: [
        ElementType.Text,
    ],
    [ShIdAttribute.Name]: [
        ElementType.Text,
    ],
    [ShIdAttribute.DateOfBirth]: [
        ElementType.Date,
    ],
    [ShIdAttribute.PlaceOfBirth]: [
        ElementType.Text,
    ],
    [ShIdAttribute.BirthName]: [
        ElementType.Text,
    ],
    [ShIdAttribute.Gender]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [ShIdAttribute.StreetAndBuilding]: [
        ElementType.Text,
    ],
    [ShIdAttribute.ZipCode]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [ShIdAttribute.City]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [ShIdAttribute.Country]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [ShIdAttribute.Email]: [
        ElementType.Text,
    ],
    [ShIdAttribute.Telephone]: [
        ElementType.Text,
    ],
    [ShIdAttribute.Mobilephone]: [
        ElementType.Text,
    ],
    [ShIdAttribute.PreferredLanguage]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [ShIdAttribute.OrgCompanyName]: [
        ElementType.Text,
    ],
    [ShIdAttribute.OrgOrganizationUnit]: [
        ElementType.Text,
    ],
    [ShIdAttribute.OrgRegisterNumber]: [
        ElementType.Text,
    ],
    [ShIdAttribute.OrgRegisterCourt]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [ShIdAttribute.OrgContactMail]: [
        ElementType.Text,
    ],
    [ShIdAttribute.OrgContactPhone]: [
        ElementType.Text,
    ],
    [ShIdAttribute.OrgStreetAndBuilding]: [
        ElementType.Text,
    ],
    [ShIdAttribute.OrgZipCode]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [ShIdAttribute.OrgCity]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [ShIdAttribute.OrgCountry]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [ShIdAttribute.OrgPostboxNumber]: [
        ElementType.Text,
    ],
    [ShIdAttribute.OrgPostboxZipCode]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [ShIdAttribute.OrgPostboxCountry]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [ShIdAttribute.ElsterDatenuebermittler]: [
        ElementType.Text,
    ],
    [ShIdAttribute.TrustLevelAuthentication]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
    [ShIdAttribute.PreferredUsername]: [
        ElementType.Text,
    ],
    [ShIdAttribute.DataportInboxId]: [
        ElementType.Text,
    ],
    [ShIdAttribute.DataportServicekontoType]: [
        ElementType.Text,
        ElementType.Select,
        ElementType.Radio,
    ],
}