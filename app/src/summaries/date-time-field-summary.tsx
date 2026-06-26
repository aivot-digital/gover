import {Grid, Typography, useTheme} from '@mui/material';
import {format} from 'date-fns';
import {BaseSummaryProps} from './base-summary';
import {DateTimeFieldElement} from '../models/elements/form/input/date-time-field-element';
import {TimeFieldComponentModelMode} from '../models/elements/form/input/time-field-element';

export function DateTimeFieldSummary(props: BaseSummaryProps<DateTimeFieldElement, string>) {
    const {
        value,
        model,
    } = props;

    const date = value != null && value.length > 0 ? new Date(value) : null;
    const theme = useTheme();
    const formatPattern = (model.mode ?? TimeFieldComponentModelMode.Minute) === TimeFieldComponentModelMode.Second
        ? 'dd.MM.yyyy, HH:mm:ss'
        : 'dd.MM.yyyy, HH:mm';

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
                    {model.label}
                </Typography>
            </Grid>
            <Grid
                size={{
                    xs: 12,
                    md: 8,
                }}
            >
                <Typography variant="body2">
                    {date != null && !isNaN(date.getTime()) ? `${format(date, formatPattern)} Uhr` : 'Keine Angabe'}
                </Typography>
            </Grid>
        </Grid>
    );
}
