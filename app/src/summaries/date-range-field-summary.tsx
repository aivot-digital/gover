import {Grid, Typography, useTheme} from '@mui/material';
import {BaseSummaryProps} from './base-summary';
import {DateRangeFieldElement, DateRangeValue} from '../models/elements/form/input/date-range-field-element';
import {format} from 'date-fns';

export function DateRangeFieldSummary(props: BaseSummaryProps<DateRangeFieldElement, DateRangeValue>) {
    const theme = useTheme();

    const startDate = props.value?.start != null ? new Date(props.value.start) : null;
    const endDate = props.value?.end != null ? new Date(props.value.end) : null;
    const isBothEmpty = startDate == null && endDate == null;

    const startLabel = startDate != null && !isNaN(startDate.getTime()) ? format(startDate, 'dd.MM.yyyy') : 'Keine Angabe';
    const endLabel = endDate != null && !isNaN(endDate.getTime()) ? format(endDate, 'dd.MM.yyyy') : 'Keine Angabe';

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
            <Grid size={{xs: 12, md: 8}}>
                <Typography variant="body2">{isBothEmpty ? 'Keine Angabe' : `${startLabel} bis ${endLabel}`}</Typography>
            </Grid>
        </Grid>
    );
}
