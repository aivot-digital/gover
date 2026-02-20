import React, {useEffect, useState} from 'react';
import {Box, Card, CardContent, CardHeader, Grid, IconButton, Tooltip, Typography} from '@mui/material';
import {
    PluginComponentTypeDisplayNames,
    PluginComponentTypeOptions,
    PluginDTO,
    PluginsApiService,
} from '../../../../../services/plugins-api-service';
import {AlertComponent} from '../../../../../components/alert/alert-component';
import ReactMarkdown from 'react-markdown';
import {Badge} from '../../../../../components/badge/badge';
import Article from '@aivot/mui-material-symbols-400-outlined/dist/article/Article';
import {useConfirm} from '../../../../../providers/confirm-provider';

export function Extensions() {
    const [plugins, setPlugins] = useState<PluginDTO[]>([]);

    const confirm = useConfirm();

    useEffect(() => {
        new PluginsApiService()
            .getPlugins()
            .then(setPlugins);
    }, []);

    return (
        <Box>
            <Typography
                variant="subtitle1"
                component="h2"
            >
                Liste der installierten Erweiterungen
            </Typography>

            <Typography sx={{mb: 3}}>
                Hier finden Sie weitere Informationen zu den Erweiterungen, die auf Ihrer Gover-Instanz installiert und
                verfügbar sind.
            </Typography>

            <Grid
                container
                spacing={3}
            >
                {plugins.map((serviceProvider) => (
                    <Grid
                        key={serviceProvider.name}
                        size={{
                            xs: 12,
                        }}
                    >
                        <Card
                            variant="outlined"
                            sx={{
                                display: 'flex',
                                flexDirection: 'column',
                                height: '100%',
                            }}
                        >
                            <CardHeader
                                title={serviceProvider.name}
                                subheader={
                                    <Box
                                        component="ul"
                                        sx={{
                                            listStyle: 'none',
                                            m: 0,
                                            p: 0,
                                            display: 'flex',
                                            '& li::before': {
                                                content: '"•"',
                                                color: 'text.secondary',
                                                display: 'inline-block',
                                                mx: 0.5,
                                            },
                                            '& li:first-child::before': {
                                                display: 'none',
                                            },
                                        }}
                                    >
                                        <li>Version {serviceProvider.version}</li>
                                        <li>
                                            {serviceProvider.vendorName}&nbsp;
                                            <a href={serviceProvider.vendorWebsite}>{serviceProvider.vendorWebsite}</a>
                                        </li>
                                    </Box>
                                }
                                slotProps={{
                                    title: {
                                        variant: 'h6',
                                    },
                                    subheader: {
                                        variant: 'body2',
                                        color: 'text.secondary',
                                        fontSize: '0.875rem',
                                    },
                                }}
                                action={
                                    <Tooltip title="Changelog anzeigen">
                                        <IconButton
                                            onClick={() => {
                                                confirm({
                                                    title: 'Changelog',
                                                    children:
                                                        <ReactMarkdown>{serviceProvider.changelog}</ReactMarkdown>,
                                                    hideCancelButton: true,
                                                    confirmButtonText: 'Schließen',
                                                });
                                            }}
                                        >
                                            <Article/>
                                        </IconButton>
                                    </Tooltip>
                                }
                            />

                            <CardContent
                                sx={{
                                    flexGrow: 1,
                                }}
                            >
                                <Box
                                    sx={{
                                        color: 'text.secondary',
                                    }}
                                >
                                    <ReactMarkdown>
                                        {serviceProvider.description}
                                    </ReactMarkdown>
                                </Box>

                                {
                                    serviceProvider.deprecationNotice != null &&
                                    <AlertComponent
                                        title="Veraltete Erweiterung"
                                        color="warning"
                                        sx={{
                                            mt: 2,
                                        }}
                                    >
                                        <ReactMarkdown>
                                            {serviceProvider.deprecationNotice}
                                        </ReactMarkdown>
                                    </AlertComponent>
                                }

                                {
                                    PluginComponentTypeOptions.map((componentType) => (
                                        <Box key={componentType}>
                                            <Typography
                                                variant="subtitle2"
                                                sx={{mt: 2}}
                                            >
                                                {PluginComponentTypeDisplayNames[componentType]}
                                            </Typography>

                                            <ul>
                                                {
                                                    serviceProvider
                                                        .components
                                                        .filter(group => group[0].componentType === componentType)
                                                        .length === 0 && (
                                                        <li>
                                                            <em>Keine Komponenten dieses Typs vorhanden.</em>
                                                        </li>
                                                    )
                                                }

                                                {
                                                    serviceProvider
                                                        .components
                                                        .filter(group => group[0].componentType === componentType)
                                                        .map((componentGroup) => {
                                                            const orderedVersions = componentGroup.sort((a, b) => b.majorVersion - a.majorVersion);
                                                            const currentVersion = orderedVersions[0];
                                                            return (
                                                                <li key={currentVersion.name}>
                                                                    <Box>
                                                                        <Box
                                                                            sx={{
                                                                                display: 'flex',
                                                                                alignItems: 'center',
                                                                                gap: 1,
                                                                            }}
                                                                        >
                                                                            {currentVersion.name}
                                                                            <Badge label={currentVersion.componentVersion}
                                                                                   color="default"/>
                                                                        </Box>

                                                                        <Typography
                                                                            color="text.secondary"
                                                                        >
                                                                            <ReactMarkdown>
                                                                                {currentVersion.description}
                                                                            </ReactMarkdown>
                                                                        </Typography>

                                                                        {
                                                                            currentVersion.deprecationNotice != null &&
                                                                            <AlertComponent
                                                                                title="Veraltete Komponente"
                                                                                color="warning"
                                                                                sx={{
                                                                                    mt: 1,
                                                                                }}
                                                                            >
                                                                                <ReactMarkdown>
                                                                                    {currentVersion.deprecationNotice}
                                                                                </ReactMarkdown>
                                                                            </AlertComponent>
                                                                        }

                                                                        {
                                                                            orderedVersions.length > 1 &&
                                                                            orderedVersions.slice(1).map((version) => (
                                                                                <Box
                                                                                    key={version.name}
                                                                                    sx={{
                                                                                        mt: 1,
                                                                                        ml: 2,
                                                                                        borderLeft: '2px solid',
                                                                                        borderColor: 'divider',
                                                                                        pl: 2,
                                                                                    }}
                                                                                >
                                                                                    <Box
                                                                                        sx={{
                                                                                            display: 'flex',
                                                                                            alignItems: 'center',
                                                                                            gap: 1,
                                                                                        }}
                                                                                    >
                                                                                        {version.name}
                                                                                        <Badge label={version.componentVersion}
                                                                                               color="default"/>
                                                                                    </Box>

                                                                                    {
                                                                                        version.deprecationNotice != null &&
                                                                                        <AlertComponent
                                                                                            title="Veraltete Komponente"
                                                                                            color="warning"
                                                                                            sx={{
                                                                                                mt: 1,
                                                                                            }}
                                                                                        >
                                                                                            <ReactMarkdown>
                                                                                                {version.deprecationNotice}
                                                                                            </ReactMarkdown>
                                                                                        </AlertComponent>
                                                                                    }
                                                                                </Box>
                                                                            ))
                                                                        }
                                                                    </Box>
                                                                </li>
                                                            );
                                                        })
                                                }
                                            </ul>
                                        </Box>
                                    ))
                                }
                            </CardContent>
                        </Card>
                    </Grid>
                ))}
            </Grid>
        </Box>
    );
}
