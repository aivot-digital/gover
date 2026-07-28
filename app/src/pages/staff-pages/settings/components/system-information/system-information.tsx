import React, {useEffect, useMemo, useState} from 'react';
import {
    Box,
    Button,
    Chip,
    CircularProgress,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableRow,
    Typography,
} from '@mui/material';
import {format} from 'date-fns';
import {type HealthData, type HealthDataComponents, type Status} from '../../../../../models/dtos/health-data';
import ErrorOutlineOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Error';
import CheckCircleOutlineOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/CheckCircle';
import {AlertComponent} from '../../../../../components/alert/alert-component';
import {AppInfo} from '../../../../../app-info';
import {StatusTable} from '../../../../../components/status-table/status-table';
import {type StatusTablePropsItem} from '../../../../../components/status-table/status-table-props';
import {DebugInformationDialog} from '../../../../../dialogs/debug-information-dialog/debug-information-dialog';
import TagIcon from '@aivot/mui-material-symbols-400-n25-outlined/Tag';
import EventIcon from '@aivot/mui-material-symbols-400-n25-outlined/Event';
import HelpOutlineIcon from '@aivot/mui-material-symbols-400-n25-outlined/Help';
import {SystemApiService} from '../../../../../modules/system/system-api-service';
import BugReport from '@aivot/mui-material-symbols-400-n25-outlined/BugReport';
import Category from '@aivot/mui-material-symbols-400-n25-outlined/Category';
import RouteIcon from '@aivot/mui-material-symbols-400-n25-outlined/Route';
import AttachFileIcon from '@aivot/mui-material-symbols-400-n25-outlined/AttachFile';
import ScheduleIcon from '@aivot/mui-material-symbols-400-n25-outlined/Schedule';
import DnsIcon from '@aivot/mui-material-symbols-400-n25-outlined/Dns';
import TravelExploreIcon from '@aivot/mui-material-symbols-400-n25-outlined/TravelExplore';
import {ModuleFlag, ModuleFlagLabels} from '../../../../../utils/module-flags';
import {ProcessNodeType} from '../../../../../modules/process/services/process-node-provider-api-service';
import {humanizeFileSize, humanizeNumber} from '../../../../../utils/humanization-utils';
import {ProviderTypeStyles} from '../../../../../modules/process/data/provider-type-styles';

function isObjectRecord(value: unknown): value is Record<string, unknown> {
    return value != null && typeof value === 'object';
}

function formatCount(count: number, singular: string, plural: string): string {
    return `${count} ${count === 1 ? singular : plural}`;
}

function formatFileExtension(extension: string): string {
    return extension.startsWith('.') ? extension : `.${extension}`;
}

function renderOptionalConfigValue(value: string | null | undefined): React.ReactNode {
    const trimmed = value?.trim();
    if (trimmed == null || trimmed.length === 0) {
        return <i>Nicht konfiguriert</i>;
    }

    return (
        <Typography
            component="span"
            sx={{
                overflowWrap: 'anywhere',
            }}
        >
            {trimmed}
        </Typography>
    );
}

export function SystemInformation(): React.ReactElement {
    const [
        health,
        setHealth,
    ] = useState<HealthData | 'error'>();
    const [
        isDebugInformationDialogOpen,
        setDebugInformationDialogOpen,
    ] = useState(false);

    useEffect(() => {
        new SystemApiService()
            .getHealth()
            .then(setHealth)
            .catch((err) => {
                if (err.details != null) {
                    setHealth(err.details);
                } else {
                    console.error(err);
                    setHealth('error');
                }
            });
    }, []);

    const getStatus = (key: keyof HealthDataComponents): Status => {
        if (health == null || health === 'error') {
            return 'DOWN';
        }

        if (health.components == null) {
            return 'UNKNOWN';
        }

        return health.components[key]?.status ?? 'UNKNOWN';
    };

    const getComponent = <Key extends keyof HealthDataComponents>(
        key: Key,
    ): HealthDataComponents[Key] | undefined => {
        if (health == null || health === 'error') {
            return undefined;
        }

        return health.components?.[key];
    };

    const getDetailsMessage = (key: keyof HealthDataComponents): string | undefined => {
        const comp = getComponent(key);
        if (comp == null || !('details' in comp)) {
            return undefined;
        }

        const details: unknown = comp.details;
        if (!isObjectRecord(details)) {
            return undefined;
        }

        const error = details.error;
        if (typeof error === 'string' && error.length > 0) {
            return error;
        }

        const hint = details.hint;
        if (typeof hint === 'string' && hint.length > 0) {
            return hint;
        }

        return undefined;
    };

    const getStatusIcon = (key: keyof HealthDataComponents): React.ReactNode => {
        if (health == null) {
            return <CircularProgress size={24}/>;
        }

        const status = getStatus(key);

        switch (status) {
            case 'UP':
                return <CheckCircleOutlineOutlinedIcon color="success"/>;
            case 'DOWN':
                return <ErrorOutlineOutlinedIcon color="error"/>;
            default:
                return <HelpOutlineIcon color="warning"/>;
        }
    };

    const getStatusLabel = (key: keyof HealthDataComponents): React.ReactNode => {
        if (health == null) {
            return <Typography
                fontStyle={'italic'}
                color={'text.secondary'}
            >Status wird geladen…</Typography>;
        }

        const status = getStatus(key);

        switch (status) {
            case 'UP':
                return 'Verfügbar';
            case 'DOWN':
                return getDetailsMessage(key) ?? 'Nicht verfügbar';
            default: {
                return getDetailsMessage(key) ?? 'Unbekannt';
            }
        }
    };

    const getStatusLabelWithDetails = (
        key: keyof HealthDataComponents,
        details: React.ReactNode,
    ): React.ReactNode => {
        if (details == null) {
            return getStatusLabel(key);
        }

        return (
            <Stack spacing={0.5}>
                <Box component="span">
                    {getStatusLabel(key)}
                </Box>
                {details}
            </Stack>
        );
    };

    const getDiskSpaceDetails = (): React.ReactNode | undefined => {
        const details = getComponent('diskSpace')?.details;
        if (details == null) {
            return undefined;
        }

        const freeSpace = humanizeFileSize(details.free);
        const totalSpace = humanizeFileSize(details.total);

        return (
            <Typography
                component="span"
                variant="body2"
                color="text.secondary"
            >
                {freeSpace} frei von {totalSpace}
            </Typography>
        );
    };

    const getStorageStatusLabel = (): React.ReactNode => {
        const comp = getComponent('storage');
        if (comp == null) {
            return getStatusLabel('storage');
        }

        const providers = comp.details?.providers ?? [];
        const providerLabel = formatCount(providers.length, 'Speicheranbieter', 'Speicheranbieter');
        const providersWithErrors = providers.filter((provider) => provider.error != null).length;
        const providersWithHints = providers.filter((provider) => provider.hint != null).length;
        const defaultProvider = providers.find((provider) => provider.isDefaultAttachmentStorage);

        let primaryLabel: React.ReactNode = getStatusLabel('storage');
        if (comp.status === 'UP') {
            primaryLabel = `${providerLabel} verfügbar`;
        } else if (comp.status === 'UNKNOWN' && (providersWithErrors > 0 || providersWithHints > 0)) {
            primaryLabel = `${providerLabel}, ${formatCount(providersWithErrors + providersWithHints, 'Hinweis', 'Hinweise')}`;
        }

        return (
            <Stack spacing={0.5}>
                <Typography component="span">
                    {primaryLabel}
                </Typography>
                {
                    defaultProvider != null &&
                    <Typography
                        component="span"
                        variant="body2"
                        color="text.secondary"
                    >
                        Zentraler Anbieter für Vorgangsanlagen: {defaultProvider.name}
                    </Typography>
                }
                {
                    providersWithErrors > 0 &&
                    <Typography
                        component="span"
                        variant="body2"
                        color="error"
                    >
                        {formatCount(providersWithErrors, 'Anbieter mit Fehler', 'Anbieter mit Fehlern')}
                    </Typography>
                }
                {
                    providersWithHints > 0 &&
                    <Typography
                        component="span"
                        variant="body2"
                        color="text.secondary"
                    >
                        {formatCount(providersWithHints, 'Anbieter mit Hinweis', 'Anbieter mit Hinweisen')}
                    </Typography>
                }
            </Stack>
        );
    };

    const componentInformationItems: StatusTablePropsItem[] = [
        {
            label: 'Datenbank',
            icon: getStatusIcon('db'),
            children: getStatusLabel('db'),
        },
        {
            label: 'SMTP-Server',
            icon: getStatusIcon('mail'),
            children: getStatusLabel('mail'),
        },
        {
            label: 'Virenscanner',
            icon: getStatusIcon('av'),
            children: getStatusLabel('av'),
        },
        {
            label: 'Speicher',
            icon: getStatusIcon('diskSpace'),
            children: getStatusLabelWithDetails('diskSpace', getDiskSpaceDetails()),
        },
        {
            label: 'Temporärer Speicher',
            icon: getStatusIcon('redis'),
            children: getStatusLabel('redis'),
        },
        {
            label: 'Nachrichtenbroker',
            icon: getStatusIcon('rabbit'),
            children: getStatusLabel('rabbit'),
        },
        {
            label: 'Speicheranbieter',
            icon: getStatusIcon('storage'),
            children: getStorageStatusLabel(),
        },
        {
            label: 'PDF Service',
            icon: getStatusIcon('gotenberg'),
            children: getStatusLabel('gotenberg'),
        },
    ];

    const hasBuildVersion = AppInfo.version !== '@buildVersion';
    const hasBuildNumber = AppInfo.number !== '@buildNumber';
    const parsedBuildDate = new Date(AppInfo.date);
    const hasBuildDate = AppInfo.date !== '@buildTimestamp' && !Number.isNaN(parsedBuildDate.getTime());

    const versionLabel = hasBuildVersion ?
        (hasBuildNumber ? `${AppInfo.version} (Build ${AppInfo.number})` : AppInfo.version) :
        '5.x (DEV)';
    const compileDate = hasBuildDate ? parsedBuildDate : new Date();
    const knownFileExtensions = useMemo(() => Array.from(
        new Set(AppConfig
            .knownFileExtensions
            .flatMap((item) => item.extensions)
            .map(formatFileExtension)),
    ).sort((a, b) => a.localeCompare(b, 'de')), []);
    const isProcessModuleEnabled = AppConfig.moduleFlags.includes(ModuleFlag.Process);
    const isErrorTrackingConfigured = AppConfig.sentryDsn.trim().length > 0;

    const softwareVersionItems: StatusTablePropsItem[] = useMemo(() => {
        return [
            {
                label: 'Version',
                icon: <TagIcon/>,
                children: versionLabel,
            },
            {
                label: 'Compile-Datum',
                icon: <EventIcon/>,
                children: format(compileDate, 'dd.MM.yyyy'),
            },
        ];
    }, [
        versionLabel,
        compileDate,
    ]);

    const operationItems: StatusTablePropsItem[] = useMemo(() => {
        return [
            {
                label: 'Zeitzone',
                icon: <ScheduleIcon/>,
                children: AppConfig.applicationTimeZone,
            },
            {
                label: 'API-Hostname',
                icon: <DnsIcon/>,
                children: renderOptionalConfigValue(AppConfig.apiHostname),
            },
            {
                label: 'Vorlagen-Registry',
                icon: <TravelExploreIcon/>,
                children: renderOptionalConfigValue(AppConfig.registryHostname),
            },
            {
                label: 'Externes Fehlertracking',
                icon: <BugReport/>,
                children: isErrorTrackingConfigured ? 'Aktiv' : 'Inaktiv',
            },
        ];
    }, [isErrorTrackingConfigured]);

    const functionalScopeItems: StatusTablePropsItem[] = useMemo(() => {
        return [
            {
                label: 'Aktivierte Module',
                icon: <Category/>,
                alignTop: AppConfig.moduleFlags.length > 0,
                children: AppConfig.moduleFlags.length === 0
                    ? <i>Keine aktiven Module</i>
                    : (
                        <Stack
                            direction="row"
                            flexWrap="wrap"
                            gap={0.75}
                        >
                            {
                                AppConfig
                                    .moduleFlags
                                    .map((flag) => (
                                        <Chip
                                            key={flag}
                                            label={ModuleFlagLabels[flag]}
                                            size="small"
                                            variant="outlined"
                                        />
                                    ))
                            }
                        </Stack>
                    ),
            },
            {
                alignTop: true,
                label: 'Prozessmodellierung',
                icon: <RouteIcon/>,
                children: isProcessModuleEnabled
                    ? 'Prozesselemente uneingeschränkt verfügbar'
                    : (
                        <Stack spacing={1}>
                            <Typography component="span">
                                Mit Prozesselement-Limits
                            </Typography>
                            <TableContainer>
                                <Table size="small">
                                    <TableBody>
                                        {
                                            Object
                                                .keys(AppConfig.processNodeLimits)
                                                .map((nodeType) => {
                                                        const styles = ProviderTypeStyles[nodeType as ProcessNodeType];
                                                        const limit = AppConfig.processNodeLimits[nodeType];
                                                        return (
                                                            <TableRow key={nodeType}>
                                                                <TableCell
                                                                    sx={{
                                                                        p: 0,
                                                                    }}
                                                                >
                                                                    <styles.Icon/>
                                                                </TableCell>
                                                                <TableCell
                                                                    sx={{
                                                                        p: 0,
                                                                        pl: 2,
                                                                    }}
                                                                >
                                                                    {styles.label}
                                                                </TableCell>
                                                                <TableCell
                                                                    sx={{
                                                                        p: 0,
                                                                        pl: 2,
                                                                    }}
                                                                >
                                                                    {
                                                                        limit === 0 ?
                                                                            'nicht zur Prozessmodellierung freigegeben'
                                                                            : limit < 0
                                                                                ? 'beliebig viele pro Prozessversion erlaubt'
                                                                                : `max. ${humanizeNumber(limit, {1: 'eins'})} pro Prozessversion`
                                                                    }
                                                                </TableCell>
                                                            </TableRow>
                                                        );
                                                    },
                                                )
                                        }
                                    </TableBody>
                                </Table>
                            </TableContainer>
                        </Stack>
                    ),
            },
            {
                alignTop: true,
                label: 'Unterstützte Dateitypen',
                icon: <AttachFileIcon/>,
                children: (
                    <Typography component="span">
                        {
                            [
                                formatCount(AppConfig.knownFileExtensions.length, 'Dateiformat', 'Dateiformate'),
                                formatCount(knownFileExtensions.length, 'Dateiendung', 'Dateiendungen'),
                            ].join(', ')
                        }
                    </Typography>
                ),
                detailsLabel: 'Dateiendungen anzeigen',
                detailsExpandedLabel: 'Dateiendungen ausblenden',
                indentDetails: true,
                details: knownFileExtensions.length > 0
                    ? (
                        <Stack
                            direction="row"
                            flexWrap="wrap"
                            gap={0.75}
                        >
                            {
                                knownFileExtensions.map((extension) => (
                                    <Chip
                                        key={extension}
                                        label={extension}
                                        size="small"
                                        variant="outlined"
                                    />
                                ))
                            }
                        </Stack>
                    )
                    : undefined,
            },
        ];
    }, [
        isProcessModuleEnabled,
        knownFileExtensions,
    ]);

    return (
        <>
            <StatusTable
                label="Softwareversion"
                labelVariant="subtitle1"
                labelSx={{}}
                description="Diese Angaben identifizieren die aktuell bereitgestellte Gover-Version und das zugehörige Build-Datum."
                descriptionSx={{
                    maxWidth: 800,
                }}
                cardSx={{
                    mt: 3,
                }}
                sx={{mt: 0}}
                cardVariant="outlined"
                items={softwareVersionItems}
            />
            <StatusTable
                label="Betriebsinformationen"
                labelVariant="subtitle1"
                labelSx={{}}
                description="Diese Angaben beschreiben zentrale Laufzeit- und Integrationsparameter dieser Instanz. Sensible Konfigurationswerte werden hier nicht angezeigt."
                descriptionSx={{
                    maxWidth: 800,
                }}
                cardSx={{
                    mt: 3,
                }}
                cardVariant="outlined"
                items={operationItems}
            />
            <StatusTable
                label="Funktionsumfang"
                labelVariant="subtitle1"
                labelSx={{}}
                description="Diese Angaben zeigen, welche Funktionsbereiche und technischen Grenzen in dieser Installation aktiv sind."
                descriptionSx={{
                    maxWidth: 800,
                }}
                cardSx={{
                    mt: 3,
                }}
                cardVariant="outlined"
                items={functionalScopeItems}
            />
            <StatusTable
                sx={{
                    mt: 4,
                }}
                label="Komponentenstatus"
                labelVariant="subtitle1"
                labelIcon={
                    health == null ?
                        <CircularProgress size="1em"/> :
                        (
                            health === 'error' || health.status !== 'UP' ?
                                <ErrorOutlineOutlinedIcon color="error"/> :
                                <CheckCircleOutlineOutlinedIcon color="success"/>
                        )
                }
                description="Der Komponentenstatus zeigt die Erreichbarkeit zentraler Dienste und Infrastrukturkomponenten. Nicht verfügbare Komponenten können die Nutzung einzelner Funktionen einschränken."
                descriptionSx={{
                    maxWidth: 800,
                    mb: 3,
                }}
                cardSx={{
                    mt: 3,
                }}
                cardVariant="outlined"
                items={componentInformationItems}
            />
            {
                health != null &&
                health === 'error' &&
                <AlertComponent color="error">
                    Der Systemstatus konnte nicht abgerufen werden.
                </AlertComponent>
            }
            <Box
                sx={{
                    mt: 4,
                }}
            >
                <Typography
                    variant="subtitle1"
                    component="h2"
                >
                    Debug-Informationen
                </Typography>
                <Typography
                    sx={{maxWidth: 800}}
                >
                    Für Supportfälle können zusätzliche Diagnoseinformationen geöffnet, kopiert oder heruntergeladen
                    werden. Der Export enthält technische Details zur aktuellen Sitzung und zur Systemverfügbarkeit.
                </Typography>
                <Button
                    variant="outlined"
                    sx={{mt: 2.5}}
                    startIcon={<BugReport/>}
                    onClick={() => {
                        setDebugInformationDialogOpen(true);
                    }}
                >
                    Debug-Informationen öffnen
                </Button>
            </Box>
            <DebugInformationDialog
                open={isDebugInformationDialogOpen}
                healthData={health}
                onClose={() => {
                    setDebugInformationDialogOpen(false);
                }}
            />
        </>
    );
}
