import React from 'react';
import {type BaseViewProps} from './base-view';
import {type SelectFieldElement, type SelectFieldElementOption} from '../models/elements/form/input/select-field-element';
import {SelectFieldComponent} from '../components/select-field/select-field-component';


export function SelectFieldView(props: BaseViewProps<SelectFieldElement, string>): JSX.Element {
    const options = (props.element.options ?? []).map((option: string | SelectFieldElementOption) => {
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
        <SelectFieldComponent
            label={props.element.label ?? ''}
            autocomplete={props.element.autocomplete}
            error={props.error}
            hint={props.element.hint}
            placeholder={props.element.placeholder}
            value={props.value ?? undefined}
            onChange={props.setValue}
            disabled={props.element.disabled}
            required={props.element.required}
            options={options}
        />
    );
}
