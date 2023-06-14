import {MultiCheckboxFieldElement} from '../../models/elements/form/input/multi-checkbox-field-element';
import {BaseViewProps} from "../../views/base-view";
import {MultiCheckboxComponent} from "./multi-checkbox-component";

export function MultiCheckboxFieldComponentView({
                                                    element,
                                                    value,
                                                    error,
                                                    setValue
                                                }: BaseViewProps<MultiCheckboxFieldElement, string[]>) {
    return (
        <>
            <MultiCheckboxComponent
                label={element.label ?? ''}
                value={value ?? undefined}
                onChange={setValue}
                options={element.options ?? []}
                disabled={element.disabled}
                required={element.required}
                error={error}
                hint={element.hint}
            />
        </>
    );
}
