import {Grid, Typography, useTheme} from '@mui/material';
import {format} from 'date-fns';
import {BaseSummaryProps} from "./base-summary";
import {DateFieldComponentModelMode, DateFieldElement} from "../models/elements/form/input/date-field-element";
import {isStringNullOrEmpty} from "../utils/string-utils";
import {formatNumStringToGermanNum} from "../utils/format-german-numbers";
import React from "react";

export function DateFieldSummary({
                                     model,
                                     value,
                                 }: BaseSummaryProps<DateFieldElement, string>) {
    const date = value != null && value.length > 0 && new Date(value);
    let formatting = 'dd.MM.yyyy';
    switch (model.mode) {
        case DateFieldComponentModelMode.Day:
            break;
        case DateFieldComponentModelMode.Month:
            formatting = 'MM.yyyy';
            break;
        case DateFieldComponentModelMode.Year:
            formatting = 'yyyy';
            break;
    }
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
                        date ? format(date, formatting) : 'Keine Angabe'
                    }
                </Typography>
            </Grid>
        </Grid>
    );
}
