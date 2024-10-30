import {BaseView} from "./base-view";
import {DateFieldComponentModelMode, DateFieldElement} from "../models/elements/form/input/date-field-element";
import {DateFieldComponent} from "../components/date-field/date-field-component";


export const DateFieldView: BaseView<DateFieldElement, string> = ({
                                                                      setValue,
                                                                      element,
                                                                      value,
                                                                      error,
                                                                  }) => {
    return (
        <DateFieldComponent
            label={element.label ?? ''}
            autocomplete={element.autocomplete}
            value={value ?? undefined}
            error={error}
            onChange={setValue}
            disabled={element.disabled}
            required={element.required}
            hint={element.hint}
            mode={element.mode ?? DateFieldComponentModelMode.Day}
        />
    );
};
