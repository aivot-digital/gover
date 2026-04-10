import {BaseViewProps} from './base-view';
import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';
import {CheckboxFieldElement} from '../models/elements/form/input/checkbox-field-element';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {useMemo} from 'react';

export function CheckboxFieldView(props: BaseViewProps<CheckboxFieldElement, boolean>) {
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
    } = element;

    const isDisabled = useMemo(() => {
        return disabled || isGloballyDisabled;
    }, [disabled, isGloballyDisabled]);

    const isBusy = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    return (
        <CheckboxFieldComponent
            label={element.label ?? ''}
            onChange={setValue}
            value={value ?? undefined}
            error={errors?.join(', ') ?? undefined}
            hint={element.hint ?? undefined}
            disabled={isDisabled}
            required={element.required ?? false}
            busy={isBusy}
            variant={element.variant ?? undefined}
        />
    );
}