import React from 'react';
import {type MultiCheckboxFieldElement} from '../../models/elements/form/input/multi-checkbox-field-element';
import {StringListInput} from '../string-list-input/string-list-input';
import {type BaseEditorProps} from '../../editors/base-editor';
import {NumberFieldComponent} from '../number-field/number-field-component';
import {type ElementTreeEntity} from '../element-tree/element-tree-entity';
import {isStringArray} from '../../utils/is-string-array';
import {OptionListInput} from '../option-list-input/option-list-input';
import {CheckboxFieldComponent} from "../checkbox-field/checkbox-field-component";

export function MultiCheckboxFieldComponentEditor(props: BaseEditorProps<MultiCheckboxFieldElement, ElementTreeEntity>): JSX.Element {
    const minRequiredError = (
        props.element.minimumRequiredOptions != null &&
        props.element.options != null &&
        props.element.minimumRequiredOptions > props.element.options.length
    );

    const options = props.element.options ?? [];

    return (
        <>
            {
                isStringArray(options) &&
                options.length > 0 &&
                <StringListInput
                    label="Optionen"
                    addLabel="Option hinzufügen"
                    hint="Die antragstellende Person kann eine oder mehrere dieser Optionen auswählen."
                    noItemsHint="Bitte fügen Sie mindestens eine Option hinzu."
                    value={options}
                    onChange={(options) => {
                        props.onPatch({
                            options,
                        });
                    }}
                    allowEmpty={false}
                    disabled={!props.editable}
                />
            }

            {
                (!isStringArray(options) ||
                options.length === 0) &&
                <OptionListInput
                    label="Optionen"
                    addLabel="Option hinzufügen"
                    hint="Die antragstellende Person kann eine oder mehrere dieser Optionen auswählen."
                    noItemsHint="Bitte fügen Sie mindestens eine Option hinzu."
                    value={options as any}
                    onChange={(options) => {
                        props.onPatch({
                            options,
                        });
                    }}
                    allowEmpty={false}
                    disabled={!props.editable}
                />
            }

            {
                props.element.required === true &&
                <NumberFieldComponent
                    label="Erforderliche Mindestanzahl"
                    value={props.element.minimumRequiredOptions ?? 1}
                    onChange={(val) => {
                        props.onPatch({
                            required: val === 0 ? false : props.element.required,
                            minimumRequiredOptions: val === 0 ? undefined : val,
                        });
                    }}
                    error={minRequiredError ? 'Sie fordern mehr Optionen als Sie definiert haben.' : undefined}
                    hint="Geben Sie 0 ein, um keine Mindestanzahl zu fordern."
                    disabled={!props.editable}
                />
            }

            <CheckboxFieldComponent
                label="Optionen nebeneinander anzeigen"
                value={props.element.displayInline}
                onChange={(checked) => {
                    props.onPatch({
                        displayInline: checked,
                    });
                }}
                disabled={!props.editable}
                hint={"Zeigt die Optionen nebeneinander anstatt untereinander an. Dies kann Platz sparen und die Übersichtlichkeit verbessern."}
            />
        </>
    );
}
