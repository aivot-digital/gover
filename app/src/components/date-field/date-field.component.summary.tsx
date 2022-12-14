import {BaseSummaryProps} from '../_lib/base-summary-props';
import {Grid, Typography} from '@mui/material';
import {format} from 'date-fns';
import {DateFieldElement, DateFieldComponentModelMode} from '../../models/elements/form-elements/input-elements/date-field-element';

export function DateFieldComponentSummary({model, value}: BaseSummaryProps<DateFieldElement>) {
    const date = value != null && value.length > 0 && new Date(value);
    let formatting = 'dd.MM.yyyy';
    switch (model.mode) {
        case DateFieldComponentModelMode.Date:
            break;
        case DateFieldComponentModelMode.Month:
            formatting = 'MM.yyyy';
            break;
        case DateFieldComponentModelMode.Year:
            formatting = 'yyyy';
            break;
    }
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
                        date ? format(date, formatting) : 'Keine Angabe'
                    }
                </Typography>
            </Grid>
        </Grid>
    );
}
