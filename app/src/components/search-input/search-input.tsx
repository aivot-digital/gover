import {Box, IconButton, InputAdornment, SxProps, TextField} from '@mui/material';
import React from 'react';
import Search from '@aivot/mui-material-symbols-400-outlined/dist/search/Search';
import Close from '@aivot/mui-material-symbols-400-outlined/dist/close/Close';

export interface SearchInputProps {
    value: string;
    onChange: (val: string) => void;
    label: string;
    placeholder?: string;
    autoFocus?: boolean;
    sx?: SxProps;
    disabled?: boolean;
    clearable?: boolean;
    ariaLabel?: string;
    size?: 'small' | 'medium';
    fullWidth?: boolean;
    hideLabel?: boolean;
    debounce?: number;
}

export function SearchInput(props: SearchInputProps) {
    const hideLabel = props.hideLabel ?? false;
    const clearable = props.clearable ?? true;
    const debounce = props.debounce ?? 0;
    const debounceTimeoutRef = React.useRef<number | null>(null);
    // Keep typing responsive while parent state may update debounced.
    const [localValue, setLocalValue] = React.useState(props.value);

    React.useEffect(() => {
        setLocalValue(props.value);
    }, [props.value]);

    React.useEffect(() => {
        return () => {
            if (debounceTimeoutRef.current != null) {
                window.clearTimeout(debounceTimeoutRef.current);
            }
        };
    }, []);

    const emitChange = (nextValue: string): void => {
        if (debounce <= 0) {
            props.onChange(nextValue);
            return;
        }

        if (debounceTimeoutRef.current != null) {
            window.clearTimeout(debounceTimeoutRef.current);
        }

        debounceTimeoutRef.current = window.setTimeout(() => {
            props.onChange(nextValue);
        }, debounce);
    };

    const handleInputChange = (nextValue: string): void => {
        setLocalValue(nextValue);
        emitChange(nextValue);
    };

    const handleClear = (): void => {
        if (debounceTimeoutRef.current != null) {
            window.clearTimeout(debounceTimeoutRef.current);
            debounceTimeoutRef.current = null;
        }

        setLocalValue('');
        props.onChange('');
    };

    const handleBlur = (): void => {
        // Align with TextFieldComponent: commit pending debounced change immediately on blur.
        if (debounceTimeoutRef.current != null) {
            window.clearTimeout(debounceTimeoutRef.current);
            debounceTimeoutRef.current = null;
            const cleaned = localValue.trim();
            props.onChange(cleaned);
            if (cleaned !== localValue) {
                setLocalValue(cleaned);
            }
            return;
        }

        const cleaned = localValue.trim();
        if (cleaned !== localValue) {
            setLocalValue(cleaned);
            props.onChange(cleaned);
        }
    };

    return (
        <Box sx={props.sx}>
            <TextField
                value={localValue}
                onChange={event => {
                    handleInputChange(event.target.value ?? '');
                }}
                onBlur={handleBlur}
                label={props.label}
                placeholder={props.placeholder}
                variant="outlined"
                fullWidth={props.fullWidth ?? true}
                autoFocus={props.autoFocus}
                disabled={props.disabled}
                size={props.size ?? 'small'}
                sx={{
                    margin: 0,
                }}
                InputLabelProps={{
                    sx: {
                        display: hideLabel ? 'none' : undefined,
                    },
                }}
                InputProps={{
                    startAdornment: (
                        <InputAdornment position="start">
                            <Search fontSize="small" />
                        </InputAdornment>
                    ),
                    endAdornment: clearable && localValue.trim().length > 0 ? (
                        <InputAdornment position="end">
                            <IconButton
                                size={props.size ?? 'small'}
                                onClick={handleClear}
                                disabled={props.disabled}
                                aria-label="Suche löschen"
                            >
                                <Close fontSize="small" />
                            </IconButton>
                        </InputAdornment>
                    ) : undefined,
                }}
                inputProps={{
                    'aria-label': props.ariaLabel ?? props.label,
                }}
            />
        </Box>
    );
}
