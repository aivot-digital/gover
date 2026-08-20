import {PageWrapper} from '../../components/page-wrapper/page-wrapper';
import {Box, Divider, Grid, Typography} from '@mui/material';
import React, {useEffect, useMemo, useState} from 'react';
import {
    CanvasConfettiOverlay,
    prosunaConfettiColors,
} from '../../components/confetti/canvas-confetti-overlay';
import {DashboardGreeting} from './components/dashboard-greeting';
import {DashboardTasksPanel} from './components/dashboard-tasks-panel';
import {DashboardLinksPanel} from './components/dashboard-links-panel';
import {DashboardActivityPanel} from './components/dashboard-activity-panel';
import {DashboardRecentProcessesPanel} from './components/dashboard-recent-processes-panel';
import {DashboardApiService} from './dashboard-api-service';
import {
    type DashboardActivity,
    DashboardActivityPeriod,
    DashboardActivityPeriodConfig,
    type DashboardOverview,
} from './models/dashboard-overview';
import {dispatchProcessAssignedTaskCountRefreshEvent} from '../process/utils/process-assigned-task-count-events';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';

const germanyFlagColors = ['#213048', '#EA312A', '#EEA53C'];

export function Dashboard() {
    const [flagConfettiPlayKey, setFlagConfettiPlayKey] = useState<number | null>(null);
    const [overview, setOverview] = useState<DashboardOverview>();
    const [activity, setActivity] = useState<DashboardActivity>();
    const [overviewError, setOverviewError] = useState(false);
    const [activityError, setActivityError] = useState(false);
    const service = useMemo(() => new DashboardApiService(), []);
    const activityEnabledConfig = useAppSelector(selectSystemConfigValue(SystemConfigKeys.dashboard.activity.enabled));
    const activityPeriodConfig = useAppSelector(selectSystemConfigValue(SystemConfigKeys.dashboard.activity.period));

    useEffect(() => {
        dispatchProcessAssignedTaskCountRefreshEvent();
        service.fetchOverview().then(setOverview).catch(() => setOverviewError(true));
        service.fetchActivity().then(setActivity).catch(() => setActivityError(true));
    }, [service]);

    const recentProcesses = overview?.recentProcesses ?? [];
    // These public display configs are known before the activity request and keep the initial layout stable.
    const activityExpected = activityEnabledConfig !== 'false';
    const initialActivityPeriod = activityPeriodConfig === DashboardActivityPeriodConfig.ThirtyDays
        ? DashboardActivityPeriod.ThirtyDays
        : DashboardActivityPeriod.ThreeMonths;
    const showActivity = activity?.available === true || (activity == null && activityExpected) || (activityError && activityExpected);
    const showRecentProcesses = recentProcesses.length > 0;

    return (
        <PageWrapper
            title="Übersicht"
            background
        >
            <DashboardGreeting/>

            <Grid container spacing={3} alignItems="stretch" sx={{mt: 2.75}}>
                <Grid size={{xs: 12, lg: showRecentProcesses ? 8 : 12}} sx={{display: 'flex'}}>
                    <DashboardTasksPanel summary={overview?.tasks} error={overviewError}/>
                </Grid>
                {showRecentProcesses && (
                    <Grid size={{xs: 12, lg: 4}} sx={{display: 'flex'}}>
                        <DashboardRecentProcessesPanel processes={recentProcesses}/>
                    </Grid>
                )}
                {showActivity && (
                    <Grid size={12}>
                        <DashboardActivityPanel
                            activity={activity}
                            error={activityError}
                            initialPeriod={initialActivityPeriod}
                        />
                    </Grid>
                )}
                <Grid size={12}>
                    <DashboardLinksPanel/>
                </Grid>
            </Grid>

            <Box sx={{mt: 5}}>
                <Divider sx={{borderColor: 'divider', mx: -2}}/>
                <Box sx={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 1.5, mt: 2}}>
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
        </PageWrapper>
    );
}
