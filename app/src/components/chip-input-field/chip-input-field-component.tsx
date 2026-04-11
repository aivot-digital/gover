import {KeyboardEvent, useEffect, useMemo, useState} from 'react';
import {Autocomplete, Box, TextField, type TextFieldProps} from '@mui/material';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showWarningSnackbar} from '../../slices/snackbar-slice';
import {pluralize} from '../../utils/humanization-utils';

interface ChipInputFieldComponentProps {
    label: string;
    value: string[] | null | undefined;
    onChange: (value: string[] | null | undefined) => void;
    placeholder?: string;
    hint?: string;
    error?: string;
    disabled?: boolean;
    required?: boolean;
    readOnly?: boolean;
    suggestions?: string[];
    allowDuplicates?: boolean;
    maxItems?: number;
    size?: TextFieldProps['size'];
}

interface NormalizedValuesResult {
    values: string[] | undefined;
    hasRejectedDuplicate: boolean;
    hasRejectedItem: boolean;
}

function normalizeValues(values: string[], allowDuplicates: boolean, maxItems?: number): NormalizedValuesResult {
    const normalizedValues: string[] = [];
    const knownEntries = new Set<string>();
    let hasRejectedDuplicate = false;
    let hasRejectedItem = false;

    for (const rawEntry of values) {
        const entry = rawEntry.trim();

        if (entry.length === 0) {
            continue;
        }

        if (!allowDuplicates && knownEntries.has(entry)) {
            hasRejectedDuplicate = true;
            hasRejectedItem = true;
            continue;
        }

        if (maxItems != null && normalizedValues.length >= maxItems) {
            hasRejectedItem = true;
            continue;
        }

        normalizedValues.push(entry);
        knownEntries.add(entry);
    }

    return {
        values: normalizedValues.length > 0 ? normalizedValues : undefined,
        hasRejectedDuplicate,
        hasRejectedItem,
    };
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
        required,
        readOnly,
        suggestions,
        allowDuplicates,
        maxItems,
        size = 'medium',
    } = props;
    const [inputValue, setInputValue] = useState('');
    const [isFocused, setIsFocused] = useState(false);
    const [feedbackMessage, setFeedbackMessage] = useState<string>();
    const dispatch = useAppDispatch();
    const selectedValues = value ?? [];
    const effectiveMaxItems = maxItems != null && maxItems > 0 ? maxItems : undefined;
    const hasReachedMaxItems = effectiveMaxItems != null && selectedValues.length >= effectiveMaxItems;
    const helperText = error ?? hint;
    const showCommitHint = (isFocused || inputValue.trim().length > 0) && !hasReachedMaxItems && readOnly !== true && disabled !== true;

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

    const applyUpdatedValue = (updatedValue: string[]) => {
        const normalized = normalizeValues(updatedValue, allowDuplicates === true, effectiveMaxItems);

        onChange(normalized.values);

        if (effectiveMaxItems != null && normalized.hasRejectedItem && !normalized.hasRejectedDuplicate) {
            setFeedbackMessage(`Maximal ${effectiveMaxItems} Einträge möglich.`);
            return;
        }

        if (normalized.hasRejectedDuplicate) {
            setFeedbackMessage(undefined);
            dispatch(showWarningSnackbar('Der Eintrag wurde nicht übernommen, weil er bereits vorhanden ist.'));
            return;
        }

        setFeedbackMessage(undefined);
    };

    return (
        <Autocomplete
            multiple
            freeSolo
            readOnly={readOnly}
            disabled={disabled}
            size={size}
            options={options}
            value={selectedValues}
            inputValue={inputValue}
            filterSelectedOptions={allowDuplicates !== true}
            onInputChange={(_, updatedValue, reason) => {
                if (reason === 'reset') {
                    setInputValue('');
                    return;
                }

                if (hasReachedMaxItems) {
                    setInputValue('');
                    return;
                }

                setFeedbackMessage(undefined);
                setInputValue(updatedValue);
            }}
            onChange={(_, updatedValue) => {
                applyUpdatedValue(updatedValue);
            }}
            renderInput={(params) => (
                <TextField
                    {...params}
                    label={label}
                    placeholder={hasReachedMaxItems ? undefined : placeholder}
                    required={required}
                    error={error != null}
                    onFocus={() => {
                        setIsFocused(true);
                    }}
                    onBlur={() => {
                        setIsFocused(false);
                    }}
                    helperText={
                        <Box
                            sx={{
                                display: 'flex',
                                alignItems: 'flex-start',
                                justifyContent: 'space-between',
                                gap: 2,
                                width: '100%',
                            }}
                        >
                            <Box
                                sx={{
                                    flex: 1,
                                    minWidth: 0,
                                }}
                            >
                                {
                                    helperText != null &&
                                    helperText.length > 0 &&
                                    <Box
                                        component="span"
                                        sx={{
                                            display: 'block',
                                            color: error != null ? 'error.main' : 'text.secondary',
                                        }}
                                    >
                                        {helperText}
                                    </Box>
                                }

                                {
                                    feedbackMessage != null &&
                                    error == null &&
                                    <Box
                                        component="span"
                                        sx={{
                                            display: 'block',
                                            mt: 0.5,
                                            color: 'text.secondary',
                                        }}
                                    >
                                        {feedbackMessage}
                                    </Box>
                                }
                            </Box>

                            {
                                effectiveMaxItems != null &&
                                <Box
                                    component="span"
                                    sx={{
                                        whiteSpace: 'nowrap',
                                        color: 'text.secondary',
                                        fontVariantNumeric: 'tabular-nums',
                                    }}
                                >
                                    {`${selectedValues.length}/${effectiveMaxItems} ${pluralize(effectiveMaxItems, 'Eintrag', 'Einträge')}`}
                                </Box>
                            }
                        </Box>
                    }
                    size={size}
                    InputLabelProps={{
                        title: label,
                    }}
                    InputProps={{
                        ...params.InputProps,
                        endAdornment: (
                            <>
                                {
                                    showCommitHint &&
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
                                }
                                {params.InputProps.endAdornment}
                            </>
                        ),
                    }}
                    inputProps={{
                        ...params.inputProps,
                        readOnly: readOnly === true || hasReachedMaxItems,
                    }}
                    onKeyDown={(event: KeyboardEvent<HTMLInputElement>) => {
                        (params.inputProps as any).onKeyDown?.(event);

                        if (event.defaultPrevented || (event.key !== 'Enter' && event.key !== 'Tab')) {
                            return;
                        }

                        const normalizedInput = inputValue.trim();
                        if (normalizedInput.length === 0) {
                            return;
                        }

                        if (effectiveMaxItems != null && selectedValues.length >= effectiveMaxItems) {
                            setFeedbackMessage(`Maximal ${effectiveMaxItems} Einträge möglich.`);
                            setInputValue('');
                            return;
                        }

                        event.preventDefault();
                        applyUpdatedValue([...selectedValues, normalizedInput]);
                        setInputValue('');
                    }}
                />
            )}
        />
    );
}
