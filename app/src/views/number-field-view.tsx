import {useEffect} from 'react';
import {BaseView} from "./base-view";
import {NumberFieldElement} from "../models/elements/form/input/number-field-element";
import {NumberFieldComponent} from "../components/number-field/number-field-component";

export const NumberFieldView: BaseView<NumberFieldElement, number> = ({
                                                                          element,
                                                                          value,
                                                                          error,
                                                                          setValue,
                                                                      }) => {
    useEffect(() => {
        if (element.id != null && element.computedValue != null && value !== element.computedValue) {
            setValue(element.computedValue);
        }
    }, [value, element.id, element.computedValue, setValue]);

    return (
        <NumberFieldComponent
            label={element.label ?? ''}
            value={value ?? undefined}
            onChange={setValue}
            placeholder={element.placeholder}
            decimalPlaces={element.decimalPlaces}
            hint={element.hint}
            error={error}
            suffix={element.suffix}
            required={element.required}
            disabled={element.disabled}
        />
    );
};
