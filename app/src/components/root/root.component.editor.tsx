import React, {useEffect, useState} from 'react';
import {IconButton, Paper, Tooltip, Typography, useTheme} from '@mui/material';
import {type BaseEditorProps} from '../../editors/base-editor';
import {type RootElement} from '../../models/elements/root-element';
import {SelectFieldComponent} from '../select-field/select-field-component';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../slices/snackbar-slice';
import {TextFieldComponent} from '../text-field/text-field-component';
import {type SelectFieldComponentOption} from '../select-field/select-field-component-option';
import {Form as Application} from '../../models/entities/form';
import ContentPasteOutlinedIcon from '@mui/icons-material/ContentPasteOutlined';
import {useThemesApi} from '../../hooks/use-themes-api';
import {useApi} from '../../hooks/use-api';
import {useDepartmentsApi} from '../../hooks/use-departments-api';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectBooleanSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {useAssetsApi} from '../../hooks/use-assets-api';

export function RootComponentEditor(props: BaseEditorProps<RootElement, Application>): JSX.Element {
    const dispatch = useAppDispatch();
    const theme = useTheme();
    const api = useApi();

    const [departments, setDepartments] = useState<SelectFieldComponentOption[]>([]);
    const [themes, setThemes] = useState<SelectFieldComponentOption[]>([]);
    const [templateOptions, setTemplateOptions] = useState<SelectFieldComponentOption[]>([]);
    const experimentalFeaturesPdfTemplates = useAppSelector(selectBooleanSystemConfigValue(SystemConfigKeys.experimentalFeatures.pdfTemplates));

    useEffect(() => {
        useDepartmentsApi(api)
            .list()
            .then((deps) => deps.map((department) => ({
                value: department.id.toString(),
                label: department.name,
            })))
            .then(setDepartments)
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Laden der Fachbereiche!'));
            });

        useThemesApi(api)
            .listThemes()
            .then((themes) => themes.map((theme) => ({
                value: theme.id.toString(),
                label: theme.name,
            })))
            .then(setThemes)
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Laden der Farbschemata!'));
            });

        useAssetsApi(api)
            .list('text/html')
            .then((assets) => assets.map((asset) => ({
                value: asset.key,
                label: asset.filename,
            })))
            .then(setTemplateOptions)
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Laden der PDF-Vorlagen!'));
            });
    }, []);

    const link = `${window.location.protocol}//${window.location.host}/${props.entity?.slug ?? ''}/${props.entity?.version ?? ''}`;

    return (
        <>
            <Typography
                variant="h6"
                sx={{
                    mt: 4,
                }}
            >
                Link des Formulars
            </Typography>

            <Paper
                sx={{
                    mt: 1,
                    p: 2,
                    backgroundColor: theme.palette.grey['50'],
                    display: 'flex',
                    alignItems: 'center',
                }}
            >
                <Typography>
                    <a
                        href={link}
                        target="_blank"
                        rel="noreferrer"
                    >{link}</a>
                </Typography>

                <Tooltip title="In die Zwischenablage kopieren">
                    <IconButton
                        sx={{
                            ml: 'auto',
                        }}
                        size="small"
                        onClick={() => {
                            navigator
                                .clipboard
                                .writeText(link)
                                .then(() => {
                                    dispatch(showSuccessSnackbar('Link in Zwischenablage kopiert!'));
                                })
                                .catch((err) => {
                                    console.error(err);
                                    dispatch(showErrorSnackbar('Fehler beim Kopieren des Links!'));
                                });
                        }}
                    >
                        <ContentPasteOutlinedIcon />
                    </IconButton>
                </Tooltip>
            </Paper>

            <Typography
                variant="h6"
                sx={{
                    mt: 4,
                }}
            >
                Zuständige Fachbereiche
            </Typography>

            <SelectFieldComponent
                label="Entwickelnder Fachbereich"
                value={props.entity?.developingDepartmentId?.toString() ?? undefined}
                onChange={(val) => {
                    props.onPatchEntity({
                        developingDepartmentId: val != null ? parseInt(val) : undefined,
                    });
                }}
                options={departments}
                required
                disabled={!props.editable}
            />

            <SelectFieldComponent
                label="Zuständiger Fachbereich"
                value={props.entity?.responsibleDepartmentId?.toString() ?? undefined}
                onChange={(val) => {
                    props.onPatchEntity({
                        responsibleDepartmentId: val != null ? parseInt(val) : undefined,
                    });
                }}
                options={departments}
                disabled={!props.editable}
            />

            <SelectFieldComponent
                label="Bewirtschaftender Fachbereich"
                value={props.entity?.managingDepartmentId?.toString() ?? undefined}
                onChange={(val) => {
                    props.onPatchEntity({
                        managingDepartmentId: val != null ? parseInt(val) : undefined,
                    });
                }}
                options={departments}
                disabled={!props.editable}
            />

            <Typography
                variant="h6"
                sx={{
                    mt: 4,
                }}
            >
                Farbschemata-Einstellung
            </Typography>

            <SelectFieldComponent
                label="Farbschema (Visuelles Erscheinungsbild)"
                value={props.entity?.themeId?.toString() ?? undefined}
                onChange={(val) => {
                    props.onPatchEntity({
                        themeId: val != null ? parseInt(val) : undefined,
                    });
                }}
                options={themes}
                disabled={!props.editable}
            />

            <Typography
                variant="h6"
                sx={{
                    mt: 4,
                }}
            >
                Über dieses Formular
            </Typography>

            <TextFieldComponent
                value={props.element.headline}
                label="Überschrift des Formulars"
                multiline
                hint="Beschränkt auf zwei Zeilen, der Name des Programms sollte sich in Zeile 2 wiederfinden."
                onChange={(val) => {
                    props.onPatch({
                        headline: val,
                    });
                }}
                onBlur={(val) => {
                    props.onPatch({
                        headline: val != null ? val.split('\n').map((l) => l.trim()).slice(0, 2).join('\n') : undefined,
                    });
                }}
                maxCharacters={120}
                disabled={!props.editable}
            />

            <TextFieldComponent
                value={props.element.tabTitle}
                label="Titel des Browser-Tabs"
                onChange={(val) => {
                    props.onPatch({
                        tabTitle: val,
                    });
                }}
                maxCharacters={60}
                disabled={!props.editable}
            />

            <Typography
                variant="h6"
                sx={{
                    mt: 4,
                }}
            >
                Fristen
            </Typography>

            <TextFieldComponent
                label="Antragsfristen"
                multiline
                value={props.element.expiring}
                onChange={(val) => {
                    props.onPatch({
                        expiring: val,
                    });
                }}
                disabled={!props.editable}
            />

            <Typography
                variant="h6"
                sx={{
                    mt: 4,
                }}
            >
                Kontakte
            </Typography>

            <SelectFieldComponent
                label="Fachlicher Support"
                value={props.entity?.legalSupportDepartmentId?.toString() ?? undefined}
                onChange={(val) => {
                    props.onPatchEntity({
                        legalSupportDepartmentId: val != null ? parseInt(val) : undefined,
                    });
                }}
                options={departments}
                disabled={!props.editable}
            />

            <SelectFieldComponent
                label="Technischer Support"
                value={props.entity?.technicalSupportDepartmentId?.toString() ?? undefined}
                onChange={(val) => {
                    props.onPatchEntity({
                        technicalSupportDepartmentId: val != null ? parseInt(val) : undefined,
                    });
                }}
                options={departments}
                disabled={!props.editable}
            />

            {
                experimentalFeaturesPdfTemplates &&
                <>
                    <Typography
                        variant="h6"
                        sx={{
                            mt: 4,
                        }}
                    >
                        PDF-Vorlage
                    </Typography>

                    <SelectFieldComponent
                        label="PDF-Vorlage"
                        value={props.entity?.pdfBodyTemplateKey ?? undefined}
                        onChange={(val) => {
                            props.onPatchEntity({
                                pdfBodyTemplateKey: val,
                            });
                        }}
                        options={templateOptions}
                        disabled={!props.editable}
                    />
                </>
            }
        </>
    );
}
