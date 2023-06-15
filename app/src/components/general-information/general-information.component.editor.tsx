import React, {useEffect, useState} from 'react';
import {IntroductionStepElement} from '../../models/elements/steps/introduction-step-element';
import {FormControl, FormGroup, InputLabel, MenuItem, Select, TextField, Typography} from '@mui/material';
import {Department} from "../../models/entities/department";
import {DepartmentsService} from "../../services/departments.service";
import {isStringNullOrEmpty} from "../../utils/string-utils";
import {CheckboxTree} from "../checkbox-tree/checkbox-tree";
import {StringListInput} from "../string-list-input/string-list-input";
import {CheckboxTreeOption} from "../checkbox-tree/checkbox-tree-option";
import {BaseEditorProps} from "../../editors/base-editor";

const eligibleEntities: CheckboxTreeOption[] = [
    {
        label: 'Rechtspersonen',
        children: [
            'Natürliche Personen',
            {
                label: 'Juristische Personen des öffentlichen Rechts',
                children: [
                    {
                        label: 'Körperschaften',
                        children: [
                            'Gebietskörperschaften',
                            'Verbandskörperschaften',
                            'Personal- und Realkörperschaften',
                        ],
                    },
                    'Anstalten des öffentlichen Rechts',
                    'Öffentlich-rechtliche Stiftungen',
                ],
            },
            {
                label: 'Juristische Personen des privaten Rechts',
                children: [
                    'Vereine (e.V., a.V.)',
                    'Aktiengesellschaften (AG)',
                    'Kommanditgesellschaften auf Aktien (KGaA)',
                    'Gesellschaften mit beschränkter Haftung (GmbH, UG)',
                    'Eingetragene Genossenschaften (eG)',
                    'Europäische Gesellschaften (SE)',
                ],
            },
        ],
    },
    {
        label: 'Personengesellschaften',
        children: [
            'Offene Handelsgesellschaften (OHG)',
            'Kommanditgesellschaften (KG)',
            'Gesellschaft bürgerlichen Rechts (GbR)',
            'Partnerschaftsgesellschaften',
            'Partenreedereien',
            'Stille Gesellschaften',
        ],
    },
    {
        label: 'Gesamthandsgemeinschaften',
        children: [
            'Gütergemeinschaften',
            'Erbengemeinschaften',
            'Wohnungseigentümergemeinschaften',
        ],
    },
];

function orderEligiblePersons(value: string[]): string[] {
    const flattenTreeOptions = (options: CheckboxTreeOption[]): string[] => {
        const flattened: string[] = [];
        for (const option of options) {
            if (typeof option === 'string') {
                flattened.push(option);
            } else {
                flattened.push(option.label);
                flattened.push(...flattenTreeOptions(option.children));
            }
        }
        return flattened;
    }

    return flattenTreeOptions(eligibleEntities)
        .filter(opt => value.includes(opt));
}

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
                    value={props.element.responsibleDepartment ?? ''}
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
                    value={props.element.managingDepartment ?? ''}
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

            <Typography
                variant="h6"
                sx={{mt: 4}}
            >
                Zugehörige Initiative
            </Typography>

            <TextField
                value={props.element.initiativeName ?? ''}
                label="Initiative"
                margin="normal"
                onChange={event => props.onPatch({
                    initiativeName: event.target.value,
                })}
            />

            <TextField
                value={props.element.initiativeLogoLink ?? ''}
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
                value={props.element.initiativeLink ?? ''}
                onChange={event => props.onPatch({
                    initiativeLink: event.target.value,
                })}
            />

            <Typography
                variant="h6"
                sx={{mt: 4}}
            >
                Informationen für Antragstellende
            </Typography>

            <TextField
                value={props.element.teaserText ?? ''}
                label="Kurzbeschreibung"
                margin="normal"
                helperText="Schildern Sie kurz und präzise den Antrag und dessen Zweck."
                multiline
                rows={4}
                onChange={event => props.onPatch({
                    teaserText: event.target.value,
                })}
            />

            <FormGroup sx={{mt: 2}}>
                <InputLabel sx={{mb: 1}}>Antragsberechtigte</InputLabel>
                <CheckboxTree
                    options={eligibleEntities}
                    value={props.element.eligiblePersons ?? []}
                    onChange={update => props.onPatch({
                        eligiblePersons: orderEligiblePersons(update),
                    })}
                />
            </FormGroup>

            <TextField
                value={props.element.expectedCosts ?? ''}
                label="Gebühren des Antrages"
                margin="normal"
                onChange={event => props.onPatch({
                    expectedCosts: event.target.value,
                })}
                onBlur={() => {
                    if (isStringNullOrEmpty(props.element.expectedCosts)) {
                        props.onPatch({
                            expectedCosts: undefined,
                        });
                    }
                }}
            />

            <Typography
                variant="h6"
                sx={{mt: 4}}
            >
                Dokumente des Antrags
            </Typography>

            <StringListInput
                label="Relevante Dokumente"
                hint="Geben Sie hier Dokumente an, welche Antragsberechtigte vor Antragstellung lesen sollten."
                addLabel="Dokument hinzufügen"
                noItemsHint="Keine relevanten Dokumente angegeben"
                value={props.element.supportingDocuments}
                onChange={supportingDocuments => props.onPatch({
                    supportingDocuments: supportingDocuments,
                })}
                allowEmpty={true}
            />

            <StringListInput
                label="Einzureichende Dokumente"
                hint="Geben Sie hier Dokumente an, welche Antragsberechtigte einzureichen haben."
                addLabel="Dokument hinzufügen"
                noItemsHint="Keine einzureichenden Dokumente angegeben"
                value={props.element.documentsToAttach}
                onChange={supportingDocuments => props.onPatch({
                    documentsToAttach: supportingDocuments,
                })}
                allowEmpty={true}
            />

        </>
    );
}
