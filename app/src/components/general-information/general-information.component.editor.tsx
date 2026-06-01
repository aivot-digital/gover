import React from 'react';
import {type IntroductionStepElement} from '../../models/elements/steps/introduction-step-element';
import {FormGroup, Grid, InputLabel, Typography} from '@mui/material';
import {CheckboxTree} from '../checkbox-tree/checkbox-tree';
import {StringListInput} from '../string-list-input/string-list-input';
import {type CheckboxTreeOption} from '../checkbox-tree/checkbox-tree-option';
import {type BaseEditorProps} from '../../editors/base-editor';
import {TextFieldComponent} from '../text-field/text-field-component';
import {RichTextInputComponent} from '../rich-text-input-component/rich-text-input-component';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from '../../utils/string-utils';
import {ElementEditorSectionHeader} from '../element-editor-section-header/element-editor-section-header';
import {AssetSelector} from '../../modules/assets/components/asset-selector';

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

function getLogoAssetSelectorValue(value: string | null | undefined): string | null {
    const trimmedValue = value?.trim();

    if (trimmedValue == null || trimmedValue.length === 0) {
        return null;
    }

    const publicAssetPathMatch = trimmedValue.match(/\/api\/public\/assets\/([^/?#]+)/);
    if (publicAssetPathMatch?.[1] != null) {
        return decodeURIComponent(publicAssetPathMatch[1]);
    }

    if (/^(https?:\/\/|data:|blob:|\/)/i.test(trimmedValue)) {
        return null;
    }

    return trimmedValue;
}

export function GeneralInformationComponentEditor(props: BaseEditorProps<IntroductionStepElement>) {
    const logoAssetSelectorValue = getLogoAssetSelectorValue(props.element.initiativeLogoLink);

    return (
        <>
            <Grid
                container
                columnSpacing={4}
                rowSpacing={2}
                sx={{mt: 2}}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <RichTextInputComponent
                        value={props.element.teaserText ?? ''}
                        label="Kurzbeschreibung"
                        hint="Schildern Sie kurz und präzise das Formular und dessen Zweck."
                        onChange={(val) => {
                            props.onPatch({
                                teaserText: val ?? undefined,
                            });
                        }}
                        disabled={!props.editable}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <Grid
                        container
                        rowSpacing={2}
                    >
                        <Grid size={12} sx={{pt: 2}}>
                            <AssetSelector
                                value={logoAssetSelectorValue}
                                label="Ergänzendes Logo zur Kurzbeschreibung (z.B. für Projekt, Programm o.Ä.)"
                                selectLabel="Ergänzendes Logo auswählen"
                                onChange={(val) => {
                                    props.onPatch({
                                        initiativeLogoLink: val ?? undefined,
                                    });
                                }}
                                hint="Optionales öffentliches Bild, das neben der Kurzbeschreibung angezeigt wird. Dieses Logo wird nur angezeigt, wenn Sie auch eine Kurzbeschreibung und einen Alternativtext angegeben haben."
                                disabled={!props.editable}
                                mimetype="image"
                                onlyPublic
                                placeholder="Kein Logo ausgewählt"
                            />

                            <TextFieldComponent
                                value={props.element.initiativeName ?? ''}
                                label="Alternativtext für das ergänzende Logo"
                                onChange={(val) => {
                                    props.onPatch({
                                        initiativeName: val,
                                    });
                                }}
                                hint="Der Alternativtext beschreibt den Bildinhalt für Nutzer mit Sehbehinderungen und sorgt so für Barrierefreiheit. Bitte kurz und aussagekräftig formulieren."
                                error={isStringNullOrEmpty(props.element.initiativeName) && isStringNotNullOrEmpty(props.element.initiativeLogoLink) ? 'Bitte geben Sie einen Alternativtext für das ausgewählte Logo an.' : undefined}
                                disabled={!props.editable}
                            />
                        </Grid>
                    </Grid>
                </Grid>
            </Grid>
            <ElementEditorSectionHeader
                title="Informationen für antragstellende Personen"
                variant={"h5"}
            />
            <Grid
                container
                columnSpacing={4}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 6
                    }}>
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

                    <RichTextInputComponent
                        value={props.element.expectedCosts ?? ''}
                        label="Gebühren des Antrages"
                        onChange={(val) => {
                            props.onPatch({
                                expectedCosts: val ?? undefined,
                            });
                        }}
                        disabled={!props.editable}
                    />
                </Grid>
            </Grid>
            <ElementEditorSectionHeader
                title="Fristen"
                variant="h5"
            >
                Geben Sie die für dieses Formular gültigen Fristen an (soweit vorhanden), welche ausfüllenden Personen im Formular angezeigt werden.
            </ElementEditorSectionHeader>
            <Grid
                container
                columnSpacing={4}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <TextFieldComponent
                        label="Fristen des Formulars"
                        multiline
                        value={props.element.expiring ?? ''}
                        onChange={(val) => {
                            props.onPatch({
                                expiring: val,
                            });
                        }}
                        disabled={!props.editable}
                    />
                </Grid>
            </Grid>
            <ElementEditorSectionHeader
                title="Dokumente des Antrages"
                variant={"h5"}
            />
            <StringListInput
                label="Relevante Dokumente"
                hint="Geben Sie hier Dokumente an, welche Antragsberechtigte vor Antragstellung lesen sollten."
                addLabel="Dokument hinzufügen"
                noItemsHint="Keine relevanten Dokumente angegeben"
                value={props.element.supportingDocuments ?? undefined}
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
                value={props.element.documentsToAttach ?? undefined}
                onChange={(supportingDocuments) => {
                    props.onPatch({
                        documentsToAttach: supportingDocuments,
                    });
                }}
                allowEmpty={true}
                disabled={!props.editable}
            />

            <ElementEditorSectionHeader
                title="Informationen zum Datenschutz"
                variant="h5"
            />
            <Grid
                container
                columnSpacing={4}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <TextFieldComponent
                        value={props.element.privacyText ?? ''}
                        label="Text für Datenschutz-Einwilligung in den Allgemeinen Informationen"
                        multiline
                        onChange={(val) => {
                            props.onPatch({
                                privacyText: val,
                            });
                        }}
                        disabled={!props.editable}
                    />

                    <Typography
                        variant={'caption'}
                        color={'text.secondary'}
                    >
                        Wenn Sie innerhalb der Informationen zum Datenschutz auf die Datenschutzerklärung verlinken möchten,
                        umschließen Sie den entsprechenden Text für den Link mit {'{privacy}'} und {'{/privacy}'}. Zum
                        Beispiel wie im Standard-Text: <i>Hier finden Sie die {'{privacy}Hinweise zum Datenschutz{/privacy}'}.</i>
                    </Typography>
                </Grid>
            </Grid>
        </>
    );
}
