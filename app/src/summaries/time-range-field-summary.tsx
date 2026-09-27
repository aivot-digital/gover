import {Grid, Typography, useTheme} from '@mui/material';
import {BaseSummaryProps} from './base-summary';
import {TimeRangeFieldElement, TimeRangeValue} from '../models/elements/form/input/time-range-field-element';
import {TimeFieldComponentModelMode} from '../models/elements/form/input/time-field-element';
import {localTimeIsoToDateTime} from '../utils/temporal-utils';

export function TimeRangeFieldSummary(props: BaseSummaryProps<TimeRangeFieldElement, TimeRangeValue>) {
    const theme = useTheme();

    const startTime = props.value?.start != null ? localTimeIsoToDateTime(props.value.start) : null;
    const endTime = props.value?.end != null ? localTimeIsoToDateTime(props.value.end) : null;
    const isBothEmpty = startTime == null && endTime == null;
    const formatPattern = (props.model.mode ?? TimeFieldComponentModelMode.Minute) === TimeFieldComponentModelMode.Second ? 'HH:mm:ss' : 'HH:mm';

    const startLabel = startTime != null ? `${startTime.toFormat(formatPattern)} Uhr` : 'Keine Angabe';
    const endLabel = endTime != null ? `${endTime.toFormat(formatPattern)} Uhr` : 'Keine Angabe';

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
