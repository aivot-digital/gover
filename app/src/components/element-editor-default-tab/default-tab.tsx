import {type AnyElement} from '../../models/elements/any-element';
import {type ElementEditorContentProps} from '../element-editor-content/element-editor-content-props';
import {Box} from '@mui/material';
import {TextFieldComponent} from '../text-field/text-field-component';
import {ElementType} from '../../data/element-type/element-type';
import {SelectFieldComponent} from '../select-field/select-field-component';
import {type AnyFormElement} from '../../models/elements/form/any-form-element';
import {isAnyInputElement} from '../../models/elements/form/input/any-input-element';
import {CheckboxFieldComponent} from '../checkbox-field/checkbox-field-component';
import {EditorDispatcher} from '../editor-dispatcher';
import React from 'react';
import {type ElementTreeEntity} from '../element-tree/element-tree-entity';

export function DefaultTab<T extends AnyElement, E extends ElementTreeEntity>(props: ElementEditorContentProps<T, E>): JSX.Element {
    return (
        <Box
            sx={{
                p: 4,
            }}
        >
            <TextFieldComponent
                label="ID (für Entwickler:innen)"
                value={props.element.id ?? ''}
                disabled
                onChange={(_) => {
                }}
            />

            {
                props.element.type !== ElementType.Root &&
                props.element.type !== ElementType.IntroductionStep &&
                props.element.type !== ElementType.SummaryStep &&
                props.element.type !== ElementType.SubmitStep &&
                <TextFieldComponent
                    label="Interner Name"
                    value={props.element.name}
                    onChange={(val) => {
                        // @ts-expect-error
                        props.onChange({
                            name: val ?? '',
                        });
                    }}
                    hint="Vergeben Sie einen Namen für dieses Element um es besser identifizieren zu können. Diesen Namen können nur Sie und Ihre Kolleg:innen einsehen"
                    maxCharacters={30}
                    disabled={!props.editable}
                />
            }

            {
                props.element.type !== ElementType.Root &&
                props.element.type !== ElementType.IntroductionStep &&
                props.element.type !== ElementType.Step &&
                props.element.type !== ElementType.SummaryStep &&
                props.element.type !== ElementType.SubmitStep &&
                <SelectFieldComponent
                    label="Breite"
                    value={(props.element as AnyFormElement)?.weight?.toString() ?? '12'}
                    onChange={(val) => {
                        props.onChange({
                            // @ts-expect-error
                            weight: val != null ? parseInt(val) : 12,
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
                            label: '50%',
                            value: '6',
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
                    hint="Bestimmen Sie die Breite des Anzeigeelements."
                    disabled={!props.editable}
                />
            }

            {
                isAnyInputElement(props.element) &&
                <>
                    <TextFieldComponent
                        value={props.element.label}
                        label="Titel"
                        onChange={(val) => {
                            props.onChange({
                                // @ts-expect-error
                                label: val,
                            });
                        }}
                        hint="Dieser Text wird den Bürger:innen im Antrag angezeigt."
                        disabled={!props.editable}
                    />

                    <TextFieldComponent
                        value={props.element.hint}
                        label="Hinweis"
                        onChange={(val) => {
                            props.onChange({
                                // @ts-expect-error
                                hint: val,
                            });
                        }}
                        hint="Der Hinweis sollte genutzt werden, um den Bürger:innen weitere Informationen über die Eingabe zu geben."
                        disabled={!props.editable}
                    />
                    </>
            }

            <EditorDispatcher
                props={props.element}
                onPatch={props.onChange}
                entity={props.entity}
                onPatchEntity={props.onChangeEntity}
                editable={props.editable}
            />

            {
                isAnyInputElement(props.element) &&
                <>
                    <CheckboxFieldComponent
                        label="Pflichtangabe"
                        value={props.element.required}
                        onChange={(checked) => {
                            props.onChange({
                                // @ts-expect-error
                                required: checked,
                                disabled: false,
                                technical: false,
                            });
                        }}
                        hint="Pflichtangaben müssen von den Bürger:innen ausgefüllt werden."
                        disabled={!props.editable || Boolean(props.element.disabled) || Boolean(props.element.technical)}
                    />

                    <CheckboxFieldComponent
                        label="Eingabe deaktiviert"
                        value={props.element.disabled}
                        onChange={(checked) => {
                            props.onChange({
                                // @ts-expect-error
                                required: false,
                                disabled: checked,
                                technical: false,
                            });
                        }}
                        hint="Deaktivierte Eingaben können nicht bearbeitet werden."
                        disabled={!props.editable || Boolean(props.element.required) || Boolean(props.element.technical)}
                    />

                    <CheckboxFieldComponent
                        label="Technisches Feld"
                        value={props.element.technical}
                        onChange={(checked) => {
                            props.onChange({
                                // @ts-expect-error
                                required: false,
                                disabled: false,
                                technical: checked,
                            });
                        }}
                        hint="Technische Felder sind für die Bürger:innen nicht sichtbar und können nicht bearbeitet werden."
                        disabled={!props.editable || Boolean(props.element.required) || Boolean(props.element.disabled)}
                    />
                </>
            }

        </Box>
    );
}
