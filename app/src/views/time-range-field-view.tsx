import {useMemo} from 'react';
import {BaseViewProps} from './base-view';
import {TimeRangeFieldElement, TimeRangeValue} from '../models/elements/form/input/time-range-field-element';
import {TimeRangeFieldComponent} from '../components/time-range-field/time-range-field-component';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {TimeFieldComponentModelMode} from '../models/elements/form/input/time-field-element';

export function TimeRangeFieldView(props: BaseViewProps<TimeRangeFieldElement, TimeRangeValue>) {
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
        <TimeRangeFieldComponent
            label={element.label ?? ''}
            value={value ?? undefined}
            onChange={setValue}
            hint={element.hint ?? undefined}
            required={element.required ?? false}
            disabled={isDisabled}
            busy={isBusy}
            error={errors?.join(', ') ?? undefined}
            mode={element.mode ?? TimeFieldComponentModelMode.Minute}
        />
    );
}
