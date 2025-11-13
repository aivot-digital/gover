import {type AnyElement} from '../../models/elements/any-element';
import {type ElementEditorContentProps} from '../element-editor-content/element-editor-content-props';
import {Box, Grid} from '@mui/material';
import {TextFieldComponent} from '../text-field/text-field-component';
import {ElementType} from '../../data/element-type/element-type';
import {SelectFieldComponent} from '../select-field/select-field-component';
import {type AnyFormElement} from '../../models/elements/form/any-form-element';
import {isAnyInputElement} from '../../models/elements/form/input/any-input-element';
import {CheckboxFieldComponent} from '../checkbox-field/checkbox-field-component';
import {EditorDispatcher} from '../editor-dispatcher';
import React from 'react';
import {type ElementTreeEntity} from '../element-tree/element-tree-entity';
import {showErrorSnackbar, showSuccessSnackbar} from '../../slices/snackbar-slice';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import ContentPasteIcon from '@mui/icons-material/ContentPaste';
import {ElementEditorSectionHeader} from '../element-editor-section-header/element-editor-section-header';
import {getElementNameForType} from '../../data/element-type/element-names';
import {AlertComponent} from '../alert/alert-component';
import {editors as Editors} from '../../editors';

export function DefaultTab<T extends AnyElement, E extends ElementTreeEntity>(props: ElementEditorContentProps<T, E>) {
    const dispatch = useAppDispatch();

    const tabDescription = (() => {
        switch (props.element.type) {
            case ElementType.Root:
                return {
                    title: 'Eigenschaften des Formulars',
                    description: 'Hier konfigurieren Sie allgemeine Eigenschaften des gesamten Formulars, z. B. Titel oder technische Einstellungen.',
                    isElement: false,
                };
            case ElementType.IntroductionStep:
                return {
                    title: 'Eigenschaften für den Abschnitt „Allgemeine Informationen“',
                    description: 'Dieser Abschnitt dient der Einführung des Antragstellers in das Formular – z. B. mit einleitenden Informationen und einem Überblick über wichtige Eckdaten wie mögliche Gebühren.',
                    isElement: false,
                };
            case ElementType.SummaryStep:
                return {
                    title: 'Eigenschaften für den Abschnitt „Zusammenfassung“',
                    description: 'Hier wird dem Antragstellenden eine Übersicht der bisherigen Eingaben angezeigt.',
                    isElement: false,
                };
            case ElementType.SubmitStep:
                return {
                    title: 'Eigenschaften für den Abschnitt „Absenden des Antrages“',
                    description: 'In diesem Schritt wird der Antrag final geprüft und abgesendet. Darüber hinaus können hier weitere Informationen angegeben werden, z. B. zur Bearbeitung des Antrages.',
                    isElement: false,
                };
            case ElementType.Step:
                return {
                    title: 'Eigenschaften des Abschnitts',
                    description: 'Konfigurieren Sie diesen Abschnitt des Formulars, z. B. mit eigenen logischen Bedingungen.',
                    isElement: false,
                };
            default:
                return {
                    title: 'Eigenschaften des Elements',
                    description: 'Legen Sie fest, wie dieses Formularelement dargestellt wird, welche Inhalte erfasst werden können und wie sich das Element gegenüber Nutzer:innen verhält.',
                    isElement: true,
                };
        }
    })();

    return (
        <Box
            sx={{
                p: 4,
            }}
        >
            <ElementEditorSectionHeader
                title={tabDescription.title}
                titleSuffix={tabDescription.isElement ? '(' + getElementNameForType(props.element.type) + ')' : undefined}
                disableMarginTop
            >
                {tabDescription.description}
            </ElementEditorSectionHeader>
            {
                props.element.type === ElementType.SummaryStep &&
                <AlertComponent color={'info'}>
                    Dieser Abschnitt hat derzeit keine weiteren Konfigurationsmöglichkeiten.
                </AlertComponent>
            }
            {
                tabDescription.isElement &&
                <ElementEditorSectionHeader
                    title="Grundlegende Angaben"
                    variant={'h5'}
                />
            }
            <Grid
                container
                columnSpacing={4}
            >

                {
                    props.element.type !== ElementType.Root &&
                    props.element.type !== ElementType.IntroductionStep &&
                    props.element.type !== ElementType.SummaryStep &&
                    props.element.type !== ElementType.SubmitStep &&
                    <Grid
                        size={{
                            xs: 12,
                            lg: 6,
                        }}
                    >
                        <TextFieldComponent
                            label="Interner Name"
                            value={props.element.name}
                            onChange={(val) => {
                                // @ts-expect-error
                                props.onChange({
                                    name: val ?? '',
                                });
                            }}
                            hint="Vergeben Sie einen internen Namen zur besseren Identifikation. Nur für Sie und Ihr Team sichtbar."
                            maxCharacters={30}
                            disabled={!props.editable}
                        />
                    </Grid>
                }

                {
                    props.element.type !== ElementType.Root &&
                    props.element.type !== ElementType.IntroductionStep &&
                    props.element.type !== ElementType.Step &&
                    props.element.type !== ElementType.SummaryStep &&
                    props.element.type !== ElementType.SubmitStep &&
                    <Grid
                        size={{
                            xs: 12,
                            lg: 6,
                        }}
                    >
                        <SelectFieldComponent
                            label="Breite des Elements in der Darstellung"
                            value={(props.element as AnyFormElement)?.weight?.toString() ?? '12'}
                            onChange={(val) => {
                                props.onChange({
                                    // @ts-expect-error
                                    weight: val != null ? parseFloat(val) : 12,
                                });
                            }}
                            options={[
                                {
                                    label: '25%',
                                    value: '3',
                                },
                                {
                                    label: '33%',
                                    value: '4',
                                },
                                {
                                    label: '37,5%',
                                    value: '4.5',
                                },
                                {
                                    label: '50%',
                                    value: '6',
                                },
                                {
                                    label: '62,5%',
                                    value: '7.5',
                                },
                                {
                                    label: '66%',
                                    value: '8',
                                },
                                {
                                    label: '75%',
                                    value: '9',
                                },
                                {
                                    label: '100%',
                                    value: '12',
                                },
                            ]}
                            hint="Legen Sie die Breite des Elements für Tablets & Desktops fest. Auf Mobilgeräten wird stets die volle Breite verwendet."
                            disabled={!props.editable}
                        />
                    </Grid>
                }

                {
                    isAnyInputElement(props.element) &&
                    <>
                        <Grid
                            size={{
                                xs: 12,
                                lg: 6,
                            }}
                        >
                            <TextFieldComponent
                                value={props.element.label}
                                label="Titel"
                                onChange={(val) => {
                                    props.onChange({
                                        // @ts-expect-error
                                        label: val,
                                    });
                                }}
                                hint="Dieser Titel wird als Label für dieses Feld im Formular angezeigt und ist u. A. relevant für die Barrierefreiheit."
                                disabled={!props.editable}
                                softLimitCharacters={20}
                                softLimitCharactersWarning={'Halten Sie das Label so kurz wie möglich (empfohlen max. 20 Zeichen), da es sonst auf kleinen Bildschirmen abgeschnitten werden kann.'}
                            />
                        </Grid>
                        <Grid
                            size={{
                                xs: 12,
                                lg: 6,
                            }}
                        >
                            <TextFieldComponent
                                value={props.element.hint}
                                label="Hinweis"
                                onChange={(val) => {
                                    props.onChange({
                                        // @ts-expect-error
                                        hint: val,
                                    });
                                }}
                                hint="Geben Sie hier zusätzliche Hinweise zur Eingabe für Antragstellende an (optional, wird unter dem Eingabefeld angezeigt)."
                                disabled={!props.editable}
                            />
                        </Grid>
                    </>
                }
            </Grid>
            {
                tabDescription.isElement &&
                // elements without additional properties – should be replaced with a more generic check if element contains additional properties
                Editors[props.element.type] != null &&
                <ElementEditorSectionHeader
                    title="Elementspezifische Eigenschaften"
                    variant={'h5'}
                />
            }
            <EditorDispatcher
                props={props.element}
                onPatch={props.onChange}
                entity={props.entity}
                onPatchEntity={props.onChangeEntity}
                editable={props.editable}
                scope={props.scope}
            />
            {
                isAnyInputElement(props.element) &&
                <>
                    <ElementEditorSectionHeader
                        title="Eingabeoptionen"
                        variant={'h5'}
                        disableMarginBottom
                    />
                    <Grid
                        container
                        columnSpacing={4}
                    >
                        <Grid
                            size={{
                                xs: 12,
                                lg: 4,
                            }}
                        >
                            <CheckboxFieldComponent
                                label="Pflichtangabe"
                                value={props.element.required ?? undefined}
                                onChange={(checked) => {
                                    props.onChange({
                                        // @ts-ignore
                                        required: checked,
                                        disabled: false,
                                        technical: false,
                                    });
                                }}
                                hint="Pflichtangaben müssen von den antragstellenden Personen ausgefüllt werden."
                                disabled={!props.editable || Boolean(props.element.disabled) || Boolean(props.element.technical)}
                            />
                        </Grid>
                        <Grid
                            size={{
                                xs: 12,
                                lg: 4,
                            }}
                        >
                            {
                                props.scope !== 'data_modelling' &&
                                <CheckboxFieldComponent
                                    label="Eingabe deaktiviert"
                                    value={props.element.disabled ?? undefined}
                                    onChange={(checked) => {
                                        props.onChange({
                                            // @ts-ignore
                                            required: false,
                                            disabled: checked,
                                            technical: false,
                                        });
                                    }}
                                    hint="Deaktivierte Eingaben können nicht bearbeitet werden."
                                    disabled={!props.editable || Boolean(props.element.required) || Boolean(props.element.technical)}
                                />
                            }
                        </Grid>
                        <Grid
                            size={{
                                xs: 12,
                                lg: 4,
                            }}
                        >
                            {
                                props.scope !== 'data_modelling' &&
                                <CheckboxFieldComponent
                                    label="Technisches Feld"
                                    value={props.element.technical ?? undefined}
                                    onChange={(checked) => {
                                        props.onChange({
                                            // @ts-ignore
                                            required: false,
                                            disabled: false,
                                            technical: checked,
                                        });
                                    }}
                                    hint="Technische Felder sind für Antragstellende unsichtbar und nicht bearbeitbar."
                                    disabled={!props.editable || Boolean(props.element.required) || Boolean(props.element.disabled)}
                                />
                            }
                        </Grid>
                    </Grid>
                </>
            }
            <ElementEditorSectionHeader
                title="Technische Informationen für Entwickler:innen"
                sx={{mt: 8}}
            >
                Hier finden Sie technische Zusatzinformationen, die insbesondere für Entwickler:innen von Bedeutung sein können.
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
                        label="ID des Elements"
                        value={props.element.id ?? ''}
                        disabled={props.scope === 'application' || props.scope === 'preset' || !props.editable}
                        onChange={(id) => {
                            if (props.scope === 'data_modelling') {
                                // @ts-ignore
                                props.onChange({
                                    id: id ?? '',
                                });
                            }
                        }}
                        endAction={{
                            icon: <ContentPasteIcon />,
                            onClick: () => {
                                navigator.clipboard.writeText(props.element.id ?? '')
                                    .then(() => {
                                        dispatch(showSuccessSnackbar('Element-ID in Zwischenablage kopiert'));
                                    })
                                    .catch((error) => {
                                        console.error('Failed to copy ID', error);
                                        dispatch(showErrorSnackbar('Element-ID konnte nicht in Zwischenablage kopiert werden'));
                                    });
                            },
                        }}
                        hint={props.scope === 'data_modelling' ? 'Wenn Sie eine bereits in Verwendung befindliche ID nachträglich ändern, werden bereits erfasste Daten verworfen. Eine automatische Migration findet derzeit nicht statt. Bitte verwenden Sie daher von Beginn an stabile, eindeutige IDs.' : undefined}
                    />
                </Grid>
            </Grid>
        </Box>
    );
}
