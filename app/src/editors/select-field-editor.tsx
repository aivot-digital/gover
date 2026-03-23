import React from 'react';
import {type BaseEditorProps} from './base-editor';
import {type SelectFieldElement, type SelectFieldElementOption} from '../models/elements/form/input/select-field-element';
import {type ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {OptionListInput} from '../components/option-list-input/option-list-input';
import {AutocompleteSelect} from '../components/autocomple-select/autocomplete-select';

export function SelectFieldEditor(props: BaseEditorProps<SelectFieldElement, ElementTreeEntity>) {
    const {
        element,
        editable,
        onPatch,
        scope,
    } = props;

    const options: SelectFieldElementOption[] = element.options ?? [];

    return (
        <>
            <OptionListInput
                label="Optionen"
                addLabel="Option hinzufügen"
                hint="Die antragstellende Person kann genau eine dieser Optionen auswählen."
                noItemsHint="Bitte fügen Sie mindestens eine Option hinzu."
                value={options}
                onChange={(options) => {
                    onPatch({
                        options,
                    });
                }}
                allowEmpty={false}
                disabled={!editable}
                variant="outlined"
            />

            {
                scope !== 'data_modelling' &&
                <AutocompleteSelect
                    type={element.type}
                    value={element.autocomplete}
                    onChange={(val) => {
                        onPatch({
                            autocomplete: val,
                        });
                    }}
                    editable={editable}
                />
            }
        </>
    );
}
