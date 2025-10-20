import {Box, InputAdornment, TextField, useTheme} from '@mui/material';
import React from 'react';
import {SearchInputProps} from './search-input-props';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';

export function SearchInput(props: SearchInputProps) {
    const theme = useTheme();

    return (
        <Box sx={props.sx}>
            <TextField
                value={props.value}
                onChange={event => {
                    props.onChange(event.target.value ?? '');
                }}

                label={props.placeholder}
                placeholder={props.placeholder}

                variant="filled"
                fullWidth={true}
                autoFocus={props.autoFocus}

                sx={{
                    margin: 0,
                }}
                InputLabelProps={{
                    sx: {
                        // Hide the label to clean up the UI but keep it accessible for screen readers
                        display: 'none',
                    },
                }}
                InputProps={{
                    startAdornment: (
                        <InputAdornment
                            position="start"
                            sx={{
                                // TODO: Workaround. Das ist keine gute Lösung. Das Icon sollte zentriert sein ohne Translaten zu müssen. Das InputAdornment sollte seine Kinder richtig platzieren.
                                transform: 'translateY(-0.5em)',
                            }}
                        >
                            <SearchOutlinedIcon />
                        </InputAdornment>
                    ),
                    sx: {
                        // The typing for theme.shape.borderRadius is string | number ???
                        borderRadius: (typeof theme.shape.borderRadius === 'number' ? theme.shape.borderRadius : 2) / 2,
                        backgroundColor: '#E2E7E2',
                        '&:hover': {
                            backgroundColor: '#E7ECE7',
                        },
                        '&::before': {
                            display: 'none',
                        },
                        '&::after': {
                            display: 'none',
                        },
                        '&.Mui-focused': {
                            backgroundColor: '#E2E7E2',
                        },
                        '& .MuiInputBase-input': {
                            padding: theme.spacing(1, 1, 1, 0),
                        },
                    },
                }}

                type="search"
                inputProps={{
                    'aria-label': 'Suche',
                }}
            />
        </Box>
    );
}
