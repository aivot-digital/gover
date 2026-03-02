import {useMemo} from 'react';
import {BaseViewProps} from './base-view';
import {DateRangeFieldElement, DateRangeValue} from '../models/elements/form/input/date-range-field-element';
import {DateRangeFieldComponent} from '../components/date-range-field/date-range-field-component';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';

export function DateRangeFieldView(props: BaseViewProps<DateRangeFieldElement, DateRangeValue>) {
    const {
        element,
        setValue,
        value,
        errors,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;

    const isDisabled = useMemo(() => {
        return element.disabled || isGloballyDisabled;
    }, [element.disabled, isGloballyDisabled]);

    const isBusy = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    return (
        <DateRangeFieldComponent
            label={element.label ?? ''}
            value={value ?? undefined}
            onChange={setValue}
            hint={element.hint ?? undefined}
            required={element.required ?? false}
            disabled={isDisabled}
            busy={isBusy}
            error={errors?.join(', ') ?? undefined}
            mode={element.mode ?? undefined}
        />
    );
}
