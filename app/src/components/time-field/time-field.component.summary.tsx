import {Grid, Typography} from '@mui/material';
import {TimeFieldElement} from '../../models/elements/form/input/time-field-element';
import {format} from 'date-fns';
import {BaseSummaryProps} from '../../summaries/base-summary';
import {TimeFieldComponentModelMode} from '../../models/elements/form/input/time-field-element';

export function TimeFieldComponentSummary({
                                              model,
                                              value,
                                          }: BaseSummaryProps<TimeFieldElement, string>) {
    const date = value != null && value.length > 0 && new Date(value);

    return (
        <Grid
            container
            sx={{
                borderBottom: "1px solid #D4D4D4",
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
                        date ? format(date, (model.mode ?? TimeFieldComponentModelMode.Minute) === TimeFieldComponentModelMode.Second ? 'HH:mm:ss' : 'HH:mm') + ' Uhr' : 'Keine Angabe'
                    }
                </Typography>
            </Grid>
        </Grid>
    );
}
