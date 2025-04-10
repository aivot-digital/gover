import React, {useEffect, useState} from 'react';
import {useApi} from '../../../../hooks/use-api';
import {selectUser} from '../../../../slices/user-slice';
import {Box, Divider, FormControlLabel, Grid, Skeleton, Switch, Typography} from "@mui/material";
import {useAppDispatch} from "../../../../hooks/use-app-dispatch";
import {useAppSelector} from "../../../../hooks/use-app-selector";
import {UserConfigDefinition, UserConfigsApiService} from "../../../../services/user-configs-api-service";
import {showErrorSnackbar, showSuccessSnackbar} from "../../../../slices/snackbar-slice";

export function AccountDetailsPageNotifications() {
    const dispatch = useAppDispatch();
    const user = useAppSelector(selectUser);
    const api = useApi();
    const [isBusy, setIsBusy] = useState(false);
    const [definitions, setDefinitions] = useState<UserConfigDefinition[]>([]);
    const [userConfigs, setUserConfigs] = useState<Record<string, string[]>>({});

    useEffect(() => {
        const userConfigsApiService = new UserConfigsApiService(api);
        setIsBusy(true);

        Promise.all([
            userConfigsApiService.listDefinitions(0, 999),
            userConfigsApiService.list(user?.id ?? 'self', 0, 999)
        ])
            .then(([definitionsRes, userConfigsRes]) => {
                const filteredDefinitions = definitionsRes.content.filter(def => def.category === "Benachrichtigungen");
                setDefinitions(filteredDefinitions);

                const configMap: Record<string, string[]> = {};
                userConfigsRes.content.forEach(cfg => {
                    configMap[cfg.key] = Array.isArray(cfg.value) ? cfg.value : [cfg.value];
                });
                setUserConfigs(configMap);
            })
            .catch(err => {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Abrufen der Benutzereinstellungen.'));
            })
            .finally(() => setIsBusy(false));
    }, [user]);

    const handleChange = (key: string, channel: string, label: string, channelLabel: string) => {
        const currentValues = userConfigs[key] || [];
        const newValues = currentValues.includes(channel)
            ? currentValues.filter(v => v !== channel)
            : [...currentValues, channel];

        setUserConfigs(prev => ({ ...prev, [key]: newValues }));

        const userConfigsApiService = new UserConfigsApiService(api);
        userConfigsApiService.save(user?.id ?? 'self', key, newValues)
            .then(() => {
                const action = newValues.includes(channel) ? 'aktiviert' : 'deaktiviert';
                dispatch(showSuccessSnackbar(`Benachrichtigung "${label}" (${channelLabel}) ${action}.`));
            })
            .catch(() => {
                dispatch(showErrorSnackbar(`Fehler beim Speichern der Benachrichtigungseinstellung: "${label}".`));
            });
    };

    const groupedDefinitions = definitions.reduce<Record<string, UserConfigDefinition[]>>((acc, def) => {
        if (!acc[def.subCategory]) {
            acc[def.subCategory] = [];
        }
        acc[def.subCategory].push(def);
        return acc;
    }, {});

    return (
        <Box sx={{pt: 2}}>
            <Typography
                variant="h5"
                sx={{mb: 1}}
            >
                Benachrichtigungen
            </Typography>
            <Typography sx={{ mb: 2, maxWidth: 900 }}>
                Als Mitarbeiter:in und im Rahmen der Mitgliedschaft in Fachbereichen erhalten Sie Benachrichtigungen zu wichtigen Ereignissen und Aktivitäten in der Anwendung.
                Sie können den Erhalt dieser Benachrichtigungen individuell nach Ihren Bedürfnissen anpassen.
            </Typography>
            <Typography sx={{ mb: 4, maxWidth: 900 }}>
                <b>Bitte beachten Sie:</b> Wenn eine Benachrichtigung an eine Fachbereichs-Mitgliedschaft gebunden ist, kann es vorkommen, dass Sie trotz aktivierter Benachrichtigungen keine E-Mail erhalten.
                Dies ist der Fall, wenn für den betreffenden Fachbereich eine zentrale E-Mail-Adresse für Systembenachrichtigungen hinterlegt wurde.
            </Typography>

            {isBusy ? (
                <Grid item xs={12}>
                    <Typography variant="h6" sx={{ mt: 2, mb: 1 }}>Optionen werden geladen...</Typography>
                    <Box sx={{ borderRadius: 1, overflow: 'hidden', border: '1px solid rgba(0, 0, 0, 0.12)' }}>
                        {[...Array(3)].map((_, index) => (
                            <Box key={index} sx={{ px: 2, py: 1 }}>
                                <Grid container alignItems="center" spacing={2}>
                                    <Grid item xs={8}>
                                        <Skeleton variant="text" width="60%" height={30} />
                                        <Skeleton variant="text" width="80%" height={20} sx={{ mt: 1, mb: 2 }} />
                                    </Grid>
                                    <Grid item xs={4} sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                                        <Skeleton variant="rectangular" width="20%" height={40} />
                                    </Grid>
                                </Grid>
                                {index < 2 && <Divider sx={{ mt: 2 }} />}
                            </Box>
                        ))}
                    </Box>
                </Grid>
            ) : (
                Object.entries(groupedDefinitions).map(([subCategory, items]) => (
                    <Grid item xs={12} key={subCategory}>
                        <Typography variant="h6" sx={{ mt: 2, mb: 1.5 }}>{subCategory}</Typography>
                        <Box sx={{ borderRadius: 1, overflow: 'hidden', border: '1px solid rgba(0, 0, 0, 0.12)', mb: 3 }}>
                            {items.map(({ key, label, description, options }, index) => (
                                <Box key={key}>
                                    <Grid container alignItems="center" spacing={2} sx={{ px: 2, pt: 1, pb: 1.5 }}>
                                        <Grid item xs={8}>
                                            <Typography variant="subtitle1">{label}</Typography>
                                            <Typography variant="body2" color="text.secondary">{description}</Typography>
                                        </Grid>
                                        <Grid item xs={4} sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                                            {options.map(({ label: optionLabel, value }) => (
                                                // Do not show "In-App" option until it's correctly implemented
                                                optionLabel != "In-App" &&
                                                <FormControlLabel
                                                    key={value}
                                                    control={
                                                        <Switch
                                                            checked={userConfigs[key]?.includes(value) || false}
                                                            onChange={() => handleChange(key, value, label, optionLabel)}
                                                        />
                                                    }
                                                    label={optionLabel}
                                                    sx={{ml: 1}}
                                                />
                                            ))}
                                        </Grid>
                                    </Grid>
                                    {index < items.length - 1 && <Divider />}
                                </Box>
                            ))}
                        </Box>
                    </Grid>
                ))
            )}

            <Typography sx={{ mt: 3, mb: 1, fontSize: "0.875rem", px: 2, color: "text.secondary" }}>
                *¹ Benachrichtigt wird nur der ranghöchste im Formular konfigurierte Fachbereich in der Reihenfolge „bewirtschaftend → zuständig → entwickelnd“.
            </Typography>
        </Box>
    );
}
