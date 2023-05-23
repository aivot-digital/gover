import React, {useEffect, useState} from 'react';
import {FormControl, InputLabel, MenuItem, Select, TextField, Typography} from '@mui/material';
import {BaseEditorProps} from "../../editors/base-editor";
import {RootElement} from "../../models/elements/root-element";
import {DepartmentsService} from "../../services/departments.service";
import {Department} from "../../models/entities/department";
import {Themes} from "../../theming/themes";

export function RootComponentEditor(props: BaseEditorProps<RootElement>) {
    const [departments, setDepartments] = useState<Department[]>([]);

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
                Theme-Einstellung
            </Typography>

            <FormControl
                fullWidth
                margin="normal"
            >
                <InputLabel>Theme (Visuelles Erscheinungsbild)</InputLabel>
                <Select
                    value={props.element.theme ?? ''}
                    label="Theme (Visuelles Erscheinungsbild)"
                    onChange={event => props.onPatch({
                        theme: event.target.value,
                    })}
                >
                    {
                        Themes.map(label => (
                            <MenuItem
                                key={label}
                                value={label}
                            >
                                {label /* TODO: Localize theme names */}
                            </MenuItem>
                        ))
                    }
                </Select>
            </FormControl>

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
