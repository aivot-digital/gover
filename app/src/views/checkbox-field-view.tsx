import {BaseView} from "./base-view";
import {CheckboxFieldComponent} from "../components/checkbox-field/checkbox-field-component";
import {CheckboxFieldElement} from "../models/elements/form/input/checkbox-field-element";

export const CheckboxFieldView: BaseView<CheckboxFieldElement, boolean> = ({
                                                                               setValue,
                                                                               element,
                                                                               error,
                                                                               value,
                                                                           }) => {
    return (
        <CheckboxFieldComponent
            label={element.label ?? ''}
            onChange={setValue}
            value={value ?? undefined}
            error={error}
            hint={element.hint}
            disabled={element.disabled}
            required={element.required}
        />
    );
};
