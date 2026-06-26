import {Grid, List, ListItem, Typography, useTheme} from '@mui/material';
import {type BaseSummaryProps} from '../../summaries/base-summary';
import {type ChipInputFieldElement} from '../../models/elements/form/input/chip-input-field-element';

export function ChipInputFieldSummary(props: BaseSummaryProps<ChipInputFieldElement, string[]>) {
    const theme = useTheme();
    const values = props.value ?? [];

    return (
        <Grid
            container
            sx={{
                borderBottom: '1px solid #D4D4D4',
                py: 1,
            }}
        >
            <Grid
                sx={{
                    textAlign: 'left',
                    pr: 5,
                    [theme.breakpoints.up('md')]: {
                        textAlign: 'right',
                    },
                }}
                size={{
                    xs: 12,
                    md: 4,
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
                size={{
                    xs: 12,
                    md: 8,
                }}
            >
                {
                    values.length > 0 &&
                    <List dense disablePadding>
                        {
                            values.map((item) => (
                                <ListItem
                                    key={item}
                                    disablePadding
                                >
                                    <Typography>{item}</Typography>
                                </ListItem>
                            ))
                        }
                    </List>
                }
                {
                    values.length === 0 &&
                    <Typography variant="body2">Keine Angabe</Typography>
                }
            </Grid>
        </Grid>
    );
}
