import {useMemo} from 'react';
import {BaseViewProps} from './base-view';
import {DateTimeFieldElement} from '../models/elements/form/input/date-time-field-element';
import {DateTimeFieldComponent} from '../components/date-time-field/date-time-field-component';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {TimeFieldComponentModelMode} from '../models/elements/form/input/time-field-element';

export function DateTimeFieldView(props: BaseViewProps<DateTimeFieldElement, string>) {
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

    const dateValue = useMemo(() => {
        if (value == null) {
            return undefined;
        }

        const date = new Date(value);
        return isNaN(date.getTime()) ? undefined : value;
    }, [value]);

    return (
        <DateTimeFieldComponent
            label={element.label ?? ''}
            value={dateValue}
            onChange={setValue}
            placeholder={element.placeholder ?? undefined}
            hint={element.hint ?? undefined}
            required={element.required ?? false}
            disabled={isDisabled}
            busy={isBusy}
            error={errors?.join(', ') ?? undefined}
            debounce={1000}
            mode={element.mode ?? TimeFieldComponentModelMode.Minute}
        />
    );
}
