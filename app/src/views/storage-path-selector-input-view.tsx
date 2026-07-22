import {useMemo} from 'react';
import {type BaseViewProps} from './base-view';
import {
    type StoragePathSelectorInputElement,
    type StoragePathSelectorInputElementValue,
} from '../models/elements/form/input/storage-path-selector-input-element';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {
    StoragePathSelectorInputComponent,
} from '../components/storage-path-selector-input/storage-path-selector-input-component';

export function StoragePathSelectorInputView(
    props: BaseViewProps<StoragePathSelectorInputElement, StoragePathSelectorInputElementValue>,
) {
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
        <StoragePathSelectorInputComponent
            label={element.label ?? ''}
            value={value}
            onChange={setValue}
            allowedStorageProviderTypes={element.allowedStorageProviderTypes}
            allowReadOnlyStorageProviders={element.allowReadOnlyStorageProviders === true}
            storageProviderSelectHint={element.storageProviderSelectHint}
            placeholder={element.placeholder}
            hint={element.hint ?? undefined}
            error={errors != null ? errors.join(' ') : undefined}
            required={element.required ?? undefined}
            disabled={isDisabled}
            readOnly={isBusy}
        />
    );
}
