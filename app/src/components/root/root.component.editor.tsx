import React, { useEffect, useState } from 'react';
import { IconButton, Paper, Tooltip, Typography, useTheme } from '@mui/material';
import { type BaseEditorProps } from '../../editors/base-editor';
import { type RootElement } from '../../models/elements/root-element';
import { DepartmentsService } from '../../services/departments-service';
import { SelectFieldComponent } from '../select-field/select-field-component';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faClipboard } from '@fortawesome/pro-light-svg-icons';
import { useAppDispatch } from '../../hooks/use-app-dispatch';
import { showErrorSnackbar, showSuccessSnackbar } from '../../slices/snackbar-slice';
import { TextFieldComponent } from '../text-field/text-field-component';
import { type SelectFieldComponentOption } from '../select-field/select-field-component-option';
import { ThemesService } from '../../services/themes-service';

export function RootComponentEditor(props: BaseEditorProps<RootElement>): JSX.Element {
    const dispatch = useAppDispatch();
    const theme = useTheme();

    const [departments, setDepartments] = useState<SelectFieldComponentOption[]>([]);
    const [themes, setThemes] = useState<SelectFieldComponentOption[]>([]);

    useEffect(() => {
        DepartmentsService
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

        ThemesService
            .list()
            .then((themes) => themes.map((theme) => ({
                value: theme.id.toString(),
                label: theme.name,
            })))
            .then(setThemes)
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Laden der Farbschemata!'));
            });
    }, []);

    const link = `${ window.location.protocol }//${ window.location.host }/#/${ props.application?.slug ?? '' }/${ props.application?.version ?? '' }`;

    return (
        <>
            <Typography
                variant="h6"
                sx={ {
                    mt: 4,
                } }
            >
                Link des Formulars
            </Typography>

            <Paper
                sx={ {
                    mt: 1,
                    p: 2,
                    backgroundColor: theme.palette.grey['50'],
                    display: 'flex',
                    alignItems: 'center',
                } }
            >
                <Typography>
                    <a
                        href={ link }
                        target="_blank"
                        rel="noreferrer"
                    >{ link }</a>
                </Typography>

                <Tooltip title="In die Zwischenablage kopieren">
                    <IconButton
                        sx={ {
                            ml: 'auto',
                        } }
                        size="small"
                        onClick={ () => {
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
                        } }
                    >
                        <FontAwesomeIcon icon={ faClipboard }/>
                    </IconButton>
                </Tooltip>
            </Paper>

            <Typography
                variant="h6"
                sx={ {
                    mt: 4,
                } }
            >
                Zuständige Fachbereiche
            </Typography>

            <SelectFieldComponent
                label="Entwickelnder Fachbereich"
                value={ props.application?.developingDepartment?.toString() ?? undefined }
                onChange={ (val) => {
                    props.onPatchApplication({
                        developingDepartment: val != null ? parseInt(val) : undefined,
                    });
                } }
                options={ departments }
                required
                disabled={ !props.editable }
            />

            <SelectFieldComponent
                label="Zuständiger Fachbereich"
                value={ props.application?.responsibleDepartment?.toString() ?? undefined }
                onChange={ (val) => {
                    props.onPatchApplication({
                        responsibleDepartment: val != null ? parseInt(val) : undefined,
                    });
                } }
                options={ departments }
                disabled={ !props.editable }
            />

            <SelectFieldComponent
                label="Bewirtschaftender Fachbereich"
                value={ props.application?.managingDepartment?.toString() ?? undefined }
                onChange={ (val) => {
                    props.onPatchApplication({
                        managingDepartment: val != null ? parseInt(val) : undefined,
                    });
                } }
                options={ departments }
                disabled={ !props.editable }
            />

            <Typography
                variant="h6"
                sx={ {
                    mt: 4,
                } }
            >
                Farbschemata-Einstellung
            </Typography>

            <SelectFieldComponent
                label="Farbschema (Visuelles Erscheinungsbild)"
                value={ props.application?.theme?.toString() ?? undefined }
                onChange={ (val) => {
                    props.onPatchApplication({
                        theme: val != null ? parseInt(val) : undefined,
                    });
                } }
                options={ themes }
                disabled={ !props.editable }
            />

            <Typography
                variant="h6"
                sx={ {
                    mt: 4,
                } }
            >
                Über dieses Formular
            </Typography>

            <TextFieldComponent
                value={ props.element.headline }
                label="Überschrift des Formulars"
                multiline
                hint="Beschränkt auf zwei Zeilen, der Name des Programms sollte sich in Zeile 2 wiederfinden."
                onChange={ (val) => {
                    props.onPatch({
                        headline: val,
                    });
                } }
                onBlur={ (val) => {
                    props.onPatch({
                        headline: val != null ? val.split('\n').map((l) => l.trim()).slice(0, 2).join('\n') : undefined,
                    });
                } }
                maxCharacters={ 120 }
                disabled={ !props.editable }
            />

            <TextFieldComponent
                value={ props.element.tabTitle }
                label="Titel des Browser-Tabs"
                onChange={ (val) => {
                    props.onPatch({
                        tabTitle: val,
                    });
                } }
                maxCharacters={ 60 }
                disabled={ !props.editable }
            />

            <Typography
                variant="h6"
                sx={ {
                    mt: 4,
                } }
            >
                Fristen
            </Typography>

            <TextFieldComponent
                label="Antragsfristen"
                multiline
                value={ props.element.expiring }
                onChange={ (val) => {
                    props.onPatch({
                        expiring: val,
                    });
                } }
                disabled={ !props.editable }
            />

            <Typography
                variant="h6"
                sx={ {
                    mt: 4,
                } }
            >
                Mindest-Vertrauensniveau
            </Typography>

            <SelectFieldComponent
                label="Mindest-Vertrauensniveau"
                value={ props.element.accessLevel }
                onChange={ (val) => {
                    props.onPatch({
                        accessLevel: val,
                    });
                } }
                options={ ['Niedrig', 'Mittel', 'Hoch'].map((key) => ({
                    label: key,
                    value: key,
                })) }
                disabled={ !props.editable }
            />

            <Typography
                variant="h6"
                sx={ {
                    mt: 4,
                } }
            >
                Kontakte
            </Typography>

            <SelectFieldComponent
                label="Fachlicher Support"
                value={ props.application?.legalSupportDepartment?.toString() ?? undefined }
                onChange={ (val) => {
                    props.onPatchApplication({
                        legalSupportDepartment: val != null ? parseInt(val) : undefined,
                    });
                } }
                options={ departments }
                disabled={ !props.editable }
            />

            <SelectFieldComponent
                label="Technischer Support"
                value={ props.application?.technicalSupportDepartment?.toString() ?? undefined }
                onChange={ (val) => {
                    props.onPatchApplication({
                        technicalSupportDepartment: val != null ? parseInt(val) : undefined,
                    });
                } }
                options={ departments }
                disabled={ !props.editable }
            />
        </>
    );
}
