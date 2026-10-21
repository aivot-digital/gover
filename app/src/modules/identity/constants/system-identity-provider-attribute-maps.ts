import {ElementType} from '../../../data/element-type/element-type';
import {BayernIdAttribute, BundIdAttribute, MukAttribute, ShIdAttribute} from './system-identity-provider-attributes';

export const BayernIdAttributes: Record<BayernIdAttribute, ElementType[]> = {
    [BayernIdAttribute.Salutation]: [
        ElementType.Text,
        ElementType.Select,
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
    ],
    [BayernIdAttribute.StreetAndBuilding]: [
        ElementType.Text,
    ],
    [BayernIdAttribute.ZipCode]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [BayernIdAttribute.City]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [BayernIdAttribute.CommunityId]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [BayernIdAttribute.Country]: [
        ElementType.Text,
        ElementType.Select,
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
    ],
    [BayernIdAttribute.DocumentType]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [BayernIdAttribute.EIdasIssuingCountry]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [BayernIdAttribute.Pseudonym]: [
        ElementType.Text,
    ],
    [BayernIdAttribute.AssertionProvedBy]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [BayernIdAttribute.ApiVersion]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [BayernIdAttribute.ApplicationId]: [
        ElementType.Text,
    ],
};

export const BundIdAttributes: Record<BundIdAttribute, ElementType[]> = {
    [BundIdAttribute.Salutation]: [
        ElementType.Text,
        ElementType.Select,
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
    ],
    [BundIdAttribute.StreetAndBuilding]: [
        ElementType.Text,
    ],
    [BundIdAttribute.ZipCode]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [BundIdAttribute.City]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [BundIdAttribute.CommunityId]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [BundIdAttribute.Country]: [
        ElementType.Text,
        ElementType.Select,
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
    ],
    [BundIdAttribute.DocumentType]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [BundIdAttribute.EIdasIssuingCountry]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [BundIdAttribute.Pseudonym]: [
        ElementType.Text,
    ],
    [BundIdAttribute.AssertionProvedBy]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [BundIdAttribute.ApiVersion]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [BundIdAttribute.ApplicationId]: [
        ElementType.Text,
    ],
};

export const MukAttributes: Record<MukAttribute, ElementType[]> = {
    [MukAttribute.ElsterIdentitaetstyp]: [
        ElementType.Text,
        ElementType.Select,
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
    ],
    [MukAttribute.OrgOccupationText]: [
        ElementType.Text,
    ],
    [MukAttribute.OrgOccupationCode]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [MukAttribute.OrgRegisterNumber]: [
        ElementType.Text,
    ],
    [MukAttribute.OrgRegisterType]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [MukAttribute.OrgRegisterCourt]: [
        ElementType.Text,
        ElementType.Select,
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
    ],
    [MukAttribute.OrgCity]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [MukAttribute.OrgCityDistrict]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [MukAttribute.OrgAddressAddition]: [
        ElementType.Text,
    ],
    [MukAttribute.OrgCountry]: [
        ElementType.Text,
        ElementType.Select,
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
    ],
    [MukAttribute.TrustLevelIdentification]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [MukAttribute.TrustLevelAuthentication]: [
        ElementType.Text,
        ElementType.Select,
    ],
};

export const ShIdAttributes: Record<ShIdAttribute, ElementType[]> = {
    [ShIdAttribute.DataportIdentitaetstyp]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [ShIdAttribute.Salutation]: [
        ElementType.Text,
        ElementType.Select,
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
    ],
    [ShIdAttribute.StreetAndBuilding]: [
        ElementType.Text,
    ],
    [ShIdAttribute.ZipCode]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [ShIdAttribute.City]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [ShIdAttribute.Country]: [
        ElementType.Text,
        ElementType.Select,
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
    ],
    [ShIdAttribute.OrgCity]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [ShIdAttribute.OrgCountry]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [ShIdAttribute.OrgPostboxNumber]: [
        ElementType.Text,
    ],
    [ShIdAttribute.OrgPostboxZipCode]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [ShIdAttribute.OrgPostboxCountry]: [
        ElementType.Text,
        ElementType.Select,
    ],
    [ShIdAttribute.ElsterDatenuebermittler]: [
        ElementType.Text,
    ],
    [ShIdAttribute.TrustLevelAuthentication]: [
        ElementType.Text,
        ElementType.Select,
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
    ],
}