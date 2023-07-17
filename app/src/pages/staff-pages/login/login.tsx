import {Box, Button, Container, Typography} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {MetaElement} from '../../../components/meta-element/meta-element';
import {useDispatch, useSelector} from 'react-redux';
import {Logo} from '../../../components/static-components/logo/logo';
import {authenticate, selectUser} from '../../../slices/user-slice';
import {isInvalidUser} from '../../../models/entities/user';
import {type Credentials} from '../../../models/dtos/credentials';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import LoginOutlinedIcon from '@mui/icons-material/LoginOutlined';

export function Login(): JSX.Element {
    const dispatch = useDispatch();
    const user = useSelector(selectUser);

    const [isAuthenticating, setIsAuthenticating] = useState(false);

    const [credentials, setCredentials] = useState<Credentials>({
        email: '',
        password: '',
    });

    useEffect(() => {
        if (user != null && isInvalidUser(user)) {
            setIsAuthenticating(false);
        }
    }, [user]);

    const handleAuthenticate = (event: React.FormEvent<HTMLFormElement>): void => {
        event.preventDefault();

        setIsAuthenticating(true);
        dispatch(authenticate(credentials));
    };

    let loginError: string | undefined;
    if (user != null && isInvalidUser(user)) {
        loginError = 'Es existiert keine Benutzer:in mit dieser Kombination aus E-Mail-Adresse und Passwort';
    }

    return (
        <>
            <MetaElement
                title="Anmelden"
            />

            <Container
                sx={{
                    mt: 5,
                }}
            >
                <Box
                    sx={{
                        px: 11,
                        py: 9,
                        mt: 5,
                        mb: 8,
                        border: '1px solid #efefef',
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
                        Bitte melden Sie sich an
                    </Typography>

                    <Typography
                        variant="body2"
                        sx={{
                            mt: 1,
                        }}
                    >
                        Zur Nutzung dieser Anwendung ist ein Benutzer-Account notwendig.
                    </Typography>

                    <Box
                        sx={{
                            mt: 1,
                            mb: 5,
                        }}
                    >
                        <form
                            onSubmit={handleAuthenticate}
                            style={{
                                maxWidth: '600px',
                            }}
                        >
                            <TextFieldComponent
                                value={credentials.email}
                                onChange={(val) => {
                                    setCredentials({
                                        ...credentials,
                                        email: val ?? '',
                                    });
                                }}
                                type="email"
                                label="E-Mail-Adresse"
                                placeholder="max.muster@gover.digital"
                                error={loginError}
                                disabled={isAuthenticating}
                            />

                            <TextFieldComponent
                                value={credentials.password}
                                onChange={(val) => {
                                    setCredentials({
                                        ...credentials,
                                        password: val ?? '',
                                    });
                                }}
                                type="password"
                                label="Passwort"
                                error={loginError}
                                disabled={isAuthenticating}
                            />

                            <Button
                                type="submit"
                                sx={{
                                    mt: 2,
                                }}
                                variant="contained"
                                size="large"
                                startIcon={
                                    <LoginOutlinedIcon
                                        sx={{
                                            marginTop: '-2px',
                                            marginRight: '4px',
                                        }}
                                    />
                                }
                                disabled={isAuthenticating}
                            >
                                Jetzt Anmelden
                            </Button>
                        </form>
                    </Box>
                </Box>
            </Container>
        </>
    );
}
