import {Grid, Typography, useTheme} from '@mui/material';
import {BaseSummaryProps} from './base-summary';
import {CheckboxFieldElement} from '../models/elements/form/input/checkbox-field-element';

export function CheckboxFieldSummary(props: BaseSummaryProps<CheckboxFieldElement, boolean>) {
    const theme = useTheme();

    const {
        model,
        value,
    } = props;

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
                    md: 4
                }}>
                <Typography
                    variant={'body2'}
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
                <Typography variant="body2">
                    {
                        value ? 'Ja' : 'Nein'
                    }
                </Typography>
            </Grid>
        </Grid>
    );
};
