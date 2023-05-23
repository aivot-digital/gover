import {Grid, Typography} from '@mui/material';
import {BaseSummary} from "./base-summary";
import {CheckboxFieldElement} from "../models/elements/form/input/checkbox-field-element";

export const CheckboxFieldSummary: BaseSummary<CheckboxFieldElement, boolean> = ({model, value}) => {
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
                        value ? 'Ja' : 'Nein'
                    }
                </Typography>
            </Grid>
        </Grid>
    );
}
