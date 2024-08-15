import React from 'react';
import {IconButton, InputAdornment, MenuItem, TextField, Typography} from '@mui/material';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import {type SelectFieldComponentProps} from './select-field-component-props';

export function SelectFieldComponent({
                                         label,
                                         autocomplete,
                                         placeholder,
                                         hint,
                                         disabled,
                                         required,
                                         error,
                                         value,
                                         onChange,
                                         options,
                                         emptyStatePlaceholder,
                                     }: SelectFieldComponentProps): JSX.Element {
    const val = value ?? '';

    return (
        <TextField
            select
            fullWidth
            label={label + ((required ?? false) ? ' *' : '')}
            autoComplete={autocomplete}
            error={error != null}
            helperText={error != null ? error : hint}
            placeholder={placeholder}
            value={val}
            onChange={(event) => {
                if (isStringNullOrEmpty(event.target.value)) {
                    onChange(undefined);
                } else {
                    onChange(event.target.value);
                }
            }}
            disabled={disabled ?? false}
            InputLabelProps={{
                title: label,
            }}
            SelectProps={{
                renderValue: (value) => {
                    const option = options.find((option) => option.value === value);
                    return option?.label ?? '';
                },
            }}
        >
            {
                !(required ?? false) &&
                options.length > 0 &&
                <MenuItem
                    value={''}
                >
                    <i>{
                        placeholder ?? 'Keine Auswahl'
                    }</i>
                </MenuItem>
            }

            {
                options.length === 0 &&
                <MenuItem
                    value={''}
                >
                    <i>{emptyStatePlaceholder ?? 'Keine Optionen vorhanden'}</i>
                </MenuItem>
            }

            {
                options
                    .map((option) => (
                        <MenuItem
                            key={option.value}
                            value={option.value}
                            sx={{
                                display: 'flex',
                                flexDirection: 'column',
                                alignItems: 'flex-start',
                            }}
                        >
                            <Typography>
                                {option.label}
                            </Typography>
                            {
                                option.subLabel != null &&
                                <Typography
                                    variant="caption"
                                >
                                    {option.subLabel}
                                </Typography>
                            }
                        </MenuItem>
                    ))
            }
        </TextField>
    );
}

