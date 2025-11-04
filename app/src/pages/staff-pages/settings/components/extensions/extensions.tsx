import React, {useEffect, useState} from 'react';
import {Box, Card, CardContent, CardHeader, Grid, Typography} from '@mui/material';
import {ServiceProviderApiService} from '../../../../../services/service-provider-api-service';
import {ServiceProviderDTO} from '../../../../../models/dtos/service-provider-dto';

export function Extensions() {
    const [serviceProviders, setServiceProviders] = useState<ServiceProviderDTO[]>([]);

    useEffect(() => {
        new ServiceProviderApiService()
            .getServiceProviders()
            .then(setServiceProviders);
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
                Hier finden Sie weitere Informationen zu den Erweiterungen, die auf Ihrer Gover-Instanz verfügbar sind.
            </Typography>

            <Grid
                container
                spacing={3}
            >
                {serviceProviders.map((serviceProvider) => (
                    <Grid
                        key={serviceProvider.packageName}
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
    );
}
