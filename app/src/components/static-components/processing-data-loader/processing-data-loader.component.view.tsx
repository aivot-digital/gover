import {Box, Container, LinearProgress, Typography, useTheme} from '@mui/material';
import React from 'react';
import GppGoodOutlinedIcon from '@mui/icons-material/GppGoodOutlined';

export function ProcessingDataLoaderComponentView({message}: {message?: string}) {
    const theme = useTheme();
    return (
        <Container
            maxWidth={'sm'}
            sx={{py: 5.5}}
        >
            <Box sx={{
                display: 'flex',
                justifyContent: 'flex-end',
                alignItems: 'center',
            }}>
                <Typography
                    variant="h6"
                    sx={{
                        mr: 'auto',
                        lineHeight: 1.2,
                        maxWidth: '300px',
                    }}
                >
                    {message ? message : 'Daten werden verarbeitet'}…
                </Typography>

                <GppGoodOutlinedIcon
                    fontSize={'large'}
                    sx={{
                        color: theme.palette.secondary.dark,
                        margin: 2,
                    }}
                />

            </Box>
            <LinearProgress sx={{mt: 2}}/>
        </Container>
    );
}
