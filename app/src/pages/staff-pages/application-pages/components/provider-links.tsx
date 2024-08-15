import React, {useEffect, useState} from 'react';
import {Container, Grid, Typography} from '@mui/material';
import {BoxLink} from '../../../../components/box-link/box-link';
import {type ProviderLink} from '../../../../models/entities/provider-link';
import {useProviderLinksApi} from '../../../../hooks/use-provider-links-api';
import {useApi} from '../../../../hooks/use-api';

export function ProviderLinks(): JSX.Element {
    const [providerLinks, setProviderLinks] = useState<ProviderLink[]>();

    const api = useApi();

    useEffect(() => {
        if (api.isAuthenticated() && providerLinks == null) {
            useProviderLinksApi(api)
                .listProviderLinks()
                .then(setProviderLinks)
                .catch((err) => {
                    console.error(err);
                });
        }
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
                    providerLinks?.map((link) => (
                        <Grid
                            key={link.text}
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
