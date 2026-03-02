import {useMemo} from 'react';
import {BaseViewProps} from './base-view';
import {DateTimeRangeFieldElement, DateTimeRangeValue} from '../models/elements/form/input/date-time-range-field-element';
import {DateTimeRangeFieldComponent} from '../components/date-time-range-field/date-time-range-field-component';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {TimeFieldComponentModelMode} from '../models/elements/form/input/time-field-element';

export function DateTimeRangeFieldView(props: BaseViewProps<DateTimeRangeFieldElement, DateTimeRangeValue>) {
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
        <DateTimeRangeFieldComponent
            label={element.label ?? ''}
            value={value ?? undefined}
            onChange={setValue}
            hint={element.hint ?? undefined}
            required={element.required ?? false}
            disabled={isDisabled}
            busy={isBusy}
            error={errors?.join(', ') ?? undefined}
            placeholder={element.placeholder ?? undefined}
            mode={element.mode ?? TimeFieldComponentModelMode.Minute}
        />
    );
}
