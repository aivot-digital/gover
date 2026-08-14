import React, {ChangeEvent, ReactNode, useCallback, useMemo} from 'react';
import {
    InputAdornment,
    ListItemIcon,
    ListItemText,
    MenuItem,
    SxProps,
    TextField,
    TextFieldProps,
    Theme,
} from '@mui/material';
import {renderIconButton} from '../text-field/text-field-component';
import {EndAction} from '../text-field/text-field-component-props';

export type SelectFieldValueType = number | string;

export interface SelectFieldComponentOption<T extends SelectFieldValueType> {
    label: string;
    subLabel?: string;
    icon?: ReactNode;
    value: T;
}

export interface SelectFieldComponentProps<T extends SelectFieldValueType> {
    label: string;
    autocomplete?: string;
    placeholder?: string;
    hint?: string;
    disabled?: boolean;
    readOnly?: boolean;
    required?: boolean;
    error?: string;
    value: T | null | undefined;
    onChange: (val: T | null, opt: SelectFieldComponentOption<T> | null) => void;
    options: SelectFieldComponentOption<T>[];
    emptyStatePlaceholder?: string;
    sx?: SxProps<Theme>;
    startIcon?: React.ReactNode;
    endAction?: EndAction | Array<EndAction>;
    muiPassTroughProps?: TextFieldProps;
}

function normalizeValue<T extends SelectFieldValueType>(val: T | null | undefined): string {
    return val != null ? val.toString() : '';
}

export function SelectFieldComponent<T extends SelectFieldValueType>(props: SelectFieldComponentProps<T>) {
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
    } = props;

    const optionElements = useMemo(() => {
        return options
            .map((option) => (
                <MenuItem
                    key={option.value}
                    value={normalizeValue(option.value)}
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

    const handleChange = useCallback((event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        const val = normalizeValue(event.target.value);

        const option = options
            .find((option) => normalizeValue(option.value) === val);

        onChange(option?.value ?? null, option ?? null);
    }, [onChange, options]);

    const isValueInOptions = useMemo(() => {
        return options.some((opt) => opt.value === value);
    }, [value, options]);

    return (
        <TextField
            {...muiPassTroughProps}
            select
            fullWidth
            label={label + ((required ?? false) ? ' *' : '')}
            autoComplete={autocomplete}
            error={error != null}
            helperText={error != null ? error : hint}
            placeholder={placeholder}
            value={isValueInOptions ? normalizeValue(value) : ''}
            onChange={handleChange}
            disabled={disabled ?? false}
            slotProps={{
                input: {
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
                },

                select: {
                    renderValue: (value) => {
                        const option = options.find((option) => normalizeValue(option.value) === value);
                        return option?.label ?? '';
                    },
                    MenuProps: {
                        slotProps: {
                            paper: {
                                sx: {
                                    width: 0,
                                },
                            },
                        },
                    },
                },

                inputLabel: {
                    title: label,
                }
            }}>
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
