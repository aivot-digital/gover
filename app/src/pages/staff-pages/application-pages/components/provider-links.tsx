import { Grid, Typography } from "@mui/material";
import { BoxLink } from "../../../../components/box-link/box-link";
import React, { useEffect, useState } from "react";
import { ProviderLink } from "../../../../models/entities/provider-link";
import { ApplicationService } from "../../../../services/application-service";
import { ProviderLinksService } from "../../../../services/provider-links-service";

export function ProviderLinks(): JSX.Element {
    const [providerLinks, setProviderLinks] = useState<ProviderLink[]>();

    useEffect(() => {
        ProviderLinksService
            .list()
            .then(setProviderLinks)
            .catch((err) => {
                console.error(err);
            });
    }, []);

    return (
        <>
            <Typography
                variant={ 'h5' }
                sx={ {fontSize: '1.75rem'} }
            >
                Service und Unterstützung
            </Typography>
            <Grid
                container
                spacing={ 4 }
                sx={ {mt: -2} }
            >
                <Grid
                    item
                    xs={ 12 }
                    md={ 6 }
                >
                    <BoxLink link="https://aivot.de/gover">
                        <span>Über Gover</span>
                        <br/>
                        Hilfen, Anleitungen und FAQs
                    </BoxLink>
                </Grid>
                {
                    providerLinks?.map(({link, text}) => (
                        <Grid
                            key={ text }
                            item
                            xs={ 12 }
                            md={ 6 }
                        >
                            <BoxLink link={ link }>
                                {
                                    text
                                        .split('\n')
                                        .map((line, index) =>
                                            index === 0 ?
                                                <React.Fragment key={ index }>
                                                    <span>{ line }</span>
                                                    <br/></React.Fragment> :
                                                <React.Fragment key={ index }>{ line }<br/></React.Fragment>,
                                        )
                                }
                            </BoxLink>
                        </Grid>
                    ))
                }
            </Grid>
        </>
    );
}
