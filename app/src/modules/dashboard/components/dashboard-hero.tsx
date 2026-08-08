import {Box, Divider, Paper, SxProps, Typography} from '@mui/material';
import React from 'react';
import ArrowForward from '@aivot/mui-material-symbols-400-n25-outlined/ArrowForward';
import NorthWest from '@aivot/mui-material-symbols-400-n25-outlined/NorthWest';

interface DashboardHeroProps {
    sx?: SxProps;
}

const Links = [
    {
        Icon: NorthWest,
        text: 'Neu in Prosuna 5: Einfacher. Schneller. Intelligenter. Jetzt die neuen Möglichkeiten für Prozesse entdecken.',
        href: 'https://docs.prosuna.de',
    },
    {
        Icon: ArrowForward,
        text: 'Neu hier? Prosuna in kurzer Einführung kennenlernen.',
        href: 'https://docs.prosuna.de',
    },
];

export function DashboardHero(props: DashboardHeroProps) {
    return (
        <Paper
            variant="outlined"
            sx={{
                backgroundColor: 'background.paper',
                borderTop: '4px solid',
                borderTopColor: 'primary.main',
                ...props.sx,
            }}
        >
            <Box
                sx={{
                    flex: 1,
                    p: 4,
                }}
            >
                <Typography
                    variant="h2"
                    fontSize="1.75rem"
                >
                    Willkommen bei Prosuna!
                </Typography>

                <Typography
                    variant="h2"
                    fontSize="2.5rem"
                    fontWeight={800}
                    lineHeight="2.625rem"
                    sx={{
                        mt: 2,
                    }}
                >
                    {AppConfig.providerName}
                </Typography>

                <Divider
                    sx={{
                        my: 4,
                        borderColor: 'divider',
                    }}
                />

                {
                    Links.map(({Icon, text, href}) => (
                        <Typography
                            key={text}
                            variant="h4"
                            fontSize="1.125rem"
                            lineHeight="1.25rem"
                            fontWeight="normal"
                            component="a"
                            href={href}
                            target="_blank"
                            sx={[
                                {
                                    maxWidth: '480px',
                                    display: 'flex',
                                    justifyContent: 'flex-general-information',
                                    mt: 2,
                                    mb: 2,
                                    transition: '200ms all ease-in-out',
                                    cursor: 'pointer',
                                    textDecoration: 'none',
                                    color: 'text.secondary',
                                },
                                {
                                    '&:hover': {
                                        color: 'text.primary',
                                    },
                                }]}
                        >
                            <Icon
                                sx={{
                                    marginRight: '6px',
                                    flexShrink: 0,
                                    fontSize: '0.9em',
                                }}
                            />
                            <span>{text}</span>
                        </Typography>
                    ))
                }
            </Box>
        </Paper>
    );
}
