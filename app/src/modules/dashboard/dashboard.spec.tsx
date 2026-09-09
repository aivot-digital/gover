import React from 'react';
import {act, fireEvent, render, screen} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {Dashboard} from './dashboard';
import {DashboardApiService} from './dashboard-api-service';
import {DashboardActivityPeriod} from './models/dashboard-overview';
import {MemoryRouter} from 'react-router-dom';

const {loadConfetti, createConfetti, fireConfetti} = vi.hoisted(() => ({
    loadConfetti: vi.fn(),
    createConfetti: vi.fn(),
    fireConfetti: vi.fn(),
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
vi.mock('./components/dashboard-tasks-panel', () => ({DashboardTasksPanel: () => null}));
vi.mock('./components/dashboard-links-panel', () => ({DashboardLinksPanel: () => null}));
vi.mock('./components/dashboard-activity-panel', () => ({DashboardActivityPanel: () => null}));
vi.mock('./components/dashboard-recent-processes-panel', () => ({DashboardRecentProcessesPanel: () => null}));

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
