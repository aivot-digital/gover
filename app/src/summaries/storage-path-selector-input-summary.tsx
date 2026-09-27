import {Grid, Typography, useTheme} from '@mui/material';
import {type BaseSummaryProps} from './base-summary';
import {
    type StoragePathSelectorInputElement,
    type StoragePathSelectorInputElementValue,
} from '../models/elements/form/input/storage-path-selector-input-element';

function buildSummaryValue(value: StoragePathSelectorInputElementValue | null | undefined): string {
    if (value?.storageProviderId == null || value.path == null || value.path.trim().length === 0) {
        return 'Keine Angabe';
    }

    return `Speicheranbieter #${value.storageProviderId}: ${value.path}`;
}

export function StoragePathSelectorInputSummary(
    props: BaseSummaryProps<StoragePathSelectorInputElement, StoragePathSelectorInputElementValue>,
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
                size={{xs: 12, md: 4}}
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

            <Grid size={{xs: 12, md: 8}}>
                <Typography variant="body2">
                    {value}
                </Typography>
            </Grid>
        </Grid>
    );
}
