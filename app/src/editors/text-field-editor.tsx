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
                error={element.maxCharacters != null && element.minCharacters != null && element.minCharacters > element.maxCharacters ? 'Sie fordern mehr Zeichen als Sie maximal zulassen.' : undefined}
                hint="Geben Sie 0 oder nichts ein, um keine Minimalanzahl zu fordern."
            />

            <NumberFieldComponent
                label="Maximalanzahl an Zeichen"
                value={element.maxCharacters}
                onChange={val => onPatch({
                    maxCharacters: val,
                })}
                error={element.maxCharacters != null && element.minCharacters != null && element.maxCharacters < element.minCharacters ? 'Sie fordern mehr Zeichen als Sie maximal zulassen.' : undefined}
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
