import React, {useMemo} from 'react';
import {type MultiCheckboxFieldElement} from '../../models/elements/form/input/multi-checkbox-field-element';
import {type BaseViewProps} from '../../views/base-view';
import {MultiCheckboxComponent} from './multi-checkbox-component';
import {hasDerivableAspects} from '../../utils/has-derivable-aspects';

export function MultiCheckboxFieldComponentView(props: BaseViewProps<MultiCheckboxFieldElement, string[]>) {
    const {
        element,
        setValue,
        value,
        errors,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;

    const {
        disabled,
        options,
    } = element;

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
            options={options ?? []}
            disabled={isDisabled}
            required={element.required ?? undefined}
            error={errors != null ? errors.join(' ') : undefined}
            hint={element.hint ?? undefined}
            displayInline={element.displayInline ?? undefined}
            busy={isBusy}
        />
    );
}
