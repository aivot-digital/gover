import {Grid, Typography, useTheme} from '@mui/material';
import {BaseSummaryProps} from './base-summary';
import {MapPointFieldElement, MapPointValue} from '../models/elements/form/input/map-point-field-element';

export function MapPointFieldSummary(props: BaseSummaryProps<MapPointFieldElement, MapPointValue>) {
    const theme = useTheme();

    const hasCoordinates = props.value?.latitude != null && props.value?.longitude != null;
    const hasAddress = props.value?.address != null && props.value.address.length > 0;

    let value = 'Keine Angabe';
    if (hasCoordinates && props.value?.latitude != null && props.value?.longitude != null) {
        value = `${props.value.latitude.toFixed(6)}, ${props.value.longitude.toFixed(6)}`;
    }
    if (hasAddress) {
        value = `${props.value?.address}${hasCoordinates ? ` (${value})` : ''}`;
    }

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
            <Grid size={{xs: 12, md: 8}}>
                <Typography variant="body2">{value}</Typography>
            </Grid>
        </Grid>
    );
}
