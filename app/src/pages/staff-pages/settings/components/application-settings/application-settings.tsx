import {Box, Button, Grid, Typography} from '@mui/material';
import React, {type FormEvent, useEffect, useState} from 'react';
import {selectSystemConfig, setSystemConfigs, type SystemConfigMap} from '../../../../../slices/system-config-slice';
import {useAppSelector} from '../../../../../hooks/use-app-selector';
import {useAppDispatch} from '../../../../../hooks/use-app-dispatch';
import {TextFieldComponent} from '../../../../../components/text-field/text-field-component';
import {SystemConfigKeys} from '../../../../../data/system-config-keys';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../../slices/snackbar-slice';
import {SelectFieldComponent} from '../../../../../components/select-field/select-field-component';
import {type SelectFieldComponentOption} from '../../../../../components/select-field/select-field-component-option';
import {useApi} from '../../../../../hooks/use-api';
import type {Department} from "../../../../../modules/departments/models/department";
import {CheckboxFieldComponent} from "../../../../../components/checkbox-field/checkbox-field-component";
import SaveOutlinedIcon from "@mui/icons-material/SaveOutlined";
import {DepartmentsApiService} from '../../../../../modules/departments/departments-api-service';
import {ThemesApiService} from '../../../../../modules/themes/themes-api-service';
import {SystemConfigsApiService} from '../../../../../modules/configs/system-configs-api-service';

export function ApplicationSettings(): JSX.Element {
    const dispatch = useAppDispatch();
    const api = useApi();

    const config = useAppSelector(selectSystemConfig);
    const [editedConfig, setEditedConfig] = useState<SystemConfigMap>({});

    const [departments, setDepartments] = useState<Department[]>([]);
    const [themes, setThemes] = useState<SelectFieldComponentOption[]>([]);

    const hasNotChanged = Object.keys(editedConfig).length === 0;//shallowEquals(config, editedConfig);

    useEffect(() => {
        new ThemesApiService(api)
            .list(0, 999, undefined, undefined, {})
            .then((themes) => {
                setThemes(themes.content.map((theme) => ({
                    value: theme.id.toString(),
                    label: theme.name,
                })));
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Farbschemata konnten nicht geladen werden'));
            });

        new SystemConfigsApiService(api)
            .listDefinitions()
            .then((definitions) => {
                // TODO: do something with definitions
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
                .map(config => {
                    return new SystemConfigsApiService(api).update(config.key, {value: config.value})
                });

            Promise.all(updatePromises)
                .then((configs) => {
                    dispatch(showSuccessSnackbar('Einstellungen erfolgreich gespeichert'));
                    setEditedConfig({});
                    return dispatch(setSystemConfigs(configs));
                })
                .catch((err) => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Einstellungen konnten nicht gespeichert werden'));
                });
        }
    };

    useEffect(() => {
        new DepartmentsApiService(api)
            .list(0, 999, undefined, undefined, {})
            .then(deps => setDepartments(deps.content))
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Die Liste der Fachbereiche konnte nicht geladen werden'));
            });
    }, []);

    const departmentOptions = departments.map((department) => ({
        value: department.id.toString(),
        label: department.name,
    }));

    return (
        <form onSubmit={handleSubmit}>
            <Typography
                variant="subtitle1"
            >
                Über den Betreiber
            </Typography>

            <Typography
                sx={{
                    maxWidth: 900,
                    mb: 1.6,
                }}
            >
                Hinterlegen Sie grundsätzliche Informationen über den Betreiber dieses Systems. Diese Informationen werden in der Anwendung angezeigt und sind für die Nutzer:innen sichtbar.
            </Typography>

            <TextFieldComponent
                label="Name des Betreibers"
                placeholder="Bad Musterstadt"
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
                            mt: 4,
                        }}
                    >
                        Farbschema der Gover-Instanz
                    </Typography>

                    <Typography
                        sx={{
                            maxWidth: 900,
                            mb: 1.6,
                        }}
                    >
                        Sie können ein eigenes Farbschema für die Benutzeroberfläche auswählen, um Gover an Ihr Corporate Design anzugleichen (wird z.B. verwendet für Administrationsoberfläche und die Index-Seite der veröffentlichten Formulare).
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
                    mt: 4,
                }}
            >
                Gover Store
            </Typography>

            <Typography
                sx={{
                    maxWidth: 900,
                    mb: 1.6,
                }}
            >
                Im Gover Store finden Sie Bausteine und Formulare zur Nachnutzung. Wenn Sie eigene Formulare und/oder Bausteine im Gover Store zur Verfügung stellen möchten, benötigen Sie einen eigenen Schlüssel (API-Key).
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

            <Typography
                variant="h6"
                sx={{
                    mt: 4,
                }}
            >
                Öffentliche Auflistung der veröffentlichten Formulare (Index-Seite)
            </Typography>

            <Typography
                sx={{
                    maxWidth: 900,
                    mb: 1.6,
                }}
            >
                Wenn die Domain des Systems direkt aufgerufen wird, wird eine öffentliche Index-Seite
                angezeigt, die alle veröffentlichten Formulare auflistet. Hier können Sie diese Seite konfigurieren und ggf. deaktivieren.
            </Typography>

            <Grid
                container
                columnSpacing={4}
            >
                <Grid
                    item
                    xs={12}
                    lg={4}
                >
                    <SelectFieldComponent
                        label="Text für das Impressum"
                        value={editedConfig[SystemConfigKeys.provider.listingPage.imprintDepartmentId] ?? config[SystemConfigKeys.provider.listingPage.imprintDepartmentId]}
                        onChange={(val) => {
                            setEditedConfig({
                                ...editedConfig,
                                [SystemConfigKeys.provider.listingPage.imprintDepartmentId]: val ?? '',
                            });
                        }}
                        options={departmentOptions}
                    />

                </Grid>
                <Grid
                    item
                    xs={12}
                    lg={4}
                >
                    <SelectFieldComponent
                        label="Text für die Datenschutzerklärung"
                        value={editedConfig[SystemConfigKeys.provider.listingPage.privacyDepartmentId] ?? config[SystemConfigKeys.provider.listingPage.privacyDepartmentId]}
                        onChange={(val) => {
                            setEditedConfig({
                                ...editedConfig,
                                [SystemConfigKeys.provider.listingPage.privacyDepartmentId]: val ?? '',
                            });
                        }}
                        options={departmentOptions}
                    />
                </Grid>
                <Grid
                    item
                    xs={12}
                    lg={4}
                >
                    <SelectFieldComponent
                        label="Text für die Erklärung der Barrierefreiheit"
                        value={editedConfig[SystemConfigKeys.provider.listingPage.accessibilityDepartmentId] ?? config[SystemConfigKeys.provider.listingPage.accessibilityDepartmentId]}
                        onChange={(val) => {
                            setEditedConfig({
                                ...editedConfig,
                                [SystemConfigKeys.provider.listingPage.accessibilityDepartmentId]: val ?? '',
                            });
                        }}
                        options={departmentOptions}
                    />
                </Grid>
            </Grid>

            <Typography
                variant="caption"
                color={"text.secondary"}
            >
                Rechtstexte werden auf Fachbereichs-Ebene hinterlegt und verwaltet. Sie können hier die Fachbereiche auswählen, deren Texte Sie verwenden und anzeigen möchten.
            </Typography>

            <CheckboxFieldComponent
                label="Öffentliche Auflistung der veröffentlichten Formulare (in Form einer Index-Seite) vollständig deaktivieren"
                value={(editedConfig[SystemConfigKeys.provider.listingPage.disableGoverListingPage] ?? config[SystemConfigKeys.provider.listingPage.disableGoverListingPage]) == 'true'}
                onChange={(checked) => {
                    setEditedConfig({
                        ...editedConfig,
                        [SystemConfigKeys.provider.listingPage.disableGoverListingPage]: checked ? 'true' : '',
                    });
                }}
                hint="Bitte nehmen Sie zur Kenntnis, dass dies die Barrierefreiheit und Zugänglichkeit Ihrer Formulare beeinträchtigen kann."
            />

            <Typography
                variant="subtitle1"
                sx={{
                    mt: 4,
                }}
            >
                Verweis auf Formular-Index aus Formularen heraus
            </Typography>

            <Typography
                sx={{
                    maxWidth: 900,
                    mb: 1.6,
                }}
            >
                Am Ende eines jeden Formulars wird Ihre Index-Seite mit dem Text „Weitere Formulare“ verlinkt.
                Diese Verlinkung dient der Barrierefreiheit (gemäß <abbr title={'Web Content Accessibility Guidelines'}>WCAG</abbr> 2.1)
                und der Zugänglichkeit Ihrer Formulare. Sie können diesen Link deaktivieren oder gegen einen eigenen Link ersetzen
                (wenn Sie zum Beispiel alle Formulare auf Ihrer eigene Webseite auflisten).
            </Typography>

            {
                (editedConfig[SystemConfigKeys.provider.listingPage.disableListingPageLink] ?? config[SystemConfigKeys.provider.listingPage.disableListingPageLink]) != 'true' &&
                <Box>
                    <TextFieldComponent
                        label="Link zu externer Formular-Auflistung"
                        placeholder="https://bad-musterstadt.de/formulare"
                        hint="Der Link wird (soweit angegeben) anstelle des regulären Links mit dem Text „Weitere Formulare“ am Ende eines jeden Formulars angezeigt."
                        value={editedConfig[SystemConfigKeys.provider.listingPage.customListingPageLink] ?? config[SystemConfigKeys.provider.listingPage.customListingPageLink]}
                        pattern={{regex: '^(https?://)([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?$', message: 'Bitte geben Sie eine gültige URL ein (z.B. https://bad-musterstadt.de/formulare).'}}
                        onChange={(val) => {
                            setEditedConfig({
                                ...editedConfig,
                                [SystemConfigKeys.provider.listingPage.customListingPageLink]: val ?? '',
                            });
                        }}
                    />
                </Box>
            }

            <CheckboxFieldComponent
                label="Verlinkung von Formularen zur Formular-Index-Seite vollständig deaktivieren"
                value={(editedConfig[SystemConfigKeys.provider.listingPage.disableListingPageLink] ?? config[SystemConfigKeys.provider.listingPage.disableListingPageLink]) == 'true'}
                onChange={(checked) => {
                    setEditedConfig({
                        ...editedConfig,
                        [SystemConfigKeys.provider.listingPage.disableListingPageLink]: checked ? 'true' : 'false',
                    });
                }}
                hint="Bitte nehmen Sie zur Kenntnis, dass dies die Barrierefreiheit und Zugänglichkeit Ihrer Formulare beeinträchtigen kann."
            />

            <Box
                sx={{
                    mt: 4,
                }}
            >
                <Button
                    type="submit"
                    disabled={hasNotChanged}
                    color="primary"
                    variant="contained"
                    startIcon={<SaveOutlinedIcon
                        sx={{
                            marginTop: '-2px',
                        }}
                    />}
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
