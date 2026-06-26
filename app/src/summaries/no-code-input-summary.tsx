import {Grid, Typography, useTheme} from '@mui/material';
import {BaseSummaryProps} from './base-summary';
import {
    NoCodeInputFieldElement,
    NoCodeInputFieldElementItem
} from '../models/elements/form/input/no-code-input-field-element';

function buildSummaryValue(value: NoCodeInputFieldElementItem | null | undefined): string {
    if (value == null) {
        return 'Keine Angabe';
    }

    return value.noCode != null ? 'No-Code-Ausdruck konfiguriert' : 'Keine Angabe';
}

export function NoCodeInputSummary(props: BaseSummaryProps<NoCodeInputFieldElement, NoCodeInputFieldElementItem>) {
    const theme = useTheme();
    const value = buildSummaryValue(props.value);

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
                <Typography variant="body2">
                    {value}
                </Typography>
            </Grid>
        </Grid>
    );
}
