import React, {useMemo} from 'react';
import {Box, IconButton, InputAdornment, TextField, Typography} from '@mui/material';
import {type TextFieldComponentProps} from './text-field-component-props';

export function TextFieldComponent(props: TextFieldComponentProps): JSX.Element {
    const isSoftLimitExceeded = (props.value ?? '').length > (props.softLimitCharacters ?? 0);

    if (props.display === true) {
        return (
            <Box>
                <Typography
                    variant="caption"
                >
                    {props.label}
                </Typography>
                <Typography
                    variant="body1"
                >
                    {props.value ?? props.placeholder}
                </Typography>
                {
                    props.hint != null &&
                    <Typography
                        variant="caption"
                        color="textSecondary"
                    >
                        {props.hint}
                    </Typography>
                }
            </Box>
        );
    }

    const patternError = useMemo(() => {
        if (props.pattern == null || props.value == null || props.value === '') {
            return undefined;
        }
        const re = new RegExp(props.pattern.regex);
        if (re.test(props.value)) {
            return undefined;
        }
        return props.pattern.message;
    }, [props.pattern, props.value]);

    const getCharacterCount = (count: number) => {
        switch (count) {
            case 1:
                return 'ein';
            case 2:
                return 'zwei';
            case 3:
                return 'drei';
            case 4:
                return 'vier';
            case 5:
                return 'fünf';
            case 6:
                return 'sechs';
            case 7:
                return 'sieben';
            case 8:
                return 'acht';
            case 9:
                return 'neun';
            case 10:
                return 'zehn';
            case 11:
                return 'elf';
            case 12:
                return 'zwölf';
            default:
                return count;
        }
    };

    return (
        <TextField
            label={props.label}
            type={props.type}
            autoComplete={props.autocomplete}
            placeholder={props.placeholder}
            variant="outlined"
            fullWidth
            error={props.error != null || patternError != null}
            multiline={props.multiline}
            rows={props.multiline === true ? (props.rows ?? 4) : undefined}
            FormHelperTextProps={{
                // @ts-ignore
                component: 'div',
            }}
            helperText={
                <>
                    <Box
                        sx={{
                            display: 'flex',
                            justifyContent: 'space-between',
                        }}
                    >
                        <Box>
                            {patternError != null ? patternError : (props.error != null ? props.error : props.hint)}
                        </Box>

                        {
                            props.maxCharacters != null &&
                            props.maxCharacters > 0 &&
                            (
                                props.minCharacters == null ||
                                props.minCharacters === 0 ||
                                (props.value ?? '').length >= props.minCharacters
                            ) &&
                            <Box
                                sx={{
                                    ml: 3,
                                }}
                            >
                                {(props.value ?? '').length}/{props.maxCharacters}
                            </Box>
                        }

                        {
                            props.minCharacters != null &&
                            props.minCharacters > 0 &&
                            (props.value ?? '').length < props.minCharacters &&
                            <Box
                                sx={{
                                    ml: 3,
                                }}
                            >
                                Noch
                                mindestens {getCharacterCount(props.minCharacters - (props.value?.length ?? 0))} Zeichen
                            </Box>
                        }
                    </Box>
                    {
                        props.softLimitCharacters &&
                        isSoftLimitExceeded &&
                        <Box
                            sx={{
                                display: 'flex',
                                justifyContent: 'space-between',
                            }}
                        >
                            <Typography
                                variant="caption"
                                color="warning.main"
                            >
                                {
                                    props.softLimitCharactersWarning ??
                                    `Wir empfehlen Ihnen eine Länge von ${props.softLimitCharacters} Zeichen nicht zu überschreiten.`

                                }
                            </Typography>
                        </Box>
                    }
                </>
            }
            value={props.value ?? ''}
            onChange={(event) => {
                const val = event.target.value;
                props.onChange(val.length === 0 ? undefined : val);
            }}
            onBlur={() => {
                if (props.value != null) {
                    const trimmedValue = props.value.trim();
                    const blurValue = trimmedValue.length === 0 ? undefined : trimmedValue;
                    props.onChange(blurValue);
                    if (props.onBlur != null) {
                        props.onBlur(blurValue);
                    }
                }
            }}
            inputProps={
                props.maxCharacters ? {
                    maxLength: props.maxCharacters,
                } : undefined
            }
            InputProps={{
                endAdornment: props.endAction != null ? (
                    <InputAdornment position="end">
                        {
                            Array.isArray(props.endAction) ? (
                                    <>
                                        {props.endAction.map((action, index) => (
                                            <IconButton
                                                key={index}
                                                onClick={action.onClick}
                                            >
                                                {action.icon}
                                            </IconButton>
                                        ))}
                                    </>
                                ) :
                                <IconButton
                                    onClick={props.endAction.onClick}
                                >
                                    {props.endAction.icon}
                                </IconButton>
                        }
                    </InputAdornment>
                ) : undefined,
            }}
            InputLabelProps={{
                title: props.label
            }}
            disabled={props.disabled}
            required={props.required}
        />
    );
}
