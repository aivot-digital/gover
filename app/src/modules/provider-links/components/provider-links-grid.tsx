import React, {useEffect, useState} from 'react';
import {Container, Grid, Typography} from '@mui/material';
import {BoxLink} from '../../../components/box-link/box-link';
import {useApi} from '../../../hooks/use-api';
import {Page} from '../../../models/dtos/page';
import {ProviderLinksApiService} from '../provider-links-api-service';
import {ProviderLink} from '../models/provider-link';

export function ProviderLinksGrid() {
    const [providerLinks, setProviderLinks] = useState<Page<ProviderLink>>();

    const api = useApi();

    useEffect(() => {
        new ProviderLinksApiService(api)
            .list(0, 999, 'text', 'ASC')
            .then(setProviderLinks)
            .catch((err) => {
                console.error(err);
            });
    }, [api]);

    return (
        <Container
            sx={{
                mt: 10,
                mb: 12,
            }}
        >
            <Typography
                variant="h5"
                sx={{
                    fontSize: '1.75rem',
                }}
            >
                Service und Unterstützung
            </Typography>
            <Grid
                container
                spacing={4}
                sx={{
                    mt: -2,
                }}
            >
                <Grid
                    item
                    xs={12}
                    md={6}
                >
                    <BoxLink
                        link="https://wiki.teamaivot.de/de/dokumentation/gover/benutzerhandbuch/home"
                        text={'Über Gover\nHilfen, Anleitungen und FAQs'}
                    />
                </Grid>
                {
                    providerLinks?.content.map((link, index) => (
                        <Grid
                            key={`link-${index}`}
                            item
                            xs={12}
                            md={6}
                        >
                            <BoxLink
                                link={link.link}
                                text={link.text}
                            />
                        </Grid>
                    ))
                }
            </Grid>
        </Container>
    );
}
