import {faShieldCheck} from '@fortawesome/pro-solid-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {Box, Container, LinearProgress, Typography, useTheme} from '@mui/material';
import React from 'react';

export function ProcessingDataLoaderComponentView({message}: { message?: string }) {
    const theme = useTheme();
    return (
        <Container
            maxWidth={'sm'}
            sx={{py: 5.5}}
        >
            <Box sx={{display: 'flex', justifyContent: 'flex-end', alignItems: 'center'}}>
                <Typography
                    variant="h6"
                    sx={{mr: 'auto', lineHeight: 1.2, maxWidth: '300px'}}
                >
                    {message ? message : 'Daten werden verarbeitet'}…
                </Typography>
                <Box sx={{width: '40px', mr: 1, flexShrink: 0, position: 'relative'}}>
                    <FontAwesomeIcon
                        icon={faShieldCheck}
                        size={'lg'}
                        style={{
                            position: 'absolute',
                            color: theme.palette.secondary.dark,
                            bottom: '0px',
                            left: '-3px',
                            padding: 2,
                            backgroundColor: '#ffffff',
                            borderRadius: '4px'
                        }}
                    />
                    {/* TODO: FIX LOGO <Logo color={theme.palette.primary.main} /> */}
                </Box>
            </Box>
            <LinearProgress sx={{mt: 2}}/>
        </Container>
    );
}
