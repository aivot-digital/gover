import React, {useEffect, useMemo, useState} from 'react';
import {Box, Button, Dialog, DialogActions, DialogContent, Stack, Typography} from '@mui/material';
import ContentCopyOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/ContentCopy';
import FileDownloadOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Download';
import {format} from 'date-fns';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {type HealthData, type HealthDataComponents} from '../../models/dtos/health-data';
import {SystemApiService} from '../../modules/system/system-api-service';
import {copyToClipboardText} from '../../utils/copy-to-clipboard';
import {downloadTextFile} from '../../utils/download-utils';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../slices/snackbar-slice';
import {AppInfo} from '../../app-info';
import {type PluginDTO, PluginsApiService} from '../../services/plugins-api-service';
import {type User} from '../../modules/users/models/user';
import {UsersApiService} from '../../modules/users/users-api-service';
import {AuthService} from '../../services/auth-service';
import {humanizeMilliseconds} from '../../utils/humanization-utils';
import {useHasSystemPermission} from '../../modules/permissions/hooks/use-permissions';
import {Permission} from '../../data/permissions/permission';

interface DebugInformationDialogProps {
    open: boolean;
    onClose: () => void;
    healthData?: HealthData | 'error';
    plugins?: PluginDTO[];
}

function anonymizeName(value: string): string {
    const trimmed = value.trim();
    if (trimmed.length === 0) {
        return 'unknown';
    }

    return trimmed
        .split(/\s+/)
        .map((part) => {
            if (part.length <= 1) {
                return '*';
            }

            return `${part[0]}${'*'.repeat(part.length - 1)}`;
        })
        .join(' ');
}

function anonymizeEmailKeepDomain(value: string): string {
    const atIndex = value.indexOf('@');
    if (atIndex <= 0 || atIndex === value.length - 1) {
        return 'unknown';
    }

    const localPart = value.slice(0, atIndex);
    const domain = value.slice(atIndex + 1);

    let maskedLocalPart: string;
    if (localPart.length === 1) {
        maskedLocalPart = '*';
    } else if (localPart.length === 2) {
        maskedLocalPart = `${localPart[0]}*`;
    } else {
        maskedLocalPart = `${localPart[0]}${'*'.repeat(localPart.length - 2)}${localPart[localPart.length - 1]}`;
    }

    return `${maskedLocalPart}@${domain}`;
}

function toDebugBooleanString(value: boolean): string {
    return value ? 'true' : 'false';
}

function toDebugOptionalBooleanString(value: boolean | undefined): string {
    if (value == null) {
        return 'unknown';
    }

    return value ? 'true' : 'false';
}

function isObjectRecord(value: unknown): value is Record<string, unknown> {
    return value != null && typeof value === 'object';
}

function appendSafeHealthDetails(lines: string[], key: keyof HealthDataComponents, details: unknown): void {
    if (!isObjectRecord(details)) {
        return;
    }

    const error = details.error;
    if (typeof error === 'string' && error.length > 0) {
        lines.push(`- ${key}.error: ${error}`);
    }

    const hint = details.hint;
    if (typeof hint === 'string' && hint.length > 0) {
        lines.push(`- ${key}.hint: ${hint}`);
    }

    if (key === 'storage') {
        const providers = details.providers;
        if (Array.isArray(providers)) {
            const providerRecords = providers.filter(isObjectRecord);
            const providersWithErrors = providerRecords.filter((provider) => typeof provider.error === 'string').length;
            const providersWithHints = providerRecords.filter((provider) => typeof provider.hint === 'string').length;

            lines.push(`- ${key}.providers: ${providerRecords.length}`);
            lines.push(`- ${key}.providersWithErrors: ${providersWithErrors}`);
            lines.push(`- ${key}.providersWithHints: ${providersWithHints}`);
        }
    }
}

export function DebugInformationDialog(props: DebugInformationDialogProps): React.ReactElement {
    const dispatch = useAppDispatch();
    const canReadPlugins = useHasSystemPermission(Permission.PLUGIN_READ);
    const [
        fetchedHealthData,
        setFetchedHealthData,
    ] = useState<HealthData | 'error'>();
    const [
        fetchedPlugins,
        setFetchedPlugins,
    ] = useState<PluginDTO[]>();
    const [
        pluginsLoadingFailed,
        setPluginsLoadingFailed,
    ] = useState(false);
    const [
        selfUser,
        setSelfUser,
    ] = useState<User>();
    const [
        selfLoadingFailed,
        setSelfLoadingFailed,
    ] = useState(false);

    useEffect(() => {
        if (!props.open || props.healthData != null) {
            return;
        }

        let isCurrent = true;
        setFetchedHealthData(undefined);

        new SystemApiService()
            .getHealth()
            .then((value) => {
                if (isCurrent) {
                    setFetchedHealthData(value);
                }
            })
            .catch((err) => {
                if (!isCurrent) {
                    return;
                }

                if (err.details != null) {
                    setFetchedHealthData(err.details);
                } else {
                    console.error(err);
                    setFetchedHealthData('error');
                }
            });

        return () => {
            isCurrent = false;
        };
    }, [
        props.open,
        props.healthData,
    ]);

    useEffect(() => {
        if (!props.open || props.plugins != null) {
            return;
        }

        if (!canReadPlugins) {
            setFetchedPlugins(undefined);
            setPluginsLoadingFailed(false);
            return;
        }

        let isCurrent = true;
        setFetchedPlugins(undefined);
        setPluginsLoadingFailed(false);

        new PluginsApiService()
            .getPlugins()
            .then((value) => {
                if (isCurrent) {
                    setFetchedPlugins(value);
                }
            })
            .catch((err) => {
                if (!isCurrent) {
                    return;
                }

                console.error(err);
                setPluginsLoadingFailed(true);
            });

        return () => {
            isCurrent = false;
        };
    }, [
        props.open,
        props.plugins,
        canReadPlugins,
    ]);

    useEffect(() => {
        if (!props.open) {
            return;
        }

        let isCurrent = true;
        setSelfUser(undefined);
        setSelfLoadingFailed(false);

        new UsersApiService()
            .retrieveSelf()
            .then((value) => {
                if (isCurrent) {
                    setSelfUser(value);
                }
            })
            .catch((err) => {
                if (!isCurrent) {
                    return;
                }

                console.error(err);
                setSelfLoadingFailed(true);
            });

        return () => {
            isCurrent = false;
        };
    }, [props.open]);

    const healthData = props.healthData ?? fetchedHealthData;
    const plugins = props.plugins ?? fetchedPlugins;
    const hasBuildVersion = AppInfo.version !== '@buildVersion';
    const hasBuildNumber = AppInfo.number !== '@buildNumber';
    const parsedBuildDate = new Date(AppInfo.date);
    const hasBuildDate = AppInfo.date !== '@buildTimestamp' && !Number.isNaN(parsedBuildDate.getTime());
    const versionLabel = hasBuildVersion ?
        (hasBuildNumber ? `${AppInfo.version} (Build ${AppInfo.number})` : AppInfo.version) :
        '5.x (DEV)';
    const compileDate = hasBuildDate ? parsedBuildDate : new Date();

    const debugInfoText = useMemo(() => {
        const lines: string[] = [
            '# Gover debug info',
            '',
            '## core',
            `- goverVersion: ${versionLabel}`,
            `- buildVersionRaw: ${AppInfo.version}`,
            `- buildNumberRaw: ${AppInfo.number}`,
            `- buildDateRaw: ${AppInfo.date}`,
            `- hasBuildVersion: ${toDebugBooleanString(hasBuildVersion)}`,
            `- hasBuildNumber: ${toDebugBooleanString(hasBuildNumber)}`,
            `- hasBuildDate: ${toDebugBooleanString(hasBuildDate)}`,
            `- compileDate: ${compileDate.toISOString()}`,
            `- appMode: ${AppInfo.mode}`,
            `- frontendMode: ${import.meta.env.MODE}`,
            `- apiHostname: ${AppConfig.apiHostname}`,
            `- oidcHostname: ${AppConfig.oidc.hostname}`,
            `- oidcRealm: ${AppConfig.oidc.realm}`,
            `- oidcClient: ${AppConfig.oidc.clientId}`,
            `- registryHostname: ${AppConfig.registryHostname}`,
            `- applicationTimeZone: ${AppConfig.applicationTimeZone}`,
            `- sentryDsnConfigured: ${toDebugBooleanString(AppConfig.sentryDsn.length > 0)}`,
            `- moduleFlags: ${AppConfig.moduleFlags.join(', ')}`,
            `- processNodeLimits: ${JSON.stringify(AppConfig.processNodeLimits)}`,
            `- page: ${window.location.origin}${window.location.pathname}`,
            '',
            '## health',
        ];

        if (healthData == null) {
            lines.push('- overallStatus: loading');
        } else if (healthData === 'error') {
            lines.push('- overallStatus: error');
        } else {
            lines.push(`- overallStatus: ${healthData.status}`);
            const componentsInOrder: Array<keyof HealthDataComponents> = [
                'db',
                'mail',
                'av',
                'diskSpace',
                'redis',
                'rabbit',
                'storage',
                'gotenberg',
            ];
            for (const key of componentsInOrder) {
                const component = healthData.components?.[key];
                if (component == null) {
                    lines.push(`- ${key}: missing`);
                    continue;
                }
                lines.push(`- ${key}: ${component.status}`);
                if ('details' in component && component.details != null) {
                    appendSafeHealthDetails(lines, key, component.details);
                }
            }
        }

        lines.push('', '## plugins');
        if (!canReadPlugins && props.plugins == null) {
            lines.push('- status: not_authorized');
        } else if (plugins == null && !pluginsLoadingFailed) {
            lines.push('- status: loading');
        } else if (pluginsLoadingFailed) {
            lines.push('- status: error');
        } else if (plugins == null || plugins.length === 0) {
            lines.push('- installed: 0');
        } else {
            lines.push(`- installed: ${plugins.length}`);
            for (const plugin of plugins) {
                const componentCount = plugin.components.reduce((acc, group) => acc + group.length, 0);
                const componentTypes = Array.from(new Set(
                    plugin.components
                        .filter((group) => group.length > 0)
                        .map((group) => group[0].componentType),
                ));
                lines.push(`- ${plugin.key}: ${plugin.name} v${plugin.version}`);
                lines.push(`- ${plugin.key}.buildDate: ${plugin.buildDate}`);
                lines.push(`- ${plugin.key}.vendor: ${plugin.vendorName} (${plugin.vendorWebsite})`);
                lines.push(`- ${plugin.key}.deprecated: ${toDebugBooleanString(plugin.deprecationNotice != null)}`);
                lines.push(`- ${plugin.key}.components: ${componentCount}`);
                const componentTypesText = componentTypes.length > 0 ? componentTypes.join(', ') : 'none';
                lines.push(`- ${plugin.key}.componentTypes: ${componentTypesText}`);
            }
        }

        lines.push('', '## user');
        if (selfUser == null && !selfLoadingFailed) {
            lines.push('- status: loading');
        } else if (selfLoadingFailed) {
            lines.push('- status: error');
        } else if (selfUser != null) {
            lines.push(`- id: ${selfUser.id}`);
            lines.push(`- fullName: ${anonymizeName(selfUser.fullName)}`);
            lines.push(`- email: ${anonymizeEmailKeepDomain(selfUser.email)}`);
            lines.push(`- enabled: ${toDebugBooleanString(selfUser.enabled)}`);
            lines.push(`- verified: ${toDebugBooleanString(selfUser.verified)}`);
            lines.push(`- systemRoleId: ${selfUser.systemRoleId ?? 'none'}`);
            lines.push(`- deletedInIdp: ${toDebugBooleanString(selfUser.deletedInIdp)}`);
        }

        lines.push('', '## session');
        const accessExpiration = AuthService.getAccessExpirationTimestamp();
        if (accessExpiration != null) {
            lines.push(`- accessTokenExpiresAt: ${new Date(accessExpiration).toISOString()}`);
            lines.push(`- timeUntilAccessTokenExpires: ${humanizeMilliseconds(accessExpiration - Date.now())}`);
        } else {
            lines.push('- accessToken: none or expired');
        }
        const refreshExpiration = AuthService.getExpirationTimestamp();
        if (refreshExpiration != null) {
            lines.push(`- refreshTokenExpiresAt: ${new Date(refreshExpiration).toISOString()}`);
            lines.push(`- timeUntilRefreshTokenExpires: ${humanizeMilliseconds(refreshExpiration - Date.now())}`);
        } else {
            lines.push('- refreshToken: none or expired');
        }

        const colorScheme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
        const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        const isTouchDevice = ('ontouchstart' in window) || navigator.maxTouchPoints > 0;
        const navigatorWithConnection = navigator as Navigator & {
            connection?: {
                effectiveType?: string;
                downlink?: number;
                rtt?: number;
                saveData?: boolean;
            };
        };

        lines.push(
            '',
            '## client',
            `- userAgent: ${navigator.userAgent}`,
            `- language: ${navigator.language}`,
            `- languages: ${navigator.languages.join(', ')}`,
            `- platform: ${navigator.platform}`,
            `- timezone: ${Intl.DateTimeFormat().resolvedOptions().timeZone}`,
            `- viewport: ${window.innerWidth}x${window.innerHeight}`,
            `- screen: ${window.screen.width}x${window.screen.height}`,
            `- visibilityState: ${document.visibilityState}`,
            `- colorScheme: ${colorScheme}`,
            `- prefersReducedMotion: ${toDebugBooleanString(reducedMotion)}`,
            `- isTouchDevice: ${toDebugBooleanString(isTouchDevice)}`,
            `- online: ${toDebugBooleanString(navigator.onLine)}`,
            `- cookieEnabled: ${toDebugBooleanString(navigator.cookieEnabled)}`,
            `- doNotTrack: ${navigator.doNotTrack ?? 'unknown'}`,
            `- hardwareConcurrency: ${navigator.hardwareConcurrency ?? 'unknown'}`,
            `- deviceMemoryGB: ${(navigator as Navigator & {deviceMemory?: number}).deviceMemory ?? 'unknown'}`,
            `- connectionEffectiveType: ${navigatorWithConnection.connection?.effectiveType ?? 'unknown'}`,
            `- connectionDownlinkMbps: ${navigatorWithConnection.connection?.downlink ?? 'unknown'}`,
            `- connectionRttMs: ${navigatorWithConnection.connection?.rtt ?? 'unknown'}`,
            `- connectionSaveData: ${
                toDebugOptionalBooleanString(navigatorWithConnection.connection?.saveData)
            }`,
            '',
            `Generated at: ${new Date().toISOString()}`,
        );

        return lines.join('\n');
    }, [
        compileDate,
        canReadPlugins,
        hasBuildDate,
        hasBuildNumber,
        hasBuildVersion,
        healthData,
        plugins,
        pluginsLoadingFailed,
        props.plugins,
        selfLoadingFailed,
        selfUser,
        versionLabel,
    ]);

    const handleCopy = async (): Promise<void> => {
        const success = await copyToClipboardText(debugInfoText);
        if (success) {
            dispatch(showSuccessSnackbar('Debug-Informationen in die Zwischenablage kopiert.'));
        } else {
            dispatch(showErrorSnackbar('Fehler beim Kopieren der Debug-Informationen.'));
        }
    };

    const handleDownload = (): void => {
        const filename = `gover-debug-info-${format(new Date(), 'yyyyMMdd-HHmmss')}.txt`;
        downloadTextFile(filename, debugInfoText, 'text/plain;charset=utf-8');
        dispatch(showSuccessSnackbar('Debug-Informationen wurden heruntergeladen.'));
    };

    return (
        <Dialog
            open={props.open}
            onClose={props.onClose}
            maxWidth="md"
            fullWidth
        >
            <DialogTitleWithClose onClose={props.onClose}>
                Debug-Informationen
            </DialogTitleWithClose>
            <DialogContent>
                <Typography sx={{maxWidth: 900}}>
                    Diese Informationen helfen dem technischen Support bei der Fehleranalyse.
                    Enthalten sind System-, Browser-, Health- und Benutzerinformationen.
                    Plugininformationen werden bei vorhandener Berechtigung ergänzt.
                    Prüfen Sie vor dem Teilen, ob sensible Daten enthalten sind.
                </Typography>

                <Box
                    sx={{
                        mt: 2,
                        p: 2,
                        border: '1px solid',
                        borderColor: 'divider',
                        borderRadius: 1.5,
                        bgcolor: 'background.paper',
                        maxHeight: 420,
                        overflow: 'auto',
                    }}
                >
                    <Typography
                        component="pre"
                        sx={{
                            m: 0,
                            whiteSpace: 'pre-wrap',
                            wordBreak: 'break-word',
                            fontFamily: 'monospace',
                            fontSize: 13,
                            lineHeight: 1.45,
                        }}
                    >
                        {debugInfoText}
                    </Typography>
                </Box>
            </DialogContent>
            <DialogActions sx={{
                px: 3, pb: 2,
            }}>
                <Stack direction="row" spacing={1}>
                    <Button
                        variant="outlined"
                        startIcon={<ContentCopyOutlinedIcon />}
                        onClick={() => {
                            handleCopy().catch(console.error);
                        }}
                    >
                        Kopieren
                    </Button>
                    <Button
                        variant="outlined"
                        startIcon={<FileDownloadOutlinedIcon />}
                        onClick={handleDownload}
                    >
                        Herunterladen (.txt)
                    </Button>
                </Stack>
                <Box sx={{flex: 1}} />
                <Button
                    variant="contained"
                    onClick={props.onClose}
                >
                    Schließen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
