import {TextFieldComponent} from "../components/text-field/text-field-component";
import {BaseView} from "./base-view";
import {TextFieldElement} from "../models/elements/form/input/text-field-element";

export const TextFieldView: BaseView<TextFieldElement, string> = ({
                                                                      element,
                                                                      value,
                                                                      error,
                                                                      setValue,
                                                                  }) => {
    return (
        <TextFieldComponent
            label={element.label ?? ''}
            autocomplete={element.autocomplete}
            placeholder={element.placeholder}
            error={error}
            hint={element.hint}
            multiline={element.isMultiline}
            required={element.required}
            disabled={element.disabled}
            maxCharacters={element.maxCharacters}
            minCharacters={element.minCharacters}
            value={value ?? undefined}
            onChange={val => setValue(val)}
        />
    );
};
