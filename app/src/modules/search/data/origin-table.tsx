import {ReactNode} from 'react';
import {ModuleIcons} from '../../../shells/staff/data/module-icons';
import {ServerEntityType} from '../../../shells/staff/data/server-entity-type';

export const OriginTableLabels: Record<ServerEntityType, string> = {
    [ServerEntityType.Assets]: 'Datei / Medieninhalt',
    [ServerEntityType.Departments]: 'Fachbereich',
    [ServerEntityType.DataObjectItems]: 'Datenobjekt',
    [ServerEntityType.DataObjectSchemas]: 'Datenmodell',
    [ServerEntityType.Destinations]: 'Schnittstelle',
    [ServerEntityType.Forms]: 'Formular',
    [ServerEntityType.IdentityProviders]: 'Nutzerkontenanbieter',
    [ServerEntityType.PaymentProviders]: 'Zahlungsanbieter',
    [ServerEntityType.Presets]: 'Vorlage',
    [ServerEntityType.ProviderLinks]: 'Link',
    [ServerEntityType.Secrets]: 'Geheimnis',
    [ServerEntityType.Submissions]: 'Vorgang',
    [ServerEntityType.Themes]: 'Farbschema',
};

export const OriginTableIcons: Record<ServerEntityType, ReactNode> = {
    [ServerEntityType.Assets]: ModuleIcons.assets,
    [ServerEntityType.Departments]: ModuleIcons.dataObjects,
    [ServerEntityType.DataObjectItems]: ModuleIcons.dataObjects,
    [ServerEntityType.DataObjectSchemas]: ModuleIcons.departments,
    [ServerEntityType.Destinations]: ModuleIcons.destinations,
    [ServerEntityType.Forms]: ModuleIcons.forms,
    [ServerEntityType.IdentityProviders]: ModuleIcons.identity,
    [ServerEntityType.PaymentProviders]: ModuleIcons.payment,
    [ServerEntityType.Presets]: ModuleIcons.presets,
    [ServerEntityType.ProviderLinks]: ModuleIcons.providerLinks,
    [ServerEntityType.Secrets]: ModuleIcons.secrets,
    [ServerEntityType.Submissions]: ModuleIcons.submissions,
    [ServerEntityType.Themes]: ModuleIcons.themes,
};
