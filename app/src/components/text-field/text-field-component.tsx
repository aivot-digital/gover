import React, {useEffect, useMemo, useRef, useState} from 'react';
import {Box, IconButton, InputAdornment, TextField, Typography} from '@mui/material';
import {type TextFieldComponentProps} from './text-field-component-props';
import Tooltip from '@mui/material/Tooltip';

// Utility function for number-to-word conversion
function getCharacterCount(count: number): string {
    const words: { [key: number]: string } = {
        1: 'ein', 2: 'zwei', 3: 'drei', 4: 'vier', 5: 'fünf', 6: 'sechs', 7: 'sieben',
        8: 'acht', 9: 'neun', 10: 'zehn', 11: 'elf', 12: 'zwölf',
    };
    return words[count] || count.toFixed(0);
}

/**
 * Clean the value for passing it to the parent.
 * Empty strings should be converted to undefined.
 * Leading and trailing whitespace should be removed if the corresponding flag is set.
 *
 * @param originalValue The original value to be cleaned.
 * @param flag The flag to determine if trailing whitespace should be kept or dropped. This is used during the debounce process to keep trailing whitespaces when debounce is triggered and the user is still typing.
 */
function cleanValue(originalValue: string | undefined, flag: 'keepTrailingWhitespace' | 'dropTrailingWhitespace'): string | undefined {
    // If the value is undefined, return undefined
    if (originalValue == null) {
        return undefined;
    }

    let cleanedValue = originalValue;

    // Remove all leading and trailing whitespace if the flag for dropping trailing whitespace is set
    if (flag === 'dropTrailingWhitespace') {
        // Remove trailing whitespace
        cleanedValue = cleanedValue.trim();
    }

    // If the value is empty after trimming, return undefined
    if (cleanedValue.length === 0) {
        return undefined;
    }

    return cleanedValue;
}

export function TextFieldComponent(props: TextFieldComponentProps) {
    const [inputValue, setInputValue] = useState(props.value ?? '');
    const debounceTimeoutRef = useRef<NodeJS.Timeout | null>(null);

    // Determine if the soft limit is exceeded
    const isSoftLimitExceeded = props.softLimitCharacters && inputValue.length > props.softLimitCharacters;

    // Handle input change
    const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        if (props.readonly) {
            return; // Ignore changes if readonly
        }

        const newValue = event.target.value;
        setInputValue(newValue); // update UI directly!

        if (props.bufferInputUntilBlur) {
            return; // Block onChange until onBlur, if buffer mode is active
        }

        if (props.debounce) {
            // Prevent multiple executions of onChange
            if (debounceTimeoutRef.current) {
                clearTimeout(debounceTimeoutRef.current);
            }
            debounceTimeoutRef.current = setTimeout(() => {
                const cleanedValue = cleanValue(newValue, 'keepTrailingWhitespace');
                props.onChange(cleanedValue);
            }, props.debounce);
        } else {
            props.onChange(newValue);
        }
    };

    // Handle blur event
    const handleBlur = () => {
        const cleanedValue = cleanValue(inputValue, 'dropTrailingWhitespace')

        if (props.bufferInputUntilBlur) {
            if (cleanedValue !== props.value) {
                props.onChange(cleanedValue);
            }
        } else {
            // call onChange directly if debounce is pending
            if (debounceTimeoutRef.current) {
                clearTimeout(debounceTimeoutRef.current);
                props.onChange(cleanedValue);
            }
        }

        // call onBlur, if defined
        props.onBlur?.(cleanedValue);
    };

    // Update local state if external value changes
    useEffect(() => {
        setInputValue(props.value ?? '');
    }, [props.value]);

    // Cleanup for debounce
    useEffect(() => {
        return () => {
            if (debounceTimeoutRef.current) {
                clearTimeout(debounceTimeoutRef.current);
            }
        };
    }, []);

    // Display mode (if should be displayed as text field only)
    if (props.display) {
        return (
            <Box sx={props.sx}>
                <Typography variant="caption">{props.label}</Typography>
                <Typography variant="body1">{inputValue || props.placeholder}</Typography>
                {props.hint && (
                    <Typography
                        variant="caption"
                        color="textSecondary"
                    >
                        {props.hint}
                    </Typography>
                )}
            </Box>
        );
    }

    // Pattern validation
    const patternError = useMemo(() => {
        if (!props.pattern || !inputValue) return undefined;
        return new RegExp(props.pattern.regex).test(inputValue) ? undefined : props.pattern.message;
    }, [props.pattern, inputValue]);

    const helperMessage = patternError ?? props.error ?? props.hint;
    const showMaxCharacters = Boolean(
        props.maxCharacters &&
        (!props.minCharacters || inputValue.length >= props.minCharacters)
    );
    const showMinCharacters = Boolean(
        props.minCharacters && inputValue.length < props.minCharacters
    );
    const minCharacters = props.minCharacters ?? 0;
    const showSoftLimitWarning = Boolean(
        props.softLimitCharacters && isSoftLimitExceeded
    );
    const hasHelperTextContent = Boolean(helperMessage || showMaxCharacters || showMinCharacters || showSoftLimitWarning);



    return (
        <TextField
            {...props.muiPassTroughProps}
            label={props.label}
            type={props.type}
            autoComplete={props.autocomplete}
            placeholder={props.placeholder}
            variant="outlined"
            fullWidth
            error={!!props.error || !!patternError}
            multiline={props.multiline}
            rows={props.multiline ? (props.rows ?? 4) : undefined}
            FormHelperTextProps={{component: 'div'}}
            helperText={
                hasHelperTextContent ? (
                    <>
                        {(helperMessage || showMaxCharacters || showMinCharacters) && (
                            <Box
                                sx={{
                                    display: 'flex',
                                    justifyContent: 'space-between',
                                    flexWrap: {
                                        xs: 'wrap',
                                        sm: 'nowrap'
                                    },
                                    columnGap: 3,
                                    rowGap: .5,
                                }}
                            >
                                <Box>
                                    {helperMessage}
                                </Box>

                                {showMaxCharacters && (
                                    <Box
                                        role="text"
                                        aria-label={`${inputValue.length} von ${props.maxCharacters} Zeichen verwendet`}
                                    >
                                        <span aria-hidden="true">
                                            {`${inputValue.length}/${props.maxCharacters}`}
                                        </span>
                                    </Box>
                                )}
                                {showMinCharacters && (
                                    <Box>
                                        {inputValue.length === 0
                                            ? `Mindestens ${getCharacterCount(minCharacters)} Zeichen`
                                            : `Noch mindestens ${getCharacterCount(minCharacters - inputValue.length)} Zeichen`}
                                    </Box>
                                )}
                            </Box>
                        )}
                        {showSoftLimitWarning && (
                            <Box sx={{display: 'flex', justifyContent: 'space-between'}}>
                                <Typography
                                    variant="caption"
                                    color="warning.main"
                                >
                                    {props.softLimitCharactersWarning ??
                                        `Wir empfehlen, eine Länge von ${props.softLimitCharacters} Zeichen nicht zu überschreiten.`}
                                </Typography>
                            </Box>
                        )}
                    </>
                ) : undefined
            }
            value={inputValue}
            onChange={handleChange}
            onBlur={handleBlur}
            inputProps={{
                ...(props.muiPassTroughProps?.inputProps),
                ...(props.maxCharacters ? {maxLength: props.maxCharacters} : undefined),
                'aria-disabled': props.busy || props.disabled,
            }}
            InputProps={{
                ...(props.muiPassTroughProps?.InputProps),
                startAdornment: props.startIcon && (
                    <InputAdornment position="start">{props.startIcon}</InputAdornment>
                ),
                endAdornment: props.endAction && (
                    <InputAdornment position="end">
                        {Array.isArray(props.endAction)
                            ? props.endAction.map(renderIconButton)
                            : renderIconButton(props.endAction)}
                    </InputAdornment>
                ),
                readOnly: props.busy,
            }}
            InputLabelProps={{
                title: props.label,
            }}
            disabled={props.disabled}
            required={props.required}
            sx={{
                ...props.sx,
                backgroundColor: props.busy ? "#F8F8F8" : undefined,
                cursor: props.busy ? "not-allowed" : undefined,
            }}
            size={props.size}
        />
    );
}

// Render function for IconButtons
export function renderIconButton(action: { icon: React.ReactNode; onClick: () => void; tooltip?: string }, key?: number) {
    if (action.tooltip != null) {
        return (
            <Tooltip
                key={key}
                title={action.tooltip}
            >
                <IconButton onClick={action.onClick}>{action.icon}</IconButton>
            </Tooltip>
        );
    }
    return (
        <IconButton
            key={key}
            onClick={action.onClick}
        >
            {action.icon}
        </IconButton>
    );
}