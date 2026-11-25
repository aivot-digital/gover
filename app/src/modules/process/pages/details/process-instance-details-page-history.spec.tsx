import {ProcessHistoryEventScope} from '../../enums/process-history-event-scope';
import {type ProcessInstanceHistoryTasks} from '../../models/process-instance-history-tasks';
import {type ProcessInstanceHistoryEvent} from '../../entities/process-instance-event-entity';
import {act, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ThemeProvider} from '@mui/material';
import {BaseTheme} from '../../../../theming/base-theme';
import {Link, MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {GenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {createGenericDetailsPageEventChannel} from '../../../../components/generic-details-page/generic-details-page-events';
import {Permission} from '../../../../data/permissions/permission';
import {hasProcessPermission, hasProcessInstancePermission} from '../../../permissions/utils/permission-utils';
import {type Page} from '../../../../models/dtos/page';
import {type ProcessDefinitionEdgeEntity} from '../../entities/process-definition-edge-entity';
import {type ProcessInstanceDetails} from '../../entities/process-instance-details';
import {type ProcessInstanceTaskEntity} from '../../entities/process-instance-task-entity';
import {type ProcessNodeEntity} from '../../entities/process-node-entity';
import {ProcessTaskStatus} from '../../enums/process-task-status';
import {ProcessInstanceStatus} from '../../enums/process-instance-status';
import {ProcessDefinitionApiService} from '../../services/process-definition-api-service';
import {ProcessDefinitionEdgeApiService} from '../../services/process-definition-edge-api-service';
import {ProcessInstanceApiService} from '../../services/process-instance-api-service';
import {ProcessNodeExecutionLogLevel} from '../../entities/process-instance-event-entity';
import {ProcessInstanceEventApiService} from '../../services/process-instance-event-api-service';
import {ProcessInstanceTaskApiService} from '../../services/process-instance-task-api-service';
import {ProcessNodeApiService} from '../../services/process-node-api-service';
import {
    type ProcessNodeProvider,
    ProcessNodeProviderApiService,
    ProcessNodeType,
} from '../../services/process-node-provider-api-service';
import {ProcessInstanceDetailsPageHistory} from './process-instance-details-page-history';

const mocks = vi.hoisted(() => ({
    dispatch: vi.fn(), permissions: new Set<string>(), systemPermissions: new Set<string>(),
    processGrantId: 10, instanceGrantId: 17,
}));

function permissionSet() {
    return {
        departmentPermissions: [], teamPermissions: [], domainPermissions: [],
        systemPermissions: [{userId: 'staff', permissions: [...mocks.systemPermissions]}],
        processPermissions: [{id: 'process', userId: 'staff', processId: mocks.processGrantId, permissions: [...mocks.permissions].filter(permission => permission.startsWith('process_definition.'))}],
        processInstancePermissions: [{id: 'instance', userId: 'staff', processInstanceId: mocks.instanceGrantId, permissions: [...mocks.permissions].filter(permission => permission.startsWith('process_instance.'))}],
    };
}
vi.mock('../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => mocks.dispatch}));
// Each hook only knows the keys of its own scope, so a check at the wrong scope fails.
vi.mock('../../../permissions/hooks/use-permissions', () => ({
    useHasProcessInstancePermission: (id: number, permission: string) => hasProcessInstancePermission(permissionSet(), id, permission),
    useHasProcessPermission: (id: number, permission: string) => hasProcessPermission(permissionSet(), id, permission),
}));

function deferred<T>() {
    let resolve!: (value: T) => void;
    let reject!: (error: unknown) => void;
    const promise = new Promise<T>((resolvePromise, rejectPromise) => {
        resolve = resolvePromise;
        reject = rejectPromise;
    });
    return {promise, resolve, reject};
}

function page<T>(content: T[], totalElements = content.length): Page<T> {
    return {
        content,
        page: {
            size: content.length,
            number: 0,
            totalElements,
            totalPages: Math.max(1, Math.ceil(totalElements / Math.max(1, content.length))),
        },
    };
}

const historyEvents = vi.fn<() => Promise<Page<ProcessInstanceHistoryEvent>>>();
let historyTasks: ProcessInstanceTaskEntity[] = [];

function history(content: ProcessInstanceTaskEntity[], totalElements?: number): ProcessInstanceHistoryTasks {
    historyTasks = content;
    const visible = content.filter(task => task.status !== ProcessTaskStatus.Restarted);
    return {tasks: page(visible, totalElements ?? visible.length), predecessors: content, activeTasks: content};
}

function provider(type: ProcessNodeType, key: string = type): ProcessNodeProvider {
    return {
        key,
        componentKey: key,
        componentType: 'ProcessNodeDefinition',
        componentVersion: '1.0.0',
        deprecationNotice: null,
        majorVersion: 1,
        type,
        executionTypes: [],
        name: key,
        abstractDescription: '',
        description: '',
        documentationUrl: null,
        parentPluginKey: 'test',
        outputs: [],
    };
}

function node(id: number, name: string, definitionKey: string = ProcessNodeType.Action, ports = ['next']): ProcessNodeEntity {
    return {
        ...ProcessNodeApiService.initialize(),
        id,
        name,
        processId: 10,
        processVersion: 1,
        processNodeDefinitionKey: definitionKey,
        processNodeDefinitionVersion: 1,
        ports: ports.map((key) => ({key, deadlineKey: null, label: key, description: ''})),
    };
}

function edge(fromNodeId: number, toNodeId: number, viaPort = 'next'): ProcessDefinitionEdgeEntity {
    return {id: fromNodeId * 100 + toNodeId, processId: 10, processVersion: 1, fromNodeId, toNodeId, viaPort};
}

function task(id: number, processNodeId: number, status = ProcessTaskStatus.Running): ProcessInstanceTaskEntity {
    return {
        ...new ProcessInstanceTaskApiService().initialize(),
        id,
        processInstanceId: 17,
        processId: 10,
        processVersion: 1,
        processNodeId,
        status,
    };
}

function historyEvent() {
    return {...new ProcessInstanceEventApiService().initialize(), processInstanceId: 17};
}

async function renderHistory(
    nodes: ProcessNodeEntity[],
    edges: ProcessDefinitionEdgeEntity[],
    tasks: ProcessInstanceTaskEntity[],
    {waitForLoad = true, instance, initialUrl = '/', totalTasks, navigationTarget}: {waitForLoad?: boolean; instance?: Partial<ProcessInstanceDetails['instance']>; initialUrl?: string; totalTasks?: number; navigationTarget?: string} = {},
) {
    vi.spyOn(ProcessNodeApiService.prototype, 'listAll').mockResolvedValue(page(nodes));
    vi.spyOn(ProcessDefinitionEdgeApiService.prototype, 'listAll').mockResolvedValue(page(edges));
    vi.spyOn(ProcessInstanceTaskApiService.prototype, 'getHistory').mockResolvedValue(history(tasks, totalTasks));
    const item: ProcessInstanceDetails = {
        instance: {...new ProcessInstanceApiService().initialize(), id: 17, processId: 10, initialProcessVersion: 1, ...instance},
        processName: 'Test process',
        departmentId: 1,
        departmentName: null,
        triggerName: 'Start',
        triggerType: null,
        activeTasks: [],
    };

    const eventChannel = createGenericDetailsPageEventChannel();
    const renderContent = (details: ProcessInstanceDetails) => (
        <MemoryRouter initialEntries={[initialUrl]}>
            <ThemeProvider theme={BaseTheme}>
                <GenericDetailsPageContext.Provider value={{
                    item: details,
                    setItem: vi.fn(),
                    setAdditionalData: vi.fn(),
                    isBusy: false,
                    setIsBusy: vi.fn(),
                    refresh: vi.fn(),
                    subscribeEvent: eventChannel.subscribeEvent,
                    isEditable: false,
                }}>
                    {navigationTarget && <Link to={navigationTarget}>Neues Sprungziel</Link>}
                    <ProcessInstanceDetailsPageHistory/>
                </GenericDetailsPageContext.Provider>
            </ThemeProvider>
        </MemoryRouter>
    );
    const rendered = render(renderContent(item));
    if (waitForLoad) {
        await screen.findByRole('heading', {name: 'Verlauf des Vorgangs'});
        await waitFor(() => expect(document.querySelector('[aria-busy="true"]')).toBeNull());
    }
    return {
        ...rendered, eventChannel,
        rerenderInstance: (updates: Partial<ProcessInstanceDetails['instance']>) => rendered.rerender(
            renderContent({...item, instance: {...item.instance, ...updates}}),
        ),
    };
}

describe('Process instance history preview', () => {
    const identities: ProcessInstanceDetails['instance']['identities'] = {
        applicant: {
            identityId: 'applicant', title: 'Antragstellende', type: 'Email',
            emailAddress: 'person@example.org', attributes: {}, providerKey: null, metadataIdentifier: null,
            uniqueIdFromIdentityProvider: null, communicationProviderBindingId: null, communicationProviderData: {},
        },
    };

    beforeEach(() => {
        mocks.permissions = new Set([Permission.PROCESS_INSTANCE_READ, Permission.PROCESS_DEFINITION_READ]);
        mocks.systemPermissions = new Set();
        mocks.processGrantId = 10;
        mocks.instanceGrantId = 17;
        vi.spyOn(ProcessDefinitionApiService.prototype, 'retrieve').mockResolvedValue(ProcessDefinitionApiService.initialize());
        historyEvents.mockReset().mockResolvedValue(page([]));
        vi.spyOn(ProcessInstanceEventApiService.prototype, 'listHistory').mockImplementation(async (_instanceId, options) => {
            const response = await historyEvents();
            const ancestors = new Set<number>();
            let current = historyTasks.find(task => task.id === options.taskId);
            while (current?.restartForTaskId != null && !ancestors.has(current.restartForTaskId)) {
                ancestors.add(current.restartForTaskId);
                current = historyTasks.find(task => task.id === current!.restartForTaskId);
            }
            const content = response.content.filter(event => options.scope === ProcessHistoryEventScope.ManualInterventions
                ? event.details.manualIntervention === true
                : options.scope === ProcessHistoryEventScope.InstanceErrors
                    ? event.processInstanceTaskId == null && event.level === ProcessNodeExecutionLogLevel.Error && event.details.manualIntervention !== true
                    : event.processInstanceTaskId === options.taskId ||
                      (ancestors.has(event.processInstanceTaskId!) && event.details.manualIntervention === true));
            return page(content);
        });
        vi.spyOn(ProcessInstanceEventApiService.prototype, 'list').mockResolvedValue(page([]));
        vi.spyOn(ProcessNodeProviderApiService.prototype, 'getNodeProviders').mockResolvedValue([
            provider(ProcessNodeType.Action),
            provider(ProcessNodeType.FlowControl),
            provider(ProcessNodeType.Termination),
            provider(ProcessNodeType.Action, 'approval'),
        ]);
    });

    describe('loading and model failures', () => {
        it('replaces the initial skeleton with a retryable error and recovers', async () => {
            vi.spyOn(ProcessInstanceTaskApiService.prototype, 'getHistory').mockRejectedValueOnce(new Error('Offline'));
            await renderHistory([node(1, 'Prüfung')], [], [task(1, 1)], {waitForLoad: false});
            expect(await screen.findByRole('alert')).toHaveTextContent('Der Verlauf konnte nicht geladen werden.');
            expect(screen.queryByRole('status', {name: 'Verlauf wird geladen'})).not.toBeInTheDocument();
            await userEvent.click(screen.getByRole('button', {name: 'Erneut versuchen'}));
            expect(await screen.findByRole('heading', {name: 'Verlauf des Vorgangs'})).toBeVisible();
            expect(ProcessInstanceEventApiService.prototype.listHistory).toHaveBeenCalledTimes(3);
        });

        it('keeps stored task results available when the model providers cannot be loaded', async () => {
            vi.mocked(ProcessNodeProviderApiService.prototype.getNodeProviders).mockRejectedValueOnce(new Error('Provider unavailable'));
            await renderHistory([node(1, 'Prüfung')], [], [{...task(1, 1), executionSummaryMarkdown: 'Gespeichertes Ergebnis'}]);
            expect(screen.getByText('Die nächsten Schritte konnten nicht ermittelt werden. Der bisherige Verlauf bleibt verfügbar.')).toBeVisible();
            await userEvent.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
            expect(screen.getByText('Gespeichertes Ergebnis')).toBeVisible();
            expect(screen.getByRole('button', {name: 'Datenänderungen anzeigen'})).toBeEnabled();
            await userEvent.click(screen.getByRole('button', {name: 'Erneut versuchen'}));
            await waitFor(() => expect(screen.queryByText('Die nächsten Schritte konnten nicht ermittelt werden. Der bisherige Verlauf bleibt verfügbar.')).not.toBeInTheDocument());
        });

        it('does not hide the tasks if an unused node has an unavailable provider', async () => {
            await renderHistory([node(1, 'Prüfung'), node(2, 'Nicht verfügbar', 'missing')], [], [task(1, 1)]);
            expect(screen.getByRole('button', {name: /^1\. Prüfung:/})).toBeVisible();
            expect(screen.getByText('Die nächsten Schritte konnten nicht ermittelt werden. Der bisherige Verlauf bleibt verfügbar.')).toBeVisible();
            expect(ProcessNodeApiService.prototype.listAll).toHaveBeenCalledWith({processId: 10, processVersion: 1});
            expect(ProcessDefinitionEdgeApiService.prototype.listAll).toHaveBeenCalledWith({processDefinitionId: 10, processDefinitionVersion: 1});
        });
    });

    describe('task pagination', () => {
        function historyPage(tasks: ProcessInstanceTaskEntity[], number: number, totalElements: number): ProcessInstanceHistoryTasks {
            const result = history(tasks);
            result.tasks.page = {number, size: 20, totalElements, totalPages: Math.ceil(totalElements / 20)};
            return result;
        }

        it('loads only the chosen task page and keeps the independent overview sections mounted', async () => {
            vi.spyOn(ProcessInstanceTaskApiService.prototype, 'getHistory').mockResolvedValueOnce(historyPage([task(1, 1)], 0, 40));
            await renderHistory([node(1, 'Prüfung')], [], [task(1, 1)]);
            const next = historyPage([task(21, 1)], 1, 40);
            vi.mocked(ProcessInstanceTaskApiService.prototype.getHistory).mockResolvedValueOnce(next);
            await userEvent.click(within(screen.getByRole('navigation', {name: 'Seiten des Vorgangsverlaufs'})).getByRole('button', {name: 'Nächste Seite'}));
            expect(await screen.findByRole('button', {name: /^21\. Prüfung:/})).toBeVisible();
            expect(screen.queryByRole('button', {name: /^1\. Prüfung:/})).not.toBeInTheDocument();
            expect(ProcessInstanceTaskApiService.prototype.getHistory).toHaveBeenLastCalledWith(17, 1, 20, undefined);
            expect(ProcessInstanceEventApiService.prototype.listHistory).toHaveBeenCalledWith(17, {
                scope: ProcessHistoryEventScope.Task, taskId: 21, size: 20, sortOrder: 'DESC',
            });
            expect(vi.mocked(ProcessInstanceEventApiService.prototype.listHistory).mock.calls.filter(([, options]) => options.scope !== ProcessHistoryEventScope.Task)).toHaveLength(2);
        });

        it.each(['1021', 'active', 'current', 'failed', 'latest'])('opens the server-resolved page for %s and keeps it on refresh', async target => {
            const selected = task(1021, 1, target === 'failed' ? ProcessTaskStatus.Failed : ProcessTaskStatus.Running);
            const response = historyPage([selected], 51, 1021);
            vi.spyOn(ProcessInstanceTaskApiService.prototype, 'getHistory').mockResolvedValueOnce(response);
            const {eventChannel} = await renderHistory([node(1, 'Prüfung')], [], [selected], {initialUrl: `/?taskId=${target}`});
            expect(screen.getByRole('button', {name: /^1021\. Prüfung:/})).toHaveAttribute('aria-expanded', 'true');
            expect(ProcessInstanceTaskApiService.prototype.getHistory).toHaveBeenCalledExactlyOnceWith(17, 0, 20, target);
            vi.mocked(ProcessInstanceTaskApiService.prototype.getHistory).mockResolvedValueOnce(response);
            act(() => eventChannel.emitEvent('refresh'));
            await waitFor(() => expect(ProcessInstanceTaskApiService.prototype.getHistory).toHaveBeenLastCalledWith(17, 51, 20, undefined));
        });

        it('waits for the new target response before resolving a generic link from an already visible page', async () => {
            await renderHistory([node(1, 'Prüfung')], [], [task(1, 1)], {navigationTarget: '/?taskId=latest'});
            const pending = deferred<ProcessInstanceHistoryTasks>();
            vi.mocked(ProcessInstanceTaskApiService.prototype.getHistory).mockReturnValueOnce(pending.promise);
            await userEvent.click(screen.getByRole('link', {name: 'Neues Sprungziel'}));
            expect(screen.getByRole('button', {name: /^1\. Prüfung:/})).toHaveAttribute('aria-expanded', 'false');
            await act(async () => pending.resolve(historyPage([task(1021, 1)], 51, 1021)));
            expect(screen.getByRole('button', {name: /^1021\. Prüfung:/})).toHaveAttribute('aria-expanded', 'true');
            expect(ProcessInstanceTaskApiService.prototype.getHistory).toHaveBeenLastCalledWith(17, 0, 20, 'latest');
        });

        it('uses off-page predecessor data and active task metadata for the preview', async () => {
            const previous = {...task(20, 1, ProcessTaskStatus.Completed), processData: {value: 'input'}};
            const current = {...task(21, 1, ProcessTaskStatus.Completed), previousProcessInstanceTaskId: 20, processData: {value: 'input'}};
            vi.spyOn(ProcessInstanceTaskApiService.prototype, 'getHistory').mockResolvedValueOnce({
                ...historyPage([current], 1, 40), predecessors: [previous], activeTasks: [task(40, 2)],
            });
            await renderHistory([node(1, 'Prüfung'), node(2, 'Aktuell'), node(3, 'Versand')], [edge(2, 3)], [current]);
            expect(screen.getByRole('list', {name: 'Voraussichtliche nächste Schritte'})).toHaveTextContent('Versand');
            await userEvent.click(screen.getByRole('button', {name: /^21\. Prüfung:/}));
            await userEvent.click(screen.getByRole('button', {name: 'Datenänderungen anzeigen'}));
            const dialog = screen.getByRole('dialog', {name: 'Änderungen an den Vorgangsdaten'});
            expect(dialog).toHaveTextContent('Diese Aufgabe hat die Vorgangsdaten nicht verändert.');
            expect(dialog).toHaveTextContent('21. Prüfung');
        });

        it('shows the latest bounded task events chronologically and links to the complete task log', async () => {
            const events = Array.from({length: 20}, (_, index) => ({...historyEvent(),
                id: 21 - index, processInstanceTaskId: 42, title: `Event ${21 - index}`,
                timestamp: '2026-11-25T10:00:00Z',
            }));
            vi.mocked(ProcessInstanceEventApiService.prototype.listHistory).mockImplementation(async (_instance, options) =>
                options.scope === ProcessHistoryEventScope.Task ? page(events, 100) : page([]));
            const getEventLog = vi.spyOn(ProcessInstanceEventApiService.prototype, 'getEventLog').mockResolvedValue({
                instance: {id: 17, caseNumber: 'V-17', started: '', finished: null, runtime: null}, task: null, events: page([]),
            });
            await renderHistory([node(1, 'Prüfung')], [], [task(42, 1)]);
            await userEvent.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
            expect(screen.getByText(/Angezeigt werden die letzten 20 von 100 Ereignissen/)).toBeVisible();
            const items = within(screen.getByRole('list', {name: 'Ereignisse und Zwischenergebnisse'})).getAllByRole('listitem');
            expect(items).toHaveLength(20);
            expect(items[0]).toHaveTextContent('Event 2');
            expect(items[19]).toHaveTextContent('Event 21');
            await userEvent.click(screen.getByRole('button', {name: 'Alle Ereignisse anzeigen'}));
            await waitFor(() => expect(getEventLog).toHaveBeenCalledWith(expect.objectContaining({processInstanceId: 17, processInstanceTaskId: 42, includeRestartHistory: true})));
        });

        it('keeps task results accessible when only their event request fails', async () => {
            vi.mocked(ProcessInstanceEventApiService.prototype.listHistory).mockImplementation(async (_instance, options) => {
                if (options.scope === ProcessHistoryEventScope.Task) throw new Error('Offline');
                return page([]);
            });
            await renderHistory([node(1, 'Prüfung')], [], [{...task(42, 1), executionSummaryMarkdown: 'Gespeichertes Ergebnis'}]);
            await userEvent.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
            expect(screen.getByText('Die Ereignisse dieser Aufgabe konnten nicht geladen werden.')).toBeVisible();
            expect(screen.getByText('Gespeichertes Ergebnis')).toBeVisible();
            expect(screen.getByText('Keine manuellen Eingriffe protokolliert.')).toBeVisible();
        });
    });

    describe('task navigation', () => {
        it.each([
            {target: '1', expanded: [1]}, {target: 'active', expanded: [2]}, {target: 'current', expanded: [2]},
            {target: 'failed', expanded: [3]}, {target: 'latest', expanded: [3]},
        ])('opens only the requested target $target', async ({target, expanded}) => {
            await renderHistory([node(1, 'Prüfung')], [], [
                task(1, 1, ProcessTaskStatus.Completed), task(2, 1, ProcessTaskStatus.InProgress), task(3, 1, ProcessTaskStatus.Failed),
            ], {initialUrl: `/?taskId=${target}`});
            for (const id of [1, 2, 3]) {
                await waitFor(() => expect(screen.getByRole('button', {name: new RegExp(`^${id}\\. Prüfung:`)}))
                    .toHaveAttribute('aria-expanded', String(expanded.includes(id))));
            }
            expect(screen.getByRole('button', {name: new RegExp(`^${expanded[0]}\\. Prüfung:`)})).toHaveFocus();
        });

        it('opens all active tasks for a generic link, including repeated executions of the same node', async () => {
            await renderHistory([node(1, 'Prüfung')], [], [task(1, 1), task(2, 1, ProcessTaskStatus.Paused)], {initialUrl: '/?taskId=active'});
            await waitFor(() => expect(screen.getByRole('button', {name: /^2\. Prüfung:/})).toHaveAttribute('aria-expanded', 'true'));
            expect(screen.getByRole('button', {name: /^1\. Prüfung:/})).toHaveAttribute('aria-expanded', 'true');
        });

        it('uses the last visible task for current when the process has no active tasks', async () => {
            await renderHistory([node(1, 'Prüfung')], [], [task(1, 1, ProcessTaskStatus.Completed)], {initialUrl: '/?taskId=current'});
            await waitFor(() => expect(screen.getByRole('button', {name: /^1\. Prüfung:/})).toHaveAttribute('aria-expanded', 'true'));
        });

        it.each(['99', 'invalid', '0', '9007199254740993'])('explains unavailable or invalid targets (%s)', async target => {
            await renderHistory([node(1, 'Prüfung')], [], [task(1, 1)], {initialUrl: `/?taskId=${target}`});
            expect(await screen.findByText(/Das Sprungziel ist im angezeigten Verlauf nicht verfügbar/)).toBeVisible();
            expect(screen.getByRole('button', {name: /^1\. Prüfung:/})).toHaveAttribute('aria-expanded', 'false');
        });

        it('resolves a URL target after a temporarily empty history is refreshed', async () => {
            const {eventChannel} = await renderHistory([node(1, 'Prüfung')], [], [], {initialUrl: '/?taskId=active'});
            expect(await screen.findByText(/Das Sprungziel ist im angezeigten Verlauf nicht verfügbar/)).toBeVisible();
            vi.mocked(ProcessInstanceTaskApiService.prototype.getHistory).mockResolvedValueOnce(history([task(1, 1)]));
            act(() => eventChannel.emitEvent('refresh'));
            const summary = await screen.findByRole('button', {name: /^1\. Prüfung:/});
            await waitFor(() => expect(summary).toHaveAttribute('aria-expanded', 'true'));
            expect(summary).toHaveFocus();
            expect(screen.queryByText(/Das Sprungziel ist im angezeigten Verlauf nicht verfügbar/)).not.toBeInTheDocument();
        });

        it('does not reopen a URL target after the user closes it and the history refreshes', async () => {
            const {eventChannel} = await renderHistory([node(1, 'Prüfung')], [], [task(1, 1)], {initialUrl: '/?taskId=1'});
            const summary = screen.getByRole('button', {name: /^1\. Prüfung:/});
            await waitFor(() => expect(summary).toHaveAttribute('aria-expanded', 'true'));
            await userEvent.click(summary);
            act(() => eventChannel.emitEvent('refresh'));
            await waitFor(() => expect(ProcessInstanceTaskApiService.prototype.getHistory).toHaveBeenCalledTimes(2));
            expect(summary).toHaveAttribute('aria-expanded', 'false');
        });

        it('leaves the initial view collapsed and does not offer a manual jump control', async () => {
            await renderHistory([node(1, 'Prüfung')], [], [task(1, 1, ProcessTaskStatus.InProgress), task(2, 1, ProcessTaskStatus.Failed)]);
            expect(screen.getByRole('button', {name: /^1\. Prüfung:/})).toHaveAttribute('aria-expanded', 'false');
            expect(screen.getByRole('button', {name: /^2\. Prüfung:/})).toHaveAttribute('aria-expanded', 'false');
            expect(screen.queryByRole('button', {name: /Zu Aufgabe springen/})).not.toBeInTheDocument();
        });
    });

    describe('task view links', () => {
        it.each([ProcessTaskStatus.Running, ProcessTaskStatus.InProgress, ProcessTaskStatus.AwaitingStaff])(
            'offers the internal task view for %s', async status => {
                await renderHistory([node(1, 'Prüfung')], [], [task(42, 1, status)]);
                await userEvent.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
                expect(screen.getByRole('link', {name: 'Aufgabe aufrufen'})).toHaveAttribute('href', '/tasks/17/42');
                expect(screen.queryByRole('link', {name: 'Aufgabe für Beteiligte aufrufen'})).not.toBeInTheDocument();
            },
        );

        it.each([ProcessTaskStatus.AwaitingCustomer, ProcessTaskStatus.AwaitingPayment])(
            'offers the participant view for %s', async status => {
                await renderHistory([node(1, 'Rückmeldung')], [], [{...task(42, 1, status), accessKey: 'task-access'}], {instance: {accessKey: 'instance-access'}});
                await userEvent.click(screen.getByRole('button', {name: /^1\. Rückmeldung:/}));
                const link = screen.getByRole('link', {name: 'Aufgabe für Beteiligte aufrufen'});
                expect(link).toHaveAttribute('href', '/process/instance-access/tasks/task-access');
                expect(link).toHaveAttribute('target', '_blank');
                expect(link).toHaveAttribute('rel', 'noopener noreferrer');
                expect(screen.queryByRole('link', {name: 'Aufgabe aufrufen'})).not.toBeInTheDocument();
            },
        );

        it.each([ProcessTaskStatus.Completed, ProcessTaskStatus.Failed, ProcessTaskStatus.Aborted, ProcessTaskStatus.Paused, ProcessTaskStatus.DeadlineExceeded])(
            'does not offer a task view for %s', async status => {
                await renderHistory([node(1, 'Prüfung')], [], [task(42, 1, status)]);
                await userEvent.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
                expect(screen.queryByRole('link', {name: /Aufgabe.*aufrufen/})).not.toBeInTheDocument();
            },
        );

        it('does not construct a participant link from missing access keys', async () => {
            await renderHistory([node(1, 'Rückmeldung')], [], [{...task(42, 1, ProcessTaskStatus.AwaitingCustomer), accessKey: ''}]);
            await userEvent.click(screen.getByRole('button', {name: /^1\. Rückmeldung:/}));
            expect(screen.queryByRole('link', {name: 'Aufgabe für Beteiligte aufrufen'})).not.toBeInTheDocument();
        });
    });

    describe('instance events and manual interventions', () => {
        it('keeps the chronology limited to tasks without changing their numbering', async () => {
            historyEvents.mockResolvedValue(page([{
                ...historyEvent(), id: 7, processInstanceTaskId: null,
                title: 'Vorgangsereignis', message: 'Ein Ereignis zum gesamten Vorgang.', timestamp: '2026-10-01T10:05:00Z',
            }]));
            await renderHistory([node(1, 'Start'), node(2, 'Prüfung')], [], [
                {...task(1, 1, ProcessTaskStatus.Completed), started: '2026-10-01T10:00:00Z'},
                {...task(2, 2), started: '2026-10-01T10:10:00Z'},
            ]);
            const first = screen.getByRole('button', {name: /^1\. Start:/});
            const second = screen.getByRole('button', {name: /^2\. Prüfung:/});
            expect(first.compareDocumentPosition(second) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
            expect(screen.queryByText('Vorgangsereignis')).not.toBeInTheDocument();
            expect(screen.queryByRole('article')).not.toBeInTheDocument();
            expect(screen.getByRole('region', {name: 'Manuelle Eingriffe'})).toHaveTextContent('Keine manuellen Eingriffe protokolliert.');
        });

        it('shows task interventions among task events and opens details directly from the overview', async () => {
            historyEvents.mockResolvedValue(page([
                {...historyEvent(), id: 6, processInstanceTaskId: 100,
                    title: 'Aufgabe begonnen', timestamp: '2026-10-01T10:00:00Z'},
                {...historyEvent(), id: 7, processInstanceTaskId: 100,
                    title: 'Zuweisung der Aufgabe geändert', message: 'Die Aufgabe wurde neu zugewiesen.',
                    triggeringUserId: 'actor', triggeringUserName: 'Ada Beispiel', details: {manualIntervention: true},
                    processNodeName: 'Prüfung', timestamp: '2026-10-01T10:05:00Z'},
            ]));
            await renderHistory([node(1, 'Prüfung')], [], [task(100, 1)]);
            expect(screen.queryByRole('article')).not.toBeInTheDocument();
            await userEvent.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
            const taskDetails = within(screen.getByRole('region', {name: /^1\. Prüfung:/}));
            const taskEvents = within(taskDetails.getByRole('list', {name: 'Ereignisse und Zwischenergebnisse'})).getAllByRole('listitem');
            expect(taskEvents).toHaveLength(2);
            expect(taskEvents[0]).toHaveTextContent('Aufgabe begonnen');
            expect(taskEvents[1]).toHaveTextContent('Zuweisung der Aufgabe geändert');
            expect(taskEvents[1]).toHaveTextContent('Manueller Eingriff');
            expect(taskEvents[1]).toHaveTextContent('Die Aufgabe wurde neu zugewiesen.');
            expect(taskEvents[1]).toHaveTextContent('01.10.2026');

            const overview = within(screen.getByRole('region', {name: 'Manuelle Eingriffe'}));
            expect(overview.getByText('Zuweisung der Aufgabe geändert')).toBeVisible();
            expect(overview.getByText(/Eingriffe in eine Aufgabe erscheinen zusätzlich bei deren Ereignissen\./)).toBeVisible();
            expect(overview.getByText(/Ada Beispiel/)).toBeVisible();
            expect(overview.queryByRole('button', {name: 'Im Verlauf anzeigen'})).not.toBeInTheDocument();
            await userEvent.click(overview.getByRole('button', {name: 'Details anzeigen'}));
            const dialog = screen.getByRole('dialog', {name: 'Zuweisung der Aufgabe geändert'});
            expect(dialog).toHaveTextContent('Die Aufgabe wurde neu zugewiesen.');
            expect(dialog).toHaveTextContent('Ada Beispiel');
            expect(dialog).toHaveTextContent('Aufgabe: Prüfung');
            expect(dialog).toHaveTextContent('01.10.2026');
            expect(dialog).not.toHaveTextContent('manualIntervention');
            expect(overview.queryByText('Die Aufgabe wurde neu zugewiesen.')).not.toBeInTheDocument();
        });

        it.each([null, 99])('keeps interventions with task reference %s accessible when no task is visible', async taskId => {
            historyEvents.mockResolvedValue(page([{
                ...historyEvent(), id: 7, processInstanceTaskId: taskId,
                title: taskId == null ? 'Zuweisung des Vorgangs geändert' : 'Erneute Ausführung der Aufgabe angefordert',
                message: 'Die Aktion wurde manuell angefordert.', triggeringUserId: 'actor',
                triggeringUserName: 'Ada Beispiel', details: {manualIntervention: true},
            }]));
            await renderHistory([node(1, 'Prüfung')], [], [task(99, 1, ProcessTaskStatus.Restarted)]);
            expect(screen.getByText('Derzeit sind keine Aufgaben im Verlauf sichtbar.')).toBeVisible();
            expect(screen.queryByRole('article')).not.toBeInTheDocument();
            const overview = within(screen.getByRole('region', {name: 'Manuelle Eingriffe'}));
            await userEvent.click(overview.getByRole('button', {name: 'Details anzeigen'}));
            expect(screen.getByRole('dialog')).toHaveTextContent('Die Aktion wurde manuell angefordert.');
        });

        it('keeps normal task processing separate from manual interventions', async () => {
            historyEvents.mockResolvedValue(page([
                {...historyEvent(), id: 1, processInstanceTaskId: 1, triggeringUserId: 'actor', title: 'Aufgabe bearbeitet'},
            ]));
            await renderHistory([node(1, 'Prüfung')], [], [task(1, 1)]);
            expect(screen.getByRole('region', {name: 'Manuelle Eingriffe'})).toHaveTextContent('Keine manuellen Eingriffe protokolliert.');
            await userEvent.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
            const details = within(screen.getByRole('region', {name: /^1\. Prüfung:/}));
            expect(details.getByText('Aufgabe bearbeitet')).toBeVisible();
            expect(details.queryByText('Manueller Eingriff')).not.toBeInTheDocument();
        });

        it.each([false, true])('shows instance errors outside the task chronology (tasks present: %s)', async hasTask => {
            historyEvents.mockResolvedValue(page([
                {...historyEvent(), id: 2, processInstanceTaskId: null, level: ProcessNodeExecutionLogLevel.Error,
                    historyRelevant: false, title: 'Start fehlgeschlagen', message: 'Das Prozesselement ist nicht verfügbar.', timestamp: '2026-10-01T10:05:00Z'},
            ]));
            await renderHistory([node(1, 'Prüfung')], [], hasTask ? [task(1, 1)] : []);
            const errors = within(screen.getByRole('region', {name: 'Fehler auf Vorgangsebene'}));
            const alert = errors.getByRole('alert');
            expect(alert).toHaveTextContent('Start fehlgeschlagen');
            expect(alert).toHaveTextContent('Das Prozesselement ist nicht verfügbar.');
            expect(alert).toHaveTextContent('01.10.2026');
            expect(screen.queryByRole('article')).not.toBeInTheDocument();
            if (!hasTask) expect(screen.getByText('Derzeit sind keine Aufgaben im Verlauf sichtbar.')).toBeVisible();
            await userEvent.click(errors.getByRole('button', {name: 'Details anzeigen'}));
            expect(screen.getByRole('dialog', {name: 'Start fehlgeschlagen'})).toHaveTextContent('Das Prozesselement ist nicht verfügbar.');
        });

        it('does not label task errors or manual interventions as instance errors', async () => {
            historyEvents.mockResolvedValue(page([
                {...historyEvent(), id: 1, processInstanceTaskId: 1,
                    level: ProcessNodeExecutionLogLevel.Error, title: 'Aufgabenfehler'},
                {...historyEvent(), id: 2, processInstanceTaskId: null,
                    level: ProcessNodeExecutionLogLevel.Error, title: 'Manuell ausgelöste Aktion', details: {manualIntervention: true}},
            ]));
            await renderHistory([node(1, 'Prüfung')], [], [task(1, 1)]);
            expect(screen.queryByRole('region', {name: 'Fehler auf Vorgangsebene'})).not.toBeInTheDocument();
            expect(screen.getByRole('region', {name: 'Manuelle Eingriffe'})).toHaveTextContent('Manuell ausgelöste Aktion');
            await userEvent.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
            expect(screen.getByRole('region', {name: /^1\. Prüfung:/})).toHaveTextContent('Aufgabenfehler');
        });
    });

    describe('failed tasks', () => {
        it('shows the latest error even when it is not history-relevant', async () => {
            const failure = {
                ...historyEvent(), id: 91, processInstanceTaskId: 1,
                level: ProcessNodeExecutionLogLevel.Error, historyRelevant: false,
                title: 'Versand fehlgeschlagen', message: 'Der Nachrichtendienst ist nicht erreichbar.',
            };
            vi.mocked(ProcessInstanceEventApiService.prototype.list).mockResolvedValueOnce(page([failure]));
            await renderHistory([node(1, 'Versand')], [], [task(1, 1, ProcessTaskStatus.Failed)]);
            await userEvent.click(screen.getByRole('button', {name: /^1\. Versand:/}));
            const details = within(screen.getByRole('region', {name: /^1\. Versand:/}));
            expect(details.getByRole('alert')).not.toHaveTextContent('Aufgabe fehlgeschlagen');
            expect(details.getByRole('alert')).toHaveTextContent(failure.title);
            expect(details.getAllByText(failure.title)).toHaveLength(1);
            expect(details.getByRole('alert')).toHaveTextContent(failure.message);
            expect(ProcessInstanceEventApiService.prototype.list).toHaveBeenCalledWith(0, 1, ['timestamp', 'id'], 'DESC', {
                processInstanceId: 17, processInstanceTaskId: 1, level: ProcessNodeExecutionLogLevel.Error,
            });
            expect(details.queryByText(/keine Zusammenfassung/)).not.toBeInTheDocument();
            expect(details.getByRole('button', {name: 'Ereignisprotokoll öffnen'})).toBeEnabled();
        });

        it.each(['missing', 'load-failed'] as const)('explains %s errors without hiding the task', async scenario => {
            if (scenario === 'load-failed') vi.mocked(ProcessInstanceEventApiService.prototype.list).mockRejectedValueOnce(new Error('Offline'));
            await renderHistory([node(1, 'Prüfung')], [], [task(1, 1, ProcessTaskStatus.Failed)]);
            await userEvent.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
            const details = within(screen.getByRole('region', {name: /^1\. Prüfung:/}));
            expect(details.getByRole('alert')).toHaveTextContent(scenario === 'missing'
                ? 'Für diese Aufgabe ist keine Fehlermeldung gespeichert.' : 'Die Fehlermeldung konnte nicht geladen werden.');
            expect(details.getByRole('button', {name: 'Ereignisprotokoll öffnen'})).toBeEnabled();
        });

        it.each(['instance', 'system'] as const)('restarts with a %s grant and refreshes the history', async scope => {
            const user = userEvent.setup();
            (scope === 'instance' ? mocks.permissions : mocks.systemPermissions).add(Permission.PROCESS_INSTANCE_RESTART_FAILED);
            const rerun = vi.spyOn(ProcessInstanceTaskApiService.prototype, 'rerunFailedTask').mockResolvedValue(task(42, 1, ProcessTaskStatus.Restarted));
            await renderHistory([node(1, 'Prüfung')], [], [task(42, 1, ProcessTaskStatus.Failed)]);
            await user.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
            vi.mocked(ProcessInstanceTaskApiService.prototype.getHistory).mockResolvedValueOnce(history([
                task(42, 1, ProcessTaskStatus.Restarted), task(43, 1, ProcessTaskStatus.Running),
            ]));
            await user.click(screen.getByRole('button', {name: 'Aufgabe erneut ausführen'}));
            expect(rerun).toHaveBeenCalledWith(42);
            await waitFor(() => expect(ProcessInstanceTaskApiService.prototype.getHistory).toHaveBeenCalledTimes(2));
            await waitFor(() => expect(screen.queryByRole('button', {name: 'Aufgabe erneut ausführen'})).not.toBeInTheDocument());
            expect(mocks.dispatch).toHaveBeenCalledWith(expect.objectContaining({payload: expect.objectContaining({severity: 'success', message: 'Die erneute Ausführung der Aufgabe wurde angefordert.'})}));
        });

        it('shows the empty state between restarting the first task and receiving its new attempt', async () => {
            mocks.permissions.add(Permission.PROCESS_INSTANCE_RESTART_FAILED);
            vi.spyOn(ProcessInstanceTaskApiService.prototype, 'rerunFailedTask').mockResolvedValue(task(42, 1, ProcessTaskStatus.Restarted));
            const {eventChannel} = await renderHistory([node(1, 'Prüfung')], [], [task(42, 1, ProcessTaskStatus.Failed)]);
            await userEvent.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
            vi.mocked(ProcessInstanceTaskApiService.prototype.getHistory).mockResolvedValueOnce(history([
                task(42, 1, ProcessTaskStatus.Restarted),
            ]));
            await userEvent.click(screen.getByRole('button', {name: 'Aufgabe erneut ausführen'}));
            expect(await screen.findByText('Derzeit sind keine Aufgaben im Verlauf sichtbar.')).toBeVisible();
            expect(screen.queryByRole('button', {name: 'Alle ausklappen'})).not.toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Ereignisprotokoll öffnen'})).toBeEnabled();

            vi.mocked(ProcessInstanceTaskApiService.prototype.getHistory).mockResolvedValueOnce(history([
                task(42, 1, ProcessTaskStatus.Restarted), task(43, 1, ProcessTaskStatus.Running),
            ]));
            act(() => eventChannel.emitEvent('refresh'));
            expect(await screen.findByRole('button', {name: /^1\. Prüfung:/})).toBeVisible();
            expect(screen.queryByText('Derzeit sind keine Aufgaben im Verlauf sichtbar.')).not.toBeInTheDocument();
        });

        it('prevents duplicate restart requests and reports a failed request without refreshing', async () => {
            mocks.permissions.add(Permission.PROCESS_INSTANCE_RESTART_FAILED);
            const pending = deferred<ProcessInstanceTaskEntity>();
            const rerun = vi.spyOn(ProcessInstanceTaskApiService.prototype, 'rerunFailedTask').mockReturnValue(pending.promise);
            await renderHistory([node(1, 'Prüfung')], [], [task(1, 1, ProcessTaskStatus.Failed)]);
            await userEvent.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
            const restart = screen.getByRole('button', {name: 'Aufgabe erneut ausführen'});
            await userEvent.click(restart);
            expect(restart).toBeDisabled();
            expect(rerun).toHaveBeenCalledTimes(1);
            await act(async () => pending.reject(new Error('Offline')));
            expect(restart).toBeEnabled();
            expect(ProcessInstanceTaskApiService.prototype.getHistory).toHaveBeenCalledTimes(1);
            expect(mocks.dispatch).toHaveBeenCalledWith(expect.objectContaining({payload: expect.objectContaining({severity: 'error', message: 'Die erneute Ausführung der Aufgabe konnte nicht angefordert werden.'})}));
        });

        it.each(['missing', 'other-instance'] as const)('hides restart with a %s grant', async scope => {
            if (scope === 'other-instance') {
                mocks.permissions.add(Permission.PROCESS_INSTANCE_RESTART_FAILED);
                mocks.instanceGrantId = 99;
                mocks.systemPermissions.add(Permission.PROCESS_INSTANCE_READ);
            }
            await renderHistory([node(1, 'Prüfung')], [], [task(1, 1, ProcessTaskStatus.Failed)]);
            await userEvent.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
            expect(screen.queryByRole('button', {name: 'Aufgabe erneut ausführen'})).not.toBeInTheDocument();
        });

        it('does not offer a restart for completed tasks', async () => {
            mocks.permissions.add(Permission.PROCESS_INSTANCE_RESTART_FAILED);
            await renderHistory([node(1, 'Prüfung')], [], [task(1, 1, ProcessTaskStatus.Completed)]);
            await userEvent.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
            expect(screen.queryByRole('button', {name: 'Aufgabe erneut ausführen'})).not.toBeInTheDocument();
            expect(ProcessInstanceEventApiService.prototype.list).not.toHaveBeenCalled();
        });
    });

    describe('history actions', () => {
        it('opens the complete event log and then the log for the selected task', async () => {
            const user = userEvent.setup();
            const getEventLog = vi.spyOn(ProcessInstanceEventApiService.prototype, 'getEventLog').mockResolvedValue({
                instance: {id: 17, caseNumber: 'V-17', started: '2026-10-01T10:00:00Z', finished: null, runtime: null},
                task: null,
                events: page([]),
            });
            await renderHistory([node(1, 'Prüfung')], [], [task(42, 1)]);
            await user.click(screen.getByRole('button', {name: 'Ereignisprotokoll öffnen'}));
            await waitFor(() => expect(getEventLog).toHaveBeenCalledWith(expect.objectContaining({
                processInstanceId: 17, processInstanceTaskId: undefined, historyRelevant: undefined,
                includeRestartHistory: false,
            })));
            await user.click(within(screen.getByRole('dialog')).getByRole('button', {name: 'Schließen'}));
            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
            await user.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
            await user.click(within(screen.getByRole('region', {name: /^1\. Prüfung:/}))
                .getByRole('button', {name: 'Ereignisprotokoll öffnen'}));
            await waitFor(() => expect(getEventLog).toHaveBeenLastCalledWith(expect.objectContaining({
                processInstanceId: 17, processInstanceTaskId: 42, historyRelevant: undefined,
                includeRestartHistory: true,
            })));
        });

        it('compares with the linked predecessor even when it is a hidden restarted attempt', async () => {
            const user = userEvent.setup();
            await renderHistory([node(1, 'Prüfung')], [], [
                {...task(9, 1, ProcessTaskStatus.Restarted), processData: {state: 'before', removed: 1}},
                {...task(2, 1, ProcessTaskStatus.Completed), processData: {state: 'unrelated'}},
                {...task(3, 1, ProcessTaskStatus.Completed), previousProcessInstanceTaskId: 9,
                    processData: {state: 'after', added: null}, processDataDiff: {state: 'wrong-diff'},
                    finished: '2026-10-01T11:00:00Z'},
            ]);
            await user.click(screen.getByRole('button', {name: /^2\. Prüfung:/}));
            await user.click(within(screen.getByRole('region', {name: /^2\. Prüfung:/}))
                .getByRole('button', {name: 'Datenänderungen anzeigen'}));
            const dialog = screen.getByRole('dialog', {name: 'Änderungen an den Vorgangsdaten'});
            expect(dialog).toHaveTextContent('2. Prüfung');
            expect(dialog).not.toHaveTextContent('Aufgabe #3');
            expect([...dialog.querySelectorAll('[data-diff-kind="removed"]')].map(line => line.textContent).join(' ')).toContain('before');
            expect([...dialog.querySelectorAll('[data-diff-kind="added"]')].map(line => line.textContent).join(' ')).toContain('after');
            expect(dialog).toHaveTextContent('removed');
            expect(dialog).toHaveTextContent('added');
            expect(dialog).not.toHaveTextContent('unrelated');
            expect(dialog).not.toHaveTextContent('wrong-diff');
            expect(dialog).not.toHaveTextContent('Zwischenstand');
            await user.click(within(dialog).getAllByRole('button', {name: 'Schließen'})[1]);
            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
            expect(screen.getByRole('button', {name: /^2\. Prüfung:/})).toHaveAttribute('aria-expanded', 'true');
        });

        it('shows the stored data without a fabricated diff if the linked predecessor is unavailable', async () => {
            await renderHistory([node(1, 'Prüfung')], [], [{
                ...task(1, 1), previousProcessInstanceTaskId: 404,
                processData: {state: 'current'}, processDataDiff: {state: 'ambiguous'},
            }]);
            await userEvent.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
            await userEvent.click(screen.getByRole('button', {name: 'Datenänderungen anzeigen'}));
            const dialog = screen.getByRole('dialog');
            expect(dialog).toHaveTextContent('Der vorherige Datenstand ist nicht verfügbar.');
            expect(dialog).toHaveTextContent('current');
            expect(dialog).toHaveTextContent('Zwischenstand');
            expect(dialog).not.toHaveTextContent('ambiguous');
            expect(dialog.querySelector('[data-diff-kind="added"]')).toBeNull();
        });

        it('compares the initial task with empty data', async () => {
            await renderHistory([node(1, 'Start')], [], [{...task(1, 1), processData: {initial: 'value'}}]);
            await userEvent.click(screen.getByRole('button', {name: /^1\. Start:/}));
            await userEvent.click(screen.getByRole('button', {name: 'Datenänderungen anzeigen'}));
            const dialog = screen.getByRole('dialog');
            expect([...dialog.querySelectorAll('[data-diff-kind="added"]')].map(line => line.textContent).join(' ')).toContain('initial');
            expect(dialog).not.toHaveTextContent('nicht verfügbar');
        });

        it('shows only the selected task’s element data and removes the dialog when access is revoked', async () => {
            const {rerenderInstance} = await renderHistory([node(1, 'Prüfung')], [], [{
                ...task(1, 1), nodeData: {result: 'element-result'},
                processData: {other: 'process-only'}, runtimeData: {internal: 'runtime-only'},
            }]);
            await userEvent.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
            await userEvent.click(screen.getByRole('button', {name: 'Elementdaten anzeigen'}));
            const dialog = screen.getByRole('dialog', {name: 'Elementdaten'});
            expect(dialog).toHaveTextContent('element-result');
            expect(dialog).not.toHaveTextContent('process-only');
            expect(dialog).not.toHaveTextContent('runtime-only');
            mocks.permissions.delete(Permission.PROCESS_INSTANCE_READ);
            rerenderInstance({});
            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });

        it('explains unchanged process data instead of showing an unexplained unmarked diff', async () => {
            await renderHistory([node(1, 'Prüfung')], [], [
                {...task(1, 1, ProcessTaskStatus.Completed), processData: {b: 2, a: 1}},
                {...task(2, 1, ProcessTaskStatus.Completed), previousProcessInstanceTaskId: 1, processData: {a: 1, b: 2}},
            ]);
            await userEvent.click(screen.getByRole('button', {name: /^2\. Prüfung:/}));
            await userEvent.click(screen.getByRole('button', {name: 'Datenänderungen anzeigen'}));
            expect(screen.getByRole('dialog')).toHaveTextContent('Diese Aufgabe hat die Vorgangsdaten nicht verändert.');
            expect(screen.getByRole('dialog').querySelector('[data-diff-kind="added"]')).toBeNull();
        });

        it('explains empty element data', async () => {
            await renderHistory([node(1, 'Prüfung')], [], [task(1, 1)]);
            await userEvent.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
            await userEvent.click(screen.getByRole('button', {name: 'Elementdaten anzeigen'}));
            expect(screen.getByRole('dialog')).toHaveTextContent('Für diese Aufgabe sind keine Elementdaten gespeichert.');
        });

        it('links to the instance’s initial process version', async () => {
            await renderHistory([], [], [], {instance: {initialProcessVersion: 7}});
            expect(screen.getByRole('link', {name: 'Im Prozessmodell ansehen'})).toHaveAttribute(
                'href', '/processes/10/versions/7/?instanceId=17',
            );
            expect(screen.getByRole('button', {name: 'Ereignisprotokoll öffnen'})).toBeEnabled();
        });

        it.each(['process', 'instance'] as const)('rejects a grant for a different %s', async scope => {
            if (scope === 'process') mocks.processGrantId = 99;
            else mocks.instanceGrantId = 99;
            await renderHistory([], [], [], {waitForLoad: false});
            expect(screen.getByRole('alert')).toHaveTextContent(/keine Berechtigung/);
            expect(screen.queryByRole('link', {name: 'Im Prozessmodell ansehen'})).not.toBeInTheDocument();
            expect(ProcessInstanceTaskApiService.prototype.getHistory).not.toHaveBeenCalled();
        });

        it('allows system permissions even with unrelated resource grants', async () => {
            mocks.processGrantId = 99;
            mocks.instanceGrantId = 99;
            mocks.systemPermissions = new Set([Permission.PROCESS_INSTANCE_READ, Permission.PROCESS_DEFINITION_READ]);
            await renderHistory([], [], []);
            expect(screen.getByRole('link', {name: 'Im Prozessmodell ansehen'})).toBeInTheDocument();
        });

        it('indicates chronological order for task events', async () => {
            historyEvents.mockResolvedValue(page([
                {...historyEvent(), id: 1, processInstanceTaskId: 1, title: 'Erstes Ereignis'},
                {...historyEvent(), id: 2, processInstanceTaskId: 1, title: 'Zweites Ereignis'},
            ]));
            await renderHistory([node(1, 'Prüfung')], [], [task(1, 1)]);
            await userEvent.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
            const region = screen.getByRole('region', {name: /^1\. Prüfung:/});
            expect(region).toHaveTextContent(/Älteste zuerst.*Erstes Ereignis.*Zweites Ereignis/);
            expect(ProcessInstanceEventApiService.prototype.listHistory).toHaveBeenCalledWith(
                17, {scope: ProcessHistoryEventScope.Task, taskId: 1, size: 20, sortOrder: 'DESC'},
            );
        });
    });

    it.each([
        {identityData: identities, identityLabel: 'Antragstellende', statusOverride: null},
        {identityData: {}, identityLabel: 'applicant', statusOverride: null},
        {identityData: identities, identityLabel: 'Antragstellende', statusOverride: 'Unterlagen angefordert'},
    ])('names $identityLabel in the waiting explanation and status tooltip ($statusOverride)', async ({identityData, identityLabel, statusOverride}) => {
        const user = userEvent.setup();
        const waitingTask = {
            ...task(1, 1, ProcessTaskStatus.AwaitingCustomer), assignedCustomerIdentityId: 'applicant', statusOverride,
        };
        await renderHistory([node(1, 'Prüfung')], [], [waitingTask], {instance: {identities: identityData}});
        const systemLabel = `Wartet auf ${identityLabel}`;
        const tooltip = statusOverride ? `${statusOverride} (Systemstatus: ${systemLabel})` : systemLabel;
        await user.hover(screen.getByLabelText(tooltip));
        expect(await screen.findByRole('tooltip')).toHaveTextContent(tooltip);
        await user.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
        const details = within(screen.getByRole('region', {name: /^1\. Prüfung:/}));
        expect(details.getByRole('alert')).toHaveTextContent(
            `Dieses Prozesselement wartet auf eine Rückmeldung der Identität „${identityLabel}“.`,
        );
    });

    it('preserves authored execution summaries instead of adding a waiting explanation', async () => {
        const waitingTask = {
            ...task(1, 1, ProcessTaskStatus.AwaitingCustomer), assignedCustomerIdentityId: 'applicant',
            executionSummaryMarkdown: 'Bitte reichen Sie die Unterlagen nach.',
        };
        await renderHistory([node(1, 'Prüfung')], [], [waitingTask], {instance: {identities}});
        await userEvent.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
        expect(screen.getByText('Bitte reichen Sie die Unterlagen nach.')).toBeVisible();
        expect(screen.queryByText(/Dieses Prozesselement wartet/)).not.toBeInTheDocument();
    });

    describe('restarted tasks', () => {
        it.each(['Erneute Ausführung der Aufgabe angefordert', 'Fortsetzung des Vorgangs angefordert'])(
            'shows %s from the replaced attempt with its original attribution', async title => {
                historyEvents.mockResolvedValue(page([
                    {...historyEvent(), id: 7, processInstanceTaskId: 42, title,
                        message: 'Die erneute Ausführung wurde manuell angefordert.', details: {manualIntervention: true}, timestamp: '2026-10-01T10:01:00Z'},
                    {...historyEvent(), id: 8, processInstanceTaskId: 42, title: 'Fehler des vorherigen Versuchs', message: 'Altes Ergebnis', level: ProcessNodeExecutionLogLevel.Error},
                    {...historyEvent(), id: 9, processInstanceTaskId: 43, title: 'Neuer Versuch gestartet', timestamp: '2026-10-01T10:02:00Z'},
                ]));
                await renderHistory([node(1, 'Prüfung')], [], [
                    task(42, 1, ProcessTaskStatus.Restarted),
                    {...task(43, 1), restartForTaskId: 42, started: '2026-10-01T10:02:00Z'},
                ]);
                expect(screen.getAllByRole('button', {name: /^\d+\. Prüfung:/})).toHaveLength(1);
                await userEvent.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
                const details = within(screen.getByRole('region', {name: /^1\. Prüfung:/}));
                const entries = within(details.getByRole('list', {name: 'Ereignisse und Zwischenergebnisse'})).getAllByRole('listitem');
                expect(entries).toHaveLength(2);
                expect(entries[0]).toHaveTextContent(title);
                expect(entries[0]).toHaveTextContent('Manueller Eingriff · vorheriger Ausführungsversuch');
                expect(entries[0].querySelector('time')).toHaveAttribute('datetime', '2026-10-01T10:01:00Z');
                expect(entries[1]).toHaveTextContent('Neuer Versuch gestartet');
                expect(entries[1]).not.toHaveTextContent('vorheriger Ausführungsversuch');
                expect(details.queryByText('Altes Ergebnis')).not.toBeInTheDocument();
                expect(screen.getByRole('region', {name: 'Manuelle Eingriffe'})).toHaveTextContent(title);
            },
        );

        it('omits restarted attempts and their details while keeping other tasks in consecutive order', async () => {
            const user = userEvent.setup();
            historyEvents.mockResolvedValue(page([{
                ...historyEvent(),
                id: 7,
                processInstanceTaskId: 2,
                title: 'Ereignis des vorherigen Versuchs',
            }]));
            await renderHistory(
                [node(1, 'Abgeschlossen'), node(2, 'Prüfung'), node(3, 'Abgebrochen'), node(4, 'Fehlgeschlagen')],
                [],
                [
                    task(1, 1, ProcessTaskStatus.Completed),
                    {...task(2, 2, ProcessTaskStatus.Restarted), executionSummaryMarkdown: 'Vorheriger Versuch'},
                    {...task(3, 2), executionSummaryMarkdown: 'Aktueller Versuch'},
                    task(4, 3, ProcessTaskStatus.Aborted),
                    task(5, 4, ProcessTaskStatus.Failed),
                ],
            );

            expect(screen.getAllByRole('button', {name: /^\d+\./})).toHaveLength(4);
            expect(screen.getByRole('button', {name: /^1\. Abgeschlossen:/})).toBeVisible();
            expect(screen.getByRole('button', {name: /^2\. Prüfung:/})).toBeVisible();
            expect(screen.getByRole('button', {name: /^3\. Abgebrochen:/})).toBeVisible();
            expect(screen.getByRole('button', {name: /^4\. Fehlgeschlagen:/})).toBeVisible();

            await user.click(screen.getByRole('button', {name: 'Alle ausklappen'}));
            expect(screen.getByText('Aktueller Versuch')).toBeVisible();
            expect(screen.queryByText('Vorheriger Versuch')).not.toBeInTheDocument();
            expect(screen.queryByText('Ereignis des vorherigen Versuchs')).not.toBeInTheDocument();
            expect(screen.getAllByRole('region', {name: /^\d+\./})).toHaveLength(4);
            expect(screen.getByRole('button', {name: 'Alle ausklappen'})).toBeDisabled();

            await user.click(screen.getByRole('button', {name: 'Alle einklappen'}));
            expect(screen.queryByRole('region', {name: /^\d+\./})).not.toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Alle einklappen'})).toBeDisabled();
        });

        it('omits task entries and bulk actions when every task was restarted', async () => {
            await renderHistory([node(1, 'Prüfung')], [], [
                task(1, 1, ProcessTaskStatus.Restarted),
                task(2, 1, ProcessTaskStatus.Restarted),
            ]);

            expect(screen.queryByRole('button', {name: /^\d+\./})).not.toBeInTheDocument();
            expect(screen.getByText('Derzeit sind keine Aufgaben im Verlauf sichtbar.')).toBeVisible();
            expect(screen.queryByRole('button', {name: 'Alle ausklappen'})).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Alle einklappen'})).not.toBeInTheDocument();
        });

        it('loads the remaining history when a restarted task references a missing node', async () => {
            await renderHistory([node(1, 'Prüfung')], [], [
                task(1, 999, ProcessTaskStatus.Restarted),
                task(2, 1),
            ]);

            expect(screen.getByRole('button', {name: /^1\. Prüfung:/})).toBeVisible();
            expect(mocks.dispatch).not.toHaveBeenCalled();
        });

        it('replaces an expanded failed attempt with its new task after refreshing', async () => {
            const user = userEvent.setup();
            const failed = {...task(1, 1, ProcessTaskStatus.Failed), executionSummaryMarkdown: 'Vorheriger Versuch'};
            const {eventChannel} = await renderHistory([node(1, 'Prüfung')], [], [failed]);
            await user.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
            expect(screen.getByText('Vorheriger Versuch')).toBeVisible();

            vi.mocked(ProcessInstanceTaskApiService.prototype.getHistory).mockResolvedValueOnce(history([
                {...failed, status: ProcessTaskStatus.Restarted},
                {...task(2, 1), executionSummaryMarkdown: 'Aktueller Versuch'},
            ]));
            await act(async () => eventChannel.emitEvent('refresh'));

            expect(screen.getAllByRole('button', {name: /^\d+\. Prüfung:/})).toHaveLength(1);
            expect(screen.getByRole('button', {name: /^1\. Prüfung:/})).toHaveAttribute('aria-expanded', 'false');
            expect(screen.queryByText('Vorheriger Versuch')).not.toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Alle einklappen'})).toBeDisabled();
            await user.click(screen.getByRole('button', {name: 'Alle ausklappen'}));
            expect(screen.getByText('Aktueller Versuch')).toBeVisible();
            expect(screen.getByRole('button', {name: 'Alle ausklappen'})).toBeDisabled();
        });
    });

    it.each([
        [ProcessTaskStatus.Running, 'Dieses Prozesselement wird derzeit ausgeführt.'],
        [ProcessTaskStatus.InProgress, 'Dieses Prozesselement wird derzeit bearbeitet.'],
        [ProcessTaskStatus.AwaitingStaff, 'Dieses Prozesselement wartet auf Bearbeitung durch Mitarbeiter:innen.'],
        [ProcessTaskStatus.AwaitingPayment, 'Dieses Prozesselement wartet auf die Zahlungsbestätigung.'],
        [ProcessTaskStatus.AwaitingCustomer, 'Dieses Prozesselement wartet auf eine Rückmeldung der zugewiesenen Person.'],
        [ProcessTaskStatus.Paused, 'Die Ausführung dieses Prozesselements ist pausiert.'],
    ])('explains an unfinished task with status %s alongside its events', async (status, notice) => {
        historyEvents.mockResolvedValue(page([{
            ...historyEvent(),
            id: 7,
            processInstanceTaskId: 1,
            title: 'Aufgabe zugewiesen',
            message: 'Die Aufgabe wurde automatisch zugewiesen.',
        }]));
        const user = userEvent.setup();
        await renderHistory([node(1, 'Prüfung')], [], [task(1, 1, status as ProcessTaskStatus)]);
        await user.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
        const details = within(screen.getByRole('region', {name: /^1\. Prüfung:/}));
        expect(details.getByRole('alert')).toHaveTextContent(notice);
        expect(details.getByText('Aufgabe zugewiesen')).toBeVisible();
        expect(details.getByRole('button', {name: 'Ereignisprotokoll öffnen'})).toBeVisible();
    });

    it('expands and collapses tasks together and individually while upcoming entries stay static', async () => {
        const user = userEvent.setup();
        const {eventChannel} = await renderHistory(
            [node(1, 'Prüfung'), node(2, 'Versand')], [edge(1, 2)],
            [task(1, 1, ProcessTaskStatus.Completed), task(2, 1)],
        );
        const first = screen.getByRole('button', {name: /^1\. Prüfung:/});
        const second = screen.getByRole('button', {name: /^2\. Prüfung:/});
        expect(screen.getByRole('button', {name: 'Alle einklappen'})).toBeDisabled();
        await user.click(screen.getByRole('button', {name: 'Alle ausklappen'}));
        expect(first).toHaveAttribute('aria-expanded', 'true');
        expect(second).toHaveAttribute('aria-expanded', 'true');
        expect(screen.getByRole('button', {name: 'Alle ausklappen'})).toBeDisabled();
        expect(within(screen.getByRole('listitem')).queryByRole('button')).not.toBeInTheDocument();

        await user.click(first);
        expect(first).toHaveAttribute('aria-expanded', 'false');
        expect(second).toHaveAttribute('aria-expanded', 'true');
        vi.mocked(ProcessInstanceTaskApiService.prototype.getHistory).mockResolvedValueOnce(history([
            task(1, 1, ProcessTaskStatus.Completed), task(2, 1), task(3, 2),
        ]));
        await act(async () => eventChannel.emitEvent('refresh'));
        expect(first).toHaveAttribute('aria-expanded', 'false');
        expect(second).toHaveAttribute('aria-expanded', 'true');
        expect(screen.getByRole('button', {name: /^3\. Versand:/})).toHaveAttribute('aria-expanded', 'false');
        await user.click(screen.getByRole('button', {name: 'Alle einklappen'}));
        expect(second).toHaveAttribute('aria-expanded', 'false');
        expect(screen.getByRole('button', {name: 'Alle einklappen'})).toBeDisabled();
    });

    it('starts collapsed when changing to another instance even when task IDs match', async () => {
        const user = userEvent.setup();
        const {rerenderInstance} = await renderHistory([node(1, 'Prüfung')], [], [task(1, 1)]);
        await user.click(screen.getByRole('button', {name: 'Alle ausklappen'}));
        mocks.instanceGrantId = 18;
        rerenderInstance({id: 18});
        expect(await screen.findByRole('button', {name: /^1\. Prüfung:/})).toHaveAttribute('aria-expanded', 'false');
        expect(ProcessInstanceTaskApiService.prototype.getHistory).toHaveBeenLastCalledWith(18, 0, 20, undefined);
    });

    it.each([ProcessInstanceStatus.Completed, ProcessInstanceStatus.Aborted])('hides the entire preview for a %s instance', async (status) => {
        const {rerenderInstance} = await renderHistory([node(1, 'Prüfung'), node(2, 'Versand')], [edge(1, 2)], [task(1, 1)]);
        rerenderInstance({status});
        expect(screen.getByRole('heading', {name: 'Verlauf des Vorgangs'})).toBeVisible();
        expect(screen.queryByRole('heading', {name: 'Voraussichtliche nächste Schritte'})).not.toBeInTheDocument();
        expect(screen.queryByRole('list')).not.toBeInTheDocument();
        expect(screen.queryByText(/Die voraussichtlichen nächsten Prozesselemente/)).not.toBeInTheDocument();
    });

    it('keeps the preview for a failed instance and omits bulk actions when there are no tasks', async () => {
        await renderHistory([], [], [], {instance: {status: ProcessInstanceStatus.Failed}});
        expect(screen.getByText('Derzeit sind keine Aufgaben im Verlauf sichtbar.')).toBeVisible();
        expect(screen.getByRole('button', {name: 'Ereignisprotokoll öffnen'})).toBeEnabled();
        expect(screen.getByRole('heading', {name: 'Voraussichtliche nächste Schritte'})).toBeVisible();
        expect(screen.getByText('Vorschau auf mögliche nächste Schritte in diesem Vorgang. Der weitere Ablauf kann sich durch Verzweigungen ändern.')).toBeVisible();
        expect(screen.queryByRole('button', {name: 'Alle ausklappen'})).not.toBeInTheDocument();
        const emptyState = screen.getByText('Keine nächsten Schritte ableitbar.');
        expect(emptyState).toBeVisible();
        expect(emptyState.parentElement).toHaveStyle({borderStyle: 'dashed'});
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('explains the open choice of an active action with multiple ports', async () => {
        await renderHistory([node(1, 'Freigabe', 'approval', ['next', 'rejected']), node(2, 'Versand')], [edge(1, 2)], [task(1, 1)]);
        expect(screen.getByText('Nächste Schritte hängen vom weiteren Prozesspfad ab.')).toBeVisible();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        expect(screen.queryByRole('listitem')).not.toBeInTheDocument();
    });

    it.each([false, true])('uses the resolved ports for a deadline with strictness %s', async strict => {
        const current = {
            ...node(1, 'Prüfung', ProcessNodeType.Action, strict ? ['next', 'phase_overdue'] : ['next']),
            deadlines: {phase: {days: 3, strict}},
        };
        await renderHistory([current, node(2, 'Versand')], [edge(1, 2)], [task(1, 1)]);

        if (strict) {
            expect(screen.queryByRole('listitem')).not.toBeInTheDocument();
            expect(screen.getByText('Nächste Schritte hängen vom weiteren Prozesspfad ab.')).toBeVisible();
        } else {
            expect(screen.getByRole('listitem')).toHaveTextContent(/^Versand:/);
        }
    });

    describe('elapsed time for ongoing tasks', () => {
        const started = '2026-10-01T10:00:00Z';

        it.each([
            ProcessTaskStatus.Running, ProcessTaskStatus.InProgress, ProcessTaskStatus.AwaitingStaff,
            ProcessTaskStatus.Paused, ProcessTaskStatus.AwaitingCustomer, ProcessTaskStatus.AwaitingPayment,
        ])('shows wall-clock time since the start for %s', async status => {
            vi.spyOn(Date, 'now').mockReturnValue(Date.parse('2026-10-01T12:00:25Z'));
            await renderHistory([node(1, 'Prüfung')], [], [{...task(1, 1, status), started}]);
            await userEvent.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
            const metadata = within(screen.getByRole('complementary', {name: 'Aufgabendetails'}));
            expect(metadata.getAllByRole('term').map(term => term.textContent)).toContain('Zeit seit Beginn');
            expect(metadata.getByText('Zwei Stunden')).toBeVisible();
            expect(metadata.getAllByRole('term').map(term => term.textContent)).toContain('Beginn');
        });

        it('refreshes elapsed time locally while an ongoing task is expanded', async () => {
            const now = vi.spyOn(Date, 'now').mockReturnValue(Date.parse('2026-10-01T10:00:15Z'));
            const intervals = vi.spyOn(window, 'setInterval');
            await renderHistory([node(1, 'Prüfung')], [], [{...task(1, 1), started}]);
            await userEvent.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
            const metadata = within(screen.getByRole('complementary', {name: 'Aufgabendetails'}));
            expect(metadata.getByText('Weniger als eine Minute')).toBeVisible();
            now.mockReturnValue(Date.parse('2026-10-01T10:02:15Z'));
            const callback = intervals.mock.calls.find(([, delay]) => delay === 60_000)?.[0];
            expect(callback).toBeTypeOf('function');
            act(() => { if (typeof callback === 'function') callback(); });
            expect(metadata.getByText('Zwei Minuten')).toBeVisible();
            expect(ProcessInstanceEventApiService.prototype.listHistory).toHaveBeenCalledTimes(3);
        });

        it.each([ProcessTaskStatus.Completed, ProcessTaskStatus.Failed, ProcessTaskStatus.Aborted, ProcessTaskStatus.DeadlineExceeded])(
            'does not keep counting for inactive %s tasks without a finish timestamp', async status => {
                vi.spyOn(Date, 'now').mockReturnValue(Date.parse('2026-10-01T12:00:00Z'));
                await renderHistory([node(1, 'Prüfung')], [], [{...task(1, 1, status), started}]);
                await userEvent.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
                expect(within(screen.getByRole('complementary', {name: 'Aufgabendetails'})).queryByText('Zeit seit Beginn', {selector: 'dt'})).not.toBeInTheDocument();
            },
        );

        it.each(['invalid', '2026-10-01T12:00:00Z'])('omits elapsed time for invalid or future start %s', async taskStarted => {
            vi.spyOn(Date, 'now').mockReturnValue(Date.parse(started));
            await renderHistory([node(1, 'Prüfung')], [], [{...task(1, 1), started: taskStarted}]);
            await userEvent.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
            expect(within(screen.getByRole('complementary', {name: 'Aufgabendetails'})).queryByText('Zeit seit Beginn', {selector: 'dt'})).not.toBeInTheDocument();
        });
    });

    it('groups manual interventions in rows with single-line titles and direct detail actions', async () => {
        const title = 'Eine sehr ausführliche Beschreibung des manuellen Eingriffs '.repeat(5);
        historyEvents.mockResolvedValue(page([
            {...historyEvent(), id: 7, title,
                triggeringUserId: 'actor', triggeringUserName: 'Ada Beispiel', details: {manualIntervention: true}},
            {...historyEvent(), id: 8, title: 'Fortsetzung angefordert', details: {manualIntervention: true}},
        ]));
        await renderHistory([], [], []);
        const overview = within(screen.getByRole('region', {name: 'Manuelle Eingriffe'}));
        const list = within(overview.getByRole('list', {name: 'Manuelle Eingriffe'}));
        expect(list.getAllByRole('listitem')).toHaveLength(2);
        const first = within(list.getAllByRole('listitem')[0]);
        const label = first.getByText(title.trim());
        expect(label).toHaveAttribute('title', title);
        expect(label).toHaveStyle({whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis'});
        expect(first.getByText(/Ada Beispiel/)).toBeVisible();
        expect(first.getByRole('button', {name: 'Details anzeigen'})).toBeEnabled();
    });

    it('separates task metadata and places event timestamps alongside the event title', async () => {
        historyEvents.mockResolvedValue(page([{
            ...historyEvent(), id: 1, processInstanceTaskId: 1,
            title: 'Aufgabe zugewiesen', message: 'Die Zuweisung erfolgte automatisch.', timestamp: '2026-10-01T10:00:00Z',
        }]));
        await renderHistory([node(1, 'Prüfung')], [], [{
            ...task(1, 1, ProcessTaskStatus.Completed), started: '2026-10-01T10:00:00Z',
            finished: '2026-10-01T10:00:39Z', runtime: 39000, deadline: '2026-10-02T10:00:00Z',
            executionSummaryMarkdown: 'Die Prüfung ist abgeschlossen.',
        }]);
        await userEvent.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
        const details = within(screen.getByRole('region', {name: /^1\. Prüfung:/}));
        const metadata = within(details.getByRole('complementary', {name: 'Aufgabendetails'}));
        expect(metadata.getAllByRole('term').map(term => term.textContent)).toEqual(['Beginn', 'Ende', 'Dauer', 'Frist']);
        expect(metadata.getByText('39 Sekunden')).toBeVisible();
        expect(metadata.queryByRole('button')).not.toBeInTheDocument();
        const events = within(details.getByRole('list', {name: 'Ereignisse und Zwischenergebnisse'}));
        const entry = events.getByRole('listitem');
        expect(entry).toHaveTextContent('Aufgabe zugewiesen');
        expect(entry.querySelector('time')).toHaveAttribute('datetime', '2026-10-01T10:00:00Z');
        expect(within(entry).getByText('Die Zuweisung erfolgte automatisch.')).toBeVisible();
        expect(details.getByRole('button', {name: 'Ereignisprotokoll öffnen'})).toHaveTextContent('Ereignisprotokoll');
    });

    it('ellipsizes long labels on one line and provides their full text through the native title', async () => {
        const name = 'Ein sehr langer Name des Prozesselements '.repeat(8);
        const description = 'Eine ausführliche Beschreibung '.repeat(8);
        await renderHistory([{...node(1, name), description}], [], [task(1, 1)]);
        const summary = screen.getByRole('button', {name: `1. ${name.trim()}: ${description.trim()}`});
        const label = summary.querySelector('[title]');
        expect(label).toHaveAttribute('title', `1. ${name.trim()}: ${description}`);
        expect(label).toHaveStyle({whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', minWidth: '0'});
        expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
    });

    it('renders the complete AI response with lists and code, and tables as text', async () => {
        const user = userEvent.setup();
        await renderHistory([node(1, 'KI-Anfrage')], [], [{
            ...task(1, 1, ProcessTaskStatus.Completed),
            executionSummaryMarkdown: '**Antwort der KI**\n\n- Erster Punkt\n- Zweiter Punkt\n\n| Feld | Wert |\n| --- | --- |\n| Ergebnis | 42 |\n\n```json\n{"ergebnis": 42}\n```\n\nLetzter Absatz.',
        }]);
        await user.click(screen.getByRole('button', {name: 'Alle ausklappen'}));
        const details = within(screen.getByRole('region', {name: /^1\. KI-Anfrage:/}));
        expect(details.getAllByRole('listitem')).toHaveLength(2);
        expect(details.queryByRole('table')).not.toBeInTheDocument();
        expect(details.getByText(/\| Ergebnis \| 42 \|/)).toBeVisible();
        expect(details.getByText('{"ergebnis": 42}').tagName).toBe('CODE');
        expect(details.getByText('Letzter Absatz.')).toBeVisible();
        expect(details.queryByRole('alert')).not.toBeInTheDocument();
    });

    describe('refresh events', () => {
        it('reloads tasks and keeps the same accordion expanded while loading and after updating', async () => {
            const user = userEvent.setup();
            const original = {...task(1, 1), executionSummaryMarkdown: 'Bisherige Zusammenfassung'};
            const {eventChannel} = await renderHistory([node(1, 'Prüfung')], [], [original]);
            const summary = screen.getByRole('button', {name: /^1\. Prüfung:/});
            await user.click(summary);
            expect(summary).toHaveAttribute('aria-expanded', 'true');
            const region = screen.getByRole('region', {name: /^1\. Prüfung:/});
            expect(within(region).getByText('Bisherige Zusammenfassung')).toBeVisible();
            const refresh = deferred<ProcessInstanceHistoryTasks>();
            vi.mocked(ProcessInstanceTaskApiService.prototype.getHistory).mockReturnValueOnce(refresh.promise);

            act(() => eventChannel.emitEvent('refresh'));
            expect(ProcessInstanceTaskApiService.prototype.getHistory).toHaveBeenCalledTimes(2);
            expect(screen.getByRole('button', {name: /^1\. Prüfung:/})).toBe(summary);
            expect(summary).toHaveAttribute('aria-expanded', 'true');
            expect(within(region).getByText('Bisherige Zusammenfassung')).toBeVisible();
            expect(screen.queryByRole('status', {name: 'Verlauf wird geladen'})).not.toBeInTheDocument();

            await act(async () => refresh.resolve(history([{
                ...original, executionSummaryMarkdown: 'Aktualisierte Zusammenfassung',
            }])));
            expect(screen.getByRole('button', {name: /^1\. Prüfung:/})).toBe(summary);
            expect(summary).toHaveAttribute('aria-expanded', 'true');
            expect(screen.getByRole('region', {name: /^1\. Prüfung:/})).toBe(region);
            expect(within(region).getByText('Aktualisierte Zusammenfassung')).toBeVisible();
            expect(within(region).queryByText('Bisherige Zusammenfassung')).not.toBeInTheDocument();
            expect(ProcessInstanceEventApiService.prototype.listHistory).toHaveBeenCalledTimes(6);
            expect(ProcessDefinitionEdgeApiService.prototype.listAll).toHaveBeenCalledTimes(2);
        });

        it('keeps the displayed history and reports a failed refresh', async () => {
            const user = userEvent.setup();
            const {eventChannel} = await renderHistory([node(1, 'Prüfung')], [], [task(1, 1)]);
            const summary = screen.getByRole('button', {name: /^1\. Prüfung:/});
            await user.click(summary);
            const error = new Error('Refresh failed');
            vi.mocked(ProcessInstanceTaskApiService.prototype.getHistory).mockRejectedValueOnce(error);

            await act(async () => eventChannel.emitEvent('refresh'));
            expect(mocks.dispatch).toHaveBeenCalledWith(expect.objectContaining({
                payload: expect.objectContaining({
                    message: 'Der Verlauf konnte nicht geladen werden.',
                    severity: 'error',
                }),
            }));
            expect(screen.getByRole('button', {name: /^1\. Prüfung:/})).toBe(summary);
            expect(summary).toHaveAttribute('aria-expanded', 'true');
            expect(screen.queryByRole('status', {name: 'Verlauf wird geladen'})).not.toBeInTheDocument();
        });

        it('ignores superseded responses and errors when refreshes overlap', async () => {
            const {eventChannel} = await renderHistory([node(1, 'Prüfung')], [], [task(1, 1)]);
            const old = deferred<ProcessInstanceHistoryTasks>();
            const current = deferred<ProcessInstanceHistoryTasks>();
            vi.mocked(ProcessInstanceTaskApiService.prototype.getHistory)
                .mockReturnValueOnce(old.promise).mockReturnValueOnce(current.promise);
            act(() => eventChannel.emitEvent('refresh'));
            act(() => eventChannel.emitEvent('refresh'));
            await act(async () => current.resolve(history([task(1, 1), task(2, 1)])));
            expect(screen.getByRole('button', {name: /^2\. Prüfung:/})).toBeInTheDocument();

            await act(async () => old.resolve(history([task(1, 1)])));
            expect(screen.getByRole('button', {name: /^2\. Prüfung:/})).toBeInTheDocument();

            const failed = deferred<ProcessInstanceHistoryTasks>();
            vi.mocked(ProcessInstanceTaskApiService.prototype.getHistory).mockReturnValueOnce(failed.promise);
            act(() => eventChannel.emitEvent('refresh'));
            act(() => eventChannel.emitEvent('refresh'));
            await waitFor(() => expect(screen.queryByRole('button', {name: /^2\. Prüfung:/})).not.toBeInTheDocument());
            await act(async () => failed.reject(new Error('Obsolete failure')));
            expect(mocks.dispatch).not.toHaveBeenCalled();
        });

        it.each(['success', 'failure'] as const)('ignores late %s after leaving the history', async outcome => {
            const {eventChannel, unmount} = await renderHistory([node(1, 'Prüfung')], [], [task(1, 1)]);
            const pending = deferred<ProcessInstanceHistoryTasks>();
            vi.mocked(ProcessInstanceTaskApiService.prototype.getHistory).mockReturnValueOnce(pending.promise);
            act(() => eventChannel.emitEvent('refresh'));
            expect(ProcessInstanceTaskApiService.prototype.getHistory).toHaveBeenCalledTimes(2);
            unmount();
            eventChannel.emitEvent('refresh');
            expect(ProcessInstanceTaskApiService.prototype.getHistory).toHaveBeenCalledTimes(2);
            await act(async () => {
                if (outcome === 'success') {
                    pending.resolve(history([task(2, 1)]));
                } else {
                    pending.reject(new Error('Late failure'));
                }
            });
            expect(mocks.dispatch).not.toHaveBeenCalled();
        });

        it('does not fetch history when the read permission is granted for a different process', async () => {
            mocks.processGrantId = 99;
            await renderHistory([node(1, 'Prüfung')], [], [task(1, 1)], {waitForLoad: false});
            expect(screen.getByRole('alert')).toHaveTextContent(/den Prozess zu diesem Vorgang/);
            expect(ProcessDefinitionApiService.prototype.retrieve).not.toHaveBeenCalled();
        });

        it.each([Permission.PROCESS_INSTANCE_READ, Permission.PROCESS_DEFINITION_READ])(
            'does not fetch history on entry or refresh without %s',
            async permission => {
                mocks.permissions.delete(permission);
                const {eventChannel} = await renderHistory([node(1, 'Prüfung')], [], [task(1, 1)], {waitForLoad: false});
                expect(screen.getByRole('alert')).toHaveTextContent(/keine Berechtigung/);
                await act(async () => eventChannel.emitEvent('refresh'));
                expect(ProcessInstanceTaskApiService.prototype.getHistory).not.toHaveBeenCalled();
                expect(ProcessDefinitionApiService.prototype.retrieve).not.toHaveBeenCalled();
                expect(ProcessInstanceEventApiService.prototype.listHistory).not.toHaveBeenCalled();
            },
        );
    });

    describe('deadline chips', () => {
        const started = '2026-10-01T10:00:00Z';
        const deadline = '2026-10-03T10:00:00Z';

        describe.each([
            ProcessTaskStatus.Running,
            ProcessTaskStatus.InProgress,
            ProcessTaskStatus.AwaitingStaff,
            ProcessTaskStatus.Paused,
            ProcessTaskStatus.AwaitingCustomer,
            ProcessTaskStatus.AwaitingPayment,
        ])('active task with status %s', status => {
            it.each([
                {now: '2026-10-02T09:59:59.999Z', exceeded: false},
                {now: '2026-10-02T22:00:00Z', exceeded: false},
                {now: '2026-10-03T09:59:59.999Z', exceeded: false},
                {now: deadline, exceeded: true},
                {now: '2026-10-04T10:00:00Z', exceeded: true},
            ])('shows a deadline or an exceeded badge at $now', async ({now, exceeded}) => {
                vi.spyOn(Date, 'now').mockReturnValue(Date.parse(now));
                await renderHistory([node(1, 'Prüfung')], [], [{...task(1, 1, status), started, deadline}]);
                const summary = screen.getByRole('button', {name: /^1\. Prüfung:/});
                const chip = summary.querySelector('.MuiChip-root');
                expect(chip).toHaveTextContent(exceeded ? 'Frist überschritten' : 'Frist: 03.10.2026 um 12:00 Uhr');
                expect(summary).not.toHaveAccessibleName(/Bald fällig|Abgelaufen/);
            });
        });

        describe.each(['2026-10-04T10:00:00Z', '2027-10-04T10:00:00Z'])('completed tasks viewed at %s', now => {
            it.each([
                {finished: '2026-10-02T12:00:00Z', exceeded: false},
                {finished: deadline, exceeded: false},
                {finished: '2026-10-03T10:00:00.001Z', exceeded: true},
            ])('only marks a missed deadline when finished at $finished', async ({finished, exceeded}) => {
                vi.spyOn(Date, 'now').mockReturnValue(Date.parse(now));
                await renderHistory([node(1, 'Prüfung')], [], [{
                    ...task(1, 1, ProcessTaskStatus.Completed), started, deadline, finished,
                }]);
                const summary = screen.getByRole('button', {name: /^1\. Prüfung:/});
                const chip = summary.querySelector('.MuiChip-root');
                if (exceeded) expect(chip).toHaveTextContent(/^Frist überschritten$/);
                else expect(chip).toBeNull();
                expect(summary).not.toHaveAccessibleName(/Frist eingehalten|Abgelaufen/);
            });
        });

        it.each([
            ProcessTaskStatus.Completed,
            ProcessTaskStatus.Aborted,
            ProcessTaskStatus.Failed,
        ])('does not infer an exceeded deadline for an inactive %s task without a completion time', async status => {
            vi.spyOn(Date, 'now').mockReturnValue(Date.parse('2026-10-04T10:00:00Z'));
            await renderHistory([node(1, 'Prüfung')], [], [{...task(1, 1, status), started, deadline}]);
            const summary = screen.getByRole('button', {name: /^1\. Prüfung:/});
            expect(summary.querySelector('.MuiChip-root')).toHaveTextContent('Frist: 03.10.2026 um 12:00 Uhr');
            expect(summary).not.toHaveAccessibleName(/Frist überschritten|Frist eingehalten/);
        });

        it('marks automatic termination even if the deadline timestamp is unavailable', async () => {
            await renderHistory([node(1, 'Prüfung')], [], [task(1, 1, ProcessTaskStatus.DeadlineExceeded)]);
            const summary = screen.getByRole('button', {name: /^1\. Prüfung:/});
            expect(summary.querySelector('.MuiChip-root')).toHaveTextContent('Frist überschritten');
        });

        it('omits deadline chips for tasks without a deadline and upcoming steps', async () => {
            await renderHistory([node(1, 'Prüfung'), node(2, 'Nächster Schritt')], [edge(1, 2)], [task(1, 1)]);
            for (const summary of [screen.getByRole('button', {name: /^1\. Prüfung:/}), screen.getByRole('listitem')]) {
                expect(summary.querySelector('.MuiChip-root')).toBeNull();
                expect(summary).not.toHaveAccessibleName(/Frist|Abgelaufen/);
            }
        });
    });

    it('shows the stored Markdown for its task before the existing events', async () => {
        const user = userEvent.setup();
        const completedTask = {
            ...task(1, 1, ProcessTaskStatus.Completed),
            executionSummaryMarkdown: '**Zählerstand:** 17\n\n- Erhöht\n- Gespeichert\n\n[Details](https://example.org/details)',
        };
        historyEvents.mockResolvedValue(page([{
            ...historyEvent(),
            id: 7,
            processInstanceId: 17,
            processInstanceTaskId: 1,
            title: 'Vorhandenes Ereignis',
            message: 'Die Aufgabe wurde zugewiesen.',
            timestamp: '2026-10-01T10:00:00Z',
        }]));
        await renderHistory([node(1, 'Zähler'), node(2, 'Aktuell')], [edge(1, 2)], [completedTask, task(2, 2)]);

        expect(screen.queryByRole('heading', {name: 'Zusammenfassung der Ausführung'})).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', {name: /^1\. Zähler:/}));
        const details = within(await screen.findByRole('region', {name: /^1\. Zähler:/}));
        const summary = details.getByRole('heading', {name: 'Zusammenfassung der Ausführung'});
        expect(details.getByText('Zählerstand:').tagName).toBe('STRONG');
        expect(within(summary.parentElement!).getAllByRole('listitem').map(item => item.textContent)).toEqual(['Erhöht', 'Gespeichert']);
        expect(details.getByRole('link', {name: 'Details'})).toHaveAttribute('href', 'https://example.org/details');
        const events = details.getByRole('heading', {name: 'Ereignisse und Zwischenergebnisse'});
        expect(summary.compareDocumentPosition(events) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
        expect(details.getByText('Vorhandenes Ereignis')).toBeVisible();

        await user.click(screen.getByRole('button', {name: /^2\. Aktuell:/}));
        expect(within(await screen.findByRole('region', {name: /^2\. Aktuell:/}))
            .queryByRole('heading', {name: 'Zusammenfassung der Ausführung'})).not.toBeInTheDocument();
    });

    it.each([null, '', '  \n\t'])('hides the summary section for absent or blank text (%j)', async (summary) => {
        const user = userEvent.setup();
        await renderHistory([node(1, 'Altbestand')], [], [{
            ...task(1, 1, ProcessTaskStatus.Completed),
            executionSummaryMarkdown: summary,
        }]);
        await user.click(screen.getByRole('button', {name: /^1\. Altbestand:/}));
        const details = within(await screen.findByRole('region', {name: /^1\. Altbestand:/}));
        expect(details.queryByRole('heading', {name: 'Zusammenfassung der Ausführung'})).not.toBeInTheDocument();
        expect(details.queryByRole('heading', {name: 'Ereignisse und Zwischenergebnisse'})).not.toBeInTheDocument();
        expect(details.getByRole('alert')).toHaveTextContent('Für diese Aufgabe liegen keine Zusammenfassung und keine verlaufsrelevanten Ereignisse vor.');
        expect(details.getByRole('button', {name: 'Ereignisprotokoll öffnen'})).toBeVisible();
    });

    it('renders summary content without executing embedded HTML or unsafe links', async () => {
        const user = userEvent.setup();
        await renderHistory([node(1, 'Prüfung')], [], [{
            ...task(1, 1, ProcessTaskStatus.Completed),
            executionSummaryMarkdown: '<script>alert("unsafe")</script>\n\n[Unsicher](javascript:alert(1))',
        }]);
        await user.click(screen.getByRole('button', {name: /^1\. Prüfung:/}));
        const region = await screen.findByRole('region', {name: /^1\. Prüfung:/});
        expect(region.querySelector('script')).toBeNull();
        expect(within(region).getByText('Unsicher')).not.toHaveAttribute('href', 'javascript:alert(1)');
    });

    it.each([ProcessTaskStatus.AwaitingStaff, ProcessTaskStatus.InProgress])(
        'shows the upcoming steps of a manual action with status %s',
        async (status) => {
            await renderHistory(
                [
                    node(1, 'Festsetzung des Steuersatzes'),
                    node(2, 'Zahlungsaufforderung'),
                    node(3, 'Bescheiderstellung'),
                    node(4, 'Versand des Steuerbescheides'),
                    node(5, 'Vorgang beenden', ProcessNodeType.Termination, []),
                ],
                [edge(1, 2), edge(2, 3), edge(3, 4), edge(4, 5)],
                [task(1, 1, status)],
            );

            expect(screen.getByRole('button', {name: /^1\. Festsetzung des Steuersatzes:/})).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: /^Festsetzung des Steuersatzes:/})).not.toBeInTheDocument();
            const preview = within(screen.getByRole('list', {name: 'Voraussichtliche nächste Schritte'})).getAllByRole('listitem');
            expect(screen.queryByRole('button', {name: /^Zahlungsaufforderung:/})).not.toBeInTheDocument();
            expect(preview).toHaveLength(4);
            [
                /^Zahlungsaufforderung:/,
                /^Bescheiderstellung:/,
                /^Versand des Steuerbescheides:/,
                /^Vorgang beenden:/,
            ].forEach((name, index) => {
                expect(preview[index]).toHaveTextContent(name);
            });
            expect(screen.queryByText('Keine nächsten Schritte ableitbar.')).not.toBeInTheDocument();
        },
    );

    it.each([ProcessNodeType.FlowControl, ProcessNodeType.Termination])(
        'includes the next %s node and stops there',
        async (boundaryType) => {
            await renderHistory(
                [node(1, 'Past'), node(2, 'Current'), node(3, 'Review'), node(4, 'Boundary', boundaryType), node(5, 'Beyond')],
                [edge(1, 2), edge(2, 3), edge(3, 4), edge(4, 5)],
                [task(1, 1, ProcessTaskStatus.Completed), task(2, 2)],
            );

            expect(screen.getByRole('button', {name: /^1\. Past:/})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: /^2\. Current:/})).toBeInTheDocument();
            const preview = screen.getAllByRole('listitem');
            expect(preview[0]).toHaveTextContent(/^Review:/);
            expect(preview[1]).toHaveTextContent(/^Boundary:/);
            expect(screen.queryByRole('button', {name: /^Current:/})).not.toBeInTheDocument();
            expect(screen.queryByText(/^Beyond:/)).not.toBeInTheDocument();
            expect(ProcessDefinitionEdgeApiService.prototype.listAll).toHaveBeenCalledWith({processDefinitionId: 10, processDefinitionVersion: 1});
        },
    );

    it('does not predict the result of an action with multiple ports even with only one connected port', async () => {
        await renderHistory(
            [node(1, 'Current'), node(2, 'Approval', 'approval', ['next', 'rejected']), node(3, 'After approval')],
            [edge(1, 2), edge(2, 3)],
            [task(1, 1)],
        );

        expect(screen.getByRole('listitem')).toHaveTextContent(/^Approval:/);
        expect(screen.queryByText(/After approval/)).not.toBeInTheDocument();
    });

    it('stops a loop without repeating the active node', async () => {
        await renderHistory(
            [node(1, 'Current'), node(2, 'Review')],
            [edge(1, 2), edge(2, 1)],
            [task(1, 1)],
        );

        expect(screen.getAllByRole('button', {name: /Current:/})).toHaveLength(1);
        expect(screen.getAllByRole('listitem')).toHaveLength(1);
        expect(screen.getByRole('listitem')).toHaveTextContent(/^Review:/);
    });

    it('combines all active task statuses in task order and includes shared successors once', async () => {
        const statuses = [
            ProcessTaskStatus.Running,
            ProcessTaskStatus.InProgress,
            ProcessTaskStatus.AwaitingStaff,
            ProcessTaskStatus.Paused,
            ProcessTaskStatus.AwaitingCustomer,
            ProcessTaskStatus.AwaitingPayment,
        ];
        const nextNodeOffset = statuses.length + 1;
        const sharedNodeId = statuses.length * 2 + 1;
        await renderHistory(
            [
                ...statuses.map((_, index) => node(index + 1, `Active ${index}`)),
                ...statuses.map((_, index) => node(index + nextNodeOffset, `Next ${index}`)),
                node(sharedNodeId, 'Shared', ProcessNodeType.FlowControl),
            ],
            statuses.flatMap((_, index) => [edge(index + 1, index + nextNodeOffset), edge(index + nextNodeOffset, sharedNodeId)]),
            statuses.map((status, index) => task(index + 1, index + 1, status)),
        );

        const preview = screen.getAllByRole('listitem');
        expect(preview).toHaveLength(7);
        [/^Next 0:/, /^Shared:/, /^Next 1:/, /^Next 2:/, /^Next 3:/, /^Next 4:/, /^Next 5:/].forEach((name, index) => {
            expect(preview[index]).toHaveTextContent(name);
        });
    });

    it('shows the empty state when all tasks are inactive', async () => {
        const statuses = [ProcessTaskStatus.Completed, ProcessTaskStatus.Aborted, ProcessTaskStatus.Failed, ProcessTaskStatus.Restarted];
        await renderHistory(
            [node(1, 'Past'), node(2, 'Next')],
            [edge(1, 2)],
            statuses.map((status, index) => task(index + 1, 1, status)),
        );

        expect(screen.getByText('Keine nächsten Schritte ableitbar.')).toBeInTheDocument();
        expect(screen.queryByRole('listitem')).not.toBeInTheDocument();
    });

    it.each(['unconnected', 'unknown port', 'multiple edges', 'active flow control'])('stops at %s', async (scenario) => {
        const edges = scenario === 'unconnected' ? []
            : scenario === 'unknown port' ? [edge(1, 2, 'unknown')]
                : scenario === 'multiple edges' ? [edge(1, 2), edge(1, 3)]
                    : [edge(1, 2)];
        await renderHistory(
            [node(1, 'Current', scenario === 'active flow control' ? ProcessNodeType.FlowControl : ProcessNodeType.Action), node(2, 'Next'), node(3, 'Other')],
            edges,
            [task(1, 1)],
        );

        expect(screen.getByText(scenario === 'multiple edges'
            ? 'Nächste Schritte hängen vom weiteren Prozesspfad ab.'
            : 'Keine nächsten Schritte ableitbar.')).toBeVisible();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        expect(screen.queryByRole('listitem')).not.toBeInTheDocument();
    });
});
