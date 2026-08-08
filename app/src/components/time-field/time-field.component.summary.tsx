import {Grid, Typography} from '@mui/material';
import {TimeFieldElement} from '../../models/elements/form/input/time-field-element';
import {BaseSummaryProps} from '../../summaries/base-summary';
import {TimeFieldComponentModelMode} from '../../models/elements/form/input/time-field-element';
import {localTimeIsoToDateTime} from '../../utils/temporal-utils';

export function TimeFieldComponentSummary({
                                              model,
                                              value,
                                          }: BaseSummaryProps<TimeFieldElement, string>) {
    const time = value != null && value.length > 0
        ? localTimeIsoToDateTime(value)
        : null;

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
                    textAlign: "right",
                    pr: 5,
                }}
                size={4}>
                <Typography variant={"body2"}>
                    {model.label}
                </Typography>
            </Grid>
            <Grid size={8}>
                <Typography variant={"body2"}>
                    {
                        time != null
                            ? `${time.toFormat((model.mode ?? TimeFieldComponentModelMode.Minute) === TimeFieldComponentModelMode.Second ? 'HH:mm:ss' : 'HH:mm')} Uhr`
                            : 'Keine Angabe'
                    }
                </Typography>
            </Grid>
        </Grid>
    );
}
