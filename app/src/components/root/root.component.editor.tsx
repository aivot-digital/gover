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
                    <a href={link} target="_blank">{link}</a>
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

            <TextField
                value={props.element.title ?? ''}
                label="Titel des Antrages"
                margin="normal"
                onChange={event => props.onPatch({
                    title: event.target.value,
                })}
                helperText="Der Titel dient als interne Bezeichnung und für Antragstellende nicht sichtbar."
            />

            <TextField
                value={props.element.headline ?? ''}
                label="Überschrift des Antrages"
                margin="normal"
                rows={2}
                multiline
                helperText="Beschränkt auf zwei Zeilen, der Name des Programms sollte sich in Zeile 2 wiederfinden."
                onChange={event => props.onPatch({
                    headline: event.target.value.split('\n').slice(0, 2).join('\n'),
                })}
            />

            <TextField
                value={props.element.tabTitle ?? ''}
                label="Titel des Browser-Tabs"
                margin="normal"
                onChange={event => props.onPatch({
                    tabTitle: event.target.value,
                })}
            />

            <Typography
                variant="h6"
                sx={{mt: 4}}
            >
                Fristen
            </Typography>

            <TextField
                rows={2}
                label="Antragsfristen"
                margin="normal"
                multiline
                value={props.element.expiring ?? ''}
                onChange={event => props.onPatch({
                    expiring: event.target.value,
                })}
            />

            <Typography
                variant="h6"
                sx={{mt: 4}}
            >
                Mindest-Vertrauensniveau
            </Typography>

            <FormControl
                margin="normal"
            >
                <InputLabel>Mindest-Vertrauensniveau</InputLabel>
                <Select
                    label="Mindest-Vertrauensniveau"
                    value={props.element.accessLevel ?? ''}
                    onChange={event => props.onPatch({
                        accessLevel: event.target.value,
                    })}
                >
                    <MenuItem
                        key={'Niedrig'}
                        value={'Niedrig'}
                    >
                        Niedrig
                    </MenuItem>
                    <MenuItem
                        key={'Mittel'}
                        value={'Mittel'}
                    >
                        Mittel
                    </MenuItem>
                    <MenuItem
                        key={'Hoch'}
                        value={'Hoch'}
                    >
                        Hoch
                    </MenuItem>
                </Select>
            </FormControl>

            <Typography
                variant="h6"
                sx={{mt: 4}}
            >
                Kontakte
            </Typography>

            <FormControl
                fullWidth
                margin="normal"
            >
                <InputLabel>Fachlicher Support</InputLabel>
                <Select
                    label="Fachlicher Support"
                    value={props.element.legalSupport ?? ''}
                    onChange={event => props.onPatch({
                        legalSupport: event.target.value as number,
                    })}
                >
                    {
                        departments.map((vendor) => (
                            <MenuItem
                                key={vendor.id}
                                value={vendor.id}
                            >
                                {vendor.name}
                            </MenuItem>
                        ))
                    }
                </Select>
            </FormControl>

            <FormControl
                fullWidth
                margin="normal"
            >
                <InputLabel>Technischer Support</InputLabel>
                <Select
                    label="Technischer Support"
                    value={props.element.technicalSupport ?? ''}
                    onChange={event => props.onPatch({
                        technicalSupport: event.target.value as number,
                    })}
                >
                    {
                        departments.map((vendor) => (
                            <MenuItem
                                key={vendor.id}
                                value={vendor.id}
                            >
                                {vendor.name}
                            </MenuItem>
                        ))
                    }
                </Select>
            </FormControl>
        </>
    );
}
