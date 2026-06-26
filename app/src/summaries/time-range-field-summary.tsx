import {Grid, Typography, useTheme} from '@mui/material';
import {BaseSummaryProps} from './base-summary';
import {TimeRangeFieldElement, TimeRangeValue} from '../models/elements/form/input/time-range-field-element';
import {format} from 'date-fns';
import {TimeFieldComponentModelMode} from '../models/elements/form/input/time-field-element';

export function TimeRangeFieldSummary(props: BaseSummaryProps<TimeRangeFieldElement, TimeRangeValue>) {
    const theme = useTheme();

    const startDate = props.value?.start != null ? new Date(props.value.start) : null;
    const endDate = props.value?.end != null ? new Date(props.value.end) : null;
    const isBothEmpty = startDate == null && endDate == null;
    const formatPattern = (props.model.mode ?? TimeFieldComponentModelMode.Minute) === TimeFieldComponentModelMode.Second ? 'HH:mm:ss' : 'HH:mm';

    const startLabel = startDate != null && !isNaN(startDate.getTime()) ? `${format(startDate, formatPattern)} Uhr` : 'Keine Angabe';
    const endLabel = endDate != null && !isNaN(endDate.getTime()) ? `${format(endDate, formatPattern)} Uhr` : 'Keine Angabe';

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
