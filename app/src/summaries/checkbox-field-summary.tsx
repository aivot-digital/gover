import {Grid, Typography, useTheme} from '@mui/material';
import {BaseSummary} from './base-summary';
import {CheckboxFieldElement} from '../models/elements/form/input/checkbox-field-element';

export const CheckboxFieldSummary: BaseSummary<CheckboxFieldElement, boolean> = ({
                                                                                     model,
                                                                                     value,
                                                                                 }) => {
    const theme = useTheme();

    return (
        <Grid
            container
            sx={{
                borderBottom: '1px solid #D4D4D4',
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
                item
                xs={12}
                md={8}
            >
                <Typography variant="body2">
                    {
                        value ? 'Ja' : 'Nein'
                    }
                </Typography>
            </Grid>
        </Grid>
    );
};
