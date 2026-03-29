import React, {useMemo} from 'react';
import {type RadioFieldElement} from '../../models/elements/form/input/radio-field-element';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import {type BaseViewProps} from '../../views/base-view';
import {hasDerivableAspects} from '../../utils/has-derivable-aspects';
import {RadioFieldComponent} from './radio-field-component';

export function RadioFieldComponentView(props: BaseViewProps<RadioFieldElement, string>) {
    const {
        element,
        setValue,
        value,
        errors,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;

    const {
        disabled: elementDisabled,
    } = element;

    const isDisabled = useMemo(() => {
        return elementDisabled || isGloballyDisabled;
    }, [elementDisabled, isGloballyDisabled]);

    const isBusy = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    return (
        <RadioFieldComponent
            label={element.label ?? ''}
            options={element.options}
            value={value}
            onChange={(newValue) => {
                if (!isBusy) {
                    setValue(isStringNullOrEmpty(newValue) ? undefined : newValue ?? undefined);
                }
            }}
            required={element.required}
            error={errors != null ? errors.join(' ') : undefined}
            hint={element.hint}
            disabled={isDisabled}
            displayInline={element.displayInline}
            toggleButtons={element.toggleButtons}
        />
    );
}
