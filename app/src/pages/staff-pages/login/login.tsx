import {Box, Button, Link, Typography, useTheme} from '@mui/material';
import React, {useMemo} from 'react';
import {MetaElement} from '../../../components/meta-element/meta-element';
import LoginOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Login';
import OpenInNew from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import {AuthService} from '../../../services/auth-service';
import {useLocation} from 'react-router-dom';
import {ProsunaLogo} from '../../../components/prosuna-logo/prosuna-logo';

function createProsunaUrl(placement: 'logo' | 'footer'): string {
    const url = new URL('https://prosuna.de/');
    url.search = new URLSearchParams({
        utm_source: 'prosuna_instance',
        utm_medium: 'referral',
        utm_campaign: 'staff_login',
        utm_content: placement === 'logo' ? 'logo' : 'footer_link',
    }).toString();
    return url.toString();
}

export function Login() {
    const location = useLocation();
    const theme = useTheme();
    const isDarkMode = theme.palette.mode === 'dark';
    const heroForeground = isDarkMode ? '#FFFFFF' : '#733635';

    const loginUrl = useMemo(() => {
        return AuthService.getLoginUrl(location);
    }, [location]);

    return (
        <>
            <MetaElement
                title="Anmeldung erforderlich"
            />

            <Box
                component="main"
                sx={{
                    width: '100%',
                    minHeight: '100dvh',
                    display: 'grid',
                    gridTemplateColumns: {
                        xs: 'minmax(0, 1fr)',
                        md: 'minmax(0, 1fr) minmax(20rem, 40%)',
                    },
                    gridTemplateAreas: {
                        xs: '"hero" "login"',
                        md: '"login hero"',
                    },
                    backgroundColor: 'background.default',
                }}
            >
                <Box
                    component="header"
                    sx={{
                        gridArea: 'hero',
                        position: 'relative',
                        minHeight: {
                            xs: '9rem',
                            sm: '11rem',
                            md: '100dvh',
                        },
                        px: {
                            xs: 3,
                            sm: 5,
                            md: 7,
                        },
                        py: {
                            xs: 3,
                            sm: 4,
                            md: 6,
                        },
                        display: 'flex',
                        flexDirection: 'column',
                        overflow: 'hidden',
                        backgroundColor: '#F3F0EB',
                        isolation: 'isolate',
                        '&::before': {
                            content: '""',
                            position: 'absolute',
                            inset: 0,
                            zIndex: -2,
                            backgroundImage: `url("/staff/assets/images/login-background-${theme.palette.mode}.jpg")`,
                            backgroundSize: 'cover',
                            backgroundPosition: {
                                xs: 'center 30%',
                                md: 'center right',
                            },
                        },
                        '&::after': {
                            content: '""',
                            position: 'absolute',
                            inset: 0,
                            zIndex: -1,
                            backgroundColor: 'rgba(255, 255, 255, 0.04)',
                        },
                    }}
                >
                    <Link
                        href={createProsunaUrl('logo')}
                        target="_blank"
                        rel="noopener noreferrer"
                        underline="none"
                        aria-label="Mehr über Prosuna"
                        title="Mehr über Prosuna – öffnet in einem neuen Tab"
                        sx={{
                            position: 'relative',
                            zIndex: 1,
                            display: 'inline-flex',
                            width: 'fit-content',
                            lineHeight: 0,
                            transition: theme.transitions.create('opacity'),
                            '&:hover': {
                                opacity: 0.82,
                            },
                            '&:focus-visible': {
                                outline: '2px solid',
                                outlineColor: heroForeground,
                                outlineOffset: 4,
                                borderRadius: 1,
                            },
                        }}
                    >
                        <ProsunaLogo
                            colorVariant={isDarkMode ? 'monochrome' : 'brand'}
                            style={{
                                width: 'min(13.5rem, 62vw)',
                                height: 'auto',
                                color: heroForeground,
                            }}
                        />
                    </Link>

                    <Box
                        sx={{
                            position: 'relative',
                            zIndex: 1,
                            display: {
                                xs: 'none',
                                md: 'flex',
                            },
                            mt: 'auto',
                            mx: -7,
                            mb: -6,
                            px: 7,
                            py: 3,
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            flexWrap: 'wrap',
                            columnGap: 4,
                            rowGap: 1.5,
                            color: heroForeground,
                            backgroundColor: isDarkMode
                                ? 'rgba(8, 8, 8, 0.58)'
                                : 'rgba(255, 255, 255, 0.68)',
                            borderTop: '1px solid',
                            borderColor: isDarkMode
                                ? 'rgba(255, 255, 255, 0.16)'
                                : 'rgba(115, 54, 53, 0.12)',
                            backdropFilter: 'blur(4px)',
                        }}
                    >
                        <Typography
                            variant="subtitle1"
                            component="p"
                            sx={{
                                maxWidth: '22rem',
                                color: 'inherit',
                                fontWeight: 600,
                                lineHeight: 1.45,
                            }}
                        >
                            Digitale Verwaltungsprozesse<br/>
                            von Anfang bis Ende.
                        </Typography>

                        <Link
                            href={createProsunaUrl('footer')}
                            target="_blank"
                            rel="noopener noreferrer"
                            underline="none"
                            title="Mehr über Prosuna – öffnet in einem neuen Tab"
                            sx={{
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: 0.75,
                                flex: '0 0 auto',
                                ml: 'auto',
                                color: 'inherit',
                                fontSize: '0.875rem',
                                fontWeight: 600,
                                whiteSpace: 'nowrap',
                                transition: theme.transitions.create('opacity'),
                                '&:hover': {
                                    opacity: 0.72,
                                },
                                '&:focus-visible': {
                                    outline: '2px solid',
                                    outlineColor: heroForeground,
                                    outlineOffset: 4,
                                    borderRadius: 1,
                                },
                            }}
                        >
                            Mehr über Prosuna
                            <OpenInNew
                                aria-hidden="true"
                                sx={{fontSize: '0.95rem'}}
                            />
                        </Link>
                    </Box>
                </Box>

                <Box
                    component="section"
                    aria-labelledby="login-heading"
                    sx={{
                        gridArea: 'login',
                        minHeight: {
                            xs: 'calc(100dvh - 9rem)',
                            sm: 'calc(100dvh - 11rem)',
                            md: '100dvh',
                        },
                        px: {
                            xs: 3,
                            sm: 6,
                            lg: 10,
                        },
                        py: {
                            xs: 5,
                            sm: 7,
                        },
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        backgroundColor: 'background.paper',
                    }}
                >
                    <Box sx={{width: '100%', maxWidth: '29rem'}}>
                        <Typography
                            id="login-heading"
                            variant="h1"
                            component="h1"
                            sx={{
                                mb: 2,
                                fontSize: {
                                    xs: '1.75rem',
                                    sm: '2rem',
                                },
                                lineHeight: 1.25,
                                letterSpacing: 0,
                            }}
                        >
                            Willkommen bei Prosuna
                        </Typography>

                        <Typography
                            variant="body1"
                            sx={{
                                color: 'text.secondary',
                                fontSize: '1.0625rem',
                                lineHeight: 1.65,
                            }}
                        >
                            Für diesen Bereich ist eine Anmeldung erforderlich. Melden Sie sich mit Ihrem
                            persönlichen Mitarbeitenden-Konto an, um darauf zuzugreifen.
                        </Typography>

                        <Button
                            variant="contained"
                            component="a"
                            startIcon={<LoginOutlinedIcon/>}
                            size="large"
                            fullWidth
                            sx={{
                                mt: 4,
                                minHeight: 52,
                                px: 3,
                                whiteSpace: 'normal',
                            }}
                            href={loginUrl}
                            disabled={loginUrl.length === 0}
                        >
                            Mit Mitarbeitenden-Konto anmelden
                        </Button>

                        <Typography
                            variant="body2"
                            sx={{
                                mt: 2,
                                color: 'text.secondary',
                                fontSize: '0.875rem',
                                textAlign: 'center',
                            }}
                        >
                            Sie werden zum verwendeten Identitätsdienst weitergeleitet.
                        </Typography>
                    </Box>
                </Box>
            </Box>
        </>
    );
}
