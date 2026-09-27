import userEvent from '@testing-library/user-event';
import {copyToClipboardText} from '../../../../utils/copy-to-clipboard';
import {render, screen, waitFor, within} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {GenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {ProcessInstanceApiService} from '../../services/process-instance-api-service';
import {ProcessInstanceEventApiService} from '../../services/process-instance-event-api-service';
import {type ProcessInstanceDetails} from '../../entities/process-instance-details';
import {ProcessTaskStatus} from '../../enums/process-task-status';
import {ProcessInstanceDetailsPageIndex} from './process-instance-details-page-index';

vi.mock('../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../../../../utils/copy-to-clipboard', () => ({copyToClipboardText: vi.fn().mockResolvedValue(true)}));

const access = vi.hoisted(() => ({canReadProcess: false}));
vi.mock('../../../permissions/hooks/use-permissions', () => ({
    useHasProcessPermission: () => access.canReadProcess,
    useHasProcessInstancePermission: () => false,
}));
vi.mock('../../components/process-assignee', () => ({
    ProcessAssignee: ({userId}: {userId: string | null}) => userId ?? 'Nicht zugewiesen',
}));

const createItem = (): ProcessInstanceDetails => ({
    instance: {
        ...new ProcessInstanceApiService().initialize(),
        id: 17,
        caseNumber: 'V-2026-17',
        started: '2026-09-22T08:00:00Z',
        assignedFileNumbers: ['AZ-42'],
        initialProcessVersion: 3,
    },
    processName: 'Bauantrag',
    departmentId: 4,
    departmentName: 'Bauamt',
    triggerName: 'Antrag eingegangen',
    triggerType: 'Formulareingang',
    activeTasks: [],
});
function renderItem(item: ProcessInstanceDetails) {
    return render(
        <MemoryRouter>
            <GenericDetailsPageContext.Provider
                value={{
                    item,
                    setItem: vi.fn(),
                    setAdditionalData: vi.fn(),
                    isBusy: false,
                    setIsBusy: vi.fn(),
                    refresh: vi.fn(),
                    isEditable: false,
                }}
            >
                <ProcessInstanceDetailsPageIndex />
            </GenericDetailsPageContext.Provider>
        </MemoryRouter>,
    );
}

describe('Process instance information', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
        access.canReadProcess = false;
        vi.spyOn(ProcessInstanceEventApiService.prototype, 'list').mockResolvedValue({content: []} as never);
    });

    it('shows instance metadata without requiring access to the process model', async () => {
        renderItem(createItem());
        expect(screen.getByText('V-2026-17')).toBeInTheDocument();
        expect(screen.getByText('AZ-42')).toBeInTheDocument();
        expect(screen.getByRole('row', {name: /Prozess Bauantrag/})).toHaveTextContent('Bauantrag (v3)');
        expect(screen.queryByRole('link', {name: 'Bauantrag (v3)'})).not.toBeInTheDocument();
        expect(screen.getByText('„Antrag eingegangen“')).toBeInTheDocument();
        expect(screen.getByText('Bauamt')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Vorgang sperren'})).toBeDisabled();
        expect(screen.getByRole('button', {name: 'Vorgang beenden'})).toBeDisabled();
        expect(screen.getByText('Keine aktive Aufgabe')).toBeInTheDocument();
        await screen.findByText('Noch kein Ereignis vorhanden');
        expect(ProcessInstanceEventApiService.prototype.list).toHaveBeenCalledWith(0, 1, ['timestamp', 'id'], 'DESC', {
            processInstanceId: 17,
        });
    });

    it.each<[string, string | null, string]>([
        ['Formulareingang', 'Formulareingang', 'Formulareingang'],
        [' Formulareingang ', 'Formulareingang', 'Formulareingang'],
        ['Antrag eingegangen', 'Formulareingang', '„Antrag eingegangen“ · Formulareingang'],
        ['Antrag eingegangen', null, 'Antrag eingegangen'],
    ])(
        'shows the trigger name and type without duplicating the default label',
        async (triggerName, triggerType, expected) => {
            renderItem({
                ...createItem(),
                triggerName,
                triggerType,
            });
            const row = screen.getByRole('row', {name: /^Auslösendes Prozesselement /});
            expect(within(row).getAllByRole('cell')[1]).toHaveTextContent(expected);
            if (triggerName.trim() === triggerType) expect(row).not.toHaveTextContent('·');
            await screen.findByText('Noch kein Ereignis vorhanden');
        },
    );

    it('separates information into sections and keeps all actions below them', async () => {
        renderItem(createItem());
        expect(screen.getAllByRole('heading', {level: 2}).map((heading) => heading.textContent)).toEqual([
            'Angaben zum Vorgang',
            'Stand des Vorgangs',
            'Aktive Aufgabe',
        ]);
        const general = within(screen.getByRole('region', {name: 'Angaben zum Vorgang'}));
        const status = within(screen.getByRole('region', {name: 'Stand des Vorgangs'}));
        const tasks = within(screen.getByRole('region', {name: 'Aktive Aufgabe'}));
        expect(general.getByRole('row', {name: /Vorgangskennung/})).toBeInTheDocument();
        expect(general.queryByRole('row', {name: /Vorgangsstatus/})).not.toBeInTheDocument();
        expect(status.getByRole('row', {name: /Vorgangsstatus/})).toBeInTheDocument();
        const assignment = screen.getByRole('button', {name: 'Vorgang zuweisen'});
        const allTasks = screen.getByRole('link', {name: 'Alle Aufgaben aufrufen'});
        expect(assignment.closest('section')).toBeNull();
        expect(allTasks.closest('section')).toBeNull();
        expect(
            screen.getByRole('region', {name: 'Aktive Aufgabe'}).compareDocumentPosition(allTasks) &
                Node.DOCUMENT_POSITION_FOLLOWING,
        ).toBeTruthy();
        expect(tasks.queryByRole('table')).not.toBeInTheDocument();
        await screen.findByText('Noch kein Ereignis vorhanden');
    });

    it('links all active tasks and identifies test instances and end times', async () => {
        access.canReadProcess = true;
        const item = createItem();
        item.instance.createdForTestClaimId = 9;
        item.instance.finished = '2026-09-22T10:00:00Z';
        item.activeTasks = [
            {
                id: 1,
                name: 'Prüfung',
                status: ProcessTaskStatus.Running,
                assignedUserId: 'Kim',
            },
            {
                id: 2,
                name: 'Zahlung',
                status: ProcessTaskStatus.AwaitingPayment,
                assignedUserId: 'Alex',
            },
        ];
        renderItem(item);
        expect(screen.getByRole('link', {name: 'Prüfung'})).toHaveAttribute(
            'href',
            `${window.location.origin}/staff/tasks/17/1`,
        );
        expect(screen.getByRole('link', {name: 'Zahlung'})).toHaveAttribute(
            'href',
            `${window.location.origin}/staff/tasks/17/2`,
        );
        expect(screen.getByText('Aktive Aufgaben')).toBeInTheDocument();
        expect(
            within(screen.getByRole('group', {name: 'Aufgabe Prüfung'})).getByRole('row', {
                name: /Aufgabe zugewiesen an/,
            }),
        ).toHaveTextContent('Kim');
        expect(
            within(screen.getByRole('group', {name: 'Aufgabe Zahlung'})).getByRole('row', {
                name: /Aufgabe zugewiesen an/,
            }),
        ).toHaveTextContent('Alex');
        expect(screen.getByText('Dieser Vorgang wurde über den Testmodus des Prozesses gestartet.')).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'Prüfung'})).toHaveAttribute('target', '_blank');
        expect(screen.getByRole('link', {name: 'Bauantrag (v3)'})).toHaveAttribute('target', '_blank');
        expect(screen.getByText('Beendet am')).toBeInTheDocument();
        await screen.findByText('Noch kein Ereignis vorhanden');
    });

    it('copies identifiers individually and shows relative dates like the task view', async () => {
        vi.spyOn(Date, 'now').mockReturnValue(Date.parse('2026-09-23T08:00:00Z'));
        const user = userEvent.setup();
        const item = createItem();
        item.instance.assignedFileNumbers = ['AZ-42', 'AZ-43'];
        renderItem(item);
        expect(screen.getByRole('row', {name: /Gestartet am/})).toHaveTextContent(/\(vor .*\)/);
        expect(screen.queryByText('Test-Vorgang')).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', {name: 'Vorgangskennung kopieren'}));
        expect(copyToClipboardText).toHaveBeenLastCalledWith('V-2026-17');
        await user.click(screen.getByRole('button', {name: 'Aktenzeichen AZ-43 kopieren'}));
        expect(copyToClipboardText).toHaveBeenLastCalledWith('AZ-43');
        await screen.findByText('Noch kein Ereignis vorhanden');
    });

    it('keeps custom and system statuses and instance and task assignments distinct', async () => {
        const item = createItem();
        item.instance.statusOverride = 'Fachliche Prüfung';
        item.instance.assignedUserId = 'Vorgangsverantwortung';
        item.activeTasks = [
            {
                id: 8,
                name: 'Unterlagen prüfen',
                status: ProcessTaskStatus.AwaitingCustomer,
                statusOverride: 'Unterlagen angefordert',
                assignedUserId: 'Aufgabenbearbeitung',
            },
        ];
        renderItem(item);
        const status = within(screen.getByRole('row', {name: /^Vorgangsstatus /}));
        expect(status.getByText('Fachliche Prüfung')).toBeInTheDocument();
        expect(status.getByText('(Systemstatus: Erstellt)')).toBeInTheDocument();
        expect(screen.getByRole('row', {name: /^Aufgabenstatus /})).toHaveTextContent('Unterlagen angefordert');
        expect(screen.getByText('(Systemstatus: Wartet auf Nutzer:in)')).toBeInTheDocument();
        expect(screen.getByRole('row', {name: /Vorgang zugewiesen an/})).toHaveTextContent('Vorgangsverantwortung');
        expect(screen.getByRole('row', {name: /Aufgabe zugewiesen an/})).toHaveTextContent('Aufgabenbearbeitung');
        await screen.findByText('Noch kein Ereignis vorhanden');
    });

    it('does not present instance responsibility as an active task assignment', async () => {
        const item = createItem();
        item.instance.assignedUserId = 'Vorgangsverantwortung';
        item.instance.statusOverride = '   ';
        renderItem(item);
        expect(screen.getByRole('row', {name: /^Vorgangsstatus /})).toHaveTextContent('Erstellt');
        expect(screen.queryByText('Wird bearbeitet durch')).not.toBeInTheDocument();
        expect(screen.getByRole('region', {name: 'Aktive Aufgabe'})).toHaveTextContent('Keine aktive Aufgabe');
        expect(screen.getByRole('row', {name: /Vorgang zugewiesen an/})).toHaveTextContent('Vorgangsverantwortung');
        await screen.findByText('Noch kein Ereignis vorhanden');
    });

    it('shows individual deadlines in separate task tables without an aggregate deadline', async () => {
        const item = createItem();
        item.activeTasks = [
            {
                id: 1,
                name: 'Später',
                status: ProcessTaskStatus.Running,
                assignedUserId: null,
                deadline: '2026-10-02T10:00:00Z',
            },
            {
                id: 2,
                name: 'Überfällig',
                status: ProcessTaskStatus.Paused,
                assignedUserId: null,
                deadline: '2026-09-01T10:00:00Z',
            },
            {
                id: 3,
                name: 'Ohne Frist',
                status: ProcessTaskStatus.Running,
                assignedUserId: null,
            },
        ];
        renderItem(item);
        expect(screen.queryByText('Nächste Aufgabenfälligkeit')).not.toBeInTheDocument();
        const tasks = within(screen.getByRole('region', {name: 'Aktive Aufgaben'}));
        expect(tasks.getAllByRole('table')).toHaveLength(3);
        for (const [name, deadline] of [
            ['Später', '02.10.2026'],
            ['Überfällig', '01.09.2026'],
            ['Ohne Frist', 'Nicht festgelegt'],
        ]) {
            const task = within(tasks.getByRole('group', {name: `Aufgabe ${name}`}));
            expect(task.getAllByRole('row')).toHaveLength(4);
            expect(task.getByRole('row', {name: /Fälligkeit/})).toHaveTextContent(deadline);
        }
        expect(screen.queryByRole('row', {name: /Zuletzt aktualisiert/})).not.toBeInTheDocument();
        expect(screen.queryByText(/unabhängig von der Bearbeitung einzelner Aufgaben/)).not.toBeInTheDocument();
        await screen.findByText('Noch kein Ereignis vorhanden');
    });

    it('distinguishes event loading failures from an empty history', async () => {
        vi.mocked(ProcessInstanceEventApiService.prototype.list).mockRejectedValue(new Error('Unavailable'));
        renderItem(createItem());
        await waitFor(() =>
            expect(screen.getByText('Das letzte Ereignis konnte nicht geladen werden.')).toBeInTheDocument(),
        );
        expect(screen.queryByText('Noch kein Ereignis vorhanden')).not.toBeInTheDocument();
    });
});
