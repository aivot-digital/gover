import {SelectFieldElement} from '../../models/elements/form/input/select-field-element';
import {BaseEditorProps} from '../_lib/base-editor-props';
import {StringListInput} from "../string-list-input/string-list-input";

export function SelectFieldComponentEditor(props: BaseEditorProps<SelectFieldElement>) {
    return (
        <>
            <StringListInput
                label="Optionen"
                addLabel="Option hinzufügen"
                hint="Die Bürger:in kann genau eine dieser Optionen auswählen."
                noItemsHint="Bitte fügen Sie mindestens eine Option hinzu."
                value={props.component.options}
                onChange={options => props.onPatch({
                    options: options,
                })}
                allowEmpty={false}
            />
        </>
    );
}
