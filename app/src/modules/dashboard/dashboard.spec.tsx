import React from 'react';
import {act, fireEvent, render, screen} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {Dashboard} from './dashboard';
import {DashboardApiService} from './dashboard-api-service';
import {DashboardActivityPeriod} from './models/dashboard-overview';
import {MemoryRouter} from 'react-router-dom';
import type {StaffEvent} from '../staff-events/staff-event-source';

const {loadConfetti, createConfetti, fireConfetti, panelProps, staffEventListeners} = vi.hoisted(() => ({
    loadConfetti: vi.fn(),
    createConfetti: vi.fn(),
    fireConfetti: vi.fn(),
    panelProps: {tasks: [] as unknown[], recentProcesses: [] as unknown[]},
    staffEventListeners: [] as Array<(event: StaffEvent | null) => void>,
}));

vi.mock('canvas-confetti', () => {
    loadConfetti();
    return {default: {create: createConfetti}};
});

vi.mock('../../components/page-wrapper/page-wrapper', () => ({
    PageWrapper: ({children}: {children: React.ReactNode}) => <main>{children}</main>,
}));
vi.mock('../../hooks/use-app-selector', () => ({useAppSelector: () => undefined}));
vi.mock('./components/dashboard-greeting', () => ({DashboardGreeting: () => null}));
vi.mock('./components/dashboard-tasks-panel', () => ({
    DashboardTasksPanel: ({summary}: {summary: unknown}) => {
        panelProps.tasks.push(summary);
        return null;
    },
}));
vi.mock('./components/dashboard-links-panel', () => ({DashboardLinksPanel: () => null}));
vi.mock('./components/dashboard-activity-panel', () => ({DashboardActivityPanel: () => null}));
vi.mock('./components/dashboard-recent-processes-panel', () => ({
    DashboardRecentProcessesPanel: ({processes}: {processes: unknown}) => {
        panelProps.recentProcesses.push(processes);
        return null;
    },
}));
vi.mock('../staff-events/staff-event-source', () => ({
    subscribeStaffEvents: (listener: (event: StaffEvent | null) => void) => {
        staffEventListeners.push(listener);
        return () => staffEventListeners.splice(staffEventListeners.indexOf(listener), 1);
    },
}));

describe('Dashboard confetti', () => {
    afterEach(() => {
        vi.useRealTimers();
        vi.unstubAllGlobals();
    });

    it('loads the effect only on a flag click without reduced motion and allows replay', async () => {
        vi.useFakeTimers();
        const matchMedia = vi.fn().mockReturnValue({matches: true});
        vi.stubGlobal('matchMedia', matchMedia);
        createConfetti.mockReturnValue(fireConfetti);
        fireConfetti.mockResolvedValue(null);
        vi.spyOn(DashboardApiService.prototype, 'fetchOverview').mockResolvedValue({
            tasks: {total: 0, overdue: 0, items: []},
            instances: {total: 0, overdue: 0, items: []},
            recentProcesses: [],
        });
        vi.spyOn(DashboardApiService.prototype, 'fetchActivity').mockResolvedValue({
            available: false,
            period: DashboardActivityPeriod.ThreeMonths,
            started: 0,
            completed: 0,
            active: 0,
            buckets: [],
        });

        const {container, unmount} = render(<MemoryRouter><Dashboard/></MemoryRouter>);
        await act(async () => {
            await Promise.resolve();
        });
        expect(loadConfetti).not.toHaveBeenCalled();

        const flag = screen.getByRole('button', {name: 'Deutschlandflagge feiern'});
        fireEvent.click(flag);
        expect(matchMedia).toHaveBeenCalledWith('(prefers-reduced-motion: reduce)');
        expect(loadConfetti).not.toHaveBeenCalled();
        expect(container.querySelector('canvas')).toBeNull();

        matchMedia.mockReturnValue({matches: false});
        await act(async () => {
            fireEvent.click(flag);
            await vi.dynamicImportSettled();
        });
        expect(loadConfetti).toHaveBeenCalledTimes(1);
        expect(container.querySelector('canvas')).toHaveAttribute('aria-hidden', 'true');
        await act(async () => {
            await vi.advanceTimersByTimeAsync(200);
        });
        expect(fireConfetti).toHaveBeenCalledTimes(2);
        expect(fireConfetti).toHaveBeenCalledWith(expect.objectContaining({disableForReducedMotion: true}));

        fireEvent.click(flag);
        await act(async () => {
            await vi.advanceTimersByTimeAsync(200);
        });
        expect(loadConfetti).toHaveBeenCalledTimes(1);
        expect(fireConfetti).toHaveBeenCalledTimes(4);

        unmount();
        const burstCount = fireConfetti.mock.calls.length;
        await vi.advanceTimersByTimeAsync(3000);
        expect(fireConfetti).toHaveBeenCalledTimes(burstCount);
    });
});

describe('Dashboard task panel', () => {
    afterEach(() => {
        vi.useRealTimers();
        vi.restoreAllMocks();
        localStorage.clear();
    });

    it('reloads only the tasks when a process instance changes', async () => {
        vi.useFakeTimers();
        const recentProcesses = [{id: 1, name: 'Prozess', updated: '2026-11-02T10:00:00Z'}];
        const fetchOverview = vi.spyOn(DashboardApiService.prototype, 'fetchOverview').mockResolvedValue({
            tasks: {total: 1, overdue: 0, items: []},
            instances: {total: 0, overdue: 0, items: []},
            recentProcesses,
        } as never);
        const fetchActivity = vi.spyOn(DashboardApiService.prototype, 'fetchActivity').mockResolvedValue({
            available: false,
            period: DashboardActivityPeriod.ThreeMonths,
            started: 0,
            completed: 0,
            active: 0,
            buckets: [],
        });
        const updatedTasks = {total: 2, overdue: 1, items: []};
        const fetchTasks = vi.spyOn(DashboardApiService.prototype, 'fetchTasks').mockResolvedValue(updatedTasks);
        panelProps.tasks.length = 0;
        panelProps.recentProcesses.length = 0;
        signIn();

        render(<MemoryRouter><Dashboard/></MemoryRouter>);
        await act(async () => {
            await Promise.resolve();
        });
        await act(async () => {
            staffEventListeners.forEach((listener) => listener({type: 'process-instance', scope: '7', userId: null}));
            await vi.advanceTimersByTimeAsync(1_000);
        });

        expect(fetchTasks).toHaveBeenCalledOnce();
        expect(fetchOverview).toHaveBeenCalledOnce();
        expect(fetchActivity).toHaveBeenCalledOnce();
        expect(panelProps.tasks.at(-1)).toBe(updatedTasks);
        expect(panelProps.recentProcesses.at(-1)).toBe(recentProcesses);
    });

    it('keeps the tasks of the latest reload when an older answer arrives later', async () => {
        vi.useFakeTimers();
        mockOverview();
        const olderTasks = {total: 2, overdue: 0, items: []};
        const newerTasks = {total: 3, overdue: 0, items: []};
        let answerOlder!: (tasks: typeof olderTasks) => void;
        vi.spyOn(DashboardApiService.prototype, 'fetchTasks')
            .mockReturnValueOnce(new Promise((resolve) => {
                answerOlder = resolve;
            }))
            .mockResolvedValueOnce(newerTasks);
        panelProps.tasks.length = 0;
        signIn();

        render(<MemoryRouter><Dashboard/></MemoryRouter>);
        await act(async () => {
            await Promise.resolve();
        });
        for (let reload = 0; reload < 2; reload++) {
            await act(async () => {
                staffEventListeners.forEach((listener) => listener({type: 'process-instance', scope: '7', userId: null}));
                await vi.advanceTimersByTimeAsync(1_000);
            });
        }
        await act(async () => answerOlder(olderTasks));

        expect(panelProps.tasks.at(-1)).toBe(newerTasks);
    });
});

function signIn() {
    localStorage.setItem('auth_expiration', JSON.stringify({
        accessExpires: Date.now() + 60_000,
        refreshExpires: Date.now() + 60_000,
        csrfToken: 'csrf-token',
    }));
}

function mockOverview() {
    vi.spyOn(DashboardApiService.prototype, 'fetchOverview').mockResolvedValue({
        tasks: {total: 1, overdue: 0, items: []},
        instances: {total: 0, overdue: 0, items: []},
        recentProcesses: [],
    } as never);
    vi.spyOn(DashboardApiService.prototype, 'fetchActivity').mockResolvedValue({
        available: false,
        period: DashboardActivityPeriod.ThreeMonths,
        started: 0,
        completed: 0,
        active: 0,
        buckets: [],
    });
}
