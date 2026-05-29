import {useMemo} from 'react';
import {BaseViewProps} from './base-view';
import {ProcessDataKeyInputFieldElement} from '../models/elements/form/input/process-data-key-input-field-element';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {AutocompleteTextField} from '../components/text-field/text-field-component';
import {
    useOptionalProcessNodeEditorContext,
} from '../modules/process/pages/details/components/process-node-editor/process-node-editor-context';
import {isStringNullOrEmpty} from '../utils/string-utils';

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
    } = props;

    const isDisabled = useMemo(() => {
        return element.disabled || isGloballyDisabled;
    }, [element.disabled, isGloballyDisabled]);

    const isBusy = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    return (
        <ProcessDataKeyInputComponent
            label={element.label ?? ''}
            value={value ?? undefined}
            onChange={(newValue) => {
                if (!isBusy) {
                    setValue(isStringNullOrEmpty(newValue) ? undefined : newValue ?? undefined);
                }
            }}
            onBlur={onBlur == null ? undefined : (val) => onBlur(val, [element.id])}
            required={element.required ?? undefined}
            error={errors != null ? errors.join(' ') : undefined}
            hint={element.hint ?? undefined}
            disabled={isDisabled}
            busy={isBusy}
            disableWildCards={element.disableWildCards ?? undefined}
        />
    );
}

interface ProcessDataKeyInputComponentProps {
    value: string | null | undefined;
    onChange: (value: string | undefined) => void;
    onBlur?: (value: string | undefined) => void;
    label: string;
    hint?: string;
    required?: boolean;
    error?: string;
    disabled?: boolean;
    busy?: boolean;
    disableWildCards?: boolean;
    prefix?: string;
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
        disableWildCards = false,
        prefix,
    } = props;

    const opec = useOptionalProcessNodeEditorContext();

    const suggestions = useMemo(() => {
        if (opec == null || opec.processDataKeyHints == null) {
            return [];
        }

        return opec.processDataKeyHints
            .filter((hint) => hint.type === 'ProcessData' && (!disableWildCards || !hint.key.includes('*')))
            .map((hint) => ({
                id: hint.key,
                label: hint.key,
                subLabel: hint.node.name ?? undefined,
            }));
    }, [opec]);

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
            busy={busy}
            startIcon={`$.${prefix ?? ''}`}
            debounce={1000}
            pattern={disableWildCards ? processDataKeyPattern : processDataKeyPatternWithWildcard}
            suggestions={suggestions}
        />
    );
}