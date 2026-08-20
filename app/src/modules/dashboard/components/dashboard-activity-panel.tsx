import {alpha, Box, CircularProgress, Typography, useTheme} from '@mui/material';
import {
    AnimatedArea,
    type AnimatedAreaProps,
    LineChart,
} from '@mui/x-charts/LineChart';
import {DateTime} from 'luxon';
import QueryStats from '@aivot/mui-material-symbols-400-n25-outlined/QueryStats';
import BarChartIcon from '@aivot/mui-material-symbols-400-n25-outlined/BarChart';
import Info from '@aivot/mui-material-symbols-400-n25-outlined/Info';
import {type DashboardActivity, DashboardActivityPeriod} from '../models/dashboard-overview';
import {DashboardPanel} from './dashboard-panel';
import Balancer from 'react-wrap-balancer';

const exampleStartedValues = [2, 4, 3, 5, 2, 6, 4, 7, 5, 8, 6, 7];
const exampleCompletedValues = [1, 2, 2, 3, 2, 4, 3, 5, 4, 6, 5, 6];

function resample(values: number[], length: number): number[] {
    if (length === values.length) return values;
    // Preserve one calm example curve for both supported time ranges instead of maintaining duplicate datasets.
    return Array.from({length}, (_, index) => {
        const sourceIndex = index * (values.length - 1) / (length - 1);
        const lowerIndex = Math.floor(sourceIndex);
        const upperIndex = Math.ceil(sourceIndex);
        const fraction = sourceIndex - lowerIndex;
        return values[lowerIndex] + (values[upperIndex] - values[lowerIndex]) * fraction;
    });
}

interface DashboardActivityPanelProps {
    activity?: DashboardActivity;
    error?: boolean;
    initialPeriod?: DashboardActivityPeriod;
}

function DashboardGradientArea({ownerState, ...props}: AnimatedAreaProps) {
    const gradientId = `dashboard-activity-gradient-${ownerState.id}`;

    return (
        <>
            <defs>
                <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor={ownerState.color} stopOpacity={0.24}/>
                    <stop offset="100%" stopColor={ownerState.color} stopOpacity={0.01}/>
                </linearGradient>
            </defs>
            <AnimatedArea
                {...props}
                ownerState={{...ownerState, gradientId}}
            />
        </>
    );
}

export function DashboardActivityPanel({activity, error = false, initialPeriod}: DashboardActivityPanelProps) {
    const theme = useTheme();
    const period = activity?.period ?? initialPeriod ?? DashboardActivityPeriod.ThreeMonths;
    const usesDailyBuckets = period === DashboardActivityPeriod.ThirtyDays;
    const fallbackBucketCount = usesDailyBuckets ? 30 : 13;
    const periodLabel = usesDailyBuckets ? 'der letzten 30 Tage' : 'der letzten 3 Monate';
    const activityDataset = activity?.buckets.map((bucket) => ({
        period: DateTime.fromISO(bucket.periodStart).setLocale('de').toFormat('dd.MM.'),
        started: bucket.started,
        completed: bucket.completed,
    })) ?? [];
    const fallbackTimeline = Array.from({length: fallbackBucketCount}, (_, index) => ({
        period: (usesDailyBuckets
            ? DateTime.now().startOf('day').minus({days: fallbackBucketCount - 1 - index})
            : DateTime.now().startOf('week').minus({weeks: fallbackBucketCount - 1 - index})
        ).setLocale('de').toFormat('dd.MM.'),
        started: 0,
        completed: 0,
    }));
    const timeline = activityDataset.length > 0 ? activityDataset : fallbackTimeline;
    const hasChartData = (activity?.started ?? 0) + (activity?.completed ?? 0) > 0;
    const exampleStarted = resample(exampleStartedValues, timeline.length);
    const exampleCompleted = resample(exampleCompletedValues, timeline.length);
    const dataset = hasChartData ? timeline : timeline.map((bucket, index) => ({
        ...bucket,
        started: exampleStarted[index],
        completed: exampleCompleted[index],
    }));
    const metrics = [
        {label: 'Gestartet', value: activity?.started ?? 0, borderColor: 'primary.main'},
        {label: 'Abgeschlossen', value: activity?.completed ?? 0, borderColor: 'secondary.main'},
        {label: 'Aktuell in Bearbeitung', value: activity?.active ?? 0, borderColor: 'divider'},
    ];

    return (
        <DashboardPanel sx={{px: {xs: 2, sm: 2.75}, pt: 2.5, pb: 2, overflow: 'hidden'}}>
            <Box sx={{display: 'flex', alignItems: 'flex-start', gap: 1.5}}>
                <Box sx={{width: 40, height: 40, display: 'grid', placeItems: 'center', borderRadius: '50%', bgcolor: alpha(theme.palette.secondary.main, 0.16), color: 'secondary.dark'}}>
                    <QueryStats/>
                </Box>
                <Box>
                    <Typography variant="h6" component="h2">Vorgangsaktivität</Typography>
                    <Typography variant="body2" color="text.secondary">Gestartete und abgeschlossene Vorgänge {periodLabel}</Typography>
                </Box>
            </Box>

            {activity == null && !error && <Box sx={{height: 300, display: 'grid', placeItems: 'center'}}><CircularProgress size={28}/></Box>}
            {error && <Box sx={{height: 260, display: 'grid', placeItems: 'center', textAlign: 'center'}}><Box><Typography fontWeight={600}>Vorgangsaktivität konnte nicht geladen werden</Typography><Typography variant="body2" color="text.secondary"><Balancer>Die Vorgänge sind weiterhin über die Navigation erreichbar.</Balancer></Typography></Box></Box>}
            {activity != null && (
                <>
                    <Box sx={{display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gap: 1, mt: 2.5, mb: 1}}>
                        {metrics.map((metric) => (
                            <Box key={metric.label} sx={{borderLeft: '2px solid', borderColor: metric.borderColor, pl: 1.5}}>
                                <Typography sx={{fontSize: '1.5rem', fontWeight: 650, lineHeight: 1.2}}>{metric.value}</Typography>
                                <Typography variant="caption" color="text.secondary">{metric.label}</Typography>
                            </Box>
                        ))}
                    </Box>
                    {!hasChartData && (
                        <Box
                            sx={{
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                gap: 1.25,
                                mt: 2,
                                mb: 0.5,
                                px: 1.5,
                                py: 1.25,
                                borderRadius: 1,
                                bgcolor: 'action.hover',
                                textAlign: 'center',
                            }}
                        >
                            <BarChartIcon sx={{fontSize: 19, color: 'text.secondary', flexShrink: 0}}/>
                            <Typography variant="body2" color="text.secondary">
                                <Balancer>
                                    <Box component="span" sx={{fontWeight: 650, color: 'text.primary'}}>
                                        Im ausgewählten Zeitraum wurden keine Vorgänge gestartet oder abgeschlossen.
                                    </Box>{' '}
                                    Das Diagramm zeigt deshalb Beispieldaten.
                                </Balancer>
                            </Typography>
                        </Box>
                    )}
                    <LineChart
                        dataset={dataset}
                        xAxis={[{
                            scaleType: 'point',
                            dataKey: 'period',
                            tickLabelInterval: (_, index) => index % (usesDailyBuckets ? 5 : 2) === 0 || index === dataset.length - 1,
                        }]}
                        yAxis={[{min: 0, valueFormatter: (value: number) => String(Math.round(value))}]}
                        series={[
                            {
                                id: 'started',
                                dataKey: 'started',
                                label: hasChartData ? 'Gestartet' : 'Gestartet (Beispiel)',
                                color: hasChartData
                                    ? theme.palette.primary.main
                                    : alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.62 : 0.42),
                                curve: 'monotoneX',
                                showMark: hasChartData,
                                disableHighlight: !hasChartData,
                                area: true,
                            },
                            {
                                id: 'completed',
                                dataKey: 'completed',
                                label: hasChartData ? 'Abgeschlossen' : 'Abgeschlossen (Beispiel)',
                                color: hasChartData
                                    ? theme.palette.secondary.main
                                    : alpha(theme.palette.secondary.main, theme.palette.mode === 'dark' ? 0.68 : 0.52),
                                curve: 'monotoneX',
                                showMark: hasChartData,
                                disableHighlight: !hasChartData,
                                area: true,
                            },
                        ]}
                        slots={{area: DashboardGradientArea}}
                        slotProps={{tooltip: {trigger: hasChartData ? 'axis' : 'none'}}}
                        disableAxisListener={!hasChartData}
                        disableLineItemHighlight={!hasChartData}
                        axisHighlight={{x: hasChartData ? 'line' : 'none'}}
                        height={280}
                        grid={{horizontal: true}}
                        margin={{left: 24, right: 24, top: 20, bottom: 24}}
                        sx={{
                            // MUI X does not expose the initial reveal duration as a chart prop.
                            '& .MuiAppearingMask-animate': {
                                animationDuration: '1600ms !important',
                            },
                        }}
                    />
                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'flex-start',
                            justifyContent: 'center',
                            gap: 0.75,
                            mt: 0.5,
                            px: {sm: 1.5},
                        }}
                    >
                        <Info sx={{fontSize: 16, color: 'text.disabled', mt: '1px', flexShrink: 0}}/>
                        <Typography variant="caption" color="text.secondary">
                            <Balancer>
                                Die Auswertung berücksichtigt nur Vorgänge, auf die Sie zugreifen dürfen. Sie bildet daher möglicherweise nicht alle Vorgänge dieser Prosuna-Instanz ab.
                            </Balancer>
                        </Typography>
                    </Box>
                </>
            )}
        </DashboardPanel>
    );
}
