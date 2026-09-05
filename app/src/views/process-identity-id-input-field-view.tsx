import {useMemo} from 'react';
import {BaseViewProps} from './base-view';
import {
    useOptionalProcessNodeEditorContext,
} from '../modules/process/pages/details/components/process-node-editor/process-node-editor-context';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {type SelectFieldComponentOption} from '../components/select-field/select-field-component-option';
import {type ProcessIdentityIdInputElement} from '../models/elements/form/input/process-identity-id-input-element';
import {
    type ProcessNodeDefinitionMetadataForwardedIdentity,
} from '../modules/process/entities/process-node-definition-metadata';

export function ProcessIdentityIdInputFieldView(props: BaseViewProps<ProcessIdentityIdInputElement, string>) {
    const {
        element,
        setValue,
        value,
        errors,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;

    const processNodeEditorContext = useOptionalProcessNodeEditorContext();
    const incomingMetadata = processNodeEditorContext?.incomingMetadata;
    const selectedIdentityId = normalizeIdentityId(value);

    const {
        options,
        forwardedIdentityIds,
    } = useMemo(() => {
        const optionsByIdentityId = new Map<string, SelectFieldComponentOption>();

        for (const identity of incomingMetadata?.forwardedIdentities ?? []) {
            const identityId = normalizeIdentityId(identity.identityId);
            if (identityId == null || optionsByIdentityId.has(identityId)) {
                continue;
            }

            optionsByIdentityId.set(identityId, createIdentityOption(identity, identityId));
        }

        const forwardedIdentityIds = new Set(optionsByIdentityId.keys());

        if (selectedIdentityId != null && !optionsByIdentityId.has(selectedIdentityId)) {
            optionsByIdentityId.set(selectedIdentityId, {
                label: selectedIdentityId,
                subLabel: incomingMetadata == null ? undefined : 'Nicht mehr verfügbar',
                value: selectedIdentityId,
            });
        }

        return {
            options: Array.from(optionsByIdentityId.values()),
            forwardedIdentityIds,
        };
    }, [incomingMetadata, selectedIdentityId]);

    const selectedIdentityIsUnavailable = (
        incomingMetadata != null &&
        selectedIdentityId != null &&
        !forwardedIdentityIds.has(selectedIdentityId)
    );

    const error = useMemo(() => {
        const messages = new Set(
            (errors ?? [])
                .map((message) => message.trim())
                .filter((message) => message.length > 0),
        );

        if (selectedIdentityIsUnavailable && selectedIdentityId != null) {
            const unavailableError = `Die ausgewählte Prozessidentität „${selectedIdentityId}“ ist nicht mehr verfügbar.`;
            if (!Array.from(messages).some((message) => message.includes(unavailableError))) {
                messages.add(unavailableError);
            }
        }

        return messages.size > 0 ? Array.from(messages).join(' ') : undefined;
    }, [errors, selectedIdentityId, selectedIdentityIsUnavailable]);

    const isDisabled = useMemo(() => {
        return element.disabled || isGloballyDisabled;
    }, [element.disabled, isGloballyDisabled]);

    const isBusy = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    return (
        <SelectFieldComponent
            label={element.label ?? ''}
            value={selectedIdentityId}
            onChange={setValue}
            placeholder={element.placeholder ?? undefined}
            hint={element.hint ?? undefined}
            error={error}
            required={element.required ?? undefined}
            disabled={isDisabled}
            readOnly={isBusy}
            options={options}
            emptyStatePlaceholder={incomingMetadata == null && processNodeEditorContext != null
                ? 'Prozessidentitäten werden geladen'
                : 'Keine Prozessidentitäten verfügbar'}
        />
    );
}

function normalizeIdentityId(value: string | null | undefined): string | null {
    const normalizedValue = value?.trim();
    return normalizedValue == null || normalizedValue.length === 0 ? null : normalizedValue;
}

function createIdentityOption(
    identity: ProcessNodeDefinitionMetadataForwardedIdentity,
    identityId: string,
): SelectFieldComponentOption {
    const normalizedLabel = identity.label.trim();
    const subLabelParts = [
        identityId,
        identity.subLabel?.trim(),
        identity.origin.name?.trim(),
    ].filter((part): part is string => part != null && part.length > 0);

    return {
        label: normalizedLabel.length > 0 ? normalizedLabel : identityId,
        subLabel: subLabelParts.join(' - '),
        value: identityId,
    };
}
