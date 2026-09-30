import {act, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {configureStore} from '@reduxjs/toolkit';
import {Provider} from 'react-redux';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {GenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {Permission} from '../../../../data/permissions/permission';
import {type Page} from '../../../../models/dtos/page';
import {setPermissions, userReducer} from '../../../../slices/user-slice';
import {type PermissionSet} from '../../../permissions/models/permission-set';
import {type ProcessInstanceDetails} from '../../entities/process-instance-details';
import {type ProcessInstanceEventEntity} from '../../entities/process-instance-event-entity';
import {ProcessInstanceStatus} from '../../enums/process-instance-status';
import {ProcessInstanceApiService} from '../../services/process-instance-api-service';
import {ProcessInstanceEventApiService} from '../../services/process-instance-event-api-service';
import {ProcessInstanceTaskApiService} from '../../services/process-instance-task-api-service';
import {ProcessNodeApiService} from '../../services/process-node-api-service';
import {ProcessNodeProviderApiService, ProcessNodeType, type ProcessNodeProvider} from '../../services/process-node-provider-api-service';
import {ProcessInstanceDetailsPageHistory} from './process-instance-details-page-history';

function page<T>(content: T[], number = 0, totalPages = 1): Page<T> {
    return {content, page: {number, totalPages, size: 999, totalElements: content.length}};
}

function permissions(instanceId: number | null = 17, processId: number | null = null, system = false): PermissionSet {
    return {
        departmentPermissions: [], teamPermissions: [], domainPermissions: [],
        processInstancePermissions: instanceId == null ? [] : [{
            id: 'instance', userId: 'reader', processInstanceId: instanceId, permissions: [Permission.PROCESS_INSTANCE_READ],
        }],
        processPermissions: processId == null ? [] : [{
            id: 'process', userId: 'reader', processId, permissions: [Permission.PROCESS_DEFINITION_READ],
        }],
        systemPermissions: system ? [{
            userId: 'reader', permissions: [Permission.PROCESS_INSTANCE_READ, Permission.PROCESS_DEFINITION_READ],
        }] : [],
    };
}

function item(id = 17): ProcessInstanceDetails {
    return {
        instance: {...new ProcessInstanceApiService().initialize(), id, processId: 42, started: '2026-09-22T08:00:00Z'},
        processName: 'Bauantrag', departmentId: 4, departmentName: 'Bauamt', triggerName: 'Eingang', triggerType: null,
        activeTasks: [],
    };
}

function task(id = 1, started = '2026-09-22T09:00:00Z') {
    return {...new ProcessInstanceTaskApiService().initialize(), id, processInstanceId: 17, processId: 42, processNodeId: 101, started};
}

function event(id: number, taskId: number | null = null, timestamp = '2026-09-22T09:30:00Z'): ProcessInstanceEventEntity {
    return {
        ...new ProcessInstanceEventApiService().initialize(), id, processInstanceId: 17, processInstanceTaskId: taskId,
        timestamp, historyRelevant: true, title: `Ereignis ${id}`, message: `Nachricht ${id}`,
    };
}

const node = {
    ...ProcessNodeApiService.initialize(), id: 101, processId: 42, processVersion: 3,
    processNodeDefinitionKey: 'review', processNodeDefinitionVersion: 2, name: 'Unterlagen prüfen', description: 'Prüfbeschreibung',
};
const provider: ProcessNodeProvider = {
    key: 'review', componentKey: 'review', componentType: 'node', componentVersion: '2.0.0', majorVersion: 2,
    type: ProcessNodeType.Action, name: 'Prüfung v2', deprecationNotice: null, executionTypes: [], abstractDescription: '',
    description: '', documentationUrl: null, parentPluginKey: '', ports: [], outputs: [],
};

function renderHistory(initialItem: ProcessInstanceDetails | undefined = item(), grants = permissions()) {
    const store = configureStore({reducer: {user: userReducer}});
    store.dispatch(setPermissions(grants));
    const tree = (currentItem: ProcessInstanceDetails | undefined) => (
        <Provider store={store}>
            <GenericDetailsPageContext.Provider value={{
                item: currentItem, setItem: vi.fn(), setAdditionalData: vi.fn(), isBusy: false,
                setIsBusy: vi.fn(), refresh: vi.fn(), isEditable: false,
            }}>
                <ProcessInstanceDetailsPageHistory />
            </GenericDetailsPageContext.Provider>
        </Provider>
    );
    const view = render(tree(initialItem));
    return {...view, store, refresh: (nextItem: ProcessInstanceDetails | undefined) => view.rerender(tree(nextItem))};
}

function deferred<T>() {
    let resolve!: (value: T) => void;
    let reject!: (reason: Error) => void;
    const promise = new Promise<T>((res, rej) => {resolve = res; reject = rej;});
    return {promise, resolve, reject};
}

describe('Process instance history', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
        vi.spyOn(ProcessInstanceEventApiService.prototype, 'list').mockResolvedValue(page([]));
        vi.spyOn(ProcessInstanceTaskApiService.prototype, 'list').mockResolvedValue(page([task()]));
        vi.spyOn(ProcessNodeApiService.prototype, 'list').mockResolvedValue(page([node]));
        vi.spyOn(ProcessNodeProviderApiService.prototype, 'getNodeProviders').mockResolvedValue([provider]);
    });

    it('groups task events and merges unassigned and orphaned events chronologically', async () => {
        vi.mocked(ProcessInstanceTaskApiService.prototype.list).mockResolvedValue(page([
            task(3, '2026-09-22T11:00:00Z'), task(2), task(1),
        ]));
        vi.mocked(ProcessInstanceEventApiService.prototype.list).mockResolvedValue(page([
            event(9, 1), event(7, null, '2026-09-22T10:00:00Z'), event(8, 1),
            event(10, 999, '2026-09-22T10:30:00Z'), event(6, 1, '2026-09-22T09:10:00Z'),
        ]));
        const current = item();
        current.instance.status = ProcessInstanceStatus.Completed;
        current.instance.statusOverride = 'Entscheidung getroffen';
        current.instance.finished = '2026-09-22T12:00:00Z';
        renderHistory(current);
        const timeline = await screen.findByRole('list', {name: 'Bisheriger Verlauf'});
        expect(Array.from(timeline.children).map((li) => li.textContent)).toEqual([
            expect.stringContaining('Vorgang gestartet'),
            expect.stringContaining('Aufgabe #1'), expect.stringContaining('Aufgabe #2'),
            expect.stringContaining('Ereignis 7'), expect.stringContaining('Ereignis 10'),
            expect.stringContaining('Aufgabe #3'), expect.stringContaining('Vorgang abgeschlossen'),
        ]);
        expect(screen.getByText('Entscheidung getroffen')).toBeInTheDocument();
        expect(screen.queryByRole('region', {name: 'Mögliche nächste Schritte'})).not.toBeInTheDocument();
        const button = screen.getByRole('button', {name: /Aufgabe #1/});
        expect(button).toHaveAttribute('aria-expanded', 'false');
        await userEvent.click(button);
        expect(button).toHaveAttribute('aria-expanded', 'true');
        const events = within(screen.getByRole('list', {name: 'Ereignisse zu Aufgabe #1'}));
        expect(events.getAllByRole('listitem').map((li) => li.textContent)).toEqual([
            expect.stringContaining('Ereignis 6'), expect.stringContaining('Ereignis 8'), expect.stringContaining('Ereignis 9'),
        ]);
        expect(events.getByText('Nachricht 8')).toBeVisible();
        expect(events.queryByText('Ereignis 7')).not.toBeInTheDocument();
        expect(ProcessInstanceEventApiService.prototype.list).toHaveBeenCalledWith(0, 999, ['timestamp', 'id'], 'ASC', {
            processInstanceId: 17, historyRelevant: true,
        });
    });

    it('uses active task names and displays an empty event state without definition access', async () => {
        const current = item();
        current.activeTasks = [{id: 1, name: 'Fachprüfung', status: task().status, assignedUserId: null}];
        renderHistory(current);
        await userEvent.click(await screen.findByRole('button', {name: /Fachprüfung/}));
        expect(screen.getByText('Keine verlaufsrelevanten Ereignisse vorhanden.')).toBeVisible();
        expect(ProcessNodeApiService.prototype.list).not.toHaveBeenCalled();
        expect(ProcessNodeProviderApiService.prototype.getNodeProviders).not.toHaveBeenCalled();
    });

    it('distinguishes initial loading and a history with no tasks', async () => {
        vi.mocked(ProcessInstanceTaskApiService.prototype.list).mockResolvedValue(page([]));
        const view = renderHistory();
        view.refresh(undefined);
        expect(screen.getByRole('status', {name: 'Verlauf wird geladen'})).toHaveAttribute('aria-busy', 'true');
        view.refresh(item());
        await screen.findByText('Für diesen Vorgang wurden noch keine Aufgaben angelegt.');
        expect(screen.queryByRole('status', {name: 'Verlauf wird geladen'})).not.toBeInTheDocument();
        expect(screen.queryByText('Vorgang abgeschlossen')).not.toBeInTheDocument();
    });

    it.each([
        ['scoped', permissions(17, 42)], ['system', permissions(null, null, true)],
    ])('loads optional metadata with %s grants and matches the provider version', async (_label, grants) => {
        vi.mocked(ProcessNodeApiService.prototype.list).mockResolvedValue(page([{...node, name: null}]));
        vi.mocked(ProcessNodeProviderApiService.prototype.getNodeProviders).mockResolvedValue([
            {...provider, majorVersion: 1, name: 'Prüfung v1'}, provider,
        ]);
        renderHistory(item(), grants);
        expect(await screen.findByRole('button', {name: /Prüfung v2/})).toHaveTextContent('Prüfbeschreibung');
        expect(screen.queryByText('Prüfung v1')).not.toBeInTheDocument();
        expect(ProcessNodeApiService.prototype.list).toHaveBeenCalledWith(0, 999, 'id', 'ASC', {processId: 42});
    });

    it('does not use definition grants for a different process', async () => {
        renderHistory(item(), permissions(17, 43));
        await screen.findByRole('button', {name: /Aufgabe #1/});
        expect(ProcessNodeApiService.prototype.list).not.toHaveBeenCalled();
        expect(ProcessNodeProviderApiService.prototype.getNodeProviders).not.toHaveBeenCalled();
    });

    it.each([permissions(null), permissions(18)])('does not request history without the matching instance grant', async (grants) => {
        renderHistory(item(), grants);
        expect(screen.getByRole('alert')).toHaveTextContent('Sie haben keine Berechtigung mehr');
        expect(ProcessInstanceTaskApiService.prototype.list).not.toHaveBeenCalled();
        expect(ProcessInstanceEventApiService.prototype.list).not.toHaveBeenCalled();
        expect(ProcessNodeApiService.prototype.list).not.toHaveBeenCalled();
    });

    it.each(['nodes', 'providers'] as const)('preserves history when optional %s cannot be loaded', async (source) => {
        if (source === 'nodes') vi.mocked(ProcessNodeApiService.prototype.list).mockRejectedValue(new Error('Forbidden'));
        else vi.mocked(ProcessNodeProviderApiService.prototype.getNodeProviders).mockRejectedValue(new Error('Unavailable'));
        renderHistory(item(), permissions(17, 42));
        await screen.findByRole('button', {name: source === 'nodes' ? /Aufgabe #1/ : /Unterlagen prüfen/});
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('loads every page of tasks, events and nodes', async () => {
        vi.mocked(ProcessInstanceTaskApiService.prototype.list)
            .mockResolvedValueOnce(page([task(1)], 0, 2)).mockResolvedValueOnce(page([task(2)], 1, 2));
        vi.mocked(ProcessInstanceEventApiService.prototype.list)
            .mockResolvedValueOnce(page([event(1, 1)], 0, 2)).mockResolvedValueOnce(page([event(2, 2)], 1, 2));
        vi.mocked(ProcessNodeApiService.prototype.list)
            .mockResolvedValueOnce(page([], 0, 2)).mockResolvedValueOnce(page([node], 1, 2));
        renderHistory(item(), permissions(17, 42));
        const buttons = await screen.findAllByRole('button', {name: /Unterlagen prüfen/});
        expect(buttons).toHaveLength(2);
        await userEvent.click(buttons[1]);
        expect(screen.getByText('Ereignis 2')).toBeVisible();
        expect(ProcessInstanceTaskApiService.prototype.list).toHaveBeenLastCalledWith(1, 999, ['started', 'id'], 'ASC', {processInstanceId: 17});
        expect(ProcessInstanceEventApiService.prototype.list).toHaveBeenLastCalledWith(1, 999, ['timestamp', 'id'], 'ASC', {processInstanceId: 17, historyRelevant: true});
        expect(ProcessNodeApiService.prototype.list).toHaveBeenLastCalledWith(1, 999, 'id', 'ASC', {processId: 42});
    });

    it.each(['tasks', 'events'] as const)('allows retry after a required %s request fails', async (source) => {
        const service = source === 'tasks' ? ProcessInstanceTaskApiService.prototype : ProcessInstanceEventApiService.prototype;
        vi.mocked(service.list).mockRejectedValueOnce(new Error('Unavailable'));
        renderHistory();
        expect(await screen.findByRole('alert')).toHaveTextContent('Der Verlauf konnte nicht geladen werden.');
        expect(screen.queryByRole('status', {name: 'Verlauf wird geladen'})).not.toBeInTheDocument();
        await userEvent.click(screen.getByRole('button', {name: 'Erneut laden'}));
        await screen.findByRole('button', {name: /Aufgabe #1/});
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it.each(['response', 'error'] as const)('ignores a late %s after parent refresh', async (lateResult) => {
        const pending = deferred<Page<ProcessInstanceEventEntity>>();
        vi.mocked(ProcessInstanceEventApiService.prototype.list).mockReturnValueOnce(pending.promise);
        const view = renderHistory();
        vi.mocked(ProcessInstanceEventApiService.prototype.list).mockResolvedValue(page([event(20)]));
        view.refresh(item());
        await screen.findByText('Ereignis 20');
        await act(async () => {
            if (lateResult === 'response') pending.resolve(page([event(10)], 0, 2));
            else pending.reject(new Error('Old request failed'));
        });
        expect(screen.getByText('Ereignis 20')).toBeVisible();
        expect(screen.queryByText('Ereignis 10')).not.toBeInTheDocument();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        expect(ProcessInstanceEventApiService.prototype.list).toHaveBeenCalledTimes(2);
    });

    it('hides old history while reloading an updated parent', async () => {
        const view = renderHistory();
        await screen.findByRole('button', {name: /Aufgabe #1/});
        const pending = deferred<Page<ProcessInstanceEventEntity>>();
        vi.mocked(ProcessInstanceEventApiService.prototype.list).mockReturnValueOnce(pending.promise);
        view.refresh(item());
        expect(screen.queryByRole('button', {name: /Aufgabe #1/})).not.toBeInTheDocument();
        expect(screen.getByRole('status', {name: 'Verlauf wird geladen'})).toBeInTheDocument();
        await act(async () => pending.resolve(page([])));
        await screen.findByRole('button', {name: /Aufgabe #1/});
    });

    it('ignores responses from a previous instance', async () => {
        const pending = deferred<Page<ProcessInstanceEventEntity>>();
        vi.mocked(ProcessInstanceEventApiService.prototype.list).mockReturnValueOnce(pending.promise);
        const view = renderHistory(item(), permissions(null, null, true));
        vi.mocked(ProcessInstanceEventApiService.prototype.list).mockResolvedValue(page([{...event(20), processInstanceId: 18}]));
        view.refresh(item(18));
        await screen.findByText('Ereignis 20');
        await act(async () => pending.resolve(page([event(10)])));
        expect(screen.queryByText('Ereignis 10')).not.toBeInTheDocument();
        expect(ProcessInstanceEventApiService.prototype.list).toHaveBeenLastCalledWith(0, 999, ['timestamp', 'id'], 'ASC', {
            processInstanceId: 18, historyRelevant: true,
        });
    });

    it('removes metadata immediately on definition permission loss', async () => {
        const view = renderHistory(item(), permissions(17, 42));
        await screen.findByRole('button', {name: /Unterlagen prüfen/});
        act(() => {view.store.dispatch(setPermissions(permissions()));});
        expect(screen.queryByText('Unterlagen prüfen')).not.toBeInTheDocument();
        expect(screen.queryByText('Prüfbeschreibung')).not.toBeInTheDocument();
        await screen.findByRole('button', {name: /Aufgabe #1/});
        expect(ProcessNodeApiService.prototype.list).toHaveBeenCalledTimes(1);
    });

    it('discards pending results on instance permission loss and reloads on regain', async () => {
        const pending = deferred<Page<ProcessInstanceEventEntity>>();
        vi.mocked(ProcessInstanceEventApiService.prototype.list).mockReturnValueOnce(pending.promise);
        const view = renderHistory();
        act(() => {view.store.dispatch(setPermissions(permissions(null)));});
        await act(async () => pending.resolve(page([event(10)])));
        expect(screen.getByRole('alert')).toHaveTextContent('Sie haben keine Berechtigung mehr');
        expect(screen.queryByText('Ereignis 10')).not.toBeInTheDocument();
        act(() => {view.store.dispatch(setPermissions(permissions()));});
        await screen.findByRole('button', {name: /Aufgabe #1/});
        expect(screen.queryByText('Ereignis 10')).not.toBeInTheDocument();
        act(() => {view.store.dispatch(setPermissions(permissions(null)));});
        expect(screen.queryByRole('list', {name: 'Bisheriger Verlauf'})).not.toBeInTheDocument();
        await waitFor(() => expect(ProcessInstanceEventApiService.prototype.list).toHaveBeenCalledTimes(2));
    });
});
