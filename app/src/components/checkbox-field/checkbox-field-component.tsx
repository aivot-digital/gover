import React from 'react';
import {Checkbox, FormControl, FormControlLabel, FormHelperText, Switch} from '@mui/material';
import {type CheckboxFieldComponentProps} from './checkbox-field-component-props';

export function CheckboxFieldComponent(props: CheckboxFieldComponentProps): JSX.Element {
    return (
        <FormControl
            error={props.error != null}
            disabled={props.disabled}
            sx={props.sx}
        >
            <FormControlLabel
                control={
                    props.variant === 'switch' ?
                        <Switch
                            checked={props.value ?? false}
                            onChange={(event) => {
                                if (!props.busy) {
                                    props.onChange(event.target.checked);
                                }
                            }}
                            disabled={props.disabled}
                            sx={{color: props.busy ? "rgba(0, 0, 0, 0.26)!important" : undefined}}
                        /> :
                        <Checkbox
                            checked={props.value ?? false}
                            onChange={(event) => {
                                if (!props.busy) {
                                    props.onChange(event.target.checked);
                                }
                            }}
                            disabled={props.disabled}
                            sx={{color: props.busy ? "rgba(0, 0, 0, 0.26)!important" : undefined}}
                        />
                }
                label={props.label + (props.required === true ? ' *' : '')}
                sx={{
                    ...(props.busy ? {
                        color: "rgba(0, 0, 0, 0.38)!important",
                        cursor: "not-allowed",
                    } : {}),
                    '& .MuiFormControlLabel-label': {
                        wordBreak: 'break-word',
                        whiteSpace: 'normal',
                    },
                }}
            />

            {
                (props.hint != null || props.error != null) &&
                <FormHelperText
                    sx={{
                        ml: 0,
                    }}
                >
                    {
                        <span>{props.error ?? props.hint}</span>
                    }
                </FormHelperText>
            }
        </FormControl>
    );
}
