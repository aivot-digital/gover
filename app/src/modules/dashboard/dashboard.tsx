import {PageWrapper} from '../../components/page-wrapper/page-wrapper';
import {GenericPageHeader} from '../../components/generic-page-header/generic-page-header';
import {ModuleIcons} from '../../shells/staff/data/module-icons';
import {Box, Container, Divider, Grid, Typography} from '@mui/material';
import Logout from '@aivot/mui-material-symbols-400-outlined/dist/logout/Logout';
import {ProviderLinksGrid} from '../provider-links/components/provider-links-grid';
import React from 'react';
import {AppMode} from '../../data/app-mode';
import {Introductory} from '../../components/introductory/introductory';
import {DashboardNotificationsPanel} from './components/dashboard-notifications-panel';
import {DashboardStatsPanel} from './components/dashboard-stats-panel';
import {useLogout} from '../../hooks/use-logout';
import {DashboardHero} from './components/dashboard-hero';
import {DashboardProviderLinks} from './components/dashboard-provider-links';
import {DashboardFormsPanel} from './components/dashboard-forms-panel';

export function Dashboard() {
    const logout = useLogout();

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
                            icon: <Logout />,
                            tooltip: 'Abmelden',
                            onClick: logout,
                        },
                    ]}
                />

                <DashboardHero
                    sx={{
                        mt: 2,
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
                        <DashboardFormsPanel />
                    </Grid>
                    <Grid
                        size={{
                            xs: 12,
                            md: 6,
                        }}
                    >
                        <DashboardStatsPanel />
                    </Grid>
                </Grid>

                <DashboardProviderLinks
                    sx={{
                        mt: 4,
                    }}
                />
                <Box sx={{mt: 4}}>
                    <Divider sx={{borderColor: 'rgba(0, 0, 0, 0.15)', mx: -2}} />
                    <Box sx={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 2}}>
                        <Typography sx={{fontSize: '0.8125rem', color: 'rgba(0, 0, 0, 0.6)'}}>
                            Gover – Die quelloffene Plattform für Ende-zu-Ende digitalisierte Antragsprozesse.
                        </Typography>
                        <Typography sx={{fontSize: '0.8125rem', color: 'rgba(0, 0, 0, 0.6)'}}>
                            Entwickelt in Deutschland für die deutsche Verwaltung.
                            <svg width="18" height="12" style={{marginLeft: '10px', transform: 'translateY(2px)'}} viewBox="0 0 18 12" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M0 2C0 0.895431 0.895431 0 2 0H16C17.1046 0 18 0.895431 18 2V4H0V2Z" fill="#213048"/>
                                <rect y="4" width="18" height="4" fill="#EA312A"/>
                                <path d="M0 8H18V10C18 11.1046 17.1046 12 16 12H2C0.89543 12 0 11.1046 0 10V8Z" fill="#EEA53C"/>
                            </svg>
                        </Typography>
                    </Box>
                </Box>
            </Container>
        </PageWrapper>
    );
}