import {render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {Dialog, DialogTitle, ThemeProvider} from '@mui/material';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {BaseTheme} from '../../../theming/base-theme';
import {Permission} from '../../../data/permissions/permission';
import {ProcessTaskStatus} from '../enums/process-task-status';
import {ProcessInstanceApiService} from '../services/process-instance-api-service';
import {ProcessInstanceTaskApiService} from '../services/process-instance-task-api-service';
import {ProcessInstanceAttachmentApiService} from '../services/process-instance-attachment-api-service';
import {type ProcessRuntimeData} from '../hooks/use-process-runtime-data';
import {ProcessNodeExecutionsDialog} from './process-node-executions-dialog';

const mocks = vi.hoisted(() => ({
    dispatch: vi.fn(),
    permissions: new Set<string>(),
    instanceId: 17,
    processId: 3,
}));
vi.mock('../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => mocks.dispatch}));
vi.mock('../../permissions/hooks/use-permissions', () => ({
    useHasProcessInstancePermission: (id: number, key: string) => id === mocks.instanceId && mocks.permissions.has(key),
    useHasProcessPermission: (id: number, key: string) => id === mocks.processId && mocks.permissions.has(key),
}));
vi.mock('./process-instance-event-dialog', () => ({
    ProcessInstanceEventDialog: ({
                                     taskId,
                                     instanceId,
                                 }: { taskId: number; instanceId: number }) =>
        <Dialog open><DialogTitle>Ereignisprotokoll</DialogTitle>Vorgang {instanceId}, Aufgabe {taskId}</Dialog>,
}));

function task(id: number, status = ProcessTaskStatus.Completed, overrides: Partial<ProcessRuntimeData['tasks'][number]> = {}) {
    return {
        ...new ProcessInstanceTaskApiService().initialize(),
        id,
        processNodeId: 5,
        processInstanceId: 17,
        processId: 3,
        status,
        started: `2026-11-20T10:0${id}:00Z`,
        finished: status === ProcessTaskStatus.Completed ? `2026-11-20T10:0${id}:30Z` : null,
        ...overrides,
    };
}

function runtime(tasks = [task(1), task(2, ProcessTaskStatus.InProgress)]): ProcessRuntimeData {
    return {
        instance: {
            ...new ProcessInstanceApiService().initialize(),
            id: 17,
            processId: 3,
            accessKey: 'instance-access',
        },
        tasks,
        tasksComplete: true,
        events: [],
        attachments: [],
        attachmentSets: [],
    };
}

function renderDialog(data = runtime(), onReload = vi.fn(), onDownloadAttachment = vi.fn()) {
    const content = (runtimeData: ProcessRuntimeData) => <ThemeProvider theme={BaseTheme}><MemoryRouter>
        <ProcessNodeExecutionsDialog nodeId={5}
                                     nodeName="Prüfung"
                                     runtimeData={runtimeData}
                                     onReload={onReload}
                                     onDownloadAttachment={onDownloadAttachment}
                                     onClose={vi.fn()}/>
    </MemoryRouter></ThemeProvider>;
    const view = render(content(data));
    return {
        ...view,
        rerenderRuntimeData: (runtimeData: ProcessRuntimeData) => view.rerender(content(runtimeData)),
        onReload,
        onDownloadAttachment,
    };
}

function rows() {
    return within(screen.getByRole('list', {name: 'Ausführungen'})).getAllByRole('listitem');
}

beforeEach(() => {
    mocks.permissions = new Set([Permission.PROCESS_INSTANCE_READ, Permission.PROCESS_DEFINITION_READ]);
    mocks.instanceId = 17;
    mocks.dispatch.mockReset();
});

describe('ProcessNodeExecutionsDialog', () => {
    it('lists only this node in reverse chronology with stable execution numbers', () => {
        renderDialog(runtime([task(2), task(3, ProcessTaskStatus.Restarted), task(1), task(4, ProcessTaskStatus.Completed, {processNodeId: 6})]));
        expect(rows()).toHaveLength(2);
        expect(rows()[0]).toHaveTextContent('Ausführung 2');
        expect(rows()[1]).toHaveTextContent('Ausführung 1');
        expect(screen.getByText('Neueste zuerst')).toBeVisible();
        expect(within(rows()[0]).getByRole('link', {name: 'Im Vorgangsverlauf ansehen'})).toHaveAttribute('href', '/process-instances/17/history?taskId=2');
    });

    it('places the secondary sort hint in the dialog title beside the close control', () => {
        renderDialog();
        const title = within(screen.getByRole('heading', {name: /Ausführungen von „Prüfung“/}));
        expect(title.getByText('Neueste zuerst')).toBeVisible();
        expect(title.getByText('Neueste zuerst')).toHaveStyle({color: BaseTheme.palette.text.secondary});
        expect(title.getByRole('button', {name: 'Schließen'})).toBeEnabled();
        expect(screen.getAllByText('Neueste zuerst')).toHaveLength(1);
    });

    it('marks only the execution represented by the model, including when it is completed', async () => {
        renderDialog(runtime([
            task(1, ProcessTaskStatus.InProgress, {updated: '2030-01-01T00:00:00Z'}),
            task(2, ProcessTaskStatus.Completed, {runtime: 30_000}),
        ]));
        const latest = within(rows()[0]);
        expect(latest.getByText('Neueste Ausführung')).toBeVisible();
        expect(latest.getByText('Abgeschlossen')).toBeVisible();
        expect(latest.getAllByLabelText('Abgeschlossen').some(element => element.tagName.toLowerCase() === 'svg')).toBe(true);
        expect(latest.getAllByRole('term').map(term => term.textContent)).toEqual(['Beginn', 'Ende', 'Dauer']);
        expect(within(rows()[1]).queryByText('Neueste Ausführung')).not.toBeInTheDocument();
        expect(within(rows()[1]).getByText('In Bearbeitung')).toBeVisible();
        await userEvent.hover(latest.getByText('Neueste Ausführung'));
        expect(await screen.findByRole('tooltip')).toHaveTextContent('Im Prozessmodell werden der Status und die Daten dieser Ausführung angezeigt.');
    });

    it('opens the event log for the selected historical task', async () => {
        renderDialog();
        await userEvent.click(within(rows()[1]).getByRole('button', {name: 'Ereignisprotokoll'}));
        expect(screen.getByRole('dialog', {name: 'Ereignisprotokoll'})).toHaveTextContent('Vorgang 17, Aufgabe 1');
    });

    it('removes the model marker while a restart is pending and assigns it to the replacement after refreshing', () => {
        const first = task(1);
        const failed = task(2, ProcessTaskStatus.Failed);
        const {rerenderRuntimeData} = renderDialog(runtime([first, failed]));
        expect(within(rows()[0]).getByText('Neueste Ausführung')).toBeVisible();

        const restarted = {
            ...failed,
            status: ProcessTaskStatus.Restarted,
        };
        rerenderRuntimeData(runtime([first, restarted]));
        expect(rows()).toHaveLength(1);
        expect(screen.queryByText('Neueste Ausführung')).not.toBeInTheDocument();

        const replacement = task(3, ProcessTaskStatus.Running, {restartForTaskId: restarted.id});
        rerenderRuntimeData(runtime([first, restarted, replacement]));
        expect(rows()).toHaveLength(2);
        expect(within(rows()[0]).getByText('Neueste Ausführung')).toBeVisible();
        expect(within(rows()[0]).getByRole('link', {name: 'Im Vorgangsverlauf ansehen'})).toHaveAttribute('href', '/process-instances/17/history?taskId=3');
        expect(within(rows()[1]).queryByText('Neueste Ausführung')).not.toBeInTheDocument();
    });

    it('compares against the referenced predecessor instead of the previous execution of this node', async () => {
        renderDialog(runtime([
            task(1, ProcessTaskStatus.Completed, {processData: {value: 'old loop'}}),
            task(2, ProcessTaskStatus.Completed, {
                processNodeId: 9,
                processData: {value: 'actual input'},
            }),
            task(3, ProcessTaskStatus.Completed, {
                previousProcessInstanceTaskId: 2,
                processData: {value: 'actual input'},
            }),
        ]));
        await userEvent.click(within(rows()[0]).getByRole('button', {name: 'Datenänderungen'}));
        const dialog = screen.getByRole('dialog', {name: 'Änderungen an den Vorgangsdaten'});
        expect(dialog).toHaveTextContent('Diese Aufgabe hat die Vorgangsdaten nicht verändert.');
        expect(dialog).toHaveTextContent('Prüfung · Ausführung 2');
    });

    it('explains unavailable predecessor snapshots', async () => {
        renderDialog(runtime([task(1, ProcessTaskStatus.Completed, {
            previousProcessInstanceTaskId: 999,
            processData: {value: 'snapshot'},
        })]));
        await userEvent.click(within(rows()[0]).getByRole('button', {name: 'Datenänderungen'}));
        expect(screen.getByRole('dialog', {name: 'Änderungen an den Vorgangsdaten'})).toHaveTextContent('Der vorherige Datenstand ist nicht verfügbar.');
    });

    it('offers the stored process snapshot separately from data changes', async () => {
        renderDialog(runtime([task(1, ProcessTaskStatus.Completed, {processData: {snapshot: 'selected execution'}})]));
        await userEvent.click(within(rows()[0]).getByRole('button', {name: 'Vorgangsdaten'}));
        const dialog = screen.getByRole('dialog', {name: 'Vorgangsdaten'});
        expect(dialog).toHaveTextContent('selected execution');
        expect(dialog).not.toHaveTextContent('Diese Aufgabe hat die Vorgangsdaten nicht verändert.');
    });

    it('keeps element data and attachments associated with their execution', async () => {
        const data = runtime([task(1, ProcessTaskStatus.Completed, {nodeData: {result: 'old result'}}), task(2)]);
        data.attachmentSets = [1, 2].map(id => ({
            id,
            name: `Anlagensatz ${id}`,
            dataKey: 'documents',
            processInstanceId: 17,
            processInstanceTaskId: id,
        }));
        data.attachments = [1, 2].map(id => ({
            ...new ProcessInstanceAttachmentApiService().initialize(),
            key: `file-${id}`,
            attachmentSetId: id,
            processInstanceId: 17,
            processInstanceTaskId: id,
            fileName: `Anlage-${id}.pdf`,
            originalFileName: `Anlage-${id}.pdf`,
        }));
        renderDialog(data);
        await userEvent.click(within(rows()[1]).getByRole('button', {name: 'Elementdaten'}));
        const dialog = screen.getByRole('dialog', {name: 'Elementdaten'});
        expect(dialog).toHaveTextContent('old result');
        expect(dialog).toHaveTextContent('Anlagensatz 1');
        expect(dialog).not.toHaveTextContent('Anlagensatz 2');
    });

    it('offers editing only for active executions and uses their own task links', () => {
        renderDialog();
        expect(within(rows()[0]).getByRole('link', {name: 'Aufgabe aufrufen'})).toHaveAttribute('href', '/tasks/17/2');
        expect(within(rows()[1]).queryByRole('link', {name: 'Aufgabe aufrufen'})).not.toBeInTheDocument();
    });

    it.each([ProcessTaskStatus.AwaitingCustomer, ProcessTaskStatus.AwaitingPayment])('links the correct participant task for %s', status => {
        renderDialog(runtime([task(1, status, {accessKey: 'task-access'})]));
        expect(screen.getByRole('link', {name: 'Aufgabe für Beteiligte aufrufen'})).toHaveAttribute('href', '/process/instance-access/tasks/task-access');
    });

    it('reruns only the selected failed task and refreshes after success', async () => {
        mocks.permissions.add(Permission.PROCESS_INSTANCE_RESTART_FAILED);
        const rerun = vi.spyOn(ProcessInstanceTaskApiService.prototype, 'rerunFailedTask').mockResolvedValue(task(1));
        const {onReload} = renderDialog(runtime([task(1, ProcessTaskStatus.Failed), task(2)]));
        expect(within(rows()[0]).queryByRole('button', {name: 'Aufgabe erneut ausführen'})).not.toBeInTheDocument();
        await userEvent.click(within(rows()[1]).getByRole('button', {name: 'Aufgabe erneut ausführen'}));
        await waitFor(() => expect(onReload).toHaveBeenCalledTimes(1));
        expect(rerun).toHaveBeenCalledExactlyOnceWith(1);
    });

    it('distinguishes a refresh failure from a failed rerun request', async () => {
        mocks.permissions.add(Permission.PROCESS_INSTANCE_RESTART_FAILED);
        vi.spyOn(ProcessInstanceTaskApiService.prototype, 'rerunFailedTask').mockResolvedValue(task(1));
        renderDialog(runtime([task(1, ProcessTaskStatus.Failed)]), vi.fn().mockRejectedValue(new Error('Refresh failed')));
        await userEvent.click(screen.getByRole('button', {name: 'Aufgabe erneut ausführen'}));
        await waitFor(() => expect(mocks.dispatch).toHaveBeenCalledWith(expect.objectContaining({
            payload: expect.objectContaining({message: 'Die erneute Ausführung wurde angefordert. Die Ausführungen konnten nicht aktualisiert werden.'}),
        })));
    });

    it('reports failed rerun requests without refreshing', async () => {
        mocks.permissions.add(Permission.PROCESS_INSTANCE_RESTART_FAILED);
        vi.spyOn(ProcessInstanceTaskApiService.prototype, 'rerunFailedTask').mockRejectedValue(new Error('Offline'));
        const {onReload} = renderDialog(runtime([task(1, ProcessTaskStatus.Failed)]));
        await userEvent.click(screen.getByRole('button', {name: 'Aufgabe erneut ausführen'}));
        await waitFor(() => expect(mocks.dispatch).toHaveBeenCalled());
        expect(onReload).not.toHaveBeenCalled();
    });

    it('hides rerun actions without the instance permission', () => {
        renderDialog(runtime([task(1, ProcessTaskStatus.Failed)]));
        expect(screen.queryByRole('button', {name: 'Aufgabe erneut ausführen'})).not.toBeInTheDocument();
    });

    it('does not expose data or actions under a grant for a different instance', () => {
        mocks.instanceId = 99;
        mocks.permissions.add(Permission.PROCESS_INSTANCE_RESTART_FAILED);
        renderDialog(runtime([task(1, ProcessTaskStatus.Failed)]));
        expect(screen.getByRole('alert')).toHaveTextContent('keine Berechtigung');
        expect(screen.queryByRole('list')).not.toBeInTheDocument();
    });

    it('marks incomplete results and does not imply complete iteration numbering', () => {
        renderDialog({
            ...runtime(),
            tasksComplete: false,
        });
        expect(screen.getByRole('alert')).toHaveTextContent('Die Aufgaben sind nicht vollständig geladen.');
    });
});
