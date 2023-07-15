import React from 'react';
import { Box, TextField } from '@mui/material';
import { type TextFieldComponentProps } from './text-field-component-props';

export function TextFieldComponent(props: TextFieldComponentProps): JSX.Element {
    return (
        <TextField
            label={ props.label }
            type={ props.type }
            placeholder={ props.placeholder }
            variant="outlined"
            fullWidth
            error={ props.error != null }
            multiline={ props.multiline }
            rows={ props.multiline === true ? (props.rows ?? 4) : undefined }
            FormHelperTextProps={ {
                // @ts-ignore
                component: 'div',
            } }
            helperText={
                <Box
                    sx={ {
                        display: 'flex',
                        justifyContent: 'space-between',
                    } }
                >
                    <Box>
                        { props.error != null ? props.error : props.hint }
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
                            sx={ {
                                ml: 3,
                            } }
                        >
                            { (props.value ?? '').length }/{ props.maxCharacters }
                        </Box>
                    }

                    {
                        props.minCharacters != null &&
                        props.minCharacters > 0 &&
                        (props.value ?? '').length < props.minCharacters &&
                        <Box
                            sx={ {
                                ml: 3,
                            } }
                        >
                            Noch mindestens { props.minCharacters - (props.value?.length ?? 0) } Zeichen
                        </Box>
                    }
                </Box>
            }
            value={ props.value ?? '' }
            onChange={ (event) => {
                const val = event.target.value;
                props.onChange(val.length === 0 ? undefined : val);
            } }
            onBlur={ () => {
                if (props.value != null) {
                    const trimmedValue = props.value.trim();
                    const blurValue = trimmedValue.length === 0 ? undefined : trimmedValue;
                    props.onChange(blurValue);
                    if (props.onBlur != null) {
                        props.onBlur(blurValue);
                    }
                }
            } }
            inputProps={
                props.maxCharacters ? {
                    maxLength: props.maxCharacters,
                } : undefined
            }
            disabled={ props.disabled }
            required={ props.required }
        />
    );
}
