import React from 'react';
import {Box, Button, Grid, Typography, useTheme} from '@mui/material';
import {type BaseSummaryProps} from './base-summary';
import {type SelectFieldElement} from '../models/elements/form/input/select-field-element';
import {stringOrDefault} from '../utils/string-utils';

export function SelectFieldSummary(props: BaseSummaryProps<SelectFieldElement, string>): JSX.Element {
    const options = (props.model.options ?? []).map((option) => {
        if (typeof option === 'string') {
            return {
                label: option,
                value: option,
            };
        } else {
            return option;
        }
    });
    const theme = useTheme();
    const value: string | null = props.value != null ? options.find((option) => option.value === props.value)?.label ?? null : null;

    return (
        <Grid
            container
            sx={{
                borderBottom: "1px solid #D4D4D4",
                py: 1,
            }}
        >
            <Grid
                item
                xs={12}
                md={4}
                sx={{
                    textAlign: 'left',
                    pr: 5,
                    [theme.breakpoints.up('md')]: {
                        textAlign: 'right',
                    },
                }}
            >
                <Typography
                    variant="body2"
                    sx={{
                        fontWeight: 'bold',
                        [theme.breakpoints.up('md')]: {
                            fontWeight: 'normal',
                        },
                    }}
                >
                    {props.model.label}
                </Typography>
            </Grid>
            <Grid
                item
                xs={12}
                md={8}
            >
                <Typography
                    variant="body2"
                >
                    {stringOrDefault(value, 'Keine Angabe')}
                </Typography>
            </Grid>
        </Grid>
    );
}
