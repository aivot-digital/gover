import {Grid, Typography, useTheme} from '@mui/material';
import {BaseSummaryProps} from "./base-summary";
import {NumberFieldElement} from "../models/elements/form/input/number-field-element";
import {isStringNullOrEmpty, stringOrDefault} from "../utils/string-utils";
import {formatNumStringToGermanNum} from "../utils/format-german-numbers";
import React from "react";

export function NumberSummary({
                                  model,
                                  value,
                              }: BaseSummaryProps<NumberFieldElement, number>) {
    const theme = useTheme();

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
                xs={12}
                md={4}
                sx={{
                    textAlign: 'left',
                    pr: 5,
                    [theme.breakpoints.up('md')]: {
                        textAlign: 'right',
                    },
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
                    {model.label}
                </Typography>
            </Grid>
            <Grid
                item
                xs={12}
                md={8}
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
