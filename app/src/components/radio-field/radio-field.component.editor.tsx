import {type RadioFieldElement} from '../../models/elements/form/input/radio-field-element';
import {StringListInput} from '../string-list-input/string-list-input';
import {type BaseEditorProps} from '../../editors/base-editor';
import {type ElementTreeEntity} from '../element-tree/element-tree-entity';
import {OptionListInput} from '../option-list-input/option-list-input';
import {type SelectFieldElementOption} from '../../models/elements/form/input/select-field-element';
import React from 'react';
import {isStringArray} from '../../utils/is-string-array';
import {CheckboxFieldComponent} from "../checkbox-field/checkbox-field-component";

export function RadioFieldComponentEditor(props: BaseEditorProps<RadioFieldElement, ElementTreeEntity>): JSX.Element {
    const options = props.element.options ?? [];

    return (
        <>
            {
                (isStringArray(options) && options.length > 0) ?
                    <StringListInput
                        label="Optionen"
                        addLabel="Option hinzufügen"
                        hint="Die antragstellende Person kann genau eine dieser Optionen auswählen."
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
                    :
                    <OptionListInput
                        label="Optionen"
                        addLabel="Option hinzufügen"
                        hint="Die antragstellende Person kann genau eine dieser Optionen auswählen."
                        noItemsHint="Bitte fügen Sie mindestens eine Option hinzu."
                        value={options as SelectFieldElementOption[]}
                        onChange={(options) => {
                            props.onPatch({
                                options,
                            });
                        }}
                        allowEmpty={false}
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
    )
}
