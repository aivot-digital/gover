import {Grid, List, ListItem, Typography} from '@mui/material';
import {MultiCheckboxFieldElement} from '../../models/elements/form/input/multi-checkbox-field-element';
import {BaseSummaryProps} from "../../summaries/base-summary";

export function MultiCheckboxFieldComponentSummary({
                                                       model,
                                                       value,
                                                   }: BaseSummaryProps<MultiCheckboxFieldElement, string[]>) {
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
                {
                    value && Array.isArray(value) && value.length > 0 ?
                        <List
                            dense
                            disablePadding
                        >
                            {
                                value.map(item => (
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
                        </List> :
                        <Typography variant={"body2"}>
                            Keine Angabe
                        </Typography>
                }
            </Grid>
        </Grid>
    );
}
