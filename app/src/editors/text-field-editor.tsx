import {TextFieldElement} from "../models/elements/form/input/text-field-element";
import {TextFieldComponent} from "../components/text-field/text-field-component";
import {BaseEditor} from "./base-editor";
import {CheckboxFieldComponent} from "../components/checkbox-field/checkbox-field-component";
import {NumberFieldComponent} from "../components/number-field/number-field-component";

export const TextFieldEditor: BaseEditor<TextFieldElement> = ({element, onPatch}) => {
    return (
        <>
            <TextFieldComponent
                value={element.placeholder}
                label="Platzhalter"
                onChange={value => onPatch({
                    placeholder: value,
                })}
            />

            <NumberFieldComponent
                label="Minimalanzahl an Zeichen"
                value={element.minCharacters}
                onChange={val => onPatch({
                    minCharacters: val,
                })}
                hint="Geben Sie 0 oder nichts ein, um keine Minimalanzahl zu fordern."
            />

            <NumberFieldComponent
                label="Maximalanzahl an Zeichen"
                value={element.maxCharacters}
                onChange={val => onPatch({
                    maxCharacters: val,
                })}
                hint="Geben Sie 0 oder nichts ein, um keine Maximalanzahl zu fordern."
            />

            <CheckboxFieldComponent
                label="Mehrzeilige Texteingabe"
                value={element.isMultiline}
                onChange={checked => onPatch({
                    isMultiline: checked,
                })}
            />
        </>
    );
}
