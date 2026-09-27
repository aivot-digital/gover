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

    it('does not repeat identical task names and types in the link title', () => {
        renderCell({...task, taskName: ' Sachbearbeitung '}, 'taskName');

        expect(screen.getByRole('link', {name: 'Sachbearbeitung'})).toHaveAttribute('title', 'Sachbearbeitung');
    });

    describe.each([
        {row: instance, label: 'In Bearbeitung'},
        {row: task, label: 'Läuft'},
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
