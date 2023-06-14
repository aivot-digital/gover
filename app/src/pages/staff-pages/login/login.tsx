import {Box, Button, Container, TextField, Typography} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {MetaElement} from '../../../components/meta-element/meta-element';
import {useDispatch, useSelector} from 'react-redux';
import {faArrowRightToBracket} from '@fortawesome/pro-light-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import strings from './login-strings.json';
import {SystemAssetsService} from '../../../services/system-assets.service';
import {Localization} from '../../../locale/localization';
import {authenticate, logout, selectAuthenticationState} from '../../../slices/auth-slice';
import {AuthState} from "../../../data/auth-state";
import { Logo } from '../../../components/static-components/logo/logo';

const __ = Localization(strings);

export function Login() {
    const dispatch = useDispatch();
    const navigate = useNavigate();
    const authState = useSelector(selectAuthenticationState);

    const [isAuthenticating, setIsAuthenticating] = useState(false);

    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');

    useEffect(() => {
        if (authState === AuthState.Authenticated) {
            navigate('/overview');
        }
        if (authState === AuthState.AuthenticationFailed) {
            setIsAuthenticating(false);
        }
    }, [navigate, authState]);

    const handleAuthenticate = (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        dispatch(logout());
        dispatch(authenticate({email: email.trim(), password}));
        setIsAuthenticating(true);
        return false;
    };

    return (
        <>
            <MetaElement
                title="Login"
            />

            <Container sx={{mt: 5}}>
                <Box
                    sx={{
                        px: 11,
                        py: 9,
                        mt: 5,
                        mb: 8,
                        border: '1px solid #efefef'
                    }}
                >
                    <Box sx={{mb: 4}}>
                        <Logo
                            width={200}
                            height={100}
                        />
                    </Box>
                    <Typography
                        variant="h5"
                        color="primary"
                    >
                        {__.title}
                    </Typography>
                    <Typography
                        variant="body2"
                        sx={{mt: 1}}
                    >
                        {__.subtitle}
                    </Typography>
                    <Box sx={{mt: 1, mb: 5}}>
                        <form
                            onSubmit={handleAuthenticate}
                            style={{maxWidth: '600px'}}
                        >
                            <TextField
                                value={email}
                                onChange={event => setEmail(event.target.value)}
                                type="email"
                                label={__.emailLabel}
                                placeholder={__.emailPlaceholder}
                                helperText={authState === AuthState.AuthenticationFailed && __.signInError}
                                error={authState === AuthState.AuthenticationFailed}
                                disabled={isAuthenticating}
                            />
                            <TextField
                                value={password}
                                onChange={event => setPassword(event.target.value)}
                                type="password"
                                label={__.passwordLabel}
                                helperText={authState === AuthState.AuthenticationFailed && __.signInError}
                                error={authState === AuthState.AuthenticationFailed}
                                disabled={isAuthenticating}
                            />
                            <Button
                                type="submit"
                                sx={{mt: 2}}
                                variant="contained"
                                size={'large'}
                                startIcon={
                                    <FontAwesomeIcon
                                        icon={faArrowRightToBracket}
                                        fixedWidth
                                        style={{marginTop: '-2px', marginRight: '4px'}}
                                    />
                                }
                                disabled={isAuthenticating}
                            >
                                {__.signInLabel}
                            </Button>
                        </form>
                    </Box></Box>
            </Container>
        </>
    );
}
