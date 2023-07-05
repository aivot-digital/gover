import {BaseView} from "./base-view";
import {SelectFieldElement} from "../models/elements/form/input/select-field-element";
import {SelectFieldComponent} from "../components/select-field/select-field-component";


export const SelectFieldView: BaseView<SelectFieldElement, string> = ({element, error, value, setValue}) => {
    return (
        <SelectFieldComponent
            label={element.label ?? ''}
            error={error}
            hint={element.hint}
            placeholder={element.placeholder}
            value={value ?? undefined}
            onChange={setValue}
            disabled={element.disabled}
            required={element.required}
            options={(element.options ?? []).map((option: string) => ({
                value: option,
                label: option,
            }))}
        />
    );
}

