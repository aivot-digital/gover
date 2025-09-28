import {PageWrapper} from '../../components/page-wrapper/page-wrapper';
import {GenericPageHeader} from '../../components/generic-page-header/generic-page-header';
import {ModuleIcons} from '../../shells/staff/data/module-icons';
import {Container, Grid} from '@mui/material';
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
                    spacing={2}
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
            </Container>
        </PageWrapper>
    );
}