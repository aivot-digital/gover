import {BaseSummaryProps} from '../_lib/base-summary-props';
import {Grid, Typography} from '@mui/material';
import {NumberFieldElement} from '../../models/elements/./form/./input/number-field-element';
import {formatNumStringToGermanNum} from '../../utils/format-german-numbers';
import {isNullOrEmpty} from '../../utils/is-null-or-empty';

export function NumberComponentSummary({model, value}: BaseSummaryProps<NumberFieldElement>) {
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
                        isNullOrEmpty(value) ?
                            'Keine Angabe' :
                            formatNumStringToGermanNum(value, model.decimalPlaces) + (!isNullOrEmpty(model.suffix) ? ' ' + model.suffix : '')
                    }
                </Typography>
            </Grid>
        </Grid>
    );
}
