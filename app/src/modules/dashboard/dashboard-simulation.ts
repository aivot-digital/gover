import {DateTime} from 'luxon';
import {getApplicationTimeZone} from '../../utils/temporal-utils';
import {DashboardActivityPeriod, type DashboardActivity, type DashboardOverview} from './models/dashboard-overview';

export const SIMULATED_DASHBOARD_TASK_COUNT = 17;

interface SimulatedDashboardData {
    overview: DashboardOverview;
    activity: DashboardActivity;
}

export function createSimulatedDashboardData(
    period: DashboardActivityPeriod,
    now: DateTime = DateTime.now(),
): SimulatedDashboardData {
    const today = now.setZone(getApplicationTimeZone()).startOf('day');
    const instant = (daysAgo: number) => today.minus({days: daysAgo}).set({hour: 10}).toUTC().toISO()!;
    const taskExamples = [
        {processTitle: 'Hundesteuer anmelden', taskName: 'Angaben zur Hundehaltung prüfen', prefix: 'HST', daysAgo: 8, dueIn: -2},
        {processTitle: 'Bewohnerparkausweis beantragen', taskName: 'Wohnsitz und Fahrzeugnachweis prüfen', prefix: 'BPA', daysAgo: 4, dueIn: 1},
        {processTitle: 'Sondernutzungserlaubnis beantragen', taskName: 'Stellungnahme zur Flächennutzung abgeben', prefix: 'SNE', daysAgo: 3, dueIn: 3},
        {processTitle: 'Fördermittel beantragen', taskName: 'Finanzierungsplan fachlich prüfen', prefix: 'FÖM', daysAgo: 2, dueIn: 5},
    ];
    const overview: DashboardOverview = {
        tasks: {
            total: SIMULATED_DASHBOARD_TASK_COUNT,
            overdue: 1,
            items: taskExamples.map((task, index) => ({
                id: -(index + 1),
                processInstanceId: -(index + 1),
                processId: -(index + 1),
                processVersion: 1,
                taskName: task.taskName,
                processTitle: task.processTitle,
                caseNumber: `${task.prefix}-${today.year}-${String(142 + index * 37).padStart(5, '0')}`,
                started: instant(task.daysAgo),
                deadline: instant(-task.dueIn),
            })),
        },
        recentProcesses: ['Mitarbeitende onboarden', 'Dienstreisen genehmigen', 'Beschaffungen freigeben']
            .map((title, index) => ({
                id: -(index + 5),
                title,
                draftedVersion: index === 1 ? null : index + 2,
                publishedVersion: index + 1,
                updated: instant(index + 1),
            })),
    };

    const daily = period === DashboardActivityPeriod.ThirtyDays;
    const bucketCount = daily ? 30 : 13;
    const bucketDays = daily ? 1 : 7;
    const lastBucketStart = daily ? today : today.startOf('week');
    // Curated profiles alternate calm sections, intake peaks and periods of catching up.
    // Both demo periods start without a backlog and end with exactly 62 active cases.
    const startedProfile = daily
        ? [6, 7, 6, 6, 7, 9, 15, 19, 23, 24, 20, 14, 9, 8, 8, 9, 8, 8, 5, 5, 6, 8, 10, 14, 18, 19, 15, 11, 10, 11]
        : [30, 31, 32, 54, 76, 55, 34, 33, 34, 51, 63, 53, 46];
    const completedProfile = daily
        ? [3, 3, 4, 3, 4, 5, 6, 7, 10, 11, 12, 13, 12, 10, 9, 10, 9, 9, 7, 6, 5, 6, 8, 14, 19, 19, 17, 12, 11, 12]
        : [17, 18, 19, 32, 48, 61, 47, 36, 37, 39, 65, 60, 51];
    const buckets = Array.from({length: bucketCount}, (_, index) => {
        const start = lastBucketStart.minus({days: (bucketCount - index - 1) * bucketDays});
        return {
            periodStart: start.toISODate()!,
            started: startedProfile[index],
            completed: completedProfile[index],
        };
    });
    const started = buckets.reduce((sum, bucket) => sum + bucket.started, 0);
    const completed = buckets.reduce((sum, bucket) => sum + bucket.completed, 0);

    return {
        overview,
        activity: {
            available: true,
            period,
            started,
            completed,
            active: started - completed,
            buckets,
        },
    };
}
