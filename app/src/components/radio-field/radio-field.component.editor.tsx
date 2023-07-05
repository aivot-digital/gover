import {RadioFieldElement} from '../../models/elements/form/input/radio-field-element';
import {StringListInput} from "../string-list-input/string-list-input";
import {BaseEditorProps} from "../../editors/base-editor";

export function RadioFieldComponentEditor(props: BaseEditorProps<RadioFieldElement>) {
    return (
        <>
            <StringListInput
                label="Optionen"
                addLabel="Option hinzufügen"
                hint="Die Bürger:in kann genau eine dieser Optionen auswählen."
                noItemsHint="Bitte fügen Sie mindestens eine Option hinzu."
                value={props.element.options}
                onChange={options => props.onPatch({
                    options: options,
                })}
                allowEmpty={false}
            />
        </>
    );
}
