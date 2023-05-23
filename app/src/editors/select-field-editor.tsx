import {BaseEditor} from "./base-editor";
import {SelectFieldElement} from "../models/elements/form/input/select-field-element";
import {StringListInput} from "../components/string-list-input/string-list-input";

export const SelectFieldEditor: BaseEditor<SelectFieldElement> = ({element, onPatch}) => {
    return (
        <>
            <StringListInput
                label="Optionen"
                addLabel="Option hinzufügen"
                hint="Die Bürger:in kann genau eine dieser Optionen auswählen."
                noItemsHint="Bitte fügen Sie mindestens eine Option hinzu."
                value={element.options}
                onChange={options => onPatch({
                    options: options,
                })}
                allowEmpty={false}
            />
        </>
    );
}
