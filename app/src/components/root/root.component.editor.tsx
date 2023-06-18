import React, {useEffect, useState} from 'react';
import {
    FormControl,
    IconButton,
    InputLabel,
    MenuItem,
    Paper,
    Select,
    TextField,
    Tooltip,
    Typography,
    useTheme
} from '@mui/material';
import {BaseEditorProps} from "../../editors/base-editor";
import {RootElement} from "../../models/elements/root-element";
import {DepartmentsService} from "../../services/departments.service";
import {Department} from "../../models/entities/department";
import {Themes} from "../../theming/themes";
import {SelectFieldComponent} from "../select-field/select-field-component";
import {useAppSelector} from "../../hooks/use-app-selector";
import {selectLoadedApplication} from "../../slices/app-slice";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {faClipboard} from "@fortawesome/pro-light-svg-icons";
import {useAppDispatch} from "../../hooks/use-app-dispatch";
import {showSuccessSnackbar} from "../../slices/snackbar-slice";
import {TextFieldComponent} from "../text-field/text-field-component";

export function RootComponentEditor(props: BaseEditorProps<RootElement>) {
    const dispatch = useAppDispatch();
    const theme = useTheme();
    const app = useAppSelector(selectLoadedApplication);

    const [departments, setDepartments] = useState<Department[]>([]);

    const link = `${window.location.protocol}//${window.location.host}/#/${app?.slug}/${app?.version}`;

    useEffect(() => {
        DepartmentsService.list()
            .then(data => {
                setDepartments(data._embedded.departments);
            });
    }, []);

    return (
        <>
            <Typography
                variant="h6"
                sx={{mt: 4}}
            >
                Link des Antrags
            </Typography>

            <Paper
                sx={{
                    mt: 1,
                    p: 2,
                    backgroundColor: theme.palette.grey["50"],
                    display: 'flex',
                    alignItems: 'center'
                }}
            >
                <Typography>
                    <a
                        href={link}
                        target="_blank"
                    >{link}</a>
                </Typography>

                <Tooltip title="In die Zwischenablage kopieren">
                    <IconButton
                        sx={{ml: 'auto'}}
                        size="small"
                        onClick={() => {
                            navigator.clipboard.writeText(link);
                            dispatch(showSuccessSnackbar('Link in Zwischenablage kopiert!'));
                        }}
                    >
                        <FontAwesomeIcon icon={faClipboard}/>
                    </IconButton>
                </Tooltip>
            </Paper>

            <Typography
                variant="h6"
                sx={{mt: 4}}
            >
                Theme-Einstellung
            </Typography>

            <SelectFieldComponent
                label="Theme (Visuelles Erscheinungsbild)"
                value={props.element.theme}
                onChange={val => props.onPatch({
                    theme: val,
                })}
                options={Themes.map(label => ({
                    label: label,
                    value: label,
                }))}
            />

            <Typography
                variant="h6"
                sx={{mt: 4}}
            >
                Über diesen Antrag
            </Typography>

            <TextFieldComponent
                value={props.element.title}
                label="Titel des Antrages"
                onChange={val => props.onPatch({
                    title: val,
                })}
                hint="Vergeben Sie einen Titel für den Antrag um ihn besser identifizieren zu können. Diesen Titel können nur Sie und ihre Kolleg:innen einsehen."
                maxCharacters={60}
            />

            <TextFieldComponent
                value={props.element.headline}
                label="Überschrift des Antrages"
                multiline
                hint="Beschränkt auf zwei Zeilen, der Name des Programms sollte sich in Zeile 2 wiederfinden."
                onChange={val => props.onPatch({
                    headline: val,
                })}
                onBlur={val => props.onPatch({
                    headline: val != null ? val.split('\n').map(l => l.trim()).slice(0, 2).join('\n') : undefined,
                })}
                maxCharacters={120}
            />

            <TextFieldComponent
                value={props.element.tabTitle}
                label="Titel des Browser-Tabs"
                onChange={val => props.onPatch({
                    tabTitle: val,
                })}
                maxCharacters={60}
            />

            <Typography
                variant="h6"
                sx={{mt: 4}}
            >
                Fristen
            </Typography>

            <TextFieldComponent
                label="Antragsfristen"
                multiline
                value={props.element.expiring}
                onChange={val => props.onPatch({
                    expiring: val,
                })}
            />

            <Typography
                variant="h6"
                sx={{mt: 4}}
            >
                Mindest-Vertrauensniveau
            </Typography>

            <SelectFieldComponent
                label="Mindest-Vertrauensniveau"
                value={props.element.accessLevel}
                onChange={val => props.onPatch({
                    accessLevel: val,
                })}
                options={['Niedrig', 'Mittel', 'Hoch'].map(key => ({
                    label: key,
                    value: key,
                }))}
            />

            <Typography
                variant="h6"
                sx={{mt: 4}}
            >
                Kontakte
            </Typography>

            <SelectFieldComponent
                label="Fachlicher Support"
                value={props.element.legalSupport != null ? props.element.legalSupport.toString() : undefined}
                onChange={val => props.onPatch({
                    legalSupport: val != null ? parseInt(val) : undefined,
                })}
                options={departments.map((vendor) => ({
                    value: vendor.id.toString(),
                    label: vendor.name,
                }))}
            />

            <SelectFieldComponent
                label="Technischer Support"
                value={props.element.technicalSupport != null ? props.element.technicalSupport.toString() : undefined}
                onChange={val => props.onPatch({
                    technicalSupport: val != null ? parseInt(val) : undefined,
                })}
                options={departments.map((vendor) => ({
                    value: vendor.id.toString(),
                    label: vendor.name,
                }))}
            />
        </>
    );
}
