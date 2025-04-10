import {Box, InputAdornment, TextField, useTheme} from '@mui/material';
import React from 'react';
import {SearchInputProps} from './search-input-props';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';

/*
TODO: Maybe use later
export function AnimatedSearchIcon(props: SVGProps<SVGSVGElement>) {
    return (
        <svg
            xmlns="http://www.w3.org/2000/svg"
            width="1.5em"
            height="1.5em"
            viewBox="0 0 24 24"
            {...props}
        >
            <g
                fill="none"
                stroke="currentColor"
                strokeWidth={2}
            >
                <path
                    strokeDasharray={8}
                    strokeDashoffset={8}
                    d="M 20 20 L 14 14"
                >
                    <animate
                        fill="freeze"
                        attributeName="stroke-dashoffset"
                        begin="0.5s"
                        dur="0.5s"
                        values="8;0"
                    />
                </path>

                <circle
                    strokeDasharray={38}
                    strokeDashoffset={38}
                    cx={9.75}
                    cy={9.25}
                    r={6}
                >
                    <animate
                        fill="freeze"
                        attributeName="stroke-dashoffset"
                        begin="1.15s"
                        dur="0.75s"
                        values="38;0"
                    />
                </circle>
            </g>
        </svg>
    );
}
*/

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
                        borderRadius: theme.shape.borderRadius / 2,
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
