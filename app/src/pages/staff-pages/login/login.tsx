import {Box, Button, Container, Typography} from '@mui/material';
import React, {useState} from 'react';
import {MetaElement} from '../../../components/meta-element/meta-element';
import {useDispatch} from 'react-redux';
import {Logo} from '../../../components/static-components/logo/logo';
import {setUser} from '../../../slices/user-slice';
import {type Credentials} from '../../../models/dtos/credentials';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import LoginOutlinedIcon from '@mui/icons-material/LoginOutlined';
import {UsersService} from '../../../services/users-service';
import {LocalStorageService} from '../../../services/local-storage-service';
import {LocalstorageKey} from '../../../data/localstorage-key';

export function Login(): JSX.Element {
    const dispatch = useDispatch();

    const [isAuthenticating, setIsAuthenticating] = useState(false);
    const [authError, setAuthError] = useState<string>();

    const [credentials, setCredentials] = useState<Credentials>({
        email: '',
        password: '',
    });

    const handleAuthenticate = (event: React.FormEvent<HTMLFormElement>): void => {
        event.preventDefault();

        setIsAuthenticating(true);

        UsersService
            .login(credentials.email, credentials.password)
            .then((auth) => {
                LocalStorageService.storeString(LocalstorageKey.JWT, auth.jwtToken);
                return UsersService.getProfile();
            })
            .then((user) => {
                dispatch(setUser(user));
            })
            .catch((err) => {
                if (err.status === 401) {
                    setAuthError('Es existiert keine Benutzer:in mit dieser Kombination aus E-Mail-Adresse und Passwort');
                } else if (err.status === 409) {
                    setAuthError('Diese Benutzer:in ist gesperrt. Ein Login ist nicht möglich.');
                } else {
                    console.error(err);
                    setAuthError('Es ist ein Fehler aufgetreten. Bitte versuchen Sie es später erneut.');
                }
            })
            .finally(() => {
                setIsAuthenticating(false);
            });
    };

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
                                error={authError}
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
                                error={authError}
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
