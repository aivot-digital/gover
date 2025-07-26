import {type RadioFieldElement} from '../../models/elements/form/input/radio-field-element';
import {type BaseEditorProps} from '../../editors/base-editor';
import {type ElementTreeEntity} from '../element-tree/element-tree-entity';
import {OptionListInput} from '../option-list-input/option-list-input';
import React from 'react';
import {CheckboxFieldComponent} from '../checkbox-field/checkbox-field-component';

export function RadioFieldComponentEditor(props: BaseEditorProps<RadioFieldElement, ElementTreeEntity>) {
    return (
        <>
            <OptionListInput
                label="Optionen"
                addLabel="Option hinzufügen"
                hint="Die antragstellende Person kann genau eine dieser Optionen auswählen."
                noItemsHint="Bitte fügen Sie mindestens eine Option hinzu."
                value={props.element.options ?? []}
                onChange={(options) => {
                    props.onPatch({
                        options,
                    });
                }}
                allowEmpty={false}
                disabled={!props.editable}
                variant="outlined"
            />

            <CheckboxFieldComponent
                label="Optionen nebeneinander anzeigen"
                value={props.element.displayInline ?? false}
                onChange={(checked) => {
                    props.onPatch({
                        displayInline: checked,
                    });
                }}
                disabled={!props.editable}
                hint="Zeigt die Optionen nebeneinander anstatt untereinander an. Dies kann Platz sparen und die Übersichtlichkeit verbessern."
            />

        </>
    );
}
