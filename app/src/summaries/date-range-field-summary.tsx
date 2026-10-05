import {Grid, Typography, useTheme} from '@mui/material';
import {BaseSummaryProps} from './base-summary';
import {DateRangeFieldElement, DateRangeValue} from '../models/elements/form/input/date-range-field-element';
import {DateFieldComponentModelMode} from '../models/elements/form/input/date-field-element';
import {dateValueToDateTime} from '../utils/temporal-utils';

export function DateRangeFieldSummary(props: BaseSummaryProps<DateRangeFieldElement, DateRangeValue>) {
    const theme = useTheme();

    const precision = props.model.mode ?? DateFieldComponentModelMode.Day;
    const startDate = props.value?.start != null
        ? dateValueToDateTime(props.value.start, precision)
        : null;
    const endDate = props.value?.end != null
        ? dateValueToDateTime(props.value.end, precision)
        : null;
    const isBothEmpty = startDate == null && endDate == null;
    const format = precision === DateFieldComponentModelMode.Day
        ? 'dd.MM.yyyy'
        : precision === DateFieldComponentModelMode.Month
            ? 'MM.yyyy'
            : 'yyyy';

    const startLabel = startDate?.toFormat(format) ?? 'Keine Angabe';
    const endLabel = endDate?.toFormat(format) ?? 'Keine Angabe';

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
