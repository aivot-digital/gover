import {Grid, Typography} from '@mui/material';
import {BaseSummaryProps} from "./base-summary";
import {NumberFieldElement} from "../models/elements/form/input/number-field-element";
import {isStringNullOrEmpty} from "../utils/string-utils";
import {formatNumStringToGermanNum} from "../utils/format-german-numbers";

export function NumberSummary({
                                  model,
                                  value,
                              }: BaseSummaryProps<NumberFieldElement, number>) {
    return (
        <Grid
            container
            sx={{
                borderBottom: "1px solid #D4D4D4",
                py: 1,
            }}
        >
            <Grid
                item
                xs={4}
                sx={{
                    textAlign: "right",
                    pr: 5,
                }}
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
                        (value == null || (typeof value === 'string' && isStringNullOrEmpty(value))) ?
                            'Keine Angabe' :
                            formatNumStringToGermanNum(value, model.decimalPlaces) + (!isStringNullOrEmpty(model.suffix) ? ' ' + model.suffix : '')
                    }
                </Typography>
            </Grid>
        </Grid>
    );
}
