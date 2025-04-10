import React from 'react';
import {Grid, List, ListItem, Typography, useTheme} from '@mui/material';
import {type MultiCheckboxFieldElement} from '../../models/elements/form/input/multi-checkbox-field-element';
import {type BaseSummaryProps} from '../../summaries/base-summary';
import {stringOrDefault} from "../../utils/string-utils";

export function MultiCheckboxFieldComponentSummary(props: BaseSummaryProps<MultiCheckboxFieldElement, string[]>): JSX.Element {
    const options = (props.model.options ?? []).map((option) => {
        if (typeof option === 'string') {
            return {
                label: option,
                value: option,
            };
        } else {
            return option;
        }
    });
    const values = (props.value ?? []).map((value) => {
        const option = options.find((option) => option.value === value);
        return option?.label ?? value;
    });
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
                    {props.model.label}
                </Typography>
            </Grid>
            <Grid
                item
                xs={12}
                md={8}
            >
                {
                    values.length > 0 &&
                    <List
                        dense
                        disablePadding
                    >
                        {
                            values.map((item) => (
                                <ListItem
                                    key={item}
                                    disablePadding
                                >
                                    <Typography>
                                        {item}
                                    </Typography>
                                </ListItem>
                            ))
                        }
                    </List>
                }
                {
                    values.length === 0 &&
                    <Typography variant={'body2'}>
                        Keine Angabe
                    </Typography>
                }
            </Grid>
        </Grid>
    );
}
