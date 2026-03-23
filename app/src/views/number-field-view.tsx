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
        <NumberFieldComponent
            label={element.label ?? ''}
            value={value ?? undefined}
            onChange={setValue}
            placeholder={element.placeholder ?? undefined}
            decimalPlaces={element.decimalPlaces ?? undefined}
            hint={element.hint ?? undefined}
            error={errors != null ? errors.join(' ') : undefined}
            suffix={element.suffix ?? undefined}
            required={element.required ?? undefined}
            disabled={isDisabled}
            debounce={1000}
            sx={{
                backgroundColor: isBusy ? '#F8F8F8' : undefined,
                cursor: isBusy ? 'not-allowed' : undefined,
            }}
            readOnly={isBusy}
        />
    );
}