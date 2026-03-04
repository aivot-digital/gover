import {useMemo} from 'react';
import {BaseViewProps} from './base-view';
import {DataModelSelectFieldElement} from '../models/elements/form/input/data-model-select-field-element';
import {DataModelSelectFieldComponent} from '../components/data-model-select-field/data-model-select-field-component';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';

export function DataModelSelectFieldView(props: BaseViewProps<DataModelSelectFieldElement, string>) {
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
        <DataModelSelectFieldComponent
            label={element.label ?? ''}
            value={value ?? undefined}
            onChange={setValue}
            placeholder={element.placeholder ?? undefined}
            hint={element.hint ?? undefined}
            error={errors != null ? errors.join(' ') : undefined}
            required={element.required ?? undefined}
            disabled={isDisabled}
            readOnly={isBusy}
        />
    );
}
