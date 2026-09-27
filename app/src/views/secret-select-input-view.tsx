import {useMemo} from 'react';
import {type BaseViewProps} from './base-view';
import {type SecretSelectInputElement} from '../models/elements/form/input/secret-select-input-element';
import {SecretSelectComponent} from '../modules/secrets/components/secret-select-component';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';

export function SecretSelectInputView(props: BaseViewProps<SecretSelectInputElement, string>) {
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
        <SecretSelectComponent
            label={element.label ?? ''}
            value={value}
            onChange={setValue}
            placeholder={element.placeholder ?? undefined}
            hint={element.hint ?? undefined}
            error={errors != null ? errors.join(' ') : undefined}
            required={element.required ?? undefined}
            disabled={isDisabled}
            readOnly={isBusy}
            busy={isBusy}
        />
    );
}
