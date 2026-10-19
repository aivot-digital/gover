import {act, cleanup, render, screen} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {createTheme, getContrastRatio, ThemeProvider} from '@mui/material/styles';
import {type GridRenderCellParams} from '@mui/x-data-grid';
import {ProcessInstanceDeadlineValue} from './process-instance-deadline-value';
import {ProcessTaskDeadlineValue} from './process-task-deadline-value';
import {ProcessTaskStatus} from '../enums/process-task-status';
import {ProcessInstanceStatus} from '../enums/process-instance-status';
import {processListColumns} from './process-list-columns';
import {type ProcessInstanceListEntry} from '../entities/process-list';

describe('deadline values', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        vi.setSystemTime(new Date('2026-10-01T10:00:00Z'));
    });

    afterEach(() => {
        cleanup();
        vi.useRealTimers();
    });

    it('adds the remaining time to the absolute deadline and updates without an API refresh', () => {
        const {container, unmount} = render(<ProcessTaskDeadlineValue task={{
            status: ProcessTaskStatus.AwaitingStaff,
            deadline: '2026-10-01T10:00:30Z',
        }}/>);
        expect(container).toHaveTextContent('01.10.2026 um 12:00 Uhr (noch 30 Sekunden)');
        expect(screen.queryByText('Frist überschritten')).not.toBeInTheDocument();

        act(() => vi.advanceTimersByTime(60_000));

        expect(container).toHaveTextContent('(seit 30 Sekunden überschritten)');
        expect(screen.getByText('Frist überschritten')).toBeVisible();
        unmount();
        expect(vi.getTimerCount()).toBe(0);
    });

    it('does not show a percentage-based warning for a task', () => {
        render(<ProcessTaskDeadlineValue task={{
            status: ProcessTaskStatus.AwaitingCustomer,
            deadline: '2026-10-01T10:01:00Z',
        }}/>);
        expect(screen.queryByText('Bald fällig')).not.toBeInTheDocument();
        expect(screen.queryByText('Frist überschritten')).not.toBeInTheDocument();
    });

    it.each([
        {finished: '2026-09-30T09:59:59Z', exceeded: false},
        {finished: '2026-09-30T10:00:00Z', exceeded: false},
        {finished: '2026-09-30T10:00:01Z', exceeded: true},
    ])('keeps the completion-time assessment for a task finished at $finished', ({finished, exceeded}) => {
        const {container} = render(<ProcessTaskDeadlineValue task={{
            status: ProcessTaskStatus.Completed,
            deadline: '2026-09-30T10:00:00Z',
            finished,
        }}/>);
        expect(container).toHaveTextContent('30.09.2026 um 12:00 Uhr');
        expect(container).not.toHaveTextContent(/noch|seit|Frist eingehalten/);
        expect(screen.queryByText('Frist überschritten') != null).toBe(exceeded);
        expect(vi.getTimerCount()).toBe(0);
    });

    it('does not infer an overdue state for an inactive task without a completion time', () => {
        const {container} = render(<ProcessTaskDeadlineValue task={{
            status: ProcessTaskStatus.Aborted,
            deadline: '2026-09-30T10:00:00Z',
        }}/>);
        expect(container).not.toHaveTextContent(/überschritten|noch|seit/);
        expect(vi.getTimerCount()).toBe(0);
    });

    it('shows the termination state even without a deadline timestamp', () => {
        render(<ProcessTaskDeadlineValue task={{status: ProcessTaskStatus.DeadlineExceeded, deadline: null}}/>);
        expect(screen.getByText('Frist überschritten')).toBeVisible();
    });

    it('shows a neutral fallback when no task deadline is active', () => {
        render(<ProcessTaskDeadlineValue task={{status: ProcessTaskStatus.Running, deadline: null}}/>);
        expect(screen.getByText('Keine aktive Frist')).toBeVisible();
        expect(vi.getTimerCount()).toBe(0);
    });

    it('updates an instance at the 75% boundary', () => {
        vi.setSystemTime(new Date('2026-10-01T09:59:00Z'));
        const {container} = render(<ProcessInstanceDeadlineValue instance={{
            status: ProcessInstanceStatus.Running,
            started: '2026-09-28T10:00:00Z',
            deadline: '2026-10-02T10:00:00Z',
        }}/>);
        expect(screen.queryByText('Bald fällig')).not.toBeInTheDocument();
        act(() => vi.advanceTimersByTime(60_000));
        expect(screen.getByText('Bald fällig')).toBeVisible();
        expect(container).toHaveTextContent('02.10.2026 um 12:00 Uhr (noch 1 Tag)');
    });

    it('does not keep counting down after an instance ends', () => {
        const {container} = render(<ProcessInstanceDeadlineValue instance={{
            status: ProcessInstanceStatus.Completed,
            started: '2026-09-01T10:00:00Z',
            deadline: '2026-09-02T10:00:00Z',
        }}/>);
        expect(container).toHaveTextContent('02.09.2026 um 12:00 Uhr');
        expect(container).not.toHaveTextContent(/überschritten|noch|seit/);
        expect(vi.getTimerCount()).toBe(0);
    });

    it.each(['light', 'dark'] as const)('uses the same readable warning color in the list and detail chip in %s mode', mode => {
        const theme = createTheme({palette: {mode}});
        const instance = {
            status: ProcessInstanceStatus.Running,
            started: '2026-09-28T10:00:00Z',
            deadline: '2026-10-02T10:00:00Z',
        };
        const column = processListColumns<ProcessInstanceListEntry>(false, vi.fn()).find(column => column.field === 'deadline')!;
        const {container} = render(
            <ThemeProvider theme={theme}>
                {column.renderCell!({row: instance} as GridRenderCellParams<ProcessInstanceListEntry>)}
                <ProcessInstanceDeadlineValue instance={instance}/>
            </ThemeProvider>,
        );
        const listColor = getComputedStyle(screen.getByText(/02\.10\.2026 – 12:00 Uhr/)).color;
        const chipColor = getComputedStyle(container.querySelector('.MuiChip-root')!).color;
        expect(listColor).toBe(chipColor);
        expect(getContrastRatio(listColor, theme.palette.background.paper)).toBeGreaterThanOrEqual(4.5);
    });
});
