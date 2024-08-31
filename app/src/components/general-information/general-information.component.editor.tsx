import React from 'react';
import {type IntroductionStepElement} from '../../models/elements/steps/introduction-step-element';
import {FormGroup, InputLabel, Typography} from '@mui/material';
import {CheckboxTree} from '../checkbox-tree/checkbox-tree';
import {StringListInput} from '../string-list-input/string-list-input';
import {type CheckboxTreeOption} from '../checkbox-tree/checkbox-tree-option';
import {type BaseEditorProps} from '../../editors/base-editor';
import {TextFieldComponent} from '../text-field/text-field-component';
import {RichTextEditorComponentView} from '../richt-text-editor/rich-text-editor.component.view';
import {ElementTreeEntity} from '../element-tree/element-tree-entity';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from "../../utils/string-utils";

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

export function GeneralInformationComponentEditor(props: BaseEditorProps<IntroductionStepElement, ElementTreeEntity>): JSX.Element {
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
                label="Zusätzliches Logo (z.B. für Projekt, Programm o.Ä.)"
                onChange={(val) => {
                    props.onPatch({
                        initiativeLogoLink: val,
                    });
                }}
                hint="Link (URL) zu einer Grafik-Datei mit transparentem oder weißem Hintergrund."
                disabled={!props.editable}
            />

            <TextFieldComponent
                value={props.element.initiativeName ?? ''}
                label="Alternativtext für das zusätzliche Logo"
                onChange={(val) => {
                    props.onPatch({
                        initiativeName: val,
                    });
                }}
                hint="Der Alternativtext (Alt-Text) sorgt für Barrierefreiheit, indem er den Inhalt des Bildes für Nutzer mit Sehbehinderungen beschreibt. Geben Sie eine kurze, prägnante Beschreibung des Bildinhalts ein, die alle wichtigen Informationen vermittelt."
                error={isStringNullOrEmpty(props.element.initiativeName) && isStringNotNullOrEmpty(props.element.initiativeLogoLink) ? 'Im Sinne der Barrierefreiheit sollten Sie immer einen Alternativtext für das Bild angeben.' : undefined}
                disabled={!props.editable}
            />

            <Typography
                variant="h6"
                sx={{
                    mt: 4,
                    mb: 2,
                }}
            >
                Informationen für antragstellende Personen
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
                    mb: 2,
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

            <RichTextEditorComponentView
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
