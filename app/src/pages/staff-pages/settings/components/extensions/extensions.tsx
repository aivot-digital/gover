import React, {useEffect, useState} from 'react';
import {Box, Card, CardContent, CardHeader, Grid, Typography} from '@mui/material';
import {ServiceProviderApiService} from '../../../../../services/service-provider-api-service';
import {ServiceProviderDTO} from '../../../../../models/dtos/service-provider-dto';
import {
    PluginComponentTypeDisplayNames,
    PluginComponentTypeOptions,
    PluginDTO,
    PluginsApiService,
} from '../../../../../services/plugins-api-service';

export function Extensions() {
    const [plugins, setPlugins] = useState<PluginDTO[]>([]);

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
                                title={serviceProvider.name + ' (v' + serviceProvider.version + ')'}
                                titleTypographyProps={{variant: 'h6'}}
                                subheaderTypographyProps={{
                                    variant: 'body2',
                                    color: 'text.secondary',
                                    fontSize: '0.875rem',
                                }}
                            />
                            <CardContent sx={{flexGrow: 1}}>
                                <Typography
                                    variant="body2"
                                    color="text.secondary"
                                >
                                    {serviceProvider.description}
                                </Typography>


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
                                                        .map((componentGroup) => (
                                                            <li key={componentGroup[0].name}>
                                                                <strong>{componentGroup[0].name} (v{componentGroup[0].componentVersion})</strong>: {componentGroup[0].description}
                                                            </li>
                                                        ))
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
