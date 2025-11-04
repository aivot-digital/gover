import {Box, Button, Paper, Typography} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {MetaElement} from '../../../components/meta-element/meta-element';
import LoginOutlinedIcon from '@mui/icons-material/LoginOutlined';
import {AuthService} from '../../../services/auth-service';
import AccountCircle from '@aivot/mui-material-symbols-400-outlined/dist/account-circle/AccountCircle';

export function Login() {
    const [loginUrl, setLoginUrl] = useState<string>('');
    useEffect(() => {
        new AuthService()
            .getLoginUrl()
            .then(setLoginUrl);
    }, []);

    return (
        <>
            <MetaElement
                title="Anmeldung erforderlich"
            />

            <Box
                sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    width: '100%',
                    height: '100vh',
                    alignItems: 'center',
                    justifyContent: 'center',
                    textAlign: 'center',
                }}
            >
                <Paper
                    sx={{
                        p: 5,
                        pb: 7,
                        maxWidth: '30rem',
                    }}
                >

                    <AccountCircle sx={{ fontSize: 64, mb: 1, color: 'primary.main' }} />
                    <Typography variant="h2" sx={{ mb: 2 }} fontWeight={600}>
                        Anmeldung erforderlich
                    </Typography>

                    <Typography variant="body1" sx={{ mt: 2 }}>
                        Für die Nutzung dieser Anwendung benötigen Sie ein Mitarbeitenden-Konto. Bitte melden Sie sich über den Identity Provider (IDP) an.
                    </Typography>

                    <Button
                        variant="contained"
                        component="a"
                        startIcon={
                            <LoginOutlinedIcon
                                sx={{
                                    marginTop: '-2px',
                                    marginRight: '4px',
                                }}
                            />
                        }
                        sx={{ mt: 4 }}
                        href={loginUrl}
                    >
                        Anmeldung via Identity Provider
                    </Button>
                </Paper>
            </Box>
        </>
    );
}
