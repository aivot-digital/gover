import {useEffect, useMemo} from 'react';
import {BaseViewProps} from './base-view';
import {ProcessDataKeyInputFieldElement} from '../models/elements/form/input/process-data-key-input-field-element';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {AutocompleteTextField} from '../components/text-field/text-field-component';
import {
    useOptionalProcessNodeEditorContext,
} from '../modules/process/pages/details/components/process-node-editor/process-node-editor-context';
import {useViewDispatcherContext} from '../components/view-dispatcher/view-dispatcher.context';
import {ElementType} from '../data/element-type/element-type';
import {resolveValue} from '../utils/element-data-utils';
import {isStringNullOrEmpty} from '../utils/string-utils';
import {
    type ProcessNodeDefinitionMetadataForwardedProcessDataKey,
} from '../modules/process/entities/process-node-definition-metadata';

const processDataKeyPatternWithWildcard = {
    regex: '^[a-zA-Z0-9.\\*_]+$',
    message: 'Der Datenschlüssel darf nur Buchstaben (a-z und A-Z), Zahlen, Punkte, Unterstriche und Sternchen enthalten.',
};

const processDataKeyPattern = {
    regex: '^[a-zA-Z0-9._]+$',
    message: 'Der Datenschlüssel darf nur Buchstaben (a-z und A-Z), Zahlen, Punkte und Unterstriche enthalten.',
};

export function ProcessDataKeyInputFieldView(props: BaseViewProps<ProcessDataKeyInputFieldElement, string>) {
    const {
        element,
        setValue,
        value,
        errors,
        isBusy: isGloballyDisabled,
        isDeriving,
        onBlur,
        authoredElementValues,
        derivedData,
    } = props;

    const {
        allElements,
        rootAuthoredElementValues,
        rootDerivedData,
    } = useViewDispatcherContext();

    const scopeElement = useMemo(() => {
        if (isStringNullOrEmpty(element.scopeProcessDataKeyInputElementId)) {
            return undefined;
        }

        return allElements.find((candidate) => (
            candidate.id === element.scopeProcessDataKeyInputElementId &&
            candidate.type === ElementType.ProcessDataKeyInput
        ));
    }, [allElements, element.scopeProcessDataKeyInputElementId]);

    const scopeProcessDataKey = useMemo(() => {
        if (scopeElement == null) {
            return undefined;
        }

        const localValue = resolveValue(scopeElement, authoredElementValues, derivedData);
        if (localValue != null) {
            return `${localValue}`;
        }

        const rootValue = resolveValue(scopeElement, rootAuthoredElementValues, rootDerivedData);
        if (rootValue != null) {
            return `${rootValue}`;
        }

        return undefined;
    }, [authoredElementValues, derivedData, rootAuthoredElementValues, rootDerivedData, scopeElement]);

    const isDisabled = useMemo(() => {
        return element.disabled || isGloballyDisabled;
    }, [element.disabled, isGloballyDisabled]);

    const isBusy = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    return (
        <ProcessDataKeyInputComponent
            label={element.label ?? ''}
            value={value}
            onChange={(newValue) => {
                if (!isBusy) {
                    setValue(newValue);
                }
            }}
            onBlur={onBlur == null ? undefined : (val) => onBlur(val, [element.id])}
            required={element.required ?? undefined}
            error={errors != null ? errors.join(' ') : undefined}
            hint={element.hint ?? undefined}
            disabled={isDisabled}
            busy={isBusy}
            disableWildCards={element.disableWildCards ?? undefined}
            scopeProcessDataKey={scopeProcessDataKey}
        />
    );
}

type ProcessDataKeySuggestion = {
    id: string;
    label: string;
    subLabel?: string;
};

interface ProcessDataKeyInputComponentProps {
    value: string | null | undefined;
    onChange: (value: string | null) => void;
    onBlur?: (value: string | null) => void;
    label: string;
    hint?: string;
    required?: boolean;
    error?: string;
    disabled?: boolean;
    busy?: boolean;
    disableWildCards?: boolean;
    prefix?: string;
    scopeProcessDataKey?: string | null;
}

export function ProcessDataKeyInputComponent(props: ProcessDataKeyInputComponentProps) {
    const {
        value,
        onChange,
        onBlur,
        hint,
        disabled,
        label,
        busy,
        required,
        error,
        disableWildCards = true,
        prefix,
        scopeProcessDataKey,
    } = props;

    const opec = useOptionalProcessNodeEditorContext();
    const hasProcessDataKeyMetadata = opec?.incomingMetadata != null;
    const hasScopeProcessDataKey = !isStringNullOrEmpty(scopeProcessDataKey);
    const effectivePrefix = hasScopeProcessDataKey
        ? normalizeProcessDataKey(scopeProcessDataKey) + '.*.'
        : prefix;

    const suggestions = useMemo(() => {
        if (!hasProcessDataKeyMetadata) {
            return [];
        }

        return (opec?.incomingMetadata?.forwardedProcessDataKeys ?? [])
            .map((hint) => createSuggestion(hint, scopeProcessDataKey))
            .filter((suggestion): suggestion is ProcessDataKeySuggestion => suggestion != null)
            .filter((suggestion) => !disableWildCards || !suggestion.id.includes('*'));
    }, [disableWildCards, hasProcessDataKeyMetadata, opec, scopeProcessDataKey]);

    useEffect(() => {
        if (!hasScopeProcessDataKey || !hasProcessDataKeyMetadata || value == null) {
            return;
        }

        if (!suggestions.some((suggestion) => suggestion.id === value)) {
            onChange(null);
        }
    }, [hasProcessDataKeyMetadata, hasScopeProcessDataKey, onChange, suggestions, value]);

    return (
        <AutocompleteTextField
            label={label}
            value={value}
            onChange={onChange}
            onBlur={onBlur}
            required={required}
            error={error}
            hint={hint}
            disabled={disabled}
            readonly={hasScopeProcessDataKey && hasProcessDataKeyMetadata}
            busy={busy}
            startIcon={`$.${effectivePrefix ?? ''}`}
            debounce={1000}
            pattern={disableWildCards ? processDataKeyPattern : processDataKeyPatternWithWildcard}
            suggestions={suggestions}
        />
    );
}

function createSuggestion(
    hint: ProcessNodeDefinitionMetadataForwardedProcessDataKey,
    scopeProcessDataKey: string | null | undefined,
): ProcessDataKeySuggestion | null {
    const processDataKey = resolveSuggestionProcessDataKey(hint.processDataKey, scopeProcessDataKey);
    if (processDataKey == null) {
        return null;
    }

    return {
        id: processDataKey,
        label: processDataKey,
        subLabel: hint.subLabel ?? hint.label ?? hint.origin.name ?? undefined,
    };
}

function resolveSuggestionProcessDataKey(
    processDataKey: string,
    scopeProcessDataKey: string | null | undefined,
): string | null {
    if (isStringNullOrEmpty(scopeProcessDataKey)) {
        return processDataKey;
    }

    const scope = normalizeProcessDataKey(scopeProcessDataKey);
    const wildcardPrefix = `${scope}.*.`;
    if (processDataKey.startsWith(wildcardPrefix)) {
        return processDataKey.substring(wildcardPrefix.length);
    }

    const directPrefix = `${scope}.`;
    if (processDataKey.startsWith(directPrefix)) {
        return processDataKey.substring(directPrefix.length);
    }

    return null;
}

function normalizeProcessDataKey(processDataKey: string | null | undefined): string {
    return (processDataKey ?? '').trim().replace(/\.\*$/, '');
}
