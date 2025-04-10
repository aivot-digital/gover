import {TimeFieldElement} from '../../models/elements/form/input/time-field-element';
import {useCallback, useMemo} from 'react';
import {BaseViewProps} from "../../views/base-view";
import {hasDerivableAspects} from '../../utils/has-derivable-aspects';
import {TimeFieldComponent} from "./time-field-component";

export function TimeFieldComponentView(props: BaseViewProps<TimeFieldElement, string>) {
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

    const dateValue = useMemo(() => {
        if (value == null) return undefined;
        const date = new Date(value);
        return isNaN(date.getTime()) ? undefined : value;
    }, [value]);

    const handleChange = useCallback((changedValue: string | undefined) => {
        setValue(changedValue);
    }, [setValue]);

    return (
        <TimeFieldComponent
            label={element.label ?? ''}
            value={dateValue}
            onChange={handleChange}
            autocomplete="off"
            hint={element.hint}
            required={element.required}
            disabled={isDisabled}
            busy={isBusy}
            error={error}
            debounce={1000}
        />
    );
}
