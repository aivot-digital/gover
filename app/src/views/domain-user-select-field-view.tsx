import {useMemo} from 'react';
import {BaseViewProps} from './base-view';
import {
    DomainAndUserSelectItem,
    DomainUserSelectFieldElement,
} from '../models/elements/form/input/domain-user-select-field-element';
import {DomainUserSelectFieldComponent} from '../components/domain-user-select-field/domain-user-select-field-component';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';

export function DomainUserSelectFieldView(props: BaseViewProps<DomainUserSelectFieldElement, DomainAndUserSelectItem[]>) {
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
        <DomainUserSelectFieldComponent
            label={element.label ?? ''}
            value={value ?? undefined}
            onChange={setValue}
            placeholder={element.placeholder ?? undefined}
            hint={element.hint ?? undefined}
            error={errors != null ? errors.join(' ') : undefined}
            required={element.required ?? undefined}
            disabled={isDisabled}
            readOnly={isBusy}
            allowedTypes={element.allowedTypes ?? undefined}
            processAccessConstraint={element.processAccessConstraint ?? undefined}
        />
    );
}
