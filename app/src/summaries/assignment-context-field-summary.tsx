import {Grid, List, ListItem, Typography, useTheme} from '@mui/material';
import {BaseSummaryProps} from './base-summary';
import {
    AssignmentContextFieldElement,
    AssignmentContextValue,
} from '../models/elements/form/input/assignment-context-field-element';
import {useEffect, useMemo, useState} from 'react';
import {
    createDomainAndUserSelectValueKey,
    formatDomainAndUserSelectValue,
    loadDomainAndUserSelectOptions,
    normalizeDomainAndUserSelectItem,
} from '../components/domain-user-select-field/domain-user-select-options';

export function AssignmentContextFieldSummary(props: BaseSummaryProps<AssignmentContextFieldElement, AssignmentContextValue>) {
    const theme = useTheme();
    const [labelLookup, setLabelLookup] = useState<Map<string, string>>(new Map());

    const selectedValues = props.value?.domainAndUserSelection ?? [];

    useEffect(() => {
        if (selectedValues.length === 0) {
            setLabelLookup(new Map());
            return;
        }

        let isMounted = true;

        loadDomainAndUserSelectOptions()
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
    }, [selectedValues.length]);

    const displayedValues = useMemo(() => {
        return selectedValues.map((entry) => {
            const normalized = normalizeDomainAndUserSelectItem(entry);
            if (normalized == null) {
                return {
                    key: JSON.stringify(entry),
                    label: 'Unbekannter Eintrag',
                };
            }

            const key = createDomainAndUserSelectValueKey(normalized);
            return {
                key,
                label: labelLookup.get(key) ?? formatDomainAndUserSelectValue(normalized),
            };
        });
    }, [labelLookup, selectedValues]);

    const enabledPreferences = useMemo(() => {
        const preferences: string[] = [];

        if (props.value?.preferPreviousTaskAssignee === true) {
            preferences.push('Bevorzuge Bearbeiter:in vorheriger Aufgabe');
        }

        if (props.value?.preferUninvolvedUser === true) {
            preferences.push('Bevorzuge eine neue, unbeteiligte Mitarbeiter:in');
        }

        if (props.value?.preferProcessInstanceAssignee === true) {
            preferences.push('Bevorzuge dem Vorgang zugewiesene Mitarbeiter:in');
        }

        return preferences;
    }, [props.value?.preferPreviousTaskAssignee, props.value?.preferProcessInstanceAssignee, props.value?.preferUninvolvedUser]);

    const isEmpty = displayedValues.length === 0 && enabledPreferences.length === 0;

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
                    !isEmpty &&
                    <List dense disablePadding>
                        {
                            displayedValues.map((item) => (
                                <ListItem key={item.key} disablePadding>
                                    <Typography>{item.label}</Typography>
                                </ListItem>
                            ))
                        }
                        {
                            enabledPreferences.map((preference) => (
                                <ListItem key={preference} disablePadding>
                                    <Typography>{preference}</Typography>
                                </ListItem>
                            ))
                        }
                    </List>
                }
                {
                    isEmpty &&
                    <Typography variant="body2">Keine Angabe</Typography>
                }
            </Grid>
        </Grid>
    );
}
