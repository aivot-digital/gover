import {useMemo} from 'react';
import {BaseViewProps} from './base-view';
import {DataObjectSelectFieldElement} from '../models/elements/form/input/data-object-select-field-element';
import {DataObjectSelectFieldComponent} from '../components/data-object-select-field/data-object-select-field-component';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';

export function DataObjectSelectFieldView(props: BaseViewProps<DataObjectSelectFieldElement, string>) {
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
        <DataObjectSelectFieldComponent
            label={element.label ?? ''}
            value={value ?? undefined}
            onChange={setValue}
            dataModelKey={element.dataModelKey ?? undefined}
            dataLabelAttributeKey={element.dataLabelAttributeKey ?? undefined}
            placeholder={element.placeholder ?? undefined}
            hint={element.hint ?? undefined}
            error={errors != null ? errors.join(' ') : undefined}
            required={element.required ?? undefined}
            disabled={isDisabled}
            readOnly={isBusy}
        />
    );
}
