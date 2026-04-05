import {Grid, Typography, useTheme} from '@mui/material';
import {BaseSummaryProps} from './base-summary';
import {
    UiDefinitionInputFieldElement,
    UiDefinitionInputFieldElementItem
} from '../models/elements/form/input/ui-definition-input-field-element';
import {flattenElements} from '../utils/flatten-elements';
import {generateComponentTitle} from '../utils/generate-component-title';
import {getElementNameForType} from '../data/element-type/element-names';

function buildSummaryValue(value: UiDefinitionInputFieldElementItem | null | undefined): string {
    if (value == null) {
        return 'Keine Angabe';
    }

    const elementCount = flattenElements(value).length;
    const rootTypeLabel = getElementNameForType(value.type);
    const rootTitle = generateComponentTitle(value);
    const countLabel = `${elementCount} Element${elementCount === 1 ? '' : 'e'}`;

    if (rootTitle === rootTypeLabel) {
        return `${countLabel}, Wurzel: ${rootTypeLabel}`;
    }

    return `${countLabel}, Start: ${rootTitle}`;
}

export function UiDefinitionInputSummary(
    props: BaseSummaryProps<UiDefinitionInputFieldElement, UiDefinitionInputFieldElementItem>
) {
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
