import {useMemo} from 'react';
import {type BaseViewProps} from './base-view';
import {type ChipInputFieldElement} from '../models/elements/form/input/chip-input-field-element';
import {ChipInputFieldComponent} from '../components/chip-input-field/chip-input-field-component';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';

export function ChipInputFieldView(props: BaseViewProps<ChipInputFieldElement, string[]>) {
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
        <ChipInputFieldComponent
            label={element.label ?? ''}
            value={value ?? undefined}
            onChange={setValue}
            placeholder={element.placeholder ?? undefined}
            hint={element.hint ?? undefined}
            error={errors != null ? errors.join(' ') : undefined}
            required={element.required ?? undefined}
            disabled={isDisabled}
            readOnly={isBusy}
            suggestions={element.suggestions ?? undefined}
            allowDuplicates={element.allowDuplicates ?? undefined}
            maxItems={element.maxItems ?? undefined}
        />
    );
}
