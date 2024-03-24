import {Box, Button, Typography} from '@mui/material';
import React, {type FormEvent, useEffect, useState} from 'react';
import {selectSystemConfig, setSystemConfigs, type SystemConfigMap} from '../../../../../slices/system-config-slice';
import {useAppSelector} from '../../../../../hooks/use-app-selector';
import {useAppDispatch} from '../../../../../hooks/use-app-dispatch';
import {TextFieldComponent} from '../../../../../components/text-field/text-field-component';
import {SystemConfigKeys, SystemConfigPublic} from '../../../../../data/system-config-keys';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../../slices/snackbar-slice';
import {shallowEquals} from '../../../../../utils/equality-utils';
import {SelectFieldComponent} from '../../../../../components/select-field/select-field-component';
import {type SelectFieldComponentOption} from '../../../../../components/select-field/select-field-component-option';
import {useThemesApi} from '../../../../../hooks/use-themes-api';
import {useApi} from '../../../../../hooks/use-api';
import {useSystemConfigsApi} from "../../../../../hooks/use-system-configs-api";

export function ApplicationSettings(): JSX.Element {
    const dispatch = useAppDispatch();
    const api = useApi();

    const config = useAppSelector(selectSystemConfig);
    const [editedConfig, setEditedConfig] = useState<SystemConfigMap>({});

    const [themes, setThemes] = useState<SelectFieldComponentOption[]>([]);

    const hasNotChanged = shallowEquals(config, editedConfig);

    useEffect(() => {
        useThemesApi(api)
            .listThemes()
            .then((themes) => {
                setThemes(themes.map((theme) => ({
                    value: theme.id.toString(),
                    label: theme.name,
                })));
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Farbschemata konnten nicht geladen werden'));
            });
    }, []);

    const handleSubmit = (event: FormEvent): void => {
        event.preventDefault();

        if (editedConfig != null) {
            const updatedConfigs = Object
                .keys(editedConfig)
                .filter((key) => editedConfig[key] !== config[key])
                .map((key) => ({
                    key,
                    value: editedConfig[key],
                }));

            const updatePromises = updatedConfigs
                .map(config => useSystemConfigsApi(api).save(config.key, config.value));

            Promise.all(updatePromises)
                .then((configs) => {
                    dispatch(showSuccessSnackbar('Konfiguration erfolgreich gespeichert'));
                    return dispatch(setSystemConfigs(configs));
                })
                .catch((err) => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Konfiguration konnte nicht gespeichert werden'));
                });
        }
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
                onChange={(val) => {
                    setEditedConfig({
                        ...editedConfig,
                        [SystemConfigKeys.provider.name]: val ?? '',
                    });
                }}
                required
            />

            {
                themes.length > 0 &&
                <>
                    <Typography
                        variant="subtitle1"
                        sx={{
                            mt: 2,
                        }}
                    >
                        Farbschema der Gover-Instanz
                    </Typography>

                    <SelectFieldComponent
                        label="Farbschema"
                        options={themes}
                        value={editedConfig[SystemConfigKeys.system.theme] ?? config[SystemConfigKeys.system.theme]}
                        onChange={(val) => {
                            setEditedConfig({
                                ...editedConfig,
                                [SystemConfigKeys.system.theme]: val ?? '',
                            });
                        }}
                    />
                </>
            }

            <Typography
                variant="subtitle1"
                sx={{
                    mt: 2,
                }}
            >
                Gover Store
            </Typography>

            <TextFieldComponent
                label="Schlüssel für den Gover Store"
                placeholder="b721fe43-5800-40a3-ae7f-d19274dd72f1"
                hint="Geben Sie hier Ihren Schlüssel für den Gover Store ein, wenn Sie eigene Formulare und/oder Vorlagen im Gover Store veröffentlichen wollen."
                value={editedConfig[SystemConfigKeys.gover.storeKey] ?? config[SystemConfigKeys.gover.storeKey]}
                onChange={(val) => {
                    setEditedConfig({
                        ...editedConfig,
                        [SystemConfigKeys.gover.storeKey]: val ?? '',
                    });
                }}
            />

            <Box
                sx={{
                    mt: 2,
                }}
            >
                <Button
                    type="submit"
                    disabled={hasNotChanged}
                >
                    Speichern
                </Button>

                <Button
                    sx={{
                        ml: 2,
                    }}
                    type="button"
                    color="error"
                    disabled={hasNotChanged}
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
