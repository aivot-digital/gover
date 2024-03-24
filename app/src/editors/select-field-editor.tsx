import React from 'react';
import {type BaseEditorProps} from './base-editor';
import {type SelectFieldElement, type SelectFieldElementOption} from '../models/elements/form/input/select-field-element';
import {StringListInput} from '../components/string-list-input/string-list-input';
import {type ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {OptionListInput} from '../components/option-list-input/option-list-input';
import {isStringArray} from '../utils/is-string-array';

export function SelectFieldEditor(props: BaseEditorProps<SelectFieldElement, ElementTreeEntity>): JSX.Element {
    const options = props.element.options ?? [];

    if (isStringArray(options) && options.length > 0) {
        return (
            <StringListInput
                label="Optionen"
                addLabel="Option hinzufügen"
                hint="Die Bürger:in kann genau eine dieser Optionen auswählen."
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
        );
    } else {
        return (
            <OptionListInput
                label="Optionen"
                addLabel="Option hinzufügen"
                hint="Die Bürger:in kann genau eine dieser Optionen auswählen."
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
        );
    }
}
