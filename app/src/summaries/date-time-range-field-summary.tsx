import {Grid, Typography, useTheme} from '@mui/material';
import {BaseSummaryProps} from './base-summary';
import {DateTimeRangeFieldElement, DateTimeRangeValue} from '../models/elements/form/input/date-time-range-field-element';
import {TimeFieldComponentModelMode} from '../models/elements/form/input/time-field-element';
import {formatInstantInApplicationTimeZone} from '../utils/temporal-utils';

export function DateTimeRangeFieldSummary(props: BaseSummaryProps<DateTimeRangeFieldElement, DateTimeRangeValue>) {
    const theme = useTheme();

    const isBothEmpty = props.value?.start == null && props.value?.end == null;
    const formatPattern = (props.model.mode ?? TimeFieldComponentModelMode.Minute) === TimeFieldComponentModelMode.Second
        ? 'dd.MM.yyyy HH:mm:ss'
        : 'dd.MM.yyyy HH:mm';

    const formattedStart = formatInstantInApplicationTimeZone(props.value?.start, formatPattern);
    const formattedEnd = formatInstantInApplicationTimeZone(props.value?.end, formatPattern);
    const startLabel = formattedStart != null ? `${formattedStart} Uhr` : 'Keine Angabe';
    const endLabel = formattedEnd != null ? `${formattedEnd} Uhr` : 'Keine Angabe';

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
