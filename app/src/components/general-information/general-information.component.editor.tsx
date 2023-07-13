import React from 'react';
import {IntroductionStepElement} from '../../models/elements/steps/introduction-step-element';
import {FormGroup, InputLabel, TextField, Typography} from '@mui/material';
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
    return (
        <>
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
                disabled={!props.editable}
            />

            <TextField
                value={props.element.initiativeLogoLink ?? ''}
                label="Logo der Initiative"
                margin="normal"
                onChange={event => props.onPatch({
                    initiativeLogoLink: event.target.value,
                })}
                helperText={'Link zu einer Grafik-Datei mit transparentem oder weißem Hintergrund.'}
                disabled={!props.editable}
            />

            <TextField
                label="URL zur Webseite der Initiative"
                margin="normal"
                helperText={'Das dargestellte Logo der Initiative verlinkt auf diese Webseite.'}
                value={props.element.initiativeLink ?? ''}
                onChange={event => props.onPatch({
                    initiativeLink: event.target.value,
                })}
                disabled={!props.editable}
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
                helperText="Schildern Sie kurz und präzise das Formular und dessen Zweck."
                multiline
                rows={4}
                onChange={event => props.onPatch({
                    teaserText: event.target.value,
                })}
                disabled={!props.editable}
            />

            <FormGroup sx={{mt: 2}}>
                <InputLabel sx={{mb: 1}}>Antragsberechtigte</InputLabel>
                <CheckboxTree
                    options={eligibleEntities}
                    value={props.element.eligiblePersons ?? []}
                    onChange={update => props.onPatch({
                        eligiblePersons: orderEligiblePersons(update),
                    })}
                    disabled={!props.editable}
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
                disabled={!props.editable}
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
                disabled={!props.editable}
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
                disabled={!props.editable}
            />

        </>
    );
}
