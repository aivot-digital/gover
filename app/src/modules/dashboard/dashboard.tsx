import {PageWrapper} from '../../components/page-wrapper/page-wrapper';
import {GenericPageHeader} from '../../components/generic-page-header/generic-page-header';
import {ModuleIcons} from '../../shells/staff/data/module-icons';
import {Box, Container, Divider, Grid, Typography} from '@mui/material';
import Logout from '@aivot/mui-material-symbols-400-n25-outlined/Logout';
import React, {useState} from 'react';
import {DashboardStatsPanel} from './components/dashboard-stats-panel';
import {useLogout} from '../../hooks/use-logout';
import {DashboardHero} from './components/dashboard-hero';
import {DashboardProviderLinks} from './components/dashboard-provider-links';
import {DashboardProcessesPanel} from './components/dashboard-processes-panel';
import {
    CanvasConfettiOverlay,
    prosunaConfettiColors,
} from '../../components/confetti/canvas-confetti-overlay';

const germanyFlagColors = ['#213048', '#EA312A', '#EEA53C'];

export function Dashboard() {
    const logout = useLogout();
    const [flagConfettiPlayKey, setFlagConfettiPlayKey] = useState<number | null>(null);

    return (
        <PageWrapper
            title="Übersicht"
            fullWidth
            background
        >
            <Container>
                <GenericPageHeader
                    icon={ModuleIcons.dashboard}
                    title="Übersicht"
                    actions={[
                        {
                            icon: ModuleIcons.settings,
                            tooltip: 'Einstellungen',
                            to: '/settings/app',
                        },
                        'separator',
                        {
                            icon: <Logout/>,
                            tooltip: 'Abmelden',
                            onClick: logout,
                        },
                    ]}
                />

                <DashboardHero
                    sx={{
                        mt: 2.75,
                        mb: 4,
                    }}
                />

                <Grid
                    container={true}
                    spacing={4}
                >
                    <Grid
                        size={{
                            xs: 12,
                            md: 6,
                        }}
                    >
                        <DashboardProcessesPanel/>
                    </Grid>
                    <Grid
                        size={{
                            xs: 12,
                            md: 6,
                        }}
                    >
                        <DashboardStatsPanel/>
                    </Grid>
                </Grid>

                <DashboardProviderLinks
                    sx={{
                        mt: 4,
                    }}
                />
                <Box sx={{mt: 4}}>
                    <Divider sx={{borderColor: 'divider', mx: -2}}/>
                    <Box sx={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 2}}>
                        <Typography sx={{fontSize: '0.8125rem', color: 'text.secondary'}}>
                            Prosuna – Die quelloffene Plattform für Ende-zu-Ende digitalisierte Verwaltungsprozesse.
                        </Typography>
                        <Typography
                            sx={{
                                fontSize: '0.8125rem',
                                color: 'text.secondary',
                                display: 'flex',
                                alignItems: 'center',
                            }}
                        >
                            Entwickelt in Deutschland für souveräne Organisationen.
                            <Box
                                component="button"
                                type="button"
                                aria-label="Deutschlandflagge feiern"
                                onClick={() => {
                                    setFlagConfettiPlayKey((currentValue) => (currentValue ?? 0) + 1);
                                }}
                                sx={{
                                    display: 'inline-flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    ml: 1.25,
                                    p: 0,
                                    border: 0,
                                    background: 'transparent',
                                    cursor: 'pointer',
                                    '& .dashboard-germany-flag-outline': {
                                        stroke: (theme) => theme.palette.mode === 'dark'
                                            ? 'rgba(255, 255, 255, 0.5)'
                                            : 'transparent',
                                    },
                                }}
                            >
                                <svg
                                    width="18"
                                    height="12"
                                    viewBox="0 0 18 12"
                                    fill="none"
                                    xmlns="http://www.w3.org/2000/svg"
                                    style={{transform: 'translateY(2px)'}}
                                >
                                    <path
                                        d="M0 2C0 0.895431 0.895431 0 2 0H16C17.1046 0 18 0.895431 18 2V4H0V2Z"
                                        fill={germanyFlagColors[0]}
                                    />
                                    <rect
                                        y="4"
                                        width="18"
                                        height="4"
                                        fill={germanyFlagColors[1]}
                                    />
                                    <path
                                        d="M0 8H18V10C18 11.1046 17.1046 12 16 12H2C0.89543 12 0 11.1046 0 10V8Z"
                                        fill={germanyFlagColors[2]}
                                    />
                                    <rect
                                        className="dashboard-germany-flag-outline"
                                        x="0.5"
                                        y="0.5"
                                        width="17"
                                        height="11"
                                        rx="1.5"
                                        fill="none"
                                        strokeWidth="1"
                                    />
                                </svg>
                            </Box>
                        </Typography>
                    </Box>
                </Box>
                <CanvasConfettiOverlay
                    playKey={flagConfettiPlayKey}
                    colors={prosunaConfettiColors}
                />
            </Container>
        </PageWrapper>
    );
}
