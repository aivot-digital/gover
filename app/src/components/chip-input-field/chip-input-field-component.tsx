import {type ClipboardEvent, type CompositionEvent, useEffect, useMemo, useRef, useState} from 'react';
import {
    Autocomplete,
    Box,
    type SxProps,
    TextField,
    type TextFieldProps,
    type Theme,
} from '@mui/material';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showWarningSnackbar} from '../../slices/snackbar-slice';
import {pluralize} from '../../utils/humanization-utils';
import {
    FormField,
    type FormFieldControlContext,
    type FormFieldLayoutProps,
    getNativeInputAriaProps,
} from '../form-field';
import {FormFieldTokens} from '../../theming/form-field-tokens';
import {getDisabledFieldBackground} from '../../theming/field-state-colors';
import {DefaultChipInputPasteSeparators} from '../../data/chip-input-paste-separators';

export interface ChipInputFieldComponentProps extends FormFieldLayoutProps {
    label: string;
    value: string[] | null | undefined;
    onChange: (value: string[] | null) => void;
    placeholder?: string;
    hint?: string;
    error?: string;
    disabled?: boolean;
    busy?: boolean;
    required?: boolean;
    readOnly?: boolean;
    suggestions?: string[];
    getOptionLabel?: (option: string) => string;
    allowDuplicates?: boolean;
    minItems?: number;
    maxItems?: number;
    maxItemLength?: number;
    pasteSeparators?: string[] | null;
    size?: TextFieldProps['size'];
    controlSx?: SxProps<Theme>;
}

export function ChipInputFieldComponent(props: ChipInputFieldComponentProps) {
    const {
        label,
        value,
        onChange,
        placeholder,
        hint,
        error,
        disabled,
        busy,
        required,
        readOnly,
        suggestions,
        getOptionLabel,
        allowDuplicates,
        minItems,
        maxItems,
        maxItemLength,
        pasteSeparators,
        size = 'small',
    } = props;
    const [inputValue, setInputValue] = useState('');
    const inputValueRef = useRef('');
    const composingRef = useRef(false);
    const commitAfterCompositionRef = useRef(false);
    const highlightedOptionRef = useRef<string | null>(null);
    const [isFocused, setIsFocused] = useState(false);
    const [feedbackMessage, setFeedbackMessage] = useState<string>();
    const dispatch = useAppDispatch();
    const selectedValues = value ?? [];
    const isInteractionDisabled = Boolean(disabled || readOnly || busy);
    const effectiveMaxItems = maxItems != null && maxItems > 0 ? maxItems : undefined;
    const effectiveMinItems = minItems != null && minItems > 0 ? minItems : undefined;
    const hasReachedMaxItems = effectiveMaxItems != null && selectedValues.length >= effectiveMaxItems;
    const showMinItems = effectiveMinItems != null && selectedValues.length < effectiveMinItems;
    const showMaxItems = effectiveMaxItems != null && !showMinItems;
    const remainingItems = (effectiveMinItems ?? 0) - selectedValues.length;
    const minimumIndicator = selectedValues.length === 0
        ? `${required ? 'Mindestens' : 'Bei Eingabe mindestens'} ${effectiveMinItems} ${pluralize(effectiveMinItems ?? 0, 'Eintrag', 'Einträge')}`
        : `Noch mindestens ${remainingItems} ${pluralize(remainingItems, 'Eintrag', 'Einträge')}`;
    const validationError = effectiveMaxItems != null && selectedValues.length > effectiveMaxItems
        ? `Maximal ${effectiveMaxItems} Einträge erlaubt.`
        : minItems != null && minItems > 0 && selectedValues.length > 0 && selectedValues.length < minItems
            ? `Mindestens ${minItems} Einträge erforderlich.`
            : maxItemLength != null && maxItemLength > 0 && selectedValues.some(entry => entry.trim().length > maxItemLength)
                ? `Jeder Eintrag darf höchstens ${maxItemLength} Zeichen enthalten.`
                : allowDuplicates !== true && new Set(selectedValues.map(entry => entry.trim())).size !== selectedValues.length
                    ? 'Mehrfach vorhandene Einträge sind nicht erlaubt.'
                    : undefined;
    const effectiveError = error || validationError;
    const helperText = effectiveError || hint;
    const showCommitHint = (isFocused || inputValue.trim().length > 0) &&
        !hasReachedMaxItems && readOnly !== true && disabled !== true && busy !== true;

    const options = useMemo(() => {
        return Array.from(new Set((suggestions ?? []).map((entry) => entry.trim()).filter((entry) => entry.length > 0)));
    }, [suggestions]);

    useEffect(() => {
        if (feedbackMessage == null) {
            return;
        }

        const timeoutId = window.setTimeout(() => {
            setFeedbackMessage(undefined);
        }, 4000);

        return () => {
            window.clearTimeout(timeoutId);
        };
    }, [feedbackMessage]);

    const updateInputValue = (nextValue: string) => {
        // Blur can follow a selection before React renders the cleared draft.
        inputValueRef.current = nextValue;
        setInputValue(nextValue);
    };

    const addEntries = (entries: string[]): boolean => {
        if (isInteractionDisabled || composingRef.current) return false;
        const additions: string[] = [];
        const knownEntries = new Set(selectedValues.map(entry => entry.trim()));
        let hasDuplicates = false;
        for (const rawEntry of entries.map(entry => entry.trim()).filter(Boolean)) {
            // Typed and pasted labels must resolve to their values, just like selected suggestions.
            const entry = getOptionLabel != null && !options.includes(rawEntry)
                ? options.find(option => getOptionLabel(option) === rawEntry) ?? rawEntry
                : rawEntry;
            if (allowDuplicates !== true && knownEntries.has(entry)) {
                hasDuplicates = true;
            } else {
                additions.push(entry);
                knownEntries.add(entry);
            }
        }
        // Check only additions. Existing values must survive stricter rules and removals.
        // Reject an oversized paste as a whole instead of silently dropping its last entries.
        if (additions.length > 0 && effectiveMaxItems != null && selectedValues.length + additions.length > effectiveMaxItems) {
            setFeedbackMessage(`Die Einträge wurden nicht übernommen. Maximal ${effectiveMaxItems} Einträge möglich.`);
            return false;
        }
        if (additions.length > 0) onChange([...selectedValues, ...additions]);
        if (hasDuplicates) {
            dispatch(showWarningSnackbar('Bereits vorhandene Einträge wurden nicht erneut übernommen.'));
        }
        setFeedbackMessage(undefined);
        return true;
    };

    const commitDraft = () => {
        if (isInteractionDisabled || composingRef.current || inputValueRef.current.length === 0) return;
        if (addEntries([inputValueRef.current])) updateInputValue('');
    };

    const hasError = Boolean(effectiveError);
    const hasHelperText = helperText != null && helperText.length > 0;
    const hasHelperContent = hasHelperText || feedbackMessage != null || showMinItems || showMaxItems;
    const helperContent = hasHelperContent ? (
        <Box
            sx={{
                display: 'flex',
                alignItems: 'flex-start',
                justifyContent: 'space-between',
                flexWrap: {xs: 'wrap', sm: 'nowrap'},
                columnGap: 2,
                rowGap: 0.5,
                width: '100%',
            }}
        >
            <Box sx={{minWidth: 0}}>
                {hasHelperText && (
                    <Box component="span" sx={{display: 'block'}}>
                        {helperText}
                    </Box>
                )}

                {feedbackMessage != null && (
                    <Box
                        component="span"
                        role="status"
                        sx={{display: 'block', mt: hasHelperText ? 0.5 : 0}}
                    >
                        {feedbackMessage}
                    </Box>
                )}
            </Box>

            {showMinItems && (
                <Box component="span" sx={{ml: 'auto', maxWidth: '100%'}}>
                    {minimumIndicator}
                </Box>
            )}

            {showMaxItems && effectiveMaxItems != null && (
                <Box
                    component="span"
                    sx={{
                        whiteSpace: 'nowrap',
                        fontVariantNumeric: 'tabular-nums',
                        ml: 'auto',
                    }}
                >
                    <Box component="span" aria-hidden="true">
                        {`${selectedValues.length}/${effectiveMaxItems} ${pluralize(effectiveMaxItems, 'Eintrag', 'Einträge')}`}
                    </Box>
                    <Box
                        component="span"
                        className="visually-hidden"
                    >
                        {`${selectedValues.length} von ${effectiveMaxItems} ${pluralize(effectiveMaxItems, 'Eintrag', 'Einträgen')} verwendet`}
                    </Box>
                </Box>
            )}
        </Box>
    ) : undefined;

    return (
        <FormField
            id={props.id}
            label={label}
            ariaLabel={props.ariaLabel}
            ariaDescribedBy={props.ariaDescribedBy}
            externalAction={props.externalAction}
            labelAction={props.labelAction}
            hint={!hasError ? helperContent : undefined}
            error={hasError ? helperContent : undefined}
            required={required}
            disabled={disabled}
            readOnly={readOnly}
            busy={busy}
            margin={props.margin ?? 'normal'}
            showOptionalIndicator={props.showOptionalIndicator}
            sx={props.sx}
        >
            {(fieldContext: FormFieldControlContext) => (
                <Autocomplete
                    id={fieldContext.controlId}
                    multiple
                    freeSolo
                    fullWidth
                    readOnly={readOnly || busy}
                    disabled={disabled}
                    size={size}
                    options={options}
                    getOptionLabel={getOptionLabel}
                    getOptionKey={option => option}
                    value={selectedValues}
                    inputValue={inputValue}
                    filterSelectedOptions={allowDuplicates !== true}
                    onHighlightChange={(_, option) => {
                        highlightedOptionRef.current = option;
                    }}
                    onClose={() => {
                        highlightedOptionRef.current = null;
                    }}
                    onKeyDown={(event) => {
                        if (isInteractionDisabled || composingRef.current || event.nativeEvent.isComposing || event.keyCode === 229) {
                            event.defaultMuiPrevented = true;
                            return;
                        }
                        // Let MUI handle deliberate suggestion selection. Free text needs its
                        // own path because MUI does not create duplicate free-solo values.
                        if (event.key === 'Enter' && highlightedOptionRef.current == null && inputValueRef.current.trim()) {
                            event.preventDefault();
                            event.defaultMuiPrevented = true;
                            commitDraft();
                        }
                        // Tab keeps its native focus movement; blur commits the draft once.
                    }}
                    onInputChange={(_, updatedValue, reason) => {
                        if (!isInteractionDisabled && (reason === 'input' || reason === 'clear')) {
                            highlightedOptionRef.current = null;
                            setFeedbackMessage(undefined);
                            updateInputValue(updatedValue);
                        }
                    }}
                    onChange={(event, updatedValue, reason, details) => {
                        if (isInteractionDisabled || composingRef.current) return;
                        if ((reason === 'selectOption' || reason === 'createOption') && details != null) {
                            if (addEntries([details.option])) updateInputValue('');
                        } else if (reason === 'removeOption' || reason === 'clear') {
                            onChange(updatedValue.length > 0 ? updatedValue : null);
                            // A suggestion can toggle off an existing chip. Consume its search
                            // text as well so blur does not add the search fragment as a new chip.
                            const toggledSuggestion = event.currentTarget?.getAttribute('role') === 'option' ||
                                ('key' in event && event.key === 'Enter');
                            if (reason === 'clear' || toggledSuggestion) updateInputValue('');
                            setFeedbackMessage(undefined);
                        }
                    }}
                    renderInput={(params) => {
                        const nativeAriaProps = getNativeInputAriaProps(
                            fieldContext,
                            params.slotProps.htmlInput,
                        );

                        return (
                            <TextField
                                {...params}
                                id={fieldContext.controlId}
                                label={undefined}
                                placeholder={hasReachedMaxItems ? undefined : placeholder}
                                required={required && selectedValues.length === 0}
                                error={fieldContext.invalid}
                                fullWidth
                                margin="none"
                                onFocus={() => {
                                    setIsFocused(true);
                                    commitAfterCompositionRef.current = false;
                                }}
                                onBlur={(event) => {
                                    if (event.currentTarget.contains(event.relatedTarget)) return;
                                    setIsFocused(false);
                                    if (composingRef.current) {
                                        commitAfterCompositionRef.current = true;
                                    } else {
                                        commitDraft();
                                    }
                                }}
                                helperText={undefined}
                                size={size}
                                slotProps={{
                                    ...params.slotProps,
                                    input: {
                                        ...params.slotProps.input,
                                        endAdornment: (
                                            <>
                                                {showCommitHint && (
                                                    <Box
                                                        component="span"
                                                        sx={{
                                                            display: 'inline-flex',
                                                            alignItems: 'center',
                                                            gap: 0.75,
                                                            ml: 0.75,
                                                            my: 0.5,
                                                            color: 'text.secondary',
                                                            typography: 'caption',
                                                            whiteSpace: 'nowrap',
                                                            pointerEvents: 'none',
                                                        }}
                                                    >
                                                        <Box component="span">mit</Box>
                                                        <Box
                                                            component="kbd"
                                                            sx={{
                                                                display: 'inline-flex',
                                                                alignItems: 'center',
                                                                justifyContent: 'center',
                                                                px: 0.75,
                                                                py: 0.25,
                                                                border: 1,
                                                                borderColor: 'divider',
                                                                borderRadius: 0.75,
                                                                bgcolor: 'action.hover',
                                                                color: 'text.primary',
                                                                fontFamily: 'inherit',
                                                                fontSize: '0.75rem',
                                                                fontWeight: 500,
                                                                lineHeight: 1,
                                                                boxShadow: (theme) => `inset 0 -1px 0 ${theme.palette.divider}`,
                                                            }}
                                                        >
                                                            Enter
                                                        </Box>
                                                        <Box component="span">bestätigen</Box>
                                                    </Box>
                                                )}
                                                {params.slotProps.input.endAdornment}
                                            </>
                                        ),
                                    },
                                    htmlInput: {
                                        ...params.slotProps.htmlInput,
                                        ...nativeAriaProps,
                                        readOnly: readOnly === true || busy === true || (hasReachedMaxItems && inputValue.length === 0),
                                        onCompositionStart: () => { composingRef.current = true; },
                                        onCompositionEnd: (event: CompositionEvent<HTMLInputElement>) => {
                                            composingRef.current = false;
                                            if (!isInteractionDisabled) {
                                                updateInputValue(event.currentTarget.value);
                                                if (commitAfterCompositionRef.current) commitDraft();
                                            }
                                            commitAfterCompositionRef.current = false;
                                        },
                                        onPaste: (event: ClipboardEvent<HTMLInputElement>) => {
                                            if (isInteractionDisabled || composingRef.current) return;
                                            const pastedText = event.clipboardData.getData('text');
                                            const separators = (pasteSeparators ?? DefaultChipInputPasteSeparators).filter(Boolean);
                                            const split = (text: string) => separators.reduce(
                                                (parts, separator) => parts.flatMap(part => separator === '\n'
                                                    ? part.split(/\r\n|\r|\n/)
                                                    : part.split(separator)),
                                                [text],
                                            );
                                            if (split(pastedText).length < 2) return;
                                            event.preventDefault();
                                            const input = event.currentTarget;
                                            const start = input.selectionStart ?? inputValueRef.current.length;
                                            const end = input.selectionEnd ?? start;
                                            const combined = inputValueRef.current.slice(0, start) + pastedText + inputValueRef.current.slice(end);
                                            if (addEntries(split(combined))) updateInputValue('');
                                        },
                                    },
                                }}
                            />
                        );
                    }}
                    sx={[
                        {
                            cursor: busy ? 'not-allowed' : undefined,
                            '& .MuiInputBase-root': {
                                minHeight: FormFieldTokens.controlMinHeight,
                                backgroundColor: busy ? getDisabledFieldBackground : undefined,
                            },
                        },
                        ...(Array.isArray(props.controlSx) ? props.controlSx : [props.controlSx]),
                    ]}
                />
            )}
        </FormField>
    );
}
