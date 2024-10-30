import {type RadioFieldElement} from '../../models/elements/form/input/radio-field-element';
import {StringListInput} from '../string-list-input/string-list-input';
import {type BaseEditorProps} from '../../editors/base-editor';
import {type ElementTreeEntity} from '../element-tree/element-tree-entity';
import {OptionListInput} from '../option-list-input/option-list-input';
import {type SelectFieldElementOption} from '../../models/elements/form/input/select-field-element';
import React from 'react';
import {isStringArray} from '../../utils/is-string-array';

export function RadioFieldComponentEditor(props: BaseEditorProps<RadioFieldElement, ElementTreeEntity>): JSX.Element {
    const options = props.element.options ?? [];

    if (isStringArray(options) && options.length > 0) {
        return (
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
        );
    } else {
        return (
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
        );
    }
}
