import {Box, Button, Grid, Typography, useTheme} from '@mui/material';
import {useState} from 'react';
import {stringOrDefault} from "../utils/string-utils";
import {TextFieldElement} from "../models/elements/form/input/text-field-element";
import {BaseSummary} from "./base-summary";

export const TextFieldSummary: BaseSummary<TextFieldElement, string> = ({
                                                                            model,
                                                                            value,
                                                                        }) => {
    const theme = useTheme();
    const [expanded, setExpanded] = useState(false);
    const content = stringOrDefault(value, 'Keine Angabe');
    const contentTooLong = content.length > 128;
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
                <Typography
                    variant="body2"
                >
                    {
                        contentTooLong && !expanded ? content.substring(0, 128) + ' …' : content
                    }
                </Typography>
                {
                    contentTooLong &&
                    <Box sx={{mt: 1}}>
                        {
                            expanded ?
                                <Button onClick={() => setExpanded(false)}>
                                    Weniger anzeigen
                                </Button> :
                                <Button onClick={() => setExpanded(true)}>
                                    Text vollständig anzeigen
                                </Button>
                        }
                    </Box>
                }
            </Grid>
        </Grid>
    );
};
