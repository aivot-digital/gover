import {Box, Grid, useTheme} from '@mui/material';
import React from 'react';

interface PreambleProps {
    text: string;
    logoLink?: string;
    logoAlt?: string;
}

export function Preamble(props: PreambleProps) {
    const theme = useTheme();
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
                            size={{
                                xs: 12,
                                md: 8,
                                lg: 7
                            }}>
                            <Box
                                sx={{maxWidth: '660px'}}
                            >
                                <div
                                    dangerouslySetInnerHTML={{__html: props.text ?? ''}}
                                    className={"content-without-margin-on-childs"}
                                />
                            </Box>
                        </Grid>
                        <Grid
                            sx={{
                                pr: 6,
                                textAlign: 'center',
                                [theme.breakpoints.down('md')]: {
                                    pr: 0,
                                    textAlign: 'left',
                                    pt: '40px!important',
                                    mb: 1,
                                }
                            }}
                            size={{
                                xs: 12,
                                md: 4,
                                lg: 5
                            }}>
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
                        <Grid size={12}>
                            <Box
                                sx={{
                                    maxWidth: '660px',
                                }}
                            >
                                <div
                                    dangerouslySetInnerHTML={{__html: props.text ?? ''}}
                                    className={"content-without-margin-on-childs"}
                                />
                            </Box>
                        </Grid>
                    </Grid>
            }
        </>
    );
}
