import {type KeyboardEvent, useEffect, useMemo, useState} from 'react';
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
    allowDuplicates?: boolean;
    maxItems?: number;
    size?: TextFieldProps['size'];
    controlSx?: SxProps<Theme>;
}

interface NormalizedValuesResult {
    values: string[] | null;
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
        values: normalizedValues.length > 0 ? normalizedValues : null,
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
        busy,
        required,
        readOnly,
        suggestions,
        allowDuplicates,
        maxItems,
        size = 'small',
    } = props;
    const [inputValue, setInputValue] = useState('');
    const [isFocused, setIsFocused] = useState(false);
    const [feedbackMessage, setFeedbackMessage] = useState<string>();
    const dispatch = useAppDispatch();
    const selectedValues = value ?? [];
    const effectiveMaxItems = maxItems != null && maxItems > 0 ? maxItems : undefined;
    const hasReachedMaxItems = effectiveMaxItems != null && selectedValues.length >= effectiveMaxItems;
    const helperText = error ?? hint;
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

    const hasError = error != null && error.length > 0;
    const hasHelperText = helperText != null && helperText.length > 0;
    const hasHelperContent = hasHelperText || (feedbackMessage != null && !hasError) || effectiveMaxItems != null;
    const helperContent = hasHelperContent ? (
        <Box
            sx={{
                display: 'flex',
                alignItems: 'flex-start',
                justifyContent: 'space-between',
                gap: 2,
                width: '100%',
            }}
        >
            <Box sx={{flex: 1, minWidth: 0}}>
                {hasHelperText && (
                    <Box component="span" sx={{display: 'block'}}>
                        {helperText}
                    </Box>
                )}

                {feedbackMessage != null && !hasError && (
                    <Box
                        component="span"
                        role="status"
                        sx={{display: 'block', mt: hasHelperText ? 0.5 : 0}}
                    >
                        {feedbackMessage}
                    </Box>
                )}
            </Box>

            {effectiveMaxItems != null && (
                <Box
                    component="span"
                    sx={{
                        whiteSpace: 'nowrap',
                        fontVariantNumeric: 'tabular-nums',
                    }}
                >
                    <Box component="span" aria-hidden="true">
                        {`${selectedValues.length}/${effectiveMaxItems} ${pluralize(effectiveMaxItems, 'Eintrag', 'Einträge')}`}
                    </Box>
                    <Box
                        component="span"
                        sx={{
                            position: 'absolute',
                            width: 1,
                            height: 1,
                            p: 0,
                            m: -1,
                            overflow: 'hidden',
                            clip: 'rect(0 0 0 0)',
                            whiteSpace: 'nowrap',
                            border: 0,
                        }}
                    >
                        {`${selectedValues.length} von ${effectiveMaxItems} Einträgen verwendet`}
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
                        if (!busy) {
                            applyUpdatedValue(updatedValue);
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
                                required={required}
                                error={fieldContext.invalid}
                                fullWidth
                                margin="none"
                                onFocus={() => {
                                    setIsFocused(true);
                                }}
                                onBlur={() => {
                                    setIsFocused(false);
                                }}
                                helperText={undefined}
                                size={size}
                                onKeyDown={(event: KeyboardEvent<HTMLInputElement>) => {
                                    (params.slotProps.htmlInput as any).onKeyDown?.(event);

                                    if (event.defaultPrevented || busy ||
                                        (event.key !== 'Enter' && event.key !== 'Tab')) {
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
                                        readOnly: readOnly === true || busy === true || hasReachedMaxItems,
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
