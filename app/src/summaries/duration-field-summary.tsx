import {Grid, Typography, useTheme} from '@mui/material';
import {BaseSummaryProps} from './base-summary';
import {DurationFieldElement} from '../models/elements/form/input/duration-field-element';
import {formatDurationSeconds} from '../components/duration-field/duration-value';
import {DurationPrecision} from '../models/elements/form/input/duration-field-element';
import React from 'react';

export function DurationFieldSummary(props: BaseSummaryProps<DurationFieldElement, number>) {
    const {
        model,
        value,
    } = props;

    const theme = useTheme();

    return (
        <Grid
            container
            sx={{
                borderBottom: '1px solid',
                borderBottomColor: 'divider',
                py: 1,
            }}
        >
            <Grid
                sx={{
                    textAlign: 'left',
                    pr: 5,
                    [theme.breakpoints.up('md')]: {
                        textAlign: 'right',
                    },
                }}
                size={{
                    xs: 12,
                    md: 4
                }}>
                <Typography
                    variant="body2"
                    sx={{
                        fontWeight: 'bold',
                        [theme.breakpoints.up('md')]: {
                            fontWeight: 'normal',
                        },
                    }}
                >
                    {model.label}
                </Typography>
            </Grid>
            <Grid
                size={{
                    xs: 12,
                    md: 8
                }}>
                <Typography variant={"body2"}>
                    {
                        formatDurationSeconds(value, model.precision ?? DurationPrecision.Minute, model.largestUnit)
                    }
                </Typography>
            </Grid>
        </Grid>
    );
}
