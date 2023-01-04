import {BaseSummaryProps} from '../_lib/base-summary-props';
import {Grid, Typography} from '@mui/material';
import {TimeFieldElement} from '../../models/elements/form-elements/input-elements/time-field-element';
import {format} from 'date-fns';

export function TimeFieldComponentSummary({model, value}: BaseSummaryProps<TimeFieldElement>) {
    const date = value != null && value.length > 0 && new Date(value);

    return (
        <Grid
            container
            sx={{
                borderBottom: "1px solid #D4D4D4",
                py: 1
            }}
        >
            <Grid
                item
                xs={4}
                sx={{textAlign: "right", pr: 5}}
            >
                <Typography variant={"body2"}>
                    {model.label}
                </Typography>
            </Grid>
            <Grid
                item
                xs={8}
            >
                <Typography variant={"body2"}>
                    {
                        date ? format(date, 'HH:mm') + ' Uhr' : 'Keine Angabe'
                    }
                </Typography>
            </Grid>
        </Grid>
    );
}
