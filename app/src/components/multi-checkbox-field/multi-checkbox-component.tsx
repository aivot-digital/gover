import React from 'react';
import {Checkbox, FormControl, FormControlLabel, FormGroup, FormHelperText, FormLabel} from '@mui/material';

export interface MultiCheckboxComponentProps {
    label: string;
    value?: string[];
    onChange: (val: string[] | undefined) => void;
    options: string[] | Array<{value: string, label: string}>;
    error?: string;
    hint?: string;
    disabled?: boolean;
    busy?: boolean;
    required?: boolean;
    displayInline?: boolean;
}

export function MultiCheckboxComponent(props: MultiCheckboxComponentProps): JSX.Element {
    const value = props.value;
    const onChange = props.onChange;
    const error = props.error;
    const label = props.label;
    const required = props.required;
    const disabled = props.disabled;
    const busy = props.busy;
    const hint = props.hint;
    const displayInline = props.displayInline ?? false;

    const options = (props.options ?? [])
        .map((opt) => {
            if (typeof opt === 'string') {
                return {
                    value: opt,
                    label: opt,
                };
            } else {
                return opt;
            }
        });

    const handleOptionToggle = (toggledOption: string): void => {
        if (value == null || value.length === 0) {
            onChange([toggledOption]);
            return;
        }

        if (value.includes(toggledOption)) {
            const splicedList = value.filter((v) => v !== toggledOption);
            onChange(splicedList.length > 0 ? splicedList : undefined);
            return;
        }

        const filteredOptions = options
            .filter((opt) => {
                return value.includes(opt.value) || opt.value === toggledOption;
            })
            .map((opt) => {
                return opt.value;
            });
        onChange(filteredOptions);
    };

    return (
        <FormControl
            error={error != null}
            component="fieldset"
        >
            {
                label != null &&
                <FormLabel
                    component="legend"
                    disabled={disabled}
                >
                    {label} {required === true && '*'}
                </FormLabel>
            }
            <FormGroup row={displayInline}>
                {
                    (options ?? []).map((option) => (
                        <FormControlLabel
                            key={option.value}
                            control={
                                <Checkbox
                                    checked={(value ?? []).includes(option.value)}
                                    onChange={() => {
                                        if (!busy) {
                                            handleOptionToggle(option.value);
                                        }
                                    }}
                                    disabled={disabled}
                                    sx={{color: busy ? "rgba(0, 0, 0, 0.26)!important" : undefined}}
                                />
                            }
                            label={option.label}
                            sx={{
                                ...(displayInline ? { mr: 3 } : {}),
                                ...(busy ? {
                                    color: "rgba(0, 0, 0, 0.38)!important",
                                    cursor: "not-allowed",
                                } : {}),
                                '& .MuiFormControlLabel-label': {
                                    wordBreak: 'break-word',
                                    whiteSpace: 'normal',
                                },
                            }}
                        />
                    ))
                }
            </FormGroup>
            {
                (hint != null || error != null) &&
                <FormHelperText sx={{ml: 0}}>
                    {
                        error == null &&
                        <span>{hint}</span>
                    }
                    {
                        error != null &&
                        <span>{error}</span>
                    }
                </FormHelperText>
            }
        </FormControl>
    );
}
