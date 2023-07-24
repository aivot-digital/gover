import {Box, Container, LinearProgress, Typography} from '@mui/material';
import React from 'react';
import CloudOutlinedIcon from '@mui/icons-material/CloudOutlined';

export function LoadingPlaceholderComponentView({message}: {message?: string}) {
    return (
        <Container
            maxWidth="sm"
            sx={{
                py: 5.5,
            }}
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
                    }}
                >
                    {message ? message : 'Daten werden geladen'}…
                </Typography>
                <LoadingIcon color={'#003087'}/> {/* TODO: Farben anpassen */}
                <LoadingIcon color={'#BC082F'}/>
                <LoadingIcon color={'#137673'}/>
                <LoadingIcon color={'#BA3B76'}/>
                <LoadingIcon color={'#8B596C'}/>
            </Box>
            <LinearProgress sx={{mt: 2}}/>
        </Container>
    );
}

function LoadingIcon({color}: {color: string}) {
    return (
        <Box
            sx={{
                width: '26px',
                mr: 1,
                flexShrink: 0,
            }}
        >
            <CloudOutlinedIcon
                sx={{color: color}}
            />
        </Box>
    );
}
