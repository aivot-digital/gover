import {Grid, Typography} from '@mui/material';
import {HeadlineElement} from '../../models/elements/form/content/headline-element';
import {BaseSummaryProps} from "../../summaries/base-summary";

export function HeadlineComponentSummary({model}: BaseSummaryProps<HeadlineElement, any>) {
    return (

        <Grid
            container
            sx={{
                mt: 2,
                borderBottom: "1px solid #D4D4D4",
                py: 1,
            }}
        >
            <Grid
                item
                md={4}
                sx={{
                    textAlign: "right",
                    pr: 5,
                }}
            >
                <Typography
                    variant="body2"
                    sx={{
                        fontWeight: 'bold',
                    }}
                >
                    {
                        /* TODO: Overthink if small mode is really relevant here
                        model.small ?
                            <small>{model.content}</small> :*/
                        model.content
                    }
                </Typography>
            </Grid>
        </Grid>
    );
}
