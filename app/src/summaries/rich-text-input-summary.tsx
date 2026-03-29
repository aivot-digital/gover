import {Grid, Typography, useTheme} from '@mui/material';
import {useMemo} from 'react';
import {BaseSummaryProps} from './base-summary';
import {RichTextInputElement} from '../models/elements/form/input/rich-text-input-element';

function normalizeValue(value: string | null | undefined): string {
    if (value == null || value.trim().length === 0) {
        return 'Keine Angabe';
    }

    return value.trim();
}

export function RichTextInputSummary(props: BaseSummaryProps<RichTextInputElement, string>) {
    const theme = useTheme();
    const value = useMemo(() => normalizeValue(props.value), [props.value]);

    return (
        <Grid
            container
            sx={{
                borderBottom: '1px solid #D4D4D4',
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
                    md: 4,
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
                size={{
                    xs: 12,
                    md: 8,
                }}
            >
                <Typography
                    variant="body2"
                    sx={{
                        whiteSpace: 'pre-wrap',
                    }}
                >
                    {value}
                </Typography>
            </Grid>
        </Grid>
    );
}
