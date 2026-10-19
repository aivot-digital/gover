import {useEffect, useMemo, useRef, useState} from 'react';
import {BaseViewProps} from './base-view';
import {ProcessDataKeyInputFieldElement} from '../models/elements/form/input/process-data-key-input-field-element';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {DataPathInput} from '../components/data-path-input/data-path-input';
import {type EndAction, type TextFieldComponentProps} from '../components/text-field/text-field-component-props';
import {type FormFieldLayoutProps} from '../components/form-field';
import Close from '@aivot/mui-material-symbols-400-n25-outlined/Close';
import DataObject from '@aivot/mui-material-symbols-400-n25-outlined/DataObject';
import {ProcessDataKeyPickerDialog} from '../components/process-data-key-picker-dialog/process-data-key-picker-dialog';
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

export type ProcessDataKeySuggestion = {
    id: string;
    label: string;
    subLabel?: string;
};

export interface ProcessDataKeyInputComponentProps extends FormFieldLayoutProps {
    endAction?: EndAction | EndAction[];
    value: string | null | undefined;
    onChange: (value: string | null) => void;
    onBlur?: (value: string | null) => void;
    label: string;
    hint?: string;
    required?: boolean;
    error?: string | string[];
    readOnly?: boolean;
    controlSx?: TextFieldComponentProps['controlSx'];
    normalizeInput?: (value: string) => string;
    validationRequested?: boolean;
    debounce?: number;
    disabled?: boolean;
    busy?: boolean;
    disableWildCards?: boolean;
    prefix?: string;
    scopeProcessDataKey?: string | null;
    suggestions?: ProcessDataKeySuggestion[];
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
        suggestions: providedSuggestions,
    } = props;

    const opec = useOptionalProcessNodeEditorContext();
    const [pickerOpen, setPickerOpen] = useState(false);
    const hasProcessDataKeyMetadata = providedSuggestions != null || opec?.incomingMetadata != null;
    const hasScopeProcessDataKey = !isStringNullOrEmpty(scopeProcessDataKey);
    const effectivePrefix = hasScopeProcessDataKey
        ? normalizeProcessDataKey(scopeProcessDataKey) + '[*].'
        : normalizeProcessDataKeyPrefix(prefix) ?? '';

    const suggestions = useMemo(() => {
        if (providedSuggestions != null) {
            return providedSuggestions;
        }
        if (!hasProcessDataKeyMetadata) {
            return [];
        }

        return createProcessDataKeySuggestions(
            opec?.incomingMetadata?.forwardedProcessDataKeys ?? [],
            {
                disableWildCards,
                prefix,
                scopeProcessDataKey,
            },
        );
    }, [disableWildCards, hasProcessDataKeyMetadata, opec, prefix, providedSuggestions, scopeProcessDataKey]);

    // Only a changed scope invalidates the selected path. Suggestions can be incomplete, for example when an
    // upstream node is broken, so a saved path that is merely missing from them must stay unchanged.
    const checkedScopeProcessDataKeyRef = useRef(scopeProcessDataKey);
    useEffect(() => {
        if (!hasProcessDataKeyMetadata) {
            return;
        }

        const scopeChanged = checkedScopeProcessDataKeyRef.current !== scopeProcessDataKey;
        checkedScopeProcessDataKeyRef.current = scopeProcessDataKey;
        if (!scopeChanged || !hasScopeProcessDataKey || value == null) {
            return;
        }

        if (!suggestions.some((suggestion) => suggestion.id === value)) {
            onChange(null);
        }
    }, [hasProcessDataKeyMetadata, hasScopeProcessDataKey, onChange, scopeProcessDataKey, suggestions, value]);

    // Scoped fields only accept the child keys suggested for their scope. Typing is blocked, but choosing from the
    // picker and clearing the field must stay possible.
    const readOnly = props.readOnly || (hasScopeProcessDataKey && hasProcessDataKeyMetadata);
    const interactionDisabled = Boolean(disabled || busy);
    const endActions = interactionDisabled ? undefined : [
        ...(!props.readOnly && !isStringNullOrEmpty(value) ? [{
            icon: <Close/>,
            tooltip: 'Vorgangsdatenpfad leeren',
            onClick: () => onChange(null),
        }] : []),
        ...(!props.readOnly && hasProcessDataKeyMetadata ? [{
            icon: <DataObject/>,
            tooltip: 'Vorgangsdatenpfad auswählen',
            onClick: () => setPickerOpen(true),
        }] : []),
        ...(Array.isArray(props.endAction) ? props.endAction : props.endAction == null ? [] : [props.endAction]),
    ];

    return <>
        <DataPathInput
            label={label}
            value={value}
            onChange={onChange}
            onBlur={onBlur}
            required={required}
            error={error}
            hint={hint}
            disabled={disabled}
            readonly={readOnly}
            controlSx={props.controlSx}
            busy={busy}
            prefix={`$.${effectivePrefix}`}
            endAction={endActions}
            margin={props.margin}
            ariaDescribedBy={props.ariaDescribedBy}
            labelAction={props.labelAction}
            id={props.id}
            ariaLabel={props.ariaLabel}
            externalAction={props.externalAction}
            showOptionalIndicator={props.showOptionalIndicator}
            sx={props.sx}
            normalizeInput={props.normalizeInput}
            validationRequested={props.validationRequested}
            debounce={props.debounce ?? 1000}
            allowWildcards={!disableWildCards}
        />

        <ProcessDataKeyPickerDialog
            open={pickerOpen && !interactionDisabled && !props.readOnly && hasProcessDataKeyMetadata}
            options={suggestions}
            value={value}
            displayPrefix={effectivePrefix}
            disableWildCards={disableWildCards}
            allowCustomPath={!readOnly}
            onClose={() => setPickerOpen(false)}
            onApply={(path) => {
                onChange(path);
                setPickerOpen(false);
            }}
        />
    </>;
}

interface CreateProcessDataKeySuggestionsOptions {
    disableWildCards: boolean;
    prefix?: string | null;
    scopeProcessDataKey?: string | null;
}

export function createProcessDataKeySuggestions(
    hints: ProcessNodeDefinitionMetadataForwardedProcessDataKey[],
    options: CreateProcessDataKeySuggestionsOptions,
): ProcessDataKeySuggestion[] {
    const suggestions: ProcessDataKeySuggestion[] = [];
    const seenIds = new Set<string>();

    for (const hint of hints) {
        const suggestion = createSuggestion(hint, options.scopeProcessDataKey, options.prefix);
        if (suggestion == null) {
            continue;
        }

        if (options.disableWildCards && suggestion.id.includes('*')) {
            continue;
        }

        if (seenIds.has(suggestion.id)) {
            continue;
        }

        seenIds.add(suggestion.id);
        suggestions.push(suggestion);
    }

    return suggestions;
}

function createSuggestion(
    hint: ProcessNodeDefinitionMetadataForwardedProcessDataKey,
    scopeProcessDataKey: string | null | undefined,
    prefix: string | null | undefined,
): ProcessDataKeySuggestion | null {
    const processDataKey = resolveSuggestionProcessDataKey(hint.processDataKey, scopeProcessDataKey, prefix);
    if (processDataKey == null) {
        return null;
    }

    return {
        id: processDataKey,
        label: hint.label || processDataKey,
        subLabel: [hint.subLabel, hint.origin.name]
            .filter((part): part is string => !isStringNullOrEmpty(part))
            .join(' · ') || undefined,
    };
}

function resolveSuggestionProcessDataKey(
    processDataKey: string,
    scopeProcessDataKey: string | null | undefined,
    prefix: string | null | undefined,
): string | null {
    if (isStringNullOrEmpty(scopeProcessDataKey)) {
        const normalizedPrefix = normalizeProcessDataKeyPrefix(prefix);
        if (normalizedPrefix == null) {
            return processDataKey;
        }

        return processDataKey.startsWith(normalizedPrefix)
            ? processDataKey.substring(normalizedPrefix.length)
            : null;
    }

    const scope = normalizeProcessDataKey(scopeProcessDataKey);
    const wildcardPrefix = `${scope}[*].`;
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
    return (processDataKey ?? '').trim().replace(/\[\*\]\.?$/, '');
}

function normalizeProcessDataKeyPrefix(prefix: string | null | undefined): string | null {
    if (isStringNullOrEmpty(prefix)) {
        return null;
    }

    const normalizedPrefix = prefix!.trim().replace(/^\$\./, '');
    return normalizedPrefix.endsWith('.') ? normalizedPrefix : normalizedPrefix + '.';
}
