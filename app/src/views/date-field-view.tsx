import {BaseViewProps} from './base-view';
import {DateFieldComponentModelMode, DateFieldElement} from '../models/elements/form/input/date-field-element';
import {DateFieldComponent} from '../components/date-field/date-field-component';
import {useMemo} from 'react';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';


export function DateFieldView(props: BaseViewProps<DateFieldElement, string>) {
    const {
        element,
        setValue,
        value,
        error,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;

    const {
        disabled
    } = element;

    const isDisabled = useMemo(() => {
        return disabled || isGloballyDisabled;
    }, [disabled, isGloballyDisabled]);

    const isBusy = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    return (
        <DateFieldComponent
            label={element.label ?? ''}
            autocomplete={element.autocomplete}
            value={value ?? undefined}
            error={error}
            onChange={setValue}
            disabled={isDisabled}
            required={element.required}
            hint={element.hint}
            mode={element.mode ?? DateFieldComponentModelMode.Day}
            busy={isBusy}
            debounce={1000}
        />
    );
}
