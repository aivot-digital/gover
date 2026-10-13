import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {act, cleanup, fireEvent, render, screen} from '@testing-library/react';
import {GridRenderCellParams} from '@mui/x-data-grid';
import {MemoryRouter} from 'react-router-dom';
import {processListColumns} from './process-list-columns';
import {ProcessInstanceListEntry, ProcessTaskListEntry} from '../entities/process-list';
import {ProcessInstanceStatus} from '../enums/process-instance-status';
import {ProcessTaskStatus} from '../enums/process-task-status';

const instance: ProcessInstanceListEntry = {
    id: 17,
    caseNumber: '7K0M-9X1Q-4R8T',
    assignedFileNumbers: ['AZ-2026-100', 'AZ-2026-101'],
    processId: 10,
    processVersion: 2,
    processName: 'Anmeldung einer Veranstaltung',
    assignedUserId: 'kim',
    assignedUserName: 'Kim Musterperson',
    status: ProcessInstanceStatus.Running,
    statusOverride: null,
    started: '2026-09-24T12:30:00Z',
    deadline: '2026-09-24T12:30:00Z',
    finished: '2026-09-24T12:30:00Z',
    test: false,
};
const task: ProcessTaskListEntry = {
    ...instance,
    id: 23,
    processInstanceId: 17,
    status: ProcessTaskStatus.Running,
    taskName: 'Angaben zur Veranstaltung prüfen',
    taskType: 'Sachbearbeitung',
    description: 'Bitte prüfen Sie die eingereichten Unterlagen.',
    assignedCustomerIdentityLabel: null,
    deadline: '2026-09-24T12:30:00Z',
};

function renderCell(row: ProcessInstanceListEntry | ProcessTaskListEntry, field: string) {
    const column = processListColumns('processInstanceId' in row, vi.fn()).find(column => column.field === field)!;
    const value = field === 'assignedFileNumbers'
        ? row.assignedFileNumbers.join(', ')
        : row[field as keyof typeof row];
    render(
        <MemoryRouter>
            {column.renderCell!({row, value} as GridRenderCellParams<typeof row>)}
        </MemoryRouter>,
    );
}

async function hover(element: HTMLElement) {
    fireEvent.mouseOver(element);
    await act(async () => {
        await vi.advanceTimersByTimeAsync(500);
    });
}

describe('Process list columns', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        vi.setSystemTime(new Date('2026-09-24T13:30:00Z'));
    });

    afterEach(() => {
        cleanup();
        vi.useRealTimers();
    });

    it.each([
        {row: instance, field: 'started'},
        {row: instance, field: 'finished'},
        {row: task, field: 'started'},
        {row: task, field: 'finished'},
        {row: task, field: 'deadline'},
        {row: instance, field: 'deadline'},
    ])('includes Uhr and the relative time in the tooltip for $field (row $row.id)', async ({row, field}) => {
        renderCell(row, field);

        await hover(screen.getByText('24.09.2026 – 14:30 Uhr'));

        expect(screen.getByRole('tooltip')).toHaveTextContent('24.09.2026 – 14:30 Uhr (vor 1 Stunde)');
    });

    it.each([null, '', 'invalid'])('keeps the empty date placeholder for %s', value => {
        renderCell({...instance, finished: value}, 'finished');

        expect(screen.getByText('—')).toBeInTheDocument();
        expect(screen.queryByText(/Uhr/)).not.toBeInTheDocument();
    });

    it.each([
        {field: 'caseNumber', title: task.caseNumber},
        {field: 'assignedFileNumbers', title: 'AZ-2026-100, AZ-2026-101'},
        {field: 'processName', title: task.processName},
        {field: 'assignedUserName', title: 'Kim Musterperson'},
        {field: 'description', title: 'Bitte prüfen Sie die eingereichten Unterlagen.'},
        {field: 'taskName', title: '„Angaben zur Veranstaltung prüfen“ · Sachbearbeitung'},
    ])('exposes the full $field in its title', ({field, title}) => {
        renderCell(task, field);

        expect(screen.getByTitle(title)).toHaveTextContent(title);
    });

    describe('instance deadline', () => {
        // Now is 2026-09-24T13:30:00Z; the instance started ten days before its deadline.
        const tracked = {...instance, started: '2026-09-20T13:30:00Z'};

        it.each([
            {deadline: '2026-09-24T12:30:00Z', status: 'Frist überschritten'},
            {deadline: '2026-09-26T13:30:00Z', status: 'Bald fällig'},
        ])('names the $status state in the tooltip and for screen readers', async ({deadline, status}) => {
            renderCell({...tracked, deadline}, 'deadline');
            const date = screen.getByText(/Uhr/);

            expect(date).toHaveTextContent(`(${status})`);
            await hover(date);
            expect(screen.getByRole('tooltip')).toHaveTextContent(` · ${status}`);
        });

        it('does not highlight a deadline before half of the period has passed', async () => {
            renderCell({...tracked, deadline: '2026-09-30T13:30:00Z'}, 'deadline');
            const date = screen.getByText('30.09.2026 – 15:30 Uhr');

            expect(date).toHaveTextContent(/^30\.09\.2026 – 15:30 Uhr$/);
            await hover(date);
            expect(screen.getByRole('tooltip')).not.toHaveTextContent('·');
        });

        it('does not highlight an exceeded deadline of a completed instance', () => {
            renderCell({...tracked, status: ProcessInstanceStatus.Completed}, 'deadline');

            expect(screen.getByText('24.09.2026 – 14:30 Uhr')).not.toHaveTextContent('Frist überschritten');
        });

        it('keeps the empty placeholder without a deadline', () => {
            renderCell({...tracked, deadline: null}, 'deadline');

            expect(screen.getByText('—')).toBeInTheDocument();
        });
    });

    it('does not repeat identical task names and types in the link title', () => {
        renderCell({...task, taskName: ' Sachbearbeitung '}, 'taskName');

        expect(screen.getByRole('link', {name: 'Sachbearbeitung'})).toHaveAttribute('title', 'Sachbearbeitung');
    });

    describe('awaiting customer identity', () => {
        const awaitingTask: ProcessTaskListEntry = {
            ...task,
            status: ProcessTaskStatus.AwaitingCustomer,
            assignedCustomerIdentityLabel: ' Antragstellende ',
        };

        it.each([null, '', '   ', 'Wartet auf Antragstellende'])('shows the identity for status override %s', async statusOverride => {
            renderCell({...awaitingTask, statusOverride}, 'status');

            await hover(screen.getByText('Wartet auf Antragstellende'));

            expect(screen.getByRole('tooltip')).toHaveTextContent(/^Wartet auf Antragstellende$/);
        });

        it.each([null, '', '   '])('uses the generic status when the identity label is %s', assignedCustomerIdentityLabel => {
            renderCell({...awaitingTask, assignedCustomerIdentityLabel}, 'status');

            expect(screen.getByText('Wartet auf Nutzer:in')).toBeInTheDocument();
        });

        it('shows a custom status and explains the identity in the system status tooltip', async () => {
            renderCell({...awaitingTask, statusOverride: ' Unterlagen nachreichen '}, 'status');

            await hover(screen.getByText('Unterlagen nachreichen'));

            expect(screen.getByRole('tooltip')).toHaveTextContent(
                'Unterlagen nachreichen (Systemstatus: Wartet auf Antragstellende)',
            );
        });

        it('exposes the full status for a long identity label', async () => {
            const assignedCustomerIdentityLabel = 'Bevollmächtigte für die Durchführung der Veranstaltung';
            renderCell({...awaitingTask, assignedCustomerIdentityLabel}, 'status');

            await hover(screen.getByText(`Wartet auf ${assignedCustomerIdentityLabel}`));

            expect(screen.getByRole('tooltip')).toHaveTextContent(`Wartet auf ${assignedCustomerIdentityLabel}`);
        });

        it('does not replace another task status with the assigned identity', async () => {
            renderCell({...awaitingTask, status: ProcessTaskStatus.Completed}, 'status');

            await hover(screen.getByText('Abgeschlossen'));

            expect(screen.queryByText(/Wartet auf/)).not.toBeInTheDocument();
            expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
        });
    });

    describe.each([
        {row: instance, label: 'Gestartet'},
        {row: task, label: 'Gestartet'},
    ])('status for row $row.id', ({row, label}) => {
        it.each([null, '', '   ', label, ` ${label} `])('omits a redundant tooltip for %s', async statusOverride => {
            renderCell({...row, statusOverride}, 'status');

            await hover(screen.getByText(label));

            expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
        });

        it('explains the system status when a different custom status is shown', async () => {
            renderCell({...row, statusOverride: ' Unterlagen prüfen '}, 'status');

            await hover(screen.getByText('Unterlagen prüfen'));

            expect(screen.getByRole('tooltip')).toHaveTextContent(`Unterlagen prüfen (Systemstatus: ${label})`);
        });
    });
});
