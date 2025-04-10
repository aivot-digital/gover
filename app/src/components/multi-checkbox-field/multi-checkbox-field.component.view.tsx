import React, {useMemo} from 'react';
import {type MultiCheckboxFieldElement, type MultiCheckboxFieldElementOption} from '../../models/elements/form/input/multi-checkbox-field-element';
import {type BaseViewProps} from '../../views/base-view';
import {MultiCheckboxComponent} from './multi-checkbox-component';
import {hasDerivableAspects} from '../../utils/has-derivable-aspects';

export function MultiCheckboxFieldComponentView(props: BaseViewProps<MultiCheckboxFieldElement, string[]>) {
    const {
        element,
        setValue,
        value,
        error,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;

    const {
        disabled,
        options: baseOptions,
    } = element;

    const options = useMemo(() => {
        if (baseOptions == null) {
            return [];
        }

        return baseOptions
            .map((option: string | MultiCheckboxFieldElementOption) => {
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
        <MultiCheckboxComponent
            label={element.label ?? ''}
            value={value ?? undefined}
            onChange={setValue}
            options={options}
            disabled={isDisabled}
            required={element.required}
            error={error}
            hint={element.hint}
            displayInline={element.displayInline}
            busy={isBusy}
        />
    );
}
