import {BaseSummaryProps} from '../_lib/base-summary-props';
import {Grid, Typography} from '@mui/material';
import {SelectFieldElement} from '../../models/elements/./form/./input/select-field-element';
import {stringOrDefault} from '../../utils/string-or-default';

export function SelectFieldComponentSummary({model, value}: BaseSummaryProps<SelectFieldElement>) {
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
                    {stringOrDefault(value, 'Keine Angabe')}
                </Typography>
            </Grid>
        </Grid>
    );
}
