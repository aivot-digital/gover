import {Grid, List, ListItem, Typography, useTheme} from '@mui/material';
import {BaseSummaryProps} from './base-summary';
import {
    DomainAndUserSelectItem,
    DomainUserSelectFieldElement,
} from '../models/elements/form/input/domain-user-select-field-element';
import {useEffect, useMemo, useState} from 'react';
import {
    createDomainAndUserSelectValueKey,
    formatDomainAndUserSelectValue,
    loadDomainAndUserSelectOptions,
    normalizeDomainAndUserSelectItem,
} from '../components/domain-user-select-field/domain-user-select-options';

export function DomainUserSelectFieldSummary(props: BaseSummaryProps<DomainUserSelectFieldElement, DomainAndUserSelectItem[]>) {
    const theme = useTheme();
    const values = props.value ?? [];
    const [labelLookup, setLabelLookup] = useState<Map<string, string>>(new Map());

    useEffect(() => {
        if (values.length === 0) {
            setLabelLookup(new Map());
            return;
        }

        let isMounted = true;

        loadDomainAndUserSelectOptions()
            .then((options) => {
                if (!isMounted) {
                    return;
                }

                const lookup = new Map(options.map((option) => [option.key, option.label]));
                setLabelLookup(lookup);
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
    }, [values.length]);

    const displayedValues = useMemo(() => {
        return values.map((entry) => {
            const normalizedValue = normalizeDomainAndUserSelectItem(entry);
            if (normalizedValue == null) {
                return {
                    key: JSON.stringify(entry),
                    label: 'Unbekannter Eintrag',
                };
            }

            const key = createDomainAndUserSelectValueKey(normalizedValue);
            return {
                key,
                label: labelLookup.get(key) ?? formatDomainAndUserSelectValue(normalizedValue),
            };
        });
    }, [labelLookup, values]);

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
                    displayedValues.length > 0 &&
                    <List dense disablePadding>
                        {
                            displayedValues.map((item) => (
                                <ListItem
                                    key={item.key}
                                    disablePadding
                                >
                                    <Typography>{item.label}</Typography>
                                </ListItem>
                            ))
                        }
                    </List>
                }
                {
                    displayedValues.length === 0 &&
                    <Typography variant="body2">Keine Angabe</Typography>
                }
            </Grid>
        </Grid>
    );
}
