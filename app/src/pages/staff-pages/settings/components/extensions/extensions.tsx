import React, {useEffect, useState} from 'react';
import {Box, Card, CardContent, CardHeader, Grid, Typography} from '@mui/material';
import {ServiceProviderApiService} from '../../../../../services/service-provider-api-service';
import {ServiceProviderDTO} from '../../../../../models/dtos/service-provider-dto';
import {PluginDTO, PluginsApiService} from "../../../../../services/plugins-api-service";

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
                Hier finden Sie weitere Informationen zu den Erweiterungen, die auf Ihrer Gover-Instanz installiert und verfügbar sind.
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
                            sm: 6,
                            md: 4,
                        }}
                    >
                        <Card
                            variant="outlined"
                            sx={{display: 'flex', flexDirection: 'column', height: '100%'}}
                        >
                            <CardHeader
                                title={serviceProvider.name}
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

                                <ul>
                                    {
                                        serviceProvider
                                            .components
                                            .map((component) => (
                                                <li key={component.name}>
                                                    <strong>{component.name}</strong>: {component.description}
                                                </li>
                                            ))
                                    }
                                </ul>
                            </CardContent>
                        </Card>
                    </Grid>
                ))}
            </Grid>
        </Box>
    );
}
