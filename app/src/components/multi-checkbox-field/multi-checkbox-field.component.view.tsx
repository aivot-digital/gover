import React from 'react';
import {type MultiCheckboxFieldElement, type MultiCheckboxFieldElementOption} from '../../models/elements/form/input/multi-checkbox-field-element';
import {type BaseViewProps} from '../../views/base-view';
import {MultiCheckboxComponent} from './multi-checkbox-component';

export function MultiCheckboxFieldComponentView(props: BaseViewProps<MultiCheckboxFieldElement, string[]>): JSX.Element {
    const options = (props.element.options ?? []).map((option: string | MultiCheckboxFieldElementOption) => {
        if (typeof option === 'string') {
            return {
                value: option,
                label: option,
            };
        } else {
            return option;
        }
    });

    return (
        <MultiCheckboxComponent
            label={props.element.label ?? ''}
            value={props.value ?? undefined}
            onChange={props.setValue}
            options={options}
            disabled={props.element.disabled}
            required={props.element.required}
            error={props.error}
            hint={props.element.hint}
        />
    );
}
