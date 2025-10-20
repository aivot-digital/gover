import React, {useEffect, useState} from 'react';
import {Grid, SxProps} from '@mui/material';
import {BoxLink} from '../../../components/box-link/box-link';
import {useApi} from '../../../hooks/use-api';
import {Page} from '../../../models/dtos/page';
import {ProviderLink} from '../../provider-links/models/provider-link';
import {ProviderLinksApiService} from '../../provider-links/provider-links-api-service';

interface DashboardProviderLinksProps {
    sx?: SxProps;
}

export function DashboardProviderLinks(props: DashboardProviderLinksProps) {
    const [providerLinks, setProviderLinks] = useState<Page<ProviderLink>>();

    const api = useApi();

    useEffect(() => {
        new ProviderLinksApiService(api)
            .listAllOrdered('text', 'ASC')
            .then(setProviderLinks);
    }, [api]);

    return (
        <Grid
            container
            spacing={4}
            sx={props.sx}
        >
            <Grid
                size={{
                    xs: 12,
                    md: 6,
                }}
            >
                <BoxLink
                    link="https://docs.gover.digital"
                    text={'Hilfen, Anleitungen und Häufig gestellte Fragen'}
                />
            </Grid>
            {
                providerLinks?.content.map((link, index) => (
                    <Grid
                        key={`link-${index}`}
                        size={{
                            xs: 12,
                            md: 6,
                        }}
                    >
                        <BoxLink
                            link={link.link}
                            text={link.text}
                        />
                    </Grid>
                ))
            }
        </Grid>
    );
}
