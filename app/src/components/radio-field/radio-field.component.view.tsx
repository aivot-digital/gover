import React from 'react';
import {FormControl, FormControlLabel, FormHelperText, FormLabel, Radio, RadioGroup} from '@mui/material';
import {type RadioFieldElement} from '../../models/elements/form/input/radio-field-element';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import {type BaseViewProps} from '../../views/base-view';
import {type SelectFieldElementOption} from '../../models/elements/form/input/select-field-element';

export function RadioFieldComponentView(props: BaseViewProps<RadioFieldElement, string>): JSX.Element {
    const options = (props.element.options ?? []).map((option: string | SelectFieldElementOption) => {
        if (typeof option === 'string') {
            return {
                value: option,
                label: option,
            };
        } else {
            return option;
        }
    });

    return (
        <FormControl
            error={props.error != null}
            disabled={props.element.disabled}
        >
            <FormLabel
                id={"label-" + props.element.id}
            >
                {props.element.label} {props.element.required && ' *'}
            </FormLabel>
            <RadioGroup
                aria-labelledby={"label-" + props.element.id}
                name={"radio-group-" + props.element.id}
                value={props.value ?? ''}
                onChange={(event) => {
                    if (isStringNullOrEmpty(event.target.value)) {
                        props.setValue(undefined);
                    } else {
                        props.setValue(event.target.value ?? '');
                    }
                }}
            >
                {
                    !props.element.required &&
                    <FormControlLabel
                        value={''}
                        control={<Radio/>}
                        label="Keine Auswahl"
                        disabled={props.element.disabled}
                        sx={{fontStyle: 'italic'}}
                    />
                }
                {
                    options.map((option) => (
                        <FormControlLabel
                            key={option.value}
                            value={option.value}
                            control={<Radio/>}
                            label={option.label}
                            disabled={props.element.disabled}
                        />
                    ))
                }
            </RadioGroup>
            {
                (props.element.hint != null || props.error != null) &&
                <FormHelperText sx={{ml: 0}}>
                    {
                        props.element.hint != null &&
                        props.error == null &&
                        props.element.hint
                    }
                    {
                        props.error != null &&
                        <span>{props.error}</span>
                    }
                </FormHelperText>
            }
        </FormControl>
    );
}
