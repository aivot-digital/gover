import React, {useEffect, useMemo, useState} from 'react';
import {
    Box,
    Button,
    CircularProgress,
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
import {ModuleFlag, ModuleFlagLabels} from '../../../../../utils/module-flags';
import {ProcessNodeType} from '../../../../../modules/process/services/process-node-provider-api-service';
import {humanizeNumber} from '../../../../../utils/humanization-utils';
import {ProviderTypeStyles} from '../../../../../modules/process/data/provider-type-styles';

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

    const systemInformationItems: StatusTablePropsItem[] = useMemo(() => {
        const res: StatusTablePropsItem[] = [
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
            {
                label: 'Aktivierte Module',
                icon: <Category/>,
                children: AppConfig.moduleFlags.length === 0
                    ? <i>Keine aktiven Module</i>
                    : AppConfig
                        .moduleFlags
                        .map((f) => ModuleFlagLabels[f])
                        .join(', '),
            },
        ];

        if (!AppConfig.moduleFlags.includes(ModuleFlag.Process)) {
            res.push({
                alignTop: true,
                label: 'Limitierung der Prozesselemente',
                children: (
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
                                                    <TableRow>
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
                                                                limit == 0 ?
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
                ),
            });
        }

        return res;
    }, [versionLabel, compileDate]);

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
                        <CircularProgress size="1em"/> :
                        (
                            health === 'error' || health.status !== 'UP' ?
                                <ErrorOutlineOutlinedIcon color="error"/> :
                                <CheckCircleOutlineOutlinedIcon color="success"/>
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
                    Die Informationen können eingesehen, kopiert oder als Datei heruntergeladen werden und helfen dem
                    technischen Support bei der Analyse.
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
