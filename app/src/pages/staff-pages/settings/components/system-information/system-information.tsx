import React, {useEffect, useState} from 'react';
import {Box, Button, Card, CardContent, CardHeader, CircularProgress, Grid, Typography} from '@mui/material';
import ProjectPackage from '../../../../../../package.json';
import {format} from 'date-fns';
import {HealthData, HealthDataComponents, Status} from '../../../../../models/dtos/health-data';
import ErrorOutlineOutlinedIcon from '@mui/icons-material/ErrorOutlineOutlined';
import CheckCircleOutlineOutlinedIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import {AlertComponent} from '../../../../../components/alert/alert-component';
import {useApi} from '../../../../../hooks/use-api';
import {useSystemApi} from '../../../../../hooks/use-system-api';
import {AppConfig} from '../../../../../app-config';
import {StatusTable} from '../../../../../components/status-table/status-table';
import {StatusTablePropsItem} from '../../../../../components/status-table/status-table-props';

import TagIcon from '@mui/icons-material/Tag';
import EventIcon from '@mui/icons-material/Event';
import HelpOutlineIcon from '@mui/icons-material/HelpOutline';
import {downloadTextFile} from '../../../../../utils/download-utils';
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined';
import {ServiceProviderApiService} from '../../../../../services/service-provider-api-service';
import {ServiceProviderDTO} from '../../../../../models/dtos/service-provider-dto';

const systemInformationItems: StatusTablePropsItem[] = [
    {
        label: 'Version',
        icon: <TagIcon />,
        children: ProjectPackage.version,
    },
    {
        label: 'Compile-Datum',
        icon: <EventIcon />,
        children: format(new Date(AppConfig.date), 'dd.MM.yyyy'),
    },
];


export function SystemInformation() {
    const api = useApi();
    const [health, setHealth] = useState<HealthData | 'error'>();
    const [serviceProviders, setServiceProviders] = useState<ServiceProviderDTO[]>([]);

    useEffect(() => {
        useSystemApi(api)
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

        new ServiceProviderApiService(api)
            .getServiceProviders()
            .then(setServiceProviders);
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

    const getStatusIcon = (key: keyof HealthDataComponents) => {
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

    const getStatusLabel = (key: keyof HealthDataComponents) => {
        if (health == null) {
            return null;
        }

        const status = getStatus(key);

        switch (status) {
            case 'UP':
                return 'Verfügbar';
            case 'DOWN':
                return 'Nicht verfügbar';
            default:
                const comp = (health as HealthData).components![key];
                if ('details' in comp) {
                    const details = comp.details;
                    if (details != null) {
                        if ('error' in details) {
                            return details.error;
                        }
                        if ('hint' in details) {
                            return details.hint;
                        }
                    }
                }
                return 'Unbekannt';
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
            label: 'PDF Service',
            icon: getStatusIcon('puppet'),
            children: getStatusLabel('puppet'),
        },
    ];

    return (
        <>
            <StatusTable
                label="Softwareinformationen"
                labelVariant="subtitle1"
                labelSx={{}}
                description="Hier finden Sie wichtige Informationen über die einzelnen Komponenten der Software. Sollten Sie mit dem technischen Support in Kontakt treten, können diese Informationen hilfreich sein um Ihnen schnell weiterzuhelfen."
                descriptionSx={{
                    maxWidth: 900,
                }}
                cardSx={{
                    mt: 3,
                }}
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
                        useSystemApi(api)
                            .getHttpExchanges()
                            .then((exchanges) => {
                                const lines: string[] = [
                                    'uri,method,timestamp,status,timing',
                                ];
                                for (const exchange of exchanges.exchanges) {
                                    lines.push(`"${exchange.request.uri}","${exchange.request.method}","${exchange.timestamp}",${exchange.response.status},"${exchange.timeTaken}"`);
                                }

                                downloadTextFile('http-austausch.csv', lines.join('\n'), 'text/csv');
                            });
                    }}
                >
                    HTTP-Austausch herunterladen (CSV)
                </Button>
            </Box>

            <Box
                sx={{
                    mt: 4,
                }}
            >
                <Typography
                    variant="subtitle1"
                    component="h2"
                >
                    Gover-Erweiterungen
                </Typography>

                <Typography sx={{mb: 3}}>
                    Hier finden Sie Informationen zu den Erweiterungen, die auf Ihrer Gover-Instanz verfügbar sind.
                </Typography>

                <Grid
                    container
                    spacing={3}
                >
                    {serviceProviders.map((serviceProvider) => (
                        <Grid
                            item
                            xs={12}
                            sm={6}
                            md={4}
                            key={serviceProvider.packageName}
                        >
                            <Card
                                variant="outlined"
                                sx={{display: 'flex', flexDirection: 'column', height: '100%'}}
                            >
                                <CardHeader
                                    title={serviceProvider.label}
                                    subheader={serviceProvider.packageName}
                                    titleTypographyProps={{variant: 'h6'}}
                                    subheaderTypographyProps={{variant: 'body2', color: 'text.secondary', fontSize: '0.875rem'}}
                                />
                                <CardContent sx={{flexGrow: 1}}>
                                    <Typography
                                        variant="body2"
                                        color="text.secondary"
                                    >
                                        {serviceProvider.description}
                                    </Typography>
                                </CardContent>
                            </Card>
                        </Grid>
                    ))}
                </Grid>
            </Box>
        </>
    );
}
