import {useMemo} from 'react';
import {BaseViewProps} from './base-view';
import {
    useOptionalProcessNodeEditorContext,
} from '../modules/process/pages/details/components/process-node-editor/process-node-editor-context';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {ChipInputFieldComponent} from '../components/chip-input-field/chip-input-field-component';
import {ProcessIdentityIdInputElement} from '../models/elements/form/input/process-identity-id-input-element';

export function ProcessIdentityIdInputFieldView(props: BaseViewProps<ProcessIdentityIdInputElement, string[]>) {
    const {
        element,
        setValue,
        value,
        errors,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;

    const opec = useOptionalProcessNodeEditorContext();

    const suggestions = useMemo(() => {
        if (opec == null || opec.incomingMetadata == null) {
            return [];
        }

        return opec
            .incomingMetadata
            .forwardedIdentities
            .map((att) => att.identityId);
    }, [opec, element]);

    const isDisabled = useMemo(() => {
        return element.disabled || isGloballyDisabled;
    }, [element.disabled, isGloballyDisabled]);

    const isBusy = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    return (
        <ChipInputFieldComponent
            label={element.label ?? ''}
            value={value}
            onChange={setValue}
            placeholder={element.placeholder ?? undefined}
            hint={element.hint ?? undefined}
            error={errors != null ? errors.join(' ') : undefined}
            required={element.required ?? undefined}
            disabled={isDisabled}
            readOnly={isBusy}
            suggestions={suggestions}
            allowDuplicates={false}
            maxItems={element.maxItems ?? undefined}
        />
    );
}
