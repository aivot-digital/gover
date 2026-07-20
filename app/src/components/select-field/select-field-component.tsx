import React, {useMemo} from 'react';
import {InputAdornment, ListItemIcon, ListItemText, MenuItem, TextField} from '@mui/material';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import {type SelectFieldComponentProps} from './select-field-component-props';
import {renderIconButton} from '../text-field/text-field-component';

export function SelectFieldComponent(props: SelectFieldComponentProps) {
    const {
        label,
        autocomplete,
        placeholder,
        hint,
        disabled,
        readOnly,
        required,
        error,
        value,
        onChange,
        options,
        emptyStatePlaceholder,
        startIcon,
        endAction,
        sx,
        muiPassTroughProps,
        size = 'medium',
    } = props;

    const val = value ?? '';

    const optionElements = useMemo(() => {
        return options
            .map((option) => (
                <MenuItem
                    key={option.value}
                    value={option.value}
                    sx={{
                        maxWidth: '100%',
                        overflow: 'hidden',
                    }}
                >
                    {
                        option.icon != null &&
                        <ListItemIcon>
                            {option.icon}
                        </ListItemIcon>
                    }

                    <ListItemText
                        primary={option.label}
                        secondary={option.subLabel}
                        sx={{
                            minWidth: 0,
                        }}
                        slotProps={{
                            primary: {
                                noWrap: true,
                            },
                            secondary: {
                                noWrap: true,
                            },
                        }}
                    />
                </MenuItem>
            ));
    }, [options]);

    const isValueInOptions = useMemo(() => {
        return options.some((opt) => opt.value === value);
    }, [value, options]);

    return (
        <TextField
            {...muiPassTroughProps}
            select
            label={label}
            autoComplete={autocomplete}
            required={required}
            error={error != null}
            helperText={error != null ? error : hint}
            placeholder={placeholder}
            value={isValueInOptions ? val : ''}
            onChange={(event) => {
                if (isStringNullOrEmpty(event.target.value)) {
                    onChange(null);
                } else {
                    onChange(event.target.value);
                }
            }}
            disabled={disabled ?? false}
            InputLabelProps={{
                title: label,
            }}
            InputProps={{
                sx: sx,
                readOnly: readOnly,
                startAdornment: startIcon && (
                    <InputAdornment position="start">{startIcon}</InputAdornment>
                ),
                endAdornment: endAction && (
                    <InputAdornment
                        position="end"
                        sx={{
                            mr: 2,
                        }}
                    >
                        {Array.isArray(endAction)
                            ? endAction.map(renderIconButton)
                            : renderIconButton(endAction)}
                    </InputAdornment>
                ),
            }}
            SelectProps={{
                renderValue: (value) => {
                    const option = options.find((option) => option.value === value);
                    return option?.label ?? '';
                },
                MenuProps: {
                    PaperProps: {
                        sx: {
                            width: 0,
                        },
                    },
                },
            }}
            size={size}
            fullWidth
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

            {optionElements}
        </TextField>
    );
}
