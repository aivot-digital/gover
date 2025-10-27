import React, {useMemo} from 'react';
import {IconButton, InputAdornment, MenuItem, TextField, Typography} from '@mui/material';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import {type SelectFieldComponentProps} from './select-field-component-props';
import Tooltip from '@mui/material/Tooltip';

export function SelectFieldComponent({
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
                                     }: SelectFieldComponentProps) {
    const val = value ?? '';

    const optionElements = useMemo(() => {
        return options
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
            ));
    }, [options]);

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
            InputProps={{
                sx: sx,
                readOnly: readOnly,
                startAdornment: startIcon && (
                    <InputAdornment position="start">{startIcon}</InputAdornment>
                ),
                endAdornment: endAction && (
                    <InputAdornment position="end" sx={{
                        mr: 2,
                    }}>
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

            {optionElements}
        </TextField>
    );
}

const renderIconButton = (action: { icon: React.ReactNode; onClick: () => void; tooltip?: string }, key?: number) => (
    action.tooltip ? (
        <Tooltip
            key={key}
            title={action.tooltip}
        >
            <IconButton onClick={action.onClick}>{action.icon}</IconButton>
        </Tooltip>
    ) : (
        <IconButton
            key={key}
            onClick={action.onClick}
        >{action.icon}</IconButton>
    )
);
