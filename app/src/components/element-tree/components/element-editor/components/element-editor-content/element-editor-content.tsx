import {DefaultTabs} from '../../data/default-tabs';
import {Box} from '@mui/material';
import {EditorDispatcher} from '../../../../../editor-dispatcher';
import {CodeTab} from '../../tabs/code-tab/code-tab';
import React from 'react';
import {StructureTab} from '../../tabs/structure-tab/structure-tab';
import {TestTab} from '../../tabs/test-tab/test-tab';
import {ElementType} from '../../../../../../data/element-type/element-type';
import {type AnyElement} from '../../../../../../models/elements/any-element';
import {type ElementEditorContentProps} from './element-editor-content-props';
import {type AnyFormElement} from '../../../../../../models/elements/form/any-form-element';
import {type BaseInputElement} from '../../../../../../models/elements/form/base-input-element';
import {isAnyInputElement} from '../../../../../../models/elements/form/input/any-input-element';
import {TextFieldComponent} from '../../../../../text-field/text-field-component';
import {SelectFieldComponent} from '../../../../../select-field/select-field-component';
import {CheckboxFieldComponent} from '../../../../../checkbox-field/checkbox-field-component';
import {type Application} from '../../../../../../models/entities/application';
import {type Preset} from '../../../../../../models/entities/preset';

export function ElementEditorContent<T extends AnyElement, E extends Application | Preset>(props: ElementEditorContentProps<T, E>): JSX.Element | null {
    switch (props.currentTab) {
        case DefaultTabs.properties:
            return <DefaultEditor {...props}/>;
        case DefaultTabs.visibility:
            return (
                <CodeTab
                    parents={props.parents}
                    key="visibility"
                    resultTitle="Sichtbarkeit festlegen"
                    resultHint="Dieses Element ist sichtbar, wenn die folgende Funktion wahr ist:"
                    element={props.element}
                    func={props.element.isVisible}
                    allowNoCode={true}
                    shouldReturnString={false}
                    onChange={(updatedFunc) => {
                        // @ts-expect-error
                        props.onChange({
                            isVisible: updatedFunc,
                        });
                    }}
                    editable={props.editable}
                />
            );
        case DefaultTabs.validation:
            return (
                <CodeTab
                    parents={props.parents}
                    key="validate"
                    resultTitle="Validierung durchführen"
                    resultHint="Dieses Element ist valide, wenn die folgende Funktion keine Meldung mit einem Validierungsproblem erzeugt:"
                    element={props.element}
                    func={(props.element as BaseInputElement<any, any>).validate}
                    allowNoCode={true}
                    shouldReturnString={true}
                    onChange={(updatedFunc) => {
                        props.onChange({
                            // @ts-expect-error
                            validate: updatedFunc,
                        });
                    }}
                    editable={props.editable}
                />
            );
        case DefaultTabs.value:
            return (
                <CodeTab
                    parents={props.parents}
                    key="value"
                    resultTitle="Dynamischen Wert bestimmen"
                    resultHint="Dieses Element bekommt den Rückgabewert der folgenden Funktion:"
                    element={props.element}
                    func={(props.element as BaseInputElement<any, any>).computeValue}
                    allowNoCode={false}
                    shouldReturnString={false}
                    onChange={(updatedFunc) => {
                        props.onChange({
                            // @ts-expect-error
                            computeValue: updatedFunc,
                        });
                    }}
                    editable={props.editable}
                />
            );
        case DefaultTabs.patch:
            return (
                <CodeTab
                    parents={props.parents}
                    key="patch"
                    resultTitle="Element aktualisieren"
                    resultHint="Dieses Element wird mit dem Rückgabewert der folgenden Funktion aktualisiert:"
                    element={props.element}
                    func={props.element.patchElement}
                    allowNoCode={false}
                    shouldReturnString={false}
                    onChange={(updatedFunc) => {
                        // @ts-expect-error
                        props.onChange({
                            patchElement: updatedFunc,
                        });
                    }}
                    editable={props.editable}
                />
            );
        case DefaultTabs.structure:
            return (
                <StructureTab
                    elementModel={props.element}
                    onChange={(struct) => {
                        props.onChange(struct);
                    }}
                    editable={props.editable}
                />
            );
        case DefaultTabs.test:
            return (
                <TestTab
                    elementModel={props.element}
                    onPatch={(updatedTestProtocolSet) => {
                        // @ts-expect-error
                        props.onChange({
                            testProtocolSet: updatedTestProtocolSet,
                        });
                    }}
                    editable={props.editable}
                />
            );
        default:
            if (props.additionalTabs.some((add) => props.currentTab === add.label)) {
                return (
                    <Box
                        sx={{
                            p: 4,
                        }}
                    >
                        <EditorDispatcher
                            props={props.element}
                            onPatch={props.onChange}
                            additionalTabIndex={props.additionalTabs.findIndex((add) => props.currentTab === add.label)}
                            entity={props.entity}
                            onPatchEntity={props.onChangeEntity}
                            editable={props.editable}
                        />
                    </Box>
                );
            }
            return null;
    }
}

function DefaultEditor<T extends AnyElement, E extends Application | Preset>(props: ElementEditorContentProps<T, E>): JSX.Element {
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
                    value={(props.element as AnyFormElement)?.weight?.toString()}
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

            <Box
                sx={{
                    m: 4,
                }}
            />

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

                    <CheckboxFieldComponent
                        label="Pflichtangabe"
                        value={props.element.required}
                        onChange={(checked) => {
                            props.onChange({
                                // @ts-expect-error
                                required: checked,
                                disabled: undefined,
                            });
                        }}
                        hint={(props.element.disabled === true) ? 'Deaktivierte Eingaben können keine Pflichtangaben sein.' : undefined}
                        disabled={!props.editable}
                    />

                    <CheckboxFieldComponent
                        label="Eingabe deaktiviert"
                        value={props.element.disabled}
                        onChange={(checked) => {
                            props.onChange({
                                // @ts-expect-error
                                required: undefined,
                                disabled: checked,
                            });
                        }}
                        hint={(props.element.disabled === true) ? 'Pflichtangaben können nicht deaktiviert werden.' : undefined}
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
        </Box>
    );
}
