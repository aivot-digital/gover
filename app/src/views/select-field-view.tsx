import React, {useMemo} from 'react';
import {type BaseViewProps} from './base-view';
import {type SelectFieldElement, type SelectFieldElementOption} from '../models/elements/form/input/select-field-element';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';


export function SelectFieldView(props: BaseViewProps<SelectFieldElement, string>): JSX.Element {
    const {
        element,
        setValue,
        value,
        error,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;

    const {
        options: baseOptions,
        disabled,
    } = element;

    const options = useMemo(() => {
        if (baseOptions == null) {
            return [];
        }

        return baseOptions
            .map((option: string | SelectFieldElementOption) => {
                if (typeof option === 'string') {
                    return {
                        value: option,
                        label: option,
                    };
                } else {
                    return option;
                }
            });
    }, [baseOptions]);

    const isDisabled = useMemo(() => {
        return disabled || isGloballyDisabled;
    }, [disabled, isGloballyDisabled]);

    const isBusy = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    return (
        <SelectFieldComponent
            label={element.label ?? ''}
            autocomplete={element.autocomplete}
            error={error}
            hint={element.hint}
            placeholder={element.placeholder}
            value={value ?? undefined}
            onChange={setValue}
            disabled={isDisabled}
            required={element.required}
            options={options}

            sx={{
                backgroundColor: isBusy ? "#F8F8F8" : undefined,
                cursor: isBusy ? "not-allowed" : undefined,
            }}
            readOnly={isBusy}
        />
    );
}
