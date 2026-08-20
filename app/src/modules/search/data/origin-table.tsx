import {ReactNode} from 'react';
import {ModuleIcons} from '../../../shells/staff/data/module-icons';
import {ServerEntityType} from '../../../shells/staff/data/server-entity-type';

export const OriginTableLabels: Record<ServerEntityType, string> = {
    [ServerEntityType.Assets]: 'Datei / Medieninhalt',
    [ServerEntityType.Departments]: 'Organisationseinheiten',
    [ServerEntityType.DataObjectItems]: 'Datenobjekt',
    [ServerEntityType.DataObjectSchemas]: 'Datenmodell',
    [ServerEntityType.IdentityProviders]: 'Nutzerkontenanbieter',
    [ServerEntityType.PaymentProviders]: 'Zahlungsanbieter',
    [ServerEntityType.Presets]: 'Vorlage',
    [ServerEntityType.Secrets]: 'Geheimnis',
    [ServerEntityType.Teams]: 'Team',
    [ServerEntityType.Users]: 'Mitarbeiter:in',
    [ServerEntityType.Themes]: 'Erscheinungsbild',
    [ServerEntityType.DomainRoles]: 'Domänenrolle',
    [ServerEntityType.SystemRoles]: 'Systemrolle',
    [ServerEntityType.StorageProviders]: 'Speicheranbieter',
    [ServerEntityType.Processes]: 'Prozess',
    [ServerEntityType.ProcessInstances]: 'Vorgang',
    [ServerEntityType.ProcessNodes]: 'Formular',
    [ServerEntityType.CodeLists]: 'Codeliste',
};

export const OriginTableIcons: Record<ServerEntityType, ReactNode> = {
    [ServerEntityType.Assets]: ModuleIcons.assets,
    [ServerEntityType.Departments]: ModuleIcons.departments,
    [ServerEntityType.DataObjectItems]: ModuleIcons.dataObjects,
    [ServerEntityType.DataObjectSchemas]: ModuleIcons.dataObjects,
    [ServerEntityType.IdentityProviders]: ModuleIcons.identity,
    [ServerEntityType.PaymentProviders]: ModuleIcons.payment,
    [ServerEntityType.Presets]: ModuleIcons.presets,
    [ServerEntityType.Secrets]: ModuleIcons.secrets,
    [ServerEntityType.Teams]: ModuleIcons.teams,
    [ServerEntityType.Users]: ModuleIcons.users,
    [ServerEntityType.Themes]: ModuleIcons.themes,
    [ServerEntityType.DomainRoles]: ModuleIcons.roles,
    [ServerEntityType.SystemRoles]: ModuleIcons.roles,
    [ServerEntityType.StorageProviders]: ModuleIcons.storage,
    [ServerEntityType.Processes]: ModuleIcons.processes,
    [ServerEntityType.ProcessInstances]: ModuleIcons.submissions,
    [ServerEntityType.ProcessNodes]: ModuleIcons.forms,
    [ServerEntityType.CodeLists]: ModuleIcons.codeLists,
};
