import {useMemo} from 'react';
import {BaseViewProps} from './base-view';
import {NumberFieldElement} from '../models/elements/form/input/number-field-element';
import {NumberFieldComponent} from '../components/number-field/number-field-component';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';

export function NumberFieldView(props: BaseViewProps<NumberFieldElement, number>) {
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
        <NumberFieldComponent
            label={element.label ?? ''}
            value={value ?? undefined}
            onChange={setValue}
            placeholder={element.placeholder}
            decimalPlaces={element.decimalPlaces}
            hint={element.hint}
            error={error}
            suffix={element.suffix}
            required={element.required}
            disabled={isDisabled}
            debounce={1000}

            sx={{
                backgroundColor: isBusy ? "#F8F8F8" : undefined,
                cursor: isBusy ? "not-allowed" : undefined,
            }}
            readOnly={isBusy}
        />
    );
}