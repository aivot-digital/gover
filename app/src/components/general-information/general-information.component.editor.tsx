import React, {useEffect, useState} from 'react';
import {BaseEditorProps} from '../_lib/base-editor-props';
import {IntroductionStepElement} from '../../models/elements/step-elements/introduction-step-element';
import {FormControl, InputLabel, MenuItem, Select, TextField, Typography} from '@mui/material';
import {Department} from "../../models/department";
import {DepartmentsService} from "../../services/departments.service";

export function GeneralInformationComponentEditor(props: BaseEditorProps<IntroductionStepElement>) {
    const [vendors, setVendors] = useState<Department[]>([]);

    useEffect(() => {
        DepartmentsService.list()
            .then(data => {
                setVendors(data._embedded.departments);
            });
    }, []);

    return (
        <>
            <FormControl
                fullWidth
                margin="normal"
            >
                <InputLabel>Zuständige Stelle</InputLabel>
                <Select
                    value={props.component.responsibleDepartment ?? ''}
                    label="Zuständige Stelle"
                    onChange={event => props.onPatch({
                        responsibleDepartment: event.target.value as number,
                    })}
                >
                    <MenuItem
                        value={''}
                    >
                        <i>Keine Auswahl</i>
                    </MenuItem>
                    {
                        vendors.map((vendor) => (
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
                <InputLabel>Bewirtschaftende Stelle</InputLabel>
                <Select
                    value={props.component.managingDepartment ?? ''}
                    label="Bewirtschaftende Stelle"
                    onChange={event => props.onPatch({
                        managingDepartment: event.target.value as number,
                    })}
                >
                    <MenuItem
                        value={''}
                    >
                        <i>Keine Auswahl</i>
                    </MenuItem>
                    {
                        vendors.map((vendor) => (
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

            <Typography variant="h6" sx={{mt: 4}}>
                Zugehörige Initiative
            </Typography>

            <TextField
                value={props.component.initiativeName ?? ''}
                label="Initiative"
                margin="normal"
                onChange={event => props.onPatch({
                    initiativeName: event.target.value,
                })}
            />

            <TextField
                value={props.component.initiativeLogoLink ?? ''}
                label="Logo der Initiative"
                margin="normal"
                onChange={event => props.onPatch({
                    initiativeLogoLink: event.target.value,
                })}
                helperText={'Link zu einer Grafik-Datei mit transparentem oder weißem Hintergrund.'}
            />

            <TextField
                label="URL zur Webseite der Initiative"
                margin="normal"
                helperText={'Das dargestellte Logo der Initiative verlinkt auf diese Webseite.'}
                value={props.component.initiativeLink ?? ''}
                onChange={event => props.onPatch({
                    initiativeLink: event.target.value,
                })}
            />

            <Typography variant="h6" sx={{mt: 4}}>
                Informationen für Antragstellende
            </Typography>

            <TextField
                value={props.component.teaserText ?? ''}
                label="Kurzbeschreibung"
                margin="normal"
                helperText={'Schildern Sie kurz und präzise den Antrag und dessen Zweck.'}
                multiline
                rows={4}
                onChange={event => props.onPatch({
                    teaserText: event.target.value,
                })}
            />

            <FormControl
                fullWidth
                margin="normal"
            >
                <InputLabel>Antragsberechtigte</InputLabel>
                <Select
                    value={(props.component.eligiblePersons ?? []).join('')}
                    label="Antragsberechtigte"
                    onChange={event => props.onPatch({
                        eligiblePersons: [event.target.value],
                    })}
                >
                    {
                        [["Natürliche Personen", "Natürliche Personen"], ["Juristische Personen", "Juristische Personen"]].map(([value, label]) => (
                            <MenuItem
                                key={label}
                                value={value}
                            >
                                {label}
                            </MenuItem>
                        ))
                    }
                </Select>
            </FormControl>

            <TextField
                value={(props.component.supportingDocuments ?? []).join('\n')}
                label="Relevante Dokumente"
                margin="normal"
                multiline
                rows={4}
                onChange={event => props.onPatch({
                    supportingDocuments: event.target.value.split('\n'),
                })}
                helperText="Dokumente, welche Antragsberechtigte vor Antragstellung lesen sollten. Bitte geben Sie pro Zeile ein Dokument an."
            />

            <TextField
                value={(props.component.documentsToAttach ?? []).join('\n')}
                label="Einzureichende Dokumente"
                margin="normal"
                multiline
                rows={4}
                onChange={event => props.onPatch({
                    documentsToAttach: event.target.value.split('\n'),
                })}
                helperText="Dokumente, welche Antragsberechtigte einzureichen haben. Bitte geben Sie pro Zeile ein Dokument an."
            />

            <TextField
                value={props.component.expectedCosts ?? ''}
                label="Gebühren des Antrages"
                margin="normal"
                onChange={event => props.onPatch({
                    expectedCosts: event.target.value,
                })}
            />
        </>
    );
}
