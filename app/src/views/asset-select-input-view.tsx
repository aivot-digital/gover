import {useMemo} from 'react';
import {type BaseViewProps} from './base-view';
import {type AssetSelectInputElement} from '../models/elements/form/input/asset-select-input-element';
import {AssetSelector} from '../modules/assets/components/asset-selector';
import {AssetVisibility} from '../modules/assets/models/asset-visibility';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';

export function AssetSelectInputView(props: BaseViewProps<AssetSelectInputElement, string>) {
    const {
        element,
        setValue,
        value,
        errors,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;

    const isDisabled = useMemo(
        () => element.disabled || isGloballyDisabled,
        [element.disabled, isGloballyDisabled],
    );
    const isBusy = useMemo(
        () => isDeriving && hasDerivableAspects(element),
        [isDeriving, element],
    );
    const label = element.label ?? '';

    return (
        <AssetSelector
            label={label}
            selectLabel={element.dialogTitle ?? `${label || 'Datei'} auswählen`}
            value={value ?? null}
            onChange={setValue}
            placeholder={element.placeholder ?? undefined}
            mimetype={element.allowedMimeTypes ?? undefined}
            visibility={element.assetVisibility ?? AssetVisibility.All}
            hint={element.hint ?? undefined}
            error={errors != null ? errors.join(' ') : undefined}
            required={element.required ?? undefined}
            disabled={isDisabled}
            readOnly={isBusy}
            isBusy={isBusy}
        />
    );
}
