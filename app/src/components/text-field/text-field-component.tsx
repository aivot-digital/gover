import React, {useState, useMemo, useEffect, useRef} from 'react';
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

export function TextFieldComponent(props: TextFieldComponentProps): JSX.Element {
    const [inputValue, setInputValue] = useState(props.value ?? '');
    const debounceTimeoutRef = useRef<NodeJS.Timeout | null>(null);

    // Determine if the soft limit is exceeded
    const isSoftLimitExceeded = props.softLimitCharacters && inputValue.length > props.softLimitCharacters;

    // Handle input change
    const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
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

    // Render function for IconButtons
    const renderIconButton = (action: { icon: React.ReactNode; onClick: () => void; tooltip?: string }, key?: number) => (
        action.tooltip ? (
            <Tooltip
                key={key}
                title={action.tooltip}
            >
                <IconButton onClick={action.onClick}>{action.icon}</IconButton>
            </Tooltip>
        ) : (
            <IconButton
                key={key}
                onClick={action.onClick}
            >{action.icon}</IconButton>
        )
    );

    return (
        <TextField
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
                <>
                    <Box sx={{display: 'flex', justifyContent: 'space-between'}}>
                        <Box>{patternError ?? props.error ?? props.hint}</Box>
                        {props.maxCharacters && (
                            !props.minCharacters ||
                            inputValue.length >= props.minCharacters
                        ) && (
                            <Box
                                role="text"
                                sx={{ml: 3}}
                                aria-label={`${inputValue.length} von ${props.maxCharacters} Zeichen verwendet`}
                            >
                                <span aria-hidden="true">
                                    {`${inputValue.length}/${props.maxCharacters}`}
                                </span>
                            </Box>
                        )}
                        {props.minCharacters && inputValue.length < props.minCharacters && (
                            <Box sx={{ml: 3}}>
                                Noch mindestens {getCharacterCount(props.minCharacters - inputValue.length)} Zeichen
                            </Box>
                        )}
                    </Box>
                    {props.softLimitCharacters && isSoftLimitExceeded && (
                        <Box sx={{display: 'flex', justifyContent: 'space-between'}}>
                            <Typography
                                variant="caption"
                                color="warning.main"
                            >
                                {props.softLimitCharactersWarning ??
                                    `Wir empfehlen Ihnen eine Länge von ${props.softLimitCharacters} Zeichen nicht zu überschreiten.`}
                            </Typography>
                        </Box>
                    )}
                </>
            }
            value={inputValue}
            onChange={handleChange}
            onBlur={handleBlur}
            inputProps={{
                ...(props.maxCharacters ? {maxLength: props.maxCharacters} : undefined),
                'aria-disabled': props.busy || props.disabled,
            }}
            InputProps={{
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
