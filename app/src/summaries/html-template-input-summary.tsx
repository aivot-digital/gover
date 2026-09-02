import {Grid, Typography, useTheme} from '@mui/material';
import {BaseSummaryProps} from './base-summary';
import {
    HtmlTemplateInputElement,
    HtmlTemplateInputValue,
} from '../models/elements/form/input/html-template-input-element';

function buildSummaryValue(value: HtmlTemplateInputValue | null | undefined): string {
    if (value?.assetKey == null || value.assetKey.trim().length === 0) {
        return 'Keine Angabe';
    }

    const filledSlotCount = Object
        .values(value.slots ?? {})
        .filter((slotValue) => slotValue != null && slotValue.trim().length > 0)
        .length;

    return `${filledSlotCount} Slot${filledSlotCount === 1 ? '' : 's'} ausgefüllt`;
}

export function HtmlTemplateInputSummary(
    props: BaseSummaryProps<HtmlTemplateInputElement, HtmlTemplateInputValue>
) {
    const theme = useTheme();
    const value = buildSummaryValue(props.value);

    return (
        <Grid
            container
            sx={{
                borderBottom: '1px solid',
                borderBottomColor: 'divider',
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
