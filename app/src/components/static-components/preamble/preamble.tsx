import {Box, Grid, Typography} from '@mui/material';
import React from 'react';

interface PreambleProps {
    text: string;
    logoLink?: string;
    logoAlt?: string;
}

export function Preamble(props: PreambleProps): JSX.Element {
    return (
        <>
            {
                (
                    props.logoLink != null &&
                    props.logoAlt != null
                ) ?
                    <Grid
                        container
                        spacing={10}
                        justifyContent={'space-between'}
                    >
                        <Grid
                            item
                            xs={12}
                            md={8}
                            lg={7}
                        >
                            <Typography
                                variant="body2"
                                dangerouslySetInnerHTML={{
                                    __html: props.text,
                                }}
                            />
                        </Grid>
                        <Grid
                            item
                            xs={12}
                            md={4}
                            lg={5}
                            sx={{
                                pr: 6,
                                textAlign: 'center',
                            }}
                        >
                            <img
                                src={props.logoLink}
                                alt={props.logoAlt}
                                style={{
                                    maxWidth: '100%',
                                    marginTop: '-10px',
                                }}
                            />
                        </Grid>
                    </Grid> :
                    <Grid
                        container
                        spacing={10}
                        justifyContent="space-between"
                    >
                        <Grid
                            item
                            xs={12}
                        >
                            <Box
                                sx={{
                                    maxWidth: '660px',
                                }}
                            >
                                <Typography
                                    variant="body2"
                                    dangerouslySetInnerHTML={{
                                        __html: props.text,
                                    }}
                                />
                            </Box>
                        </Grid>
                    </Grid>
            }
        </>
    );
}
