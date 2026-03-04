import React, {useEffect, useState} from 'react';
import {Box, Button, CircularProgress, Typography} from '@mui/material';
import {format} from 'date-fns';
import {type HealthData, type HealthDataComponents, type Status} from '../../../../../models/dtos/health-data';
import ErrorOutlineOutlinedIcon from '@mui/icons-material/ErrorOutlineOutlined';
import CheckCircleOutlineOutlinedIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import {AlertComponent} from '../../../../../components/alert/alert-component';
import {AppInfo} from '../../../../../app-info';
import {StatusTable} from '../../../../../components/status-table/status-table';
import {type StatusTablePropsItem} from '../../../../../components/status-table/status-table-props';
import {DebugInformationDialog} from '../../../../../dialogs/debug-information-dialog/debug-information-dialog';

import TagIcon from '@mui/icons-material/Tag';
import EventIcon from '@mui/icons-material/Event';
import HelpOutlineIcon from '@mui/icons-material/HelpOutline';
import {downloadTextFile} from '../../../../../utils/download-utils';
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined';
import {SystemApiService} from '../../../../../modules/system/system-api-service';
import BugReport from '@aivot/mui-material-symbols-400-outlined/dist/bug-report/BugReport';

function isObjectRecord(value: unknown): value is Record<string, unknown> {
    return value != null && typeof value === 'object';
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

        return health.components[key].status;
    };

    const getStatusIcon = (key: keyof HealthDataComponents): React.ReactNode => {
        if (health == null) {
            return <CircularProgress size={24} />;
        }

        const status = getStatus(key);

        switch (status) {
            case 'UP':
                return <CheckCircleOutlineOutlinedIcon color="success" />;
            case 'DOWN':
                return <ErrorOutlineOutlinedIcon color="error" />;
            default:
                return <HelpOutlineIcon color="warning" />;
        }
    };

    const getStatusLabel = (key: keyof HealthDataComponents): React.ReactNode => {
        if (health == null) {
            return <Typography fontStyle={'italic'} color={'text.secondary'}>Status wird geladen…</Typography>;
        }

        const status = getStatus(key);

        switch (status) {
            case 'UP':
                return 'Verfügbar';
            case 'DOWN':
                return 'Nicht verfügbar';
            default: {
                const comp = health === 'error' ? undefined : health.components?.[key];
                if (comp == null) {
                    return 'Unbekannt';
                }

                if ('details' in comp) {
                    const details: unknown = comp.details;
                    if (isObjectRecord(details)) {
                        const error = details.error;
                        if (typeof error === 'string' && error.length > 0) {
                            return error;
                        }

                        const hint = details.hint;
                        if (typeof hint === 'string' && hint.length > 0) {
                            return hint;
                        }
                    }
                }
                return 'Unbekannt';
            }
        }
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
            children: getStatusLabel('diskSpace'),
        },
        {
            label: 'Externer Datenspeicher',
            icon: getStatusIcon('s3'),
            children: getStatusLabel('s3'),
        },
        {
            label: 'Temporärer Speicher',
            icon: getStatusIcon('redis'),
            children: getStatusLabel('redis'),
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

    const systemInformationItems: StatusTablePropsItem[] = [
        {
            label: 'Version',
            icon: <TagIcon />,
            children: versionLabel,
        },
        {
            label: 'Compile-Datum',
            icon: <EventIcon />,
            children: format(compileDate, 'dd.MM.yyyy'),
        },
    ];

    return (
        <>
            <StatusTable
                label="Softwareinformationen"
                labelVariant="subtitle1"
                labelSx={{}}
                description={[
                    'Hier finden Sie wichtige Informationen über die einzelnen Komponenten der Software.',
                    'Sollten Sie mit dem technischen Support in Kontakt treten, können diese Informationen',
                    'hilfreich sein um Ihnen schnell weiterzuhelfen.',
                ].join(' ')}
                descriptionSx={{
                    maxWidth: 900,
                }}
                cardSx={{
                    mt: 3,
                }}
                sx={{mt: 0}}
                cardVariant="outlined"
                items={systemInformationItems}
            />
            <StatusTable
                sx={{
                    mt: 4,
                }}
                label="Allgemeiner Systemstatus"
                labelVariant="subtitle1"
                labelIcon={
                    health == null ?
                        <CircularProgress size="1em" /> :
                        (
                            health === 'error' || health.status !== 'UP' ?
                                <ErrorOutlineOutlinedIcon color="error" /> :
                                <CheckCircleOutlineOutlinedIcon color="success" />
                        )
                }
                description="Der Systemstatus gibt Auskunft über die Verfügbarkeit und Funktion der einzelnen System-Komponenten. Sollte eine Komponente nicht verfügbar sein, kann dies zu Problemen bei der Nutzung der Software führen."
                descriptionSx={{
                    maxWidth: 900,
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
                    sx={{maxWidth: 900}}
                >
                    Im Debug-Dialog finden Sie System-, Browser-, Health-, Plugin- und Benutzerinformationen.
                    Die Informationen können eingesehen, kopiert oder als Datei heruntergeladen werden und helfen dem technischen Support bei der Analyse.
                </Typography>
                <Button
                    variant="outlined"
                    sx={{mt: 2.5}}
                    startIcon={<BugReport />}
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
            <Box
                sx={{
                    mt: 4,
                }}
            >

                <Typography
                    variant="subtitle1"
                    component="h2"
                >
                    HTTP-Austausch
                </Typography>
                <Typography
                    sx={{maxWidth: 900}}
                >
                    Hier können Sie einen Auszug der letzten 100 HTTP-Requests und HTTP-Responses herunterladen.
                    Diese Informationen können hilfreich sein, um z.B. Probleme bei der Anbindung an Drittsysteme zu analysieren.
                </Typography>

                <Button
                    variant="outlined"
                    sx={{mt: 2.5}}
                    startIcon={<FileDownloadOutlinedIcon />}
                    onClick={() => {
                        void new SystemApiService()
                            .getHttpExchanges()
                            .then((exchanges) => {
                                const lines: string[] = ['uri,method,timestamp,status,timing'];
                                for (const exchange of exchanges.exchanges) {
                                    lines.push(`"${exchange.request.uri}","${exchange.request.method}","${exchange.timestamp}",${exchange.response.status},"${exchange.timeTaken}"`);
                                }

                                downloadTextFile('http-austausch.csv', lines.join('\n'), 'text/csv');
                            })
                            .catch((err) => {
                                console.error(err);
                            });
                    }}
                >
                    HTTP-Austausch herunterladen (CSV)
                </Button>
            </Box>
        </>
    );
}
