import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    Divider,
    Grid,
    Paper,
    Typography,
} from '@mui/material';
import {useCallback, useEffect, useMemo, useState} from 'react';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {GenericPageHeader} from '../../../components/generic-page-header/generic-page-header';
import Add from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import Edit from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import ArrowUpward from '@aivot/mui-material-symbols-400-n25-outlined/ArrowUpward';
import ArrowDownward from '@aivot/mui-material-symbols-400-n25-outlined/ArrowDownward';
import OpenInNew from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import ArrowBack from '@aivot/mui-material-symbols-400-n25-outlined/ArrowBack';
import QueryStats from '@aivot/mui-material-symbols-400-n25-outlined/QueryStats';
import Save from '@aivot/mui-material-symbols-400-n25-outlined/Save';
import {IconButton} from '../../../components/icon-button/icon-button';
import {useApi} from '../../../hooks/use-api';
import {CustomLinksApiService} from '../../custom-links/custom-links-api-service';
import {type CustomLink, type CustomLinkRequest, CustomLinkType} from '../../custom-links/models/custom-link';
import {CustomLinkDialog} from '../../custom-links/dialogs/custom-link-dialog';
import {getCustomLinkIcon} from '../../custom-links/data/custom-link-icons';
import {SystemConfigsApiService} from '../../configs/system-configs-api-service';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {Permission} from '../../../data/permissions/permission';
import {useHasSystemPermission, useRequireSystemPermission} from '../../permissions/hooks/use-permissions';
import {useConfirm} from '../../../providers/confirm-provider';
import Balancer from 'react-wrap-balancer';
import {SystemConfigKeys} from '../../../data/system-config-keys';
import {CheckboxFieldComponent} from '../../../components/checkbox-field/checkbox-field-component';
import {SelectFieldComponent} from '../../../components/select-field/select-field-component';
import {setSystemConfigs} from '../../../slices/system-config-slice';
import {type SystemConfigResponseDto} from '../../configs/dtos/system-config-response-dto';
import {DashboardActivityPeriodConfig} from '../models/dashboard-overview';
import {ModuleIcons} from '../../../shells/staff/data/module-icons';

interface DashboardActivitySettings {
    enabled: boolean;
    period: DashboardActivityPeriodConfig;
}

export function DashboardSettingsPage() {
    useRequireSystemPermission(Permission.SYSTEM_CONFIG_READ);
    const api = useApi();
    const dispatch = useAppDispatch();
    const confirm = useConfirm();
    const customLinksService = useMemo(() => new CustomLinksApiService(api), [api]);
    const systemConfigService = useMemo(() => new SystemConfigsApiService(api), [api]);
    const [links, setLinks] = useState<CustomLink[] | null>(null);
    const [linksLoadFailed, setLinksLoadFailed] = useState(false);
    const [editedLink, setEditedLink] = useState<CustomLink | null | undefined>(undefined);
    const [isBusy, setIsBusy] = useState(false);
    const [activitySettings, setActivitySettings] = useState<DashboardActivitySettings | null>(null);
    const [savedActivitySettings, setSavedActivitySettings] = useState<DashboardActivitySettings | null>(null);
    const [activitySettingsLoadFailed, setActivitySettingsLoadFailed] = useState(false);
    const [activitySettingsBusy, setActivitySettingsBusy] = useState(false);
    const canCreate = useHasSystemPermission(Permission.SYSTEM_CONFIG_CREATE);
    const canUpdate = useHasSystemPermission(Permission.SYSTEM_CONFIG_UPDATE);
    const canDelete = useHasSystemPermission(Permission.SYSTEM_CONFIG_DELETE);

    const loadLinks = useCallback(() => {
        setLinksLoadFailed(false);
        customLinksService.list(CustomLinkType.Dashboard)
            .then((page) => setLinks(page.content))
            .catch((error) => {
                setLinksLoadFailed(true);
                dispatch(showApiErrorSnackbar(error, 'Links konnten nicht geladen werden.'));
            });
    }, [customLinksService, dispatch]);

    useEffect(loadLinks, [loadLinks]);

    const loadActivitySettings = useCallback(() => {
        setActivitySettingsLoadFailed(false);
        setActivitySettings(null);
        setSavedActivitySettings(null);
        systemConfigService.listAll()
            .then(({content}) => {
                const enabledConfig = content.find(({key}) => key === SystemConfigKeys.dashboard.activity.enabled);
                const periodConfig = content.find(({key}) => key === SystemConfigKeys.dashboard.activity.period);
                dispatch(setSystemConfigs([enabledConfig, periodConfig].filter(
                    (config): config is SystemConfigResponseDto => config != null,
                )));
                const settings: DashboardActivitySettings = {
                    enabled: String(enabledConfig?.value ?? 'true').toLowerCase() === 'true',
                    period: String(periodConfig?.value) === DashboardActivityPeriodConfig.ThirtyDays
                        ? DashboardActivityPeriodConfig.ThirtyDays
                        : DashboardActivityPeriodConfig.ThreeMonths,
                };
                setActivitySettings(settings);
                setSavedActivitySettings(settings);
            })
            .catch((error) => {
                setActivitySettingsLoadFailed(true);
                dispatch(showApiErrorSnackbar(error, 'Einstellungen der Übersicht konnten nicht geladen werden.'));
            });
    }, [dispatch, systemConfigService]);

    useEffect(loadActivitySettings, [loadActivitySettings]);

    const activitySettingsChanged = activitySettings != null && savedActivitySettings != null && (
        activitySettings.enabled !== savedActivitySettings.enabled ||
        activitySettings.period !== savedActivitySettings.period
    );

    const saveActivitySettings = () => {
        if (activitySettings == null || savedActivitySettings == null || !activitySettingsChanged) return;
        setActivitySettingsBusy(true);
        const updates: Promise<SystemConfigResponseDto>[] = [];
        if (activitySettings.enabled !== savedActivitySettings.enabled) {
            updates.push(systemConfigService.update(SystemConfigKeys.dashboard.activity.enabled, {
                value: String(activitySettings.enabled),
            }));
        }
        if (activitySettings.period !== savedActivitySettings.period) {
            updates.push(systemConfigService.update(SystemConfigKeys.dashboard.activity.period, {
                value: activitySettings.period,
            }));
        }

        Promise.all(updates)
            .then((updatedConfigs) => {
                dispatch(setSystemConfigs(updatedConfigs));
                setSavedActivitySettings(activitySettings);
                dispatch(showSuccessSnackbar('Einstellungen der Übersicht wurden gespeichert.'));
            })
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Einstellungen der Übersicht konnten nicht vollständig gespeichert werden.'));
                // Multiple system configs cannot be updated atomically through the generic API; reload their actual state.
                loadActivitySettings();
            })
            .finally(() => setActivitySettingsBusy(false));
    };

    const handleSave = (request: CustomLinkRequest) => {
        setIsBusy(true);
        const operation = editedLink == null
            ? customLinksService.create(request)
            : customLinksService.update(editedLink.id, request);
        operation
            .then(() => {
                dispatch(showSuccessSnackbar(editedLink == null ? 'Link wurde hinzugefügt.' : 'Link wurde aktualisiert.'));
                setEditedLink(undefined);
                loadLinks();
            })
            .catch((error) => dispatch(showApiErrorSnackbar(error, 'Link konnte nicht gespeichert werden.')))
            .finally(() => setIsBusy(false));
    };

    const handleDelete = async (link: CustomLink) => {
        const confirmed = await confirm({
            title: 'Link löschen',
            isDestructive: true,
            children: <Typography>Möchten Sie den Link „{link.label}“ wirklich löschen?</Typography>,
        });
        if (!confirmed) return;
        setIsBusy(true);
        customLinksService.destroy(link.id)
            .then(() => {
                dispatch(showSuccessSnackbar('Link wurde gelöscht.'));
                loadLinks();
            })
            .catch((error) => dispatch(showApiErrorSnackbar(error, 'Link konnte nicht gelöscht werden.')))
            .finally(() => setIsBusy(false));
    };

    const moveLink = (index: number, direction: -1 | 1) => {
        if (links == null) return;
        const targetIndex = index + direction;
        if (targetIndex < 0 || targetIndex >= links.length) return;
        const reordered = [...links];
        [reordered[index], reordered[targetIndex]] = [reordered[targetIndex], reordered[index]];
        setLinks(reordered);
        setIsBusy(true);
        customLinksService.reorder(CustomLinkType.Dashboard, reordered.map((link) => link.id))
            .then(setLinks)
            .catch((error) => {
                setLinks(links);
                dispatch(showApiErrorSnackbar(error, 'Reihenfolge konnte nicht gespeichert werden.'));
            })
            .finally(() => setIsBusy(false));
    };

    return (
        <PageWrapper title="Übersicht konfigurieren" background>
            <GenericPageHeader
                icon={ModuleIcons.dashboardSettings}
                title="Übersicht konfigurieren"
                actions={[
                    {
                        label: 'Zur Übersicht',
                        icon: <ArrowBack/>,
                        iconPosition: 'start',
                        variant: 'text',
                        to: '/',
                    },
                ]}
            />

            <Paper>
                <Box sx={{mt: 2.75, p: 2}}>
                    <Typography variant="subtitle1" component="h2">
                        Vorgangsaktivität
                    </Typography>
                    <Typography sx={{maxWidth: 900, mb: 1.6}}>
                        Legen Sie fest, ob die Übersicht eine zusammengefasste Auswertung gestarteter und abgeschlossener Vorgänge zeigt und welcher Zeitraum ausgewertet wird. Sie können die Auswertung bewusst ausblenden, wenn Kennzahlen im Arbeitsalltag als unerwünschter Leistungsdruck wahrgenommen werden.
                    </Typography>

                    {activitySettings == null && !activitySettingsLoadFailed && (
                        <Box sx={{display: 'grid', placeItems: 'center', minHeight: 140, maxWidth: 900}}>
                            <CircularProgress size={28}/>
                        </Box>
                    )}
                    {activitySettingsLoadFailed && (
                        <Alert severity="error" sx={{mb: 2, maxWidth: 900}}>
                            Die Einstellungen der Übersicht konnten nicht geladen werden. Laden Sie die Seite erneut, um Änderungen vorzunehmen.
                        </Alert>
                    )}
                    {activitySettings != null && (
                        <Grid container columnSpacing={4}>
                            <Grid size={{xs: 12, lg: 6}}>
                                <CheckboxFieldComponent
                                    variant="switch"
                                    label="Vorgangsaktivität auf der Übersicht anzeigen"
                                    hint="Wenn die Auswertung deaktiviert ist, wird sie für alle Mitarbeiter:innen ausgeblendet."
                                    value={activitySettings.enabled}
                                    onChange={(enabled) => setActivitySettings((current) => current == null ? current : {...current, enabled})}
                                    disabled={!canUpdate || activitySettingsBusy}
                                />

                                <Box sx={{mt: 2.5}}>
                                    <SelectFieldComponent
                                        label="Auswertungszeitraum"
                                        value={activitySettings.period}
                                        onChange={(period) => setActivitySettings((current) => current == null || period == null ? current : {
                                            ...current,
                                            period: period as DashboardActivityPeriodConfig,
                                        })}
                                        options={[
                                            {value: DashboardActivityPeriodConfig.ThirtyDays, label: 'Letzte 30 Tage'},
                                            {value: DashboardActivityPeriodConfig.ThreeMonths, label: 'Letzte 3 Monate'},
                                        ]}
                                        includeEmptyOption={false}
                                        hint="Kurze Zeiträume werden täglich, längere Zeiträume wöchentlich zusammengefasst."
                                        disabled={!canUpdate || activitySettingsBusy || !activitySettings.enabled}
                                        startIcon={<QueryStats/>}
                                    />
                                </Box>

                                <Button
                                    variant="contained"
                                    startIcon={<Save/>}
                                    onClick={saveActivitySettings}
                                    disabled={!canUpdate || activitySettingsBusy || !activitySettingsChanged}
                                    sx={{mt: 2.5}}
                                >
                                    Einstellungen speichern
                                </Button>
                            </Grid>
                        </Grid>
                    )}

                    <Typography variant="subtitle1" component="h2" sx={{mt: 4}}>
                        Relevante Links
                    </Typography>
                    <Typography sx={{maxWidth: 900, mb: 1.6}}>
                        Stellen Sie Mitarbeiter:innen Links zu internen Leitfäden, zum Intranet, zu einer Statusseite oder zu häufig genutzten Diensten bereit. Aktivierte Links erscheinen auf der Übersicht.
                    </Typography>
                    <Box sx={{mt: 2, border: '1px solid', borderColor: 'divider', borderRadius: 1, overflow: 'hidden'}}>
                        {links == null && !linksLoadFailed && (
                            <Box sx={{display: 'grid', placeItems: 'center', minHeight: 160}}>
                                <CircularProgress size={28}/>
                            </Box>
                        )}
                        {linksLoadFailed && (
                            <Alert
                                severity="error"
                                action={(
                                    <Button color="inherit" size="small" onClick={loadLinks}>
                                        Erneut versuchen
                                    </Button>
                                )}
                                sx={{m: 2}}
                            >
                                Die Links konnten nicht geladen werden.
                            </Alert>
                        )}
                        {links?.length === 0 && (
                            <Box sx={{px: 3, py: 4, textAlign: 'center'}}>
                                <Typography sx={{
                                    fontWeight: 600
                                }}>Noch keine Links eingerichtet</Typography>
                                <Typography
                                    sx={{
                                        color: "text.secondary",
                                        mt: 0.5,
                                        mx: 'auto',
                                        maxWidth: 680
                                    }}>
                                    <Balancer>
                                        Fügen Sie einen Link hinzu, um Mitarbeiter:innen häufig benötigte Informationen und Dienste direkt auf der Übersicht bereitzustellen.
                                    </Balancer>
                                </Typography>
                            </Box>
                        )}
                        {links?.map((link, index) => {
                            const LinkIcon = getCustomLinkIcon(link.icon);
                            return (
                                <Box key={link.id}>
                                    {index > 0 && <Divider/>}
                                    <Box
                                        sx={{
                                            display: 'grid',
                                            gridTemplateColumns: {xs: '32px minmax(0, 1fr)', sm: '36px minmax(0, 1fr) auto'},
                                            alignItems: 'center',
                                            gap: 2,
                                            px: 2,
                                            py: 1.75,
                                        }}
                                    >
                                        <Box sx={{width: 32, height: 32, display: 'grid', placeItems: 'center', color: 'text.secondary'}}>
                                            <LinkIcon/>
                                        </Box>
                                        <Box sx={{minWidth: 0}}>
                                            <Box sx={{display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap'}}>
                                                <Typography sx={{
                                                    fontWeight: 600
                                                }}>{link.label}</Typography>
                                                {!link.enabled && <Chip label="Ausgeblendet" size="small"/>}
                                            </Box>
                                            {link.description && <Typography variant="body2" sx={{
                                                color: "text.secondary"
                                            }}>{link.description}</Typography>}
                                            <Typography variant="body2" noWrap sx={{
                                                color: "text.secondary"
                                            }}>{link.url}</Typography>
                                        </Box>
                                        <Box sx={{display: 'flex', alignItems: 'center', gridColumn: {xs: '2', sm: 'auto'}, justifyContent: {xs: 'flex-start', sm: 'flex-end'}}}>
                                            <IconButton tooltipProps={{title: 'Link öffnen', arrow: true}} buttonProps={{onClick: () => window.open(link.url, '_blank', 'noopener,noreferrer'), size: 'small', 'aria-label': `${link.label} öffnen`}}><OpenInNew/></IconButton>
                                            <IconButton tooltipProps={{title: 'Nach oben verschieben', arrow: true}} buttonProps={{onClick: () => moveLink(index, -1), disabled: isBusy || !canUpdate || index === 0, size: 'small', 'aria-label': `${link.label} nach oben verschieben`}}><ArrowUpward/></IconButton>
                                            <IconButton tooltipProps={{title: 'Nach unten verschieben', arrow: true}} buttonProps={{onClick: () => moveLink(index, 1), disabled: isBusy || !canUpdate || index === links.length - 1, size: 'small', 'aria-label': `${link.label} nach unten verschieben`}}><ArrowDownward/></IconButton>
                                            <IconButton tooltipProps={{title: 'Bearbeiten', arrow: true}} buttonProps={{onClick: () => setEditedLink(link), disabled: isBusy || !canUpdate, size: 'small', 'aria-label': `${link.label} bearbeiten`}}><Edit/></IconButton>
                                            <IconButton tooltipProps={{title: 'Löschen', arrow: true}} buttonProps={{onClick: () => void handleDelete(link), disabled: isBusy || !canDelete, size: 'small', color: 'error', 'aria-label': `${link.label} löschen`}}><Delete/></IconButton>
                                        </Box>
                                    </Box>
                                </Box>
                            );
                        })}
                    </Box>
                    {canCreate && (
                        <Button
                            variant="contained"
                            startIcon={<Add/>}
                            onClick={() => setEditedLink(null)}
                            sx={{mt: 2}}
                        >
                            Link hinzufügen
                        </Button>
                    )}
                </Box>
            </Paper>

            <CustomLinkDialog
                open={editedLink !== undefined}
                link={editedLink ?? null}
                isBusy={isBusy}
                onClose={() => setEditedLink(undefined)}
                onSave={handleSave}
            />
        </PageWrapper>
    );
}
