import {ReactNode} from 'react';
import {ModuleIcons} from '../../../shells/staff/data/module-icons';

export const OriginTableLabels: Record<string, string> = {
    'assets': 'Datei / Medieninhalt',
    'departments': 'Fachbereich',
    'data_object_items': 'Datenobjekt',
    'data_object_schemas': 'Datenobjektschema',
    'destinations': 'Schnittstelle',
    'forms': 'Formular',
    'identity_providers': 'Nutzerkontenanbieter',
    'payment_providers': 'Zahlungsanbieter',
    'presets': 'Vorlage',
    'provider_links': 'Link',
    'secrets': 'Geheimnis',
    'submissions': 'Vorgang',
    'themes': 'Farbschema',
};

export const OriginTableIcons: Record<string, ReactNode> = {
    'assets': ModuleIcons.assets,
    'data_object_items': ModuleIcons.dataObjects,
    'data_object_schemas': ModuleIcons.dataObjects,
    'departments': ModuleIcons.departments,
    'destinations': ModuleIcons.destinations,
    'forms': ModuleIcons.forms,
    'identity_providers': ModuleIcons.identity,
    'payment_providers': ModuleIcons.payment,
    'presets': ModuleIcons.presets,
    'provider_links': ModuleIcons.providerLinks,
    'secrets': ModuleIcons.secrets,
    'submissions': ModuleIcons.submissions,
    'themes': ModuleIcons.themes,
};
