import {useMemo} from 'react';
import {type BaseViewProps} from './base-view';
import {
    useOptionalProcessNodeEditorContext,
} from '../modules/process/pages/details/components/process-node-editor/process-node-editor-context';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {type ProcessIdentitySelectElement} from '../models/elements/form/input/process-identity-select-element';
import {ProcessIdentitySelect} from '../components/process-identity-select/process-identity-select';

export function ProcessIdentitySelectFieldView(props: BaseViewProps<ProcessIdentitySelectElement, string[]>) {
    const {
        element,
        setValue,
        value,
        errors,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;

    const processNodeEditorContext = useOptionalProcessNodeEditorContext();

    const isDisabled = useMemo(() => {
        return element.disabled || isGloballyDisabled;
    }, [element.disabled, isGloballyDisabled]);

    const isBusy = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    return (
        <ProcessIdentitySelect
            identities={processNodeEditorContext?.incomingMetadata?.forwardedIdentities}
            value={value}
            onChange={setValue}
            label={element.label ?? ''}
            placeholder={element.placeholder}
            hint={element.hint}
            errors={errors}
            required={element.required}
            disabled={isDisabled}
            readOnly={isBusy}
            maxItems={element.maxItems}
        />
    );
}
