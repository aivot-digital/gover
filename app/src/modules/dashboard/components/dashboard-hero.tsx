import HeroDecord from './hero-decor.svg';
import {Box, Divider, Paper, Skeleton, SxProps, Typography, useTheme} from '@mui/material';
import React from 'react';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectSetup} from '../../../slices/shell-slice';
import ArrowForward from '@aivot/mui-material-symbols-400-outlined/dist/arrow-forward/ArrowForward';
import NorthWest from '@aivot/mui-material-symbols-400-outlined/dist/north-west/NorthWest';

interface DashboardHeroProps {
    sx?: SxProps;
}

const Links = [
    {
        Icon: ArrowForward,
        text: 'Zur Benutzereinführung der Gover-Plattform',
        href: 'https://wiki.teamaivot.de/dokumentation/gover/benutzerhandbuch/erste-schritte/home',
    },
    {
        Icon: NorthWest,
        text: 'Alles neu – was jetzt? Die neuen Arbeitsabläufe und Prozessschritte für Online-Formulare einfach erklärt',
        href: 'https://wiki.teamaivot.de/dokumentation/gover/benutzerhandbuch/home',
    },
];

export function DashboardHero(props: DashboardHeroProps) {
    const theme = useTheme();
    const setup = useAppSelector(selectSetup);

    if (setup == null) {
        return <Skeleton height="150px" />;
    }

    return (
        <Paper
            sx={{
                display: 'flex',
                backgroundColor: 'primary.dark',
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
                    color="white"
                >
                    Herzlich willkommen!
                </Typography>

                <Typography
                    variant="h2"
                    fontSize="2.5rem"
                    fontWeight={800}
                    lineHeight="2.625rem"
                    color="white"
                    sx={{
                        mt: 2,
                    }}
                >
                    <span style={{color: theme.palette.secondary.main}}>
                        Online-Antrags-Platform
                    </span>
                    <br />
                    {setup.providerName}
                </Typography>

                <Divider
                    sx={{
                        my: 4,
                        borderColor: (theme) => theme.palette.secondary.main,
                    }}
                />

                {
                    Links.map(({Icon, text, href}) => (
                        <Typography
                            key={href}
                            variant="h4"
                            fontSize="1.125rem"
                            lineHeight="1.25rem"
                            color="white"
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
                                },
                                {
                                    '&:hover': {
                                        color: (theme) => theme.palette.secondary.main,
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
            <Box
                sx={{
                    flex: 1,
                    backgroundImage: `url(${HeroDecord})`,
                    backgroundPosition: 'center',
                }}
            >
            </Box>
        </Paper>
    );
}