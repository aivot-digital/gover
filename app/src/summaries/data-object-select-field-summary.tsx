import {Grid, Typography, useTheme} from '@mui/material';
import {BaseSummaryProps} from './base-summary';
import {DataObjectSelectFieldElement} from '../models/elements/form/input/data-object-select-field-element';
import {useEffect, useMemo, useState} from 'react';
import {
    formatDataObjectSelectValue,
    loadDataObjectSelectOptions,
    normalizeDataLabelAttributeKey,
    normalizeDataObjectId,
} from '../components/data-object-select-field/data-object-select-options';
import {normalizeDataModelKey} from '../components/data-model-select-field/data-model-select-options';

export function DataObjectSelectFieldSummary(props: BaseSummaryProps<DataObjectSelectFieldElement, string>) {
    const theme = useTheme();
    const normalizedValue = normalizeDataObjectId(props.value);
    const normalizedDataModelKey = normalizeDataModelKey(props.model.dataModelKey);
    const normalizedDataLabelAttributeKey = normalizeDataLabelAttributeKey(props.model.dataLabelAttributeKey);
    const [labelLookup, setLabelLookup] = useState<Map<string, string>>(new Map());

    useEffect(() => {
        if (normalizedValue == null || normalizedDataModelKey == null) {
            setLabelLookup(new Map());
            return;
        }

        let isMounted = true;

        loadDataObjectSelectOptions(normalizedDataModelKey, normalizedDataLabelAttributeKey)
            .then((options) => {
                if (!isMounted) {
                    return;
                }

                setLabelLookup(new Map(options.map((option) => [option.key, option.label])));
            })
            .catch(() => {
                if (!isMounted) {
                    return;
                }

                setLabelLookup(new Map());
            });

        return () => {
            isMounted = false;
        };
    }, [normalizedDataLabelAttributeKey, normalizedDataModelKey, normalizedValue]);

    const value = useMemo(() => {
        if (normalizedValue == null) {
            return 'Keine Angabe';
        }

        return labelLookup.get(normalizedValue) ?? formatDataObjectSelectValue(normalizedValue);
    }, [labelLookup, normalizedValue]);

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
