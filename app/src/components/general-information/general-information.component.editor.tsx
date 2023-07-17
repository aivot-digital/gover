import React from 'react';
import {type IntroductionStepElement} from '../../models/elements/steps/introduction-step-element';
import {FormGroup, InputLabel, Typography} from '@mui/material';
import {CheckboxTree} from '../checkbox-tree/checkbox-tree';
import {StringListInput} from '../string-list-input/string-list-input';
import {type CheckboxTreeOption} from '../checkbox-tree/checkbox-tree-option';
import {type BaseEditorProps} from '../../editors/base-editor';
import {TextFieldComponent} from '../text-field/text-field-component';
import {RichTextEditorComponentView} from '../richt-text-editor/rich-text-editor.component.view';
import {Application} from '../../models/entities/application';
import {Preset} from '../../models/entities/preset';

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
    };

    return flattenTreeOptions(eligibleEntities)
        .filter((opt) => value.includes(opt));
}

export function GeneralInformationComponentEditor(props: BaseEditorProps<IntroductionStepElement, Application | Preset>): JSX.Element {
    return (
        <>
            <Typography
                variant="h6"
                sx={{
                    mt: 4,
                }}
            >
                Zusätzliche Informationen
            </Typography>

            <TextFieldComponent
                value={props.element.initiativeLogoLink ?? ''}
                label="Zusätzliches Logo"
                onChange={(val) => {
                    props.onPatch({
                        initiativeLogoLink: val,
                    });
                }}
                hint="Link zu einer Grafik-Datei mit transparentem oder weißem Hintergrund."
                disabled={!props.editable}
            />

            <TextFieldComponent
                value={props.element.initiativeName ?? ''}
                label="Beschreibungstext des Logos"
                onChange={(val) => {
                    props.onPatch({
                        initiativeName: val,
                    });
                }}
                hint="Beschrieben Sie kurz, was auf dem Logo zu sehen ist."
                disabled={!props.editable}
            />

            <Typography
                variant="h6"
                sx={{
                    mt: 4,
                }}
            >
                Informationen für Antragstellende
            </Typography>

            <RichTextEditorComponentView
                value={props.element.teaserText ?? ''}
                label="Kurzbeschreibung"
                hint="Schildern Sie kurz und präzise das Formular und dessen Zweck."
                onChange={(val) => {
                    props.onPatch({
                        teaserText: val,
                    });
                }}
                disabled={!props.editable}
            />

            <FormGroup
                sx={{
                    mt: 2,
                }}
            >
                <InputLabel
                    sx={{
                        mb: 1,
                    }}
                >Antragsberechtigte</InputLabel>
                <CheckboxTree
                    options={eligibleEntities}
                    value={props.element.eligiblePersons ?? []}
                    onChange={(update) => {
                        props.onPatch({
                            eligiblePersons: orderEligiblePersons(update),
                        });
                    }}
                    disabled={!props.editable}
                />
            </FormGroup>

            <TextFieldComponent
                value={props.element.expectedCosts ?? ''}
                label="Gebühren des Antrages"
                onChange={(val) => {
                    props.onPatch({
                        expectedCosts: val,
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
                Dokumente des Antrags
            </Typography>

            <StringListInput
                label="Relevante Dokumente"
                hint="Geben Sie hier Dokumente an, welche Antragsberechtigte vor Antragstellung lesen sollten."
                addLabel="Dokument hinzufügen"
                noItemsHint="Keine relevanten Dokumente angegeben"
                value={props.element.supportingDocuments}
                onChange={(supportingDocuments) => {
                    props.onPatch({
                        supportingDocuments,
                    });
                }}
                allowEmpty={true}
                disabled={!props.editable}
            />

            <StringListInput
                label="Einzureichende Dokumente"
                hint="Geben Sie hier Dokumente an, welche Antragsberechtigte einzureichen haben."
                addLabel="Dokument hinzufügen"
                noItemsHint="Keine einzureichenden Dokumente angegeben"
                value={props.element.documentsToAttach}
                onChange={(supportingDocuments) => {
                    props.onPatch({
                        documentsToAttach: supportingDocuments,
                    });
                }}
                allowEmpty={true}
                disabled={!props.editable}
            />

        </>
    );
}
