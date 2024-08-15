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
import {showSuccessSnackbar} from '../../slices/snackbar-slice';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import ContentPasteIcon from '@mui/icons-material/ContentPaste';

export function DefaultTab<T extends AnyElement, E extends ElementTreeEntity>(props: ElementEditorContentProps<T, E>): JSX.Element {
    const dispatch = useAppDispatch();

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
                endAction={{
                    icon: <ContentPasteIcon />,
                    onClick: () => {
                        navigator.clipboard.writeText(props.element.id ?? '')
                            .then(() => {
                                dispatch(showSuccessSnackbar('ID kopiert'));
                            })
                            .catch((error) => {
                                console.error('Failed to copy ID', error);
                                dispatch(showSuccessSnackbar('ID konnte nicht kopiert werden'));
                            });
                    },
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
                    label="Breite des Elements (Darstellung)"
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
                    hint="Bestimmen Sie die Breite des Anzeigeelements. Diese ist nur für Tablets und Desktops relevant. Auf mobilen Geräten wird das Element immer über die volle Breite dargestellt."
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
                        hint="Dieser Titel wird im Formular als Label für dieses Feld angezeigt. Bitte beachten Sie die Relevanz dieser Angabe für die Barrierefreiheit des Formulars."
                        disabled={!props.editable}
                        softLimitCharacters={20}
                        softLimitCharactersWarning={'Bitte formulieren Sie das Label so kurz/prägnant wie möglich. Label mit mehr als 20 Zeichen können auf kleinen Bildschirmen abgeschnitten werden und sind ggf. nicht vollständig lesbar.'}
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
                        hint="Dieser Hinweis sollte genutzt werden, um den antragstellenden Personen weitere Informationen über die Eingabe zu geben."
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
                        hint="Pflichtangaben müssen von den antragstellenden Personen ausgefüllt werden."
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
                        hint="Technische Felder sind für die antragstellenden Personen nicht sichtbar und können nicht bearbeitet werden."
                        disabled={!props.editable || Boolean(props.element.required) || Boolean(props.element.disabled)}
                    />
                </>
            }

        </Box>
    );
}
