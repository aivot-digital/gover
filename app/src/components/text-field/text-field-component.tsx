import React, {useEffect, useMemo, useRef, useState} from 'react';
import {Box, IconButton, InputAdornment, ListItemText, TextField, Typography} from '@mui/material';
import {type TextFieldComponentProps} from './text-field-component-props';
import Tooltip from '@mui/material/Tooltip';
import Autocomplete from '@mui/material/Autocomplete';
import type {TextFieldOwnerState} from '@mui/material/TextField';
import {CopyToClipboardButton} from '../copy-to-clipboard-button/copy-to-clipboard-button';
import {getDisabledFieldBackground} from '../../theming/field-state-colors';

const COPY_VALUE_TEMPLATE_PLACEHOLDER = '{value}';

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
 * Empty strings should be converted to null.
 * Leading and trailing whitespace should be removed if the corresponding flag is set.
 *
 * @param originalValue The original value to be cleaned.
 * @param flag The flag to determine if trailing whitespace should be kept or dropped. This is used during the debounce process to keep trailing whitespaces when debounce is triggered and the user is still typing.
 */
function cleanValue(originalValue: string | null | undefined, flag: 'keepTrailingWhitespace' | 'dropTrailingWhitespace'): string | null {
    // Missing input is emitted as an explicit clear once it originated from user interaction.
    if (originalValue == null) {
        return null;
    }

    let cleanedValue = originalValue;

    // Remove all leading and trailing whitespace if the flag for dropping trailing whitespace is set
    if (flag === 'dropTrailingWhitespace') {
        // Remove trailing whitespace
        cleanedValue = cleanedValue.trim();
    }

    // If the value is empty after trimming, return an explicit clear
    if (cleanedValue.length === 0) {
        return null;
    }

    return cleanedValue;
}

function renderStartAdornment(startIcon: React.ReactNode | undefined, existingStartAdornment: React.ReactNode): React.ReactNode {
    if (startIcon == null) {
        return existingStartAdornment;
    }

    const prefixAdornment = (
        <InputAdornment
            position="start"
            sx={{
                whiteSpace: 'nowrap',
                '> p': {
                    whiteSpace: 'nowrap',
                },
            }}
        >
            {startIcon}
        </InputAdornment>
    );

    if (existingStartAdornment == null) {
        return prefixAdornment;
    }

    return (
        <>
            {prefixAdornment}
            {existingStartAdornment}
        </>
    );
}

function renderEndAdornment(
    endAction: TextFieldComponentProps['endAction'],
    copyButton: React.ReactNode,
    existingEndAdornment: React.ReactNode,
): React.ReactNode {
    const customAdornmentChildren = [
        ...(Array.isArray(endAction)
            ? endAction.map((action, index) => renderIconButton(action, index))
            : endAction != null
                ? [renderIconButton(endAction, 0)]
                : []),
        copyButton,
    ].filter((child): child is React.ReactNode => child != null);

    const customAdornment = customAdornmentChildren.length > 0 ? (
        <InputAdornment position="end">
            <Box sx={{display: 'flex', alignItems: 'center', gap: .5}}>
                {customAdornmentChildren}
            </Box>
        </InputAdornment>
    ) : undefined;

    if (customAdornment == null) {
        return existingEndAdornment;
    }

    if (existingEndAdornment == null) {
        return customAdornment;
    }

    return (
        <>
            {customAdornment}
            {existingEndAdornment}
        </>
    );
}

export function AutocompleteTextField(props: TextFieldComponentProps & {
    suggestions: string[] | {
        id: string;
        label: string;
        subLabel?: string;
    }[];
}) {
    const {
        suggestions,
        ...rest
    } = props;

    const options = useMemo(() => {
        return suggestions
            .map((s) => typeof s === 'string'
                ? ({
                    id: s,
                    label: s,
                    subLabel: undefined,
                })
                : ({
                    id: s.id,
                    label: s.label,
                    subLabel: s.subLabel,
                }));
    }, [suggestions]);

    return (
        <Autocomplete
            disablePortal
            freeSolo
            fullWidth
            readOnly={rest.readonly}
            onChange={(_, value) => {
                if (rest.disabled || rest.readonly) {
                    return;
                }

                if (value == null) {
                    rest.onChange(null);
                } else {
                    rest.onChange((value as any).id ?? null);
                }
            }}
            value={rest.value ?? null}
            options={options}
            renderOption={({key, ...optionProps}, option) => (
                <Box component="li" key={key} {...optionProps}>
                    <ListItemText
                        primary={option.label}
                        secondary={option.subLabel}
                    />
                </Box>
            )}
            renderInput={(params) => (
                <TextFieldComponent {...rest} muiPassTroughProps={params}
                                    disabled={rest.disabled}/>
            )}
        />
    );
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
            props.onChange(cleanValue(newValue, 'keepTrailingWhitespace'));
        }
    };

    // Handle blur event
    const handleBlur = () => {
        const cleanedValue = cleanValue(inputValue, 'dropTrailingWhitespace');

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

    // Pattern validation
    const patternError = useMemo(() => {
        if (!props.pattern || !inputValue) return undefined;
        return new RegExp(props.pattern.regex).test(inputValue) ? undefined : props.pattern.message;
    }, [props.pattern, inputValue]);

    const errorMessages = useMemo(() => {
        if (props.error == null) {
            return [];
        }

        return (Array.isArray(props.error) ? props.error : [props.error])
            .filter((errorMessage) => errorMessage.length > 0);
    }, [props.error]);

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

    const errorHelperMessage = errorMessages.length > 1 ? (
        <Box
            component="ul"
            sx={{
                m: 0,
                pl: 2,
            }}
        >
            {errorMessages.map((errorMessage, index) => (
                <li key={index}>{errorMessage}</li>
            ))}
        </Box>
    ) : errorMessages[0];
    const helperMessage = patternError ?? errorHelperMessage ?? props.hint;
    const showMaxCharacters = Boolean(
        props.maxCharacters &&
        (!props.minCharacters || inputValue.length >= props.minCharacters),
    );
    const showMinCharacters = Boolean(
        props.minCharacters && inputValue.length < props.minCharacters,
    );
    const minCharacters = props.minCharacters ?? 0;
    const showSoftLimitWarning = Boolean(
        props.softLimitCharacters && isSoftLimitExceeded,
    );
    const hasHelperTextContent = Boolean(helperMessage || showMaxCharacters || showMinCharacters || showSoftLimitWarning);
    // Autocomplete supplies refs, event handlers, and accessibility attributes through these slots.
    // Resolve callback-based props before adding local behavior so those bindings remain intact.
    const passThroughSlotProps = props.muiPassTroughProps?.slotProps;
    const copyValue = props.copyValueTemplate == null || props.copyValueTemplate.length === 0
        ? inputValue
        : props.copyValueTemplate.split(COPY_VALUE_TEMPLATE_PLACEHOLDER).join(inputValue);
    const copyButton = props.copyable ? (
        <CopyToClipboardButton
            key="copy"
            text={copyValue}
            ariaLabel={props.label ? `${props.label} kopieren` : 'Kopieren'}
            disabled={inputValue.length === 0}
            size="small"
        />
    ) : undefined;


    return (
        <TextField
            {...props.muiPassTroughProps}
            label={props.label}
            type={props.type}
            autoComplete={props.autocomplete}
            placeholder={props.placeholder}
            variant="outlined"
            fullWidth
            error={errorMessages.length > 0 || !!patternError}
            multiline={props.multiline}
            rows={props.multiline ? (props.rows ?? 4) : undefined}
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
                                        sm: 'nowrap',
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
                                    sx={{
                                        color: "warning.main"
                                    }}
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
            disabled={props.disabled}
            required={props.required}
            sx={{
                ...props.sx,
                backgroundColor: props.busy ? getDisabledFieldBackground : undefined,
                cursor: props.busy ? 'not-allowed' : undefined,
            }}
            size={props.size}
            slotProps={{
                ...passThroughSlotProps,
                input: (ownerState: TextFieldOwnerState) => {
                    const inputSlotProps = typeof passThroughSlotProps?.input === 'function'
                        ? passThroughSlotProps.input(ownerState)
                        : passThroughSlotProps?.input;

                    return {
                        ...inputSlotProps,
                        startAdornment: renderStartAdornment(props.startIcon, inputSlotProps?.startAdornment),
                        endAdornment: renderEndAdornment(props.endAction, copyButton, inputSlotProps?.endAdornment),
                        readOnly: props.readonly || props.busy || inputSlotProps?.readOnly,
                    };
                },

                htmlInput: (ownerState: TextFieldOwnerState) => {
                    const htmlInputSlotProps = typeof passThroughSlotProps?.htmlInput === 'function'
                        ? passThroughSlotProps.htmlInput(ownerState)
                        : passThroughSlotProps?.htmlInput;

                    return {
                        ...htmlInputSlotProps,
                        ...(props.maxCharacters ? {maxLength: props.maxCharacters} : undefined),
                        'aria-disabled': props.busy || props.disabled,
                    };
                },

                inputLabel: (ownerState: TextFieldOwnerState) => {
                    const inputLabelSlotProps = typeof passThroughSlotProps?.inputLabel === 'function'
                        ? passThroughSlotProps.inputLabel(ownerState)
                        : passThroughSlotProps?.inputLabel;

                    return {
                        ...inputLabelSlotProps,
                        title: props.label,
                    };
                },

                formHelperText: (ownerState: TextFieldOwnerState) => {
                    const formHelperTextSlotProps = typeof passThroughSlotProps?.formHelperText === 'function'
                        ? passThroughSlotProps.formHelperText(ownerState)
                        : passThroughSlotProps?.formHelperText;

                    return {
                        ...formHelperTextSlotProps,
                        component: 'div',
                    };
                },
            }} />
    );
}

// Render function for IconButtons
export function renderIconButton(action: {
    icon: React.ReactNode;
    onClick: () => void;
    tooltip?: string
}, key?: number) {
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
