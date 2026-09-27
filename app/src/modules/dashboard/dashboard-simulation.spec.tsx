import React from 'react';
import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {MemoryRouter, useNavigate} from 'react-router-dom';
import {DateTime} from 'luxon';
import {Dashboard} from './dashboard';
import {DashboardApiService} from './dashboard-api-service';
import {createSimulatedDashboardData} from './dashboard-simulation';
import {DashboardActivityPeriod, type DashboardActivity, type DashboardOverview} from './models/dashboard-overview';
import {useAppSelector} from '../../hooks/use-app-selector';
import {dispatchProcessAssignedTaskCountRefreshEvent} from '../process/utils/process-assigned-task-count-events';

vi.mock('../../components/page-wrapper/page-wrapper', () => ({
    PageWrapper: ({children}: {children: React.ReactNode}) => <main>{children}</main>,
}));
vi.mock('../../hooks/use-app-selector', () => ({useAppSelector: vi.fn()}));
vi.mock('./components/dashboard-greeting', () => ({DashboardGreeting: () => null}));
vi.mock('./components/dashboard-links-panel', () => ({DashboardLinksPanel: () => null}));
vi.mock('./components/dashboard-activity-panel', () => ({
    DashboardActivityPanel: ({activity}: {activity?: DashboardActivity}) => (
        <section aria-label="Vorgangsaktivität">{activity?.period}: {activity?.started} gestartet</section>
    ),
}));
vi.mock('../process/utils/process-assigned-task-count-events', () => ({
    dispatchProcessAssignedTaskCountRefreshEvent: vi.fn(),
}));

const liveOverview: DashboardOverview = {
    tasks: {total: 0, overdue: 0, items: []},
    recentProcesses: [{id: 42, title: 'Echter Prozess', draftedVersion: 2, publishedVersion: 1, updated: '2026-09-01T10:00:00Z'}],
};
const liveActivity: DashboardActivity = {
    available: false, period: DashboardActivityPeriod.ThreeMonths, started: 0, completed: 0, active: 0, buckets: [],
};

function Navigation() {
    const navigate = useNavigate();
    return <>
        <button onClick={() => navigate('/?simulate=1')}>Simulation öffnen</button>
        <button onClick={() => navigate('/')}>Echtdaten öffnen</button>
    </>;
}

function renderDashboard(url: string) {
    return render(<MemoryRouter initialEntries={[url]}><Navigation/><Dashboard/></MemoryRouter>);
}

describe('Dashboard simulation', () => {
    beforeEach(() => {
        vi.mocked(useAppSelector).mockReturnValue(undefined);
        vi.spyOn(DashboardApiService.prototype, 'fetchOverview').mockResolvedValue(liveOverview);
        vi.spyOn(DashboardApiService.prototype, 'fetchActivity').mockResolvedValue(liveActivity);
    });

    it('shows demo data even with activity disabled and never links to fictitious resources', () => {
        vi.mocked(useAppSelector).mockReturnValue('false');
        renderDashboard('/?simulate=1');

        expect(screen.getByText('17 offen')).toBeVisible();
        expect(screen.getByText(/Hundesteuer anmelden/)).toBeVisible();
        expect(screen.getByText('Mitarbeitende onboarden')).toBeVisible();
        expect(screen.getByRole('region', {name: 'Vorgangsaktivität'})).toHaveTextContent('ThreeMonths');
        expect(screen.queryByRole('link', {name: /Hundehaltung/})).not.toBeInTheDocument();
        expect(screen.queryByRole('link', {name: /Mitarbeitende onboarden/})).not.toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'Alle Aufgaben ansehen'})).toHaveAttribute('href', '/tasks');
        expect(DashboardApiService.prototype.fetchOverview).not.toHaveBeenCalled();
        expect(DashboardApiService.prototype.fetchActivity).not.toHaveBeenCalled();
        expect(dispatchProcessAssignedTaskCountRefreshEvent).not.toHaveBeenCalled();
    });

    it('uses the configured 30-day period', () => {
        vi.mocked(useAppSelector).mockReturnValue('0');
        renderDashboard('/?simulate=1');
        expect(screen.getByRole('region', {name: 'Vorgangsaktivität'})).toHaveTextContent('ThirtyDays');
    });

    it.each(['/', '/?simulate=0', '/?simulate=true'])('keeps live data and navigation for %s', async (url) => {
        renderDashboard(url);
        expect(await screen.findByRole('link', {name: /Echter Prozess/})).toHaveAttribute('href', '/processes/42/versions/2');
        expect(screen.getByText('Alles erledigt')).toBeVisible();
        expect(screen.queryByRole('region', {name: 'Vorgangsaktivität'})).not.toBeInTheDocument();
        expect(DashboardApiService.prototype.fetchOverview).toHaveBeenCalledTimes(1);
        expect(DashboardApiService.prototype.fetchActivity).toHaveBeenCalledTimes(1);
        expect(dispatchProcessAssignedTaskCountRefreshEvent).toHaveBeenCalledTimes(1);
    });

    it('ignores pending live requests after entering simulation and reloads when leaving', async () => {
        let resolveOverview!: (overview: DashboardOverview) => void;
        let rejectActivity!: (reason: Error) => void;
        vi.mocked(DashboardApiService.prototype.fetchOverview).mockReturnValueOnce(new Promise((resolve) => { resolveOverview = resolve; }));
        vi.mocked(DashboardApiService.prototype.fetchActivity).mockReturnValueOnce(new Promise((_, reject) => { rejectActivity = reject; }));
        renderDashboard('/');
        fireEvent.click(screen.getByRole('button', {name: 'Simulation öffnen'}));
        await act(async () => {
            resolveOverview(liveOverview);
            rejectActivity(new Error('Request failed'));
        });
        expect(screen.getByText(/Hundesteuer anmelden/)).toBeVisible();
        expect(screen.queryByText('Echter Prozess')).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Echtdaten öffnen'}));
        expect(await screen.findByText('Echter Prozess')).toBeVisible();
        expect(screen.queryByText(/Hundesteuer anmelden/)).not.toBeInTheDocument();
        await waitFor(() => expect(DashboardApiService.prototype.fetchOverview).toHaveBeenCalledTimes(2));
        expect(screen.queryByRole('region', {name: 'Vorgangsaktivität'})).not.toBeInTheDocument();
    });

    it('clears existing loading errors when entering simulation', async () => {
        vi.mocked(DashboardApiService.prototype.fetchOverview).mockRejectedValueOnce(new Error('Request failed'));
        renderDashboard('/');
        expect(await screen.findByText('Aufgaben konnten nicht geladen werden')).toBeVisible();
        fireEvent.click(screen.getByRole('button', {name: 'Simulation öffnen'}));
        expect(screen.queryByText('Aufgaben konnten nicht geladen werden')).not.toBeInTheDocument();
        expect(screen.getByText('17 offen')).toBeVisible();
    });
});

describe('Simulated dashboard data', () => {
    it.each([DashboardActivityPeriod.ThirtyDays, DashboardActivityPeriod.ThreeMonths])('balances intake and completions with exactly 62 remaining cases for %s', (period) => {
        const {activity} = createSimulatedDashboardData(period);
        expect(activity.started - activity.completed).toBe(62);
        expect(activity.active).toBe(62);
        expect(activity.buckets.filter((bucket) => bucket.completed >= bucket.started).length)
            .toBeGreaterThanOrEqual(Math.ceil(activity.buckets.length / 2));
        expect(activity.buckets.slice(-3).every((bucket) => bucket.completed > bucket.started)).toBe(true);

        let active = 0;
        for (const bucket of activity.buckets) {
            active += bucket.started - bucket.completed;
            expect(active).toBeGreaterThanOrEqual(0);
        }
    });

    it.each([DashboardActivityPeriod.ThirtyDays, DashboardActivityPeriod.ThreeMonths])('keeps demo metrics below four digits with calm sections and individual peaks for %s', (period) => {
        for (let month = 1; month <= 12; month++) {
            const now = DateTime.fromObject({year: 2026, month, day: 15}, {zone: 'Europe/Berlin'});
            const {activity} = createSimulatedDashboardData(period, now);
            for (const metric of [activity.started, activity.completed, activity.active]) {
                expect(metric).toBeGreaterThan(0);
                expect(metric).toBeLessThan(1000);
            }
            expect(activity.active).toBe(62);
            for (const series of ['started', 'completed'] as const) {
                const values = activity.buckets.map((bucket) => bucket[series]);
                const maximum = Math.max(...values);
                const changes = values.slice(1).map((value, index) => Math.abs(value - values[index]));
                const calmThreshold = Math.ceil(maximum * 0.05);
                expect(changes.some((change, index) => change <= calmThreshold && changes[index + 1] <= calmThreshold)).toBe(true);
                expect(Math.max(...changes)).toBeGreaterThanOrEqual(maximum * 0.15);
                expect(maximum).toBeGreaterThan(Math.min(...values) * 1.5);
                expect(new Set(values).size).toBeGreaterThan(5);
            }
        }
    });

    it.each([DashboardActivityPeriod.ThirtyDays, DashboardActivityPeriod.ThreeMonths])('generates consistent current dates and totals for %s', (period) => {
        const now = DateTime.fromISO('2027-01-04T12:00:00', {zone: 'Europe/Berlin'});
        const data = createSimulatedDashboardData(period, now);
        expect(createSimulatedDashboardData(period, now)).toEqual(data);
        expect(data.overview.tasks.items).toHaveLength(4);
        expect(data.overview.recentProcesses).toHaveLength(3);
        expect(data.overview.tasks.items.filter((task) => Date.parse(task.deadline!) < now.toMillis())).toHaveLength(data.overview.tasks.overdue);
        expect(data.overview.tasks.items.every((task) => Date.parse(task.started) < Date.parse(task.deadline!))).toBe(true);
        expect(data.overview.tasks.items[0].caseNumber).toContain('2027');
        expect(data.activity.buckets).toHaveLength(period === DashboardActivityPeriod.ThirtyDays ? 30 : 13);
        expect(data.activity.buckets.at(-1)?.periodStart).toBe('2027-01-04');
        expect(data.activity.started).toBe(data.activity.buckets.reduce((sum, bucket) => sum + bucket.started, 0));
        expect(data.activity.completed).toBe(data.activity.buckets.reduce((sum, bucket) => sum + bucket.completed, 0));
        expect(data.activity.buckets.every((bucket) => bucket.started >= 0 && bucket.completed >= 0)).toBe(true);
    });
});
