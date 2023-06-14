import React from 'react';
import {SystemConfigKeys} from '../../../../../data/system-config-keys';
import {SettingsGenericConfig} from './settings-generic-config';
import {Themes} from '../../../../../theming/themes';
import strings from './application-settings-strings.json';
import {Localization} from '../../../../../locale/localization';

const __ = Localization(strings);

// TODO: Refactor this settings because its an overkill for the use case.

const fields = [
    __.providerTitle,
    {
        label: __.nameLabel,
        placeholder: __.namePlaceholder,
        hint: __.nameHint,
        key: SystemConfigKeys.provider.name,
    },
    __.installationTitle,
    {
        label: __.hostnameLabel,
        placeholder: __.hostnamePlaceholder,
        hint: __.hostnameHint,
        key: SystemConfigKeys.system.host,
        // TODO: Validate input for trailing slash
    },
    {
        label: __.defaultThemeLabel,
        hint: __.defaultThemeHint,
        key: SystemConfigKeys.system.theme,
        options: Themes.map(label => [label, label]) as [string, string][], // TODO: Localize theme names
    },
    'Gover Store',
    {
        label: 'Schlüssel für den Gover Store',
        hint: 'Geben Sie hier ihren Schlüssel für den Gover Store ein, wenn Sie eigene Formulare oder Vorlagen im Gover Store veröffentlichen wollen.',
        key: SystemConfigKeys.gover.storeKey,
    },
];

export const SettingsProviderConfig = () => <SettingsGenericConfig fields={fields}/>;
