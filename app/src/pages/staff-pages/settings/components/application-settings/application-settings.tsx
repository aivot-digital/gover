import {Box, Button, Typography} from '@mui/material';
import React, {FormEvent, useState} from 'react';
import {fetchSystemConfig, selectSystemConfig, SystemConfigMap} from '../../../../../slices/system-config-slice';
import {SystemConfigsService} from '../../../../../services/system-configs-service';
import {useAppSelector} from '../../../../../hooks/use-app-selector';
import {useAppDispatch} from '../../../../../hooks/use-app-dispatch';
import {TextFieldComponent} from "../../../../../components/text-field/text-field-component";
import {SystemConfigKeys, SystemConfigPublic} from "../../../../../data/system-config-keys";
import {SelectFieldComponent} from "../../../../../components/select-field/select-field-component";
import {Themes} from "../../../../../theming/themes";


export function ApplicationSettings() {
    const dispatch = useAppDispatch();
    const config = useAppSelector(selectSystemConfig);

    const [editedConfig, setEditedConfig] = useState<SystemConfigMap>({});

    const handleSubmit = async (event: FormEvent) => {
        event.preventDefault();

        if (editedConfig != null) {
            for (const key of Object.keys(editedConfig)) {
                if (editedConfig[key] !== config[key]) {
                    await SystemConfigsService.update(key, {
                        key: key,
                        value: editedConfig[key],
                        publicConfig: SystemConfigPublic[key] ?? false,
                        created: "",
                        updated: "",
                    });
                }
            }
            setEditedConfig({});
            dispatch(fetchSystemConfig());
            (document.activeElement as HTMLInputElement)?.blur();
        }

        return false;
    };

    return (
        <form onSubmit={handleSubmit}>
            <Typography
                variant="subtitle1"
            >
                Über den Betreiber
            </Typography>

            <TextFieldComponent
                label="Name des Betreibers"
                placeholder="Komune XYZ"
                hint="Der Name wird in der Anwendung angezeigt"
                value={editedConfig[SystemConfigKeys.provider.name] ?? config[SystemConfigKeys.provider.name]}
                onChange={val => setEditedConfig({
                    ...editedConfig,
                    [SystemConfigKeys.provider.name]: val ?? '',
                })}
                required
            />

            <Typography
                variant="subtitle1"
                sx={{mt: 2}}
            >
                Über die Gover-Instanz
            </Typography>

            <SelectFieldComponent
                label="Standard-Theme"
                options={Themes.map(label => ({value: label, label: label}))}
                value={editedConfig[SystemConfigKeys.system.theme] ?? config[SystemConfigKeys.system.theme]}
                onChange={val => setEditedConfig({
                    ...editedConfig,
                    [SystemConfigKeys.system.theme]: val ?? '',
                })}
                required
            />

            <Typography
                variant="subtitle1"
                sx={{mt: 2}}
            >
                Gover Store
            </Typography>

            <TextFieldComponent
                label="Schlüssel für den Gover Store"
                placeholder="b721fe43-5800-40a3-ae7f-d19274dd72f1"
                hint="Geben Sie hier Ihren Schlüssel für den Gover Store ein, wenn Sie eigene Formulare und/oder Vorlagen im Gover Store veröffentlichen wollen."
                value={editedConfig[SystemConfigKeys.gover.storeKey] ?? config[SystemConfigKeys.gover.storeKey]}
                onChange={val => setEditedConfig({
                    ...editedConfig,
                    [SystemConfigKeys.gover.storeKey]: val ?? '',
                })}
            />

            <Box sx={{mt: 2}}>
                <Button
                    type="submit"
                    disabled={Object.keys(editedConfig).length === 0}
                >
                    Speichern
                </Button>

                <Button
                    sx={{ml: 2}}
                    type="button"
                    color="error"
                    disabled={Object.keys(editedConfig).length === 0}
                    onClick={() => {
                        setEditedConfig({});
                    }}
                >
                    Zurücksetzen
                </Button>
            </Box>
        </form>
    );
}
