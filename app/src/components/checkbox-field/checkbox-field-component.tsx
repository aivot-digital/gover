import React from 'react';
import {Checkbox, FormControl, FormControlLabel, FormHelperText} from '@mui/material';
import {type CheckboxFieldComponentProps} from './checkbox-field-component-props';

export function CheckboxFieldComponent(props: CheckboxFieldComponentProps): JSX.Element {
    return (
        <FormControl
            error={props.error != null}
            disabled={props.disabled}
        >
            <FormControlLabel
                control={
                    <Checkbox
                        checked={props.value ?? false}
                        onChange={(event) => {
                            props.onChange(event.target.checked);
                        }}
                        disabled={props.disabled}
                    />
                }
                label={props.label + (props.required === true ? ' *' : '')}
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
