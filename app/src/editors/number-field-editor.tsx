import {NumberFieldElement} from "../models/elements/form/input/number-field-element";
import {BaseEditor} from "./base-editor";
import {TextFieldComponent} from "../components/text-field/text-field-component";
import {NumberFieldComponent} from "../components/number-field/number-field-component";

export const NumberFieldEditor: BaseEditor<NumberFieldElement> = ({element, onPatch}) => {
    return (
        <>
            <TextFieldComponent
                value={element.placeholder ?? ''}
                label="Platzhalter"
                onChange={val => onPatch({
                    placeholder: val,
                })}
            />

            <TextFieldComponent
                value={element.suffix ?? ''}
                label="Einheit"
                onChange={val => onPatch({
                    suffix: val,
                })}
            />

            <NumberFieldComponent
                value={element.decimalPlaces}
                label="Dezimalstellen"
                onChange={val => onPatch({
                    decimalPlaces: val,
                })}
            />
        </>
    );
}
