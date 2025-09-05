import React from 'react';
import {type BaseEditorProps} from './base-editor';
import {type SelectFieldElement, type SelectFieldElementOption} from '../models/elements/form/input/select-field-element';
import {type ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {OptionListInput} from '../components/option-list-input/option-list-input';
import {AutocompleteSelect} from '../components/autocomple-select/autocomplete-select';

export function SelectFieldEditor(props: BaseEditorProps<SelectFieldElement, ElementTreeEntity>) {
    const options = props.element.options ?? [];


    return (
        <>
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
                variant="outlined"
            />

            <AutocompleteSelect
                type={props.element.type}
                value={props.element.autocomplete}
                onChange={(val) => {
                    props.onPatch({
                        autocomplete: val,
                    });
                }}
                editable={props.editable}
            />
        </>
    );
}
