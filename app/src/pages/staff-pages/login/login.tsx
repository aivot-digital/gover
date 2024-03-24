import {Box, Button, Container, Typography} from '@mui/material';
import React from 'react';
import {MetaElement} from '../../../components/meta-element/meta-element';
import {Logo} from '../../../components/logo/logo';
import LoginOutlinedIcon from '@mui/icons-material/LoginOutlined';
import {getUrlWithoutQuery} from "../../../utils/location-utils";

export function Login(): JSX.Element {
    return (
        <>
            <MetaElement
                title="Authentifizierung erforderlich"
            />

            <Container
                sx={{
                    mt: 5,
                    display: 'flex',
                    justifyContent: 'center',
                }}
            >
                <Box
                    sx={{
                        px: 9,
                        py: 7,
                        pb: 9,
                        mt: 5,
                        mb: 8,
                        border: '1px solid #efefef',
                        borderRadius: '4px',
                        maxWidth: '550px',
                        textAlign: 'center',
                    }}
                >
                    <Box
                        sx={{
                            mb: 4,
                        }}
                    >
                        <Logo
                            width={200}
                            height={100}
                        />
                    </Box>

                    <Typography
                        variant="h5"
                        color="primary"
                    >
                        Authentifizierung erforderlich
                    </Typography>

                    <Typography
                        variant="body2"
                        sx={{
                            mt: 1,
                        }}
                    >
                        Zur Nutzung dieser Anwendung ist ein Mitarbeitenden-Account notwendig. Bitte melden Sie sich über den Identity Provider (IDP) an.
                    </Typography>

                    <Button
                        variant="contained"
                        size="large"
                        component="a"
                        fullWidth
                        startIcon={
                            <LoginOutlinedIcon
                                sx={{
                                    marginTop: '-2px',
                                    marginRight: '4px',
                                }}
                            />
                        }
                        sx={{mt: 4}}
                        href={`/idp/realms/staff/protocol/openid-connect/auth?${new URLSearchParams({
                            client_id: 'app',
                            redirect_uri: getUrlWithoutQuery(),
                            response_type: 'code',
                            scope: 'openid profile email',
                        }).toString()}`}
                    >
                        Zur Anmeldung über IDP
                    </Button>
                </Box>
            </Container>
        </>
    );
}
