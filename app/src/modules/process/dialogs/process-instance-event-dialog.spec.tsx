import {render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {ProcessNodeExecutionLogLevel} from '../entities/process-instance-event-entity';
import {type ProcessInstanceEventLog, type ProcessInstanceEventLogEntry} from '../models/process-instance-event-log';
import {ProcessInstanceEventApiService} from '../services/process-instance-event-api-service';
import {ProcessInstanceEventDialog} from './process-instance-event-dialog';

const firstEvent: ProcessInstanceEventLogEntry = {
    id: 1,
    processInstanceId: 12,
    processInstanceTaskId: 34,
    level: ProcessNodeExecutionLogLevel.Info,
    technical: true,
    audit: false,
    historyRelevant: false,
    title: 'Aufgabe gestartet',
    message: 'Die Aufgabe wurde zur Bearbeitung vorbereitet.',
    details: {},
    timestamp: '2026-08-14T08:05:00Z',
    triggeringUserId: null,
    triggeringUserName: null,
    concernedUserId: null,
    concernedUserName: null,
    concernedIdentityId: null,
    concernedIdentityTitle: null,
    processNodeName: 'Antrag prüfen',
};

const secondEvent: ProcessInstanceEventLogEntry = {
    ...firstEvent,
    id: 2,
    level: ProcessNodeExecutionLogLevel.Warn,
    title: 'Prüfung verzögert',
    message: 'Die Prüfung konnte noch nicht abgeschlossen werden.',
    details: {attempt: 2},
    triggeringUserId: '00000000-0000-0000-0000-000000000001',
    triggeringUserName: 'Alex Beispiel',
    historyRelevant: true,
    concernedUserId: '00000000-0000-0000-0000-000000000002',
    concernedUserName: 'Robin Beispiel (inaktiv)',
    concernedIdentityId: 'identity-2',
    concernedIdentityTitle: 'Antragstellende Person',
};

function createEventLog(events = [firstEvent, secondEvent]): ProcessInstanceEventLog {
    return {
        instance: {
            id: 12,
            caseNumber: 'V-2026-0012',
            started: '2026-08-14T08:00:00Z',
            finished: null,
            runtime: null,
        },
        task: {
            id: 34,
            name: 'Antrag prüfen',
            started: '2026-08-14T08:04:00Z',
            finished: null,
            runtime: null,
        },
        events: {
            content: events,
            page: {
                size: 50,
                number: 0,
                totalElements: events.length,
                totalPages: events.length === 0 ? 0 : 1,
            },
        },
    };
}

describe('ProcessInstanceEventDialog', () => {
    afterEach(() => vi.restoreAllMocks());

    it('renders event context and keeps the selected event in a separate detail pane', async () => {
        vi.spyOn(ProcessInstanceEventApiService.prototype, 'getEventLog')
            .mockResolvedValue(createEventLog());
        const user = userEvent.setup();

        render(
            <ProcessInstanceEventDialog
                open
                onClose={vi.fn()}
                instanceId={12}
                taskId={34}
            />,
        );

        expect(await screen.findByText('V-2026-0012')).toBeInTheDocument();
        expect(screen.getAllByText('Antrag prüfen').length).toBeGreaterThan(0);
        expect(screen.getAllByText('Aufgabe gestartet')).toHaveLength(2);
        expect(screen.getByText('Technisch')).toBeInTheDocument();
        expect(screen.getByText('Nicht audit-relevant')).toBeInTheDocument();
        expect(screen.getByText('Nicht verlaufsrelevant')).toBeInTheDocument();
        expect(screen.getByText('Betroffene Person').nextElementSibling).toHaveTextContent('–');
        expect(screen.getByText('Betroffene Identität').nextElementSibling).toHaveTextContent('–');

        await user.click(screen.getByText('Prüfung verzögert').closest('[role="button"]')!);

        expect(screen.getByText('Prüfung verzögert', {selector: 'h2'})).toBeInTheDocument();
        expect(screen.getByText('Alex Beispiel', {selector: 'p'})).toBeInTheDocument();
        expect(screen.getByText('Verlaufsrelevant')).toBeInTheDocument();
        expect(screen.getByText('Betroffene Person').nextElementSibling).toHaveTextContent('Robin Beispiel (inaktiv)');
        expect(screen.getByText('Betroffene Person').nextElementSibling).toHaveTextContent(secondEvent.concernedUserId!);
        expect(screen.getByText('Betroffene Identität').nextElementSibling).toHaveTextContent('Antragstellende Person');
        expect(screen.getByText('Betroffene Identität').nextElementSibling).toHaveTextContent('identity-2');
        expect(screen.getByTestId('expandable-code-block')).toHaveTextContent('"attempt": 2');
    });

    it('requests noteworthy events when the quick filter is selected', async () => {
        const getEventLog = vi.spyOn(ProcessInstanceEventApiService.prototype, 'getEventLog')
            .mockResolvedValue(createEventLog());
        const user = userEvent.setup();

        render(
            <ProcessInstanceEventDialog
                open
                onClose={vi.fn()}
                instanceId={12}
                taskId={null}
            />,
        );

        await screen.findByText('V-2026-0012');
        await user.click(screen.getByText('Warnungen und Fehler'));

        await waitFor(() => expect(getEventLog).toHaveBeenLastCalledWith(expect.objectContaining({
            filter: 'notable',
            processInstanceId: 12,
        })));
    });

    it('keeps the search focused while filtered events are loading', async () => {
        let resolveSearch!: (value: ProcessInstanceEventLog) => void;
        const searchRequest = new Promise<ProcessInstanceEventLog>((resolve) => {
            resolveSearch = resolve;
        });
        const getEventLog = vi.spyOn(ProcessInstanceEventApiService.prototype, 'getEventLog')
            .mockResolvedValueOnce(createEventLog())
            .mockReturnValueOnce(searchRequest);
        const user = userEvent.setup();

        render(
            <ProcessInstanceEventDialog
                open
                onClose={vi.fn()}
                instanceId={12}
                taskId={34}
            />,
        );

        await screen.findByText('V-2026-0012');
        const searchInput = screen.getByLabelText('Ereignisse durchsuchen');
        await user.type(searchInput, 'Prüfung');

        await waitFor(() => expect(getEventLog).toHaveBeenCalledTimes(2));
        expect(searchInput).toHaveFocus();
        expect(screen.getAllByText('Aufgabe gestartet')).toHaveLength(2);

        resolveSearch(createEventLog([secondEvent]));
        await waitFor(() => expect(screen.queryAllByText('Aufgabe gestartet')).toHaveLength(0));
    });

    it('shows a retry action after a loading error', async () => {
        const getEventLog = vi.spyOn(ProcessInstanceEventApiService.prototype, 'getEventLog')
            .mockRejectedValueOnce(new Error('network'))
            .mockResolvedValueOnce(createEventLog());
        const user = userEvent.setup();

        render(
            <ProcessInstanceEventDialog
                open
                onClose={vi.fn()}
                instanceId={12}
                taskId={null}
            />,
        );

        expect(await screen.findByText('Ereignisprotokoll konnte nicht geladen werden')).toBeInTheDocument();
        await user.click(screen.getByText('Erneut versuchen'));

        expect(await screen.findByText('V-2026-0012')).toBeInTheDocument();
        expect(getEventLog).toHaveBeenCalledTimes(2);
    });

    it('combines history relevance with search and warnings, preserves pagination and resets to all', async () => {
        const page = createEventLog();
        page.events.page.totalPages = 2;
        page.events.page.totalElements = 51;
        const getEventLog = vi.spyOn(ProcessInstanceEventApiService.prototype, 'getEventLog')
            .mockResolvedValue(page);
        const user = userEvent.setup();
        render(<ProcessInstanceEventDialog open onClose={vi.fn()} instanceId={12} taskId={null}/>);
        await screen.findByText('V-2026-0012');
        await user.click(screen.getByText('Warnungen und Fehler'));
        await user.type(screen.getByLabelText('Ereignisse durchsuchen'), 'Robin');
        await waitFor(() => expect(getEventLog).toHaveBeenLastCalledWith(expect.objectContaining({search: 'Robin'})));

        await user.click(screen.getByRole('combobox', {name: 'Verlaufsrelevanz'}));
        await user.click(screen.getByRole('option', {name: 'Nicht relevant'}));
        await waitFor(() => expect(getEventLog).toHaveBeenLastCalledWith(expect.objectContaining({
            page: 0, historyRelevant: false, filter: 'notable', search: 'Robin',
        })));

        await user.click(screen.getByRole('button', {name: /Weitere Ereignisse/}));
        await waitFor(() => expect(getEventLog).toHaveBeenLastCalledWith(expect.objectContaining({
            page: 1, historyRelevant: false, filter: 'notable', search: 'Robin',
        })));

        await user.click(screen.getByRole('combobox', {name: 'Verlaufsrelevanz'}));
        await user.click(screen.getByRole('option', {name: 'Relevant'}));
        await waitFor(() => expect(getEventLog).toHaveBeenLastCalledWith(expect.objectContaining({page: 0, historyRelevant: true})));

        await user.click(screen.getByRole('combobox', {name: 'Verlaufsrelevanz'}));
        await user.click(screen.getByRole('option', {name: 'Alle'}));
        await waitFor(() => expect(getEventLog).toHaveBeenLastCalledWith(expect.objectContaining({page: 0, historyRelevant: undefined})));
    });

    it('keeps history relevance on retry and displays a filtered empty state', async () => {
        const getEventLog = vi.spyOn(ProcessInstanceEventApiService.prototype, 'getEventLog')
            .mockResolvedValueOnce(createEventLog())
            .mockRejectedValueOnce(new Error('network'))
            .mockResolvedValueOnce(createEventLog([]));
        const user = userEvent.setup();
        render(<ProcessInstanceEventDialog open onClose={vi.fn()} instanceId={12} taskId={null}/>);
        await screen.findByText('V-2026-0012');

        await user.click(screen.getByRole('combobox', {name: 'Verlaufsrelevanz'}));
        await user.click(screen.getByRole('option', {name: 'Relevant'}));
        await user.click(await screen.findByRole('button', {name: 'Erneut versuchen'}));

        expect(await screen.findByText('Keine passenden Ereignisse')).toBeInTheDocument();
        expect(getEventLog).toHaveBeenLastCalledWith(expect.objectContaining({historyRelevant: true, page: 0}));
    });

    it('shows the concerned user ID when the user cannot be resolved', async () => {
        vi.spyOn(ProcessInstanceEventApiService.prototype, 'getEventLog')
            .mockResolvedValue(createEventLog([{...secondEvent, concernedUserName: null}]));
        render(<ProcessInstanceEventDialog open onClose={vi.fn()} instanceId={12} taskId={null}/>);

        await screen.findByText('Unbekannte Person');
        const details = within(screen.getByText('Betroffene Person').nextElementSibling as HTMLElement);
        expect(details.getByText(secondEvent.concernedUserId!)).toBeInTheDocument();
        expect(details.queryByText('System')).not.toBeInTheDocument();
    });
});
