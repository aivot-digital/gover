import {act, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {GenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {createGenericDetailsPageEventChannel} from '../../../../components/generic-details-page/generic-details-page-events';
import {Permission} from '../../../../data/permissions/permission';
import {showApiErrorSnackbar} from '../../../../slices/snackbar-slice';
import {type Page} from '../../../../models/dtos/page';
import {type ProcessDefinitionEdgeEntity} from '../../entities/process-definition-edge-entity';
import {type ProcessInstanceDetails} from '../../entities/process-instance-details';
import {type ProcessInstanceTaskEntity} from '../../entities/process-instance-task-entity';
import {type ProcessNodeEntity} from '../../entities/process-node-entity';
import {ProcessTaskStatus} from '../../enums/process-task-status';
import {ProcessDefinitionApiService} from '../../services/process-definition-api-service';
import {ProcessDefinitionEdgeApiService} from '../../services/process-definition-edge-api-service';
import {ProcessInstanceApiService} from '../../services/process-instance-api-service';
import {ProcessInstanceEventApiService} from '../../services/process-instance-event-api-service';
import {ProcessInstanceTaskApiService} from '../../services/process-instance-task-api-service';
import {ProcessNodeApiService} from '../../services/process-node-api-service';
import {
    type ProcessNodeProvider,
    ProcessNodeProviderApiService,
    ProcessNodeType,
} from '../../services/process-node-provider-api-service';
import {ProcessInstanceDetailsPageHistory} from './process-instance-details-page-history';

const mocks = vi.hoisted(() => ({dispatch: vi.fn(), permissions: new Set<string>()}));
vi.mock('../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => mocks.dispatch}));
vi.mock('../../../permissions/hooks/use-permissions', () => ({
    useHasProcessInstancePermission: (_id: number, permission: string) => mocks.permissions.has(permission),
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

function page<T>(content: T[]): Page<T> {
    return {content, page: {size: content.length, number: 0, totalElements: content.length, totalPages: 1}};
}

function provider(type: ProcessNodeType, ports = ['next'], key: string = type): ProcessNodeProvider {
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
        ports: ports.map((key) => ({key, label: key, description: ''})),
        outputs: [],
    };
}

function node(id: number, name: string, definitionKey: string = ProcessNodeType.Action): ProcessNodeEntity {
    return {
        ...ProcessNodeApiService.initialize(),
        id,
        name,
        processId: 10,
        processVersion: 1,
        processNodeDefinitionKey: definitionKey,
        processNodeDefinitionVersion: 1,
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

async function renderHistory(
    nodes: ProcessNodeEntity[],
    edges: ProcessDefinitionEdgeEntity[],
    tasks: ProcessInstanceTaskEntity[],
    {waitForLoad = true}: {waitForLoad?: boolean} = {},
) {
    vi.spyOn(ProcessNodeApiService.prototype, 'listAll').mockResolvedValue(page(nodes));
    vi.spyOn(ProcessDefinitionEdgeApiService.prototype, 'listAll').mockResolvedValue(page(edges));
    vi.spyOn(ProcessInstanceTaskApiService.prototype, 'listAllOrdered').mockResolvedValue(page(tasks));
    const item: ProcessInstanceDetails = {
        instance: {...new ProcessInstanceApiService().initialize(), id: 17, processId: 10},
        processName: 'Test process',
        departmentId: 1,
        departmentName: null,
        triggerName: 'Start',
        triggerType: null,
        activeTasks: [],
    };

    const eventChannel = createGenericDetailsPageEventChannel();
    const rendered = render(
        <MemoryRouter>
            <GenericDetailsPageContext.Provider value={{
                item,
                setItem: vi.fn(),
                setAdditionalData: vi.fn(),
                isBusy: false,
                setIsBusy: vi.fn(),
                refresh: vi.fn(),
                subscribeEvent: eventChannel.subscribeEvent,
                isEditable: false,
            }}>
                <ProcessInstanceDetailsPageHistory/>
            </GenericDetailsPageContext.Provider>
        </MemoryRouter>,
    );
    if (waitForLoad) {
        await screen.findByRole('heading', {name: 'Voraussichtliche nächste Schritte'});
    }
    return {...rendered, eventChannel};
}

describe('Process instance history preview', () => {
    beforeEach(() => {
        mocks.permissions = new Set([Permission.PROCESS_INSTANCE_READ, Permission.PROCESS_DEFINITION_READ]);
        vi.spyOn(ProcessDefinitionApiService.prototype, 'retrieve').mockResolvedValue(ProcessDefinitionApiService.initialize());
        vi.spyOn(ProcessInstanceEventApiService.prototype, 'listAllOrdered').mockResolvedValue(page([]));
        vi.spyOn(ProcessNodeProviderApiService.prototype, 'getNodeProviders').mockResolvedValue([
            provider(ProcessNodeType.Action),
            provider(ProcessNodeType.FlowControl),
            provider(ProcessNodeType.Termination, []),
            provider(ProcessNodeType.Action, ['next', 'rejected'], 'approval'),
        ]);
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
            const refresh = deferred<Page<ProcessInstanceTaskEntity>>();
            vi.mocked(ProcessInstanceTaskApiService.prototype.listAllOrdered).mockReturnValueOnce(refresh.promise);

            act(() => eventChannel.emitEvent('refresh'));
            expect(ProcessInstanceTaskApiService.prototype.listAllOrdered).toHaveBeenCalledTimes(2);
            expect(screen.getByRole('button', {name: /^1\. Prüfung:/})).toBe(summary);
            expect(summary).toHaveAttribute('aria-expanded', 'true');
            expect(within(region).getByText('Bisherige Zusammenfassung')).toBeVisible();
            expect(screen.queryByRole('status', {name: 'Verlauf wird geladen'})).not.toBeInTheDocument();

            await act(async () => refresh.resolve(page([{
                ...original, executionSummaryMarkdown: 'Aktualisierte Zusammenfassung',
            }])));
            expect(screen.getByRole('button', {name: /^1\. Prüfung:/})).toBe(summary);
            expect(summary).toHaveAttribute('aria-expanded', 'true');
            expect(screen.getByRole('region', {name: /^1\. Prüfung:/})).toBe(region);
            expect(within(region).getByText('Aktualisierte Zusammenfassung')).toBeVisible();
            expect(within(region).queryByText('Bisherige Zusammenfassung')).not.toBeInTheDocument();
            expect(ProcessInstanceEventApiService.prototype.listAllOrdered).toHaveBeenCalledTimes(2);
            expect(ProcessDefinitionEdgeApiService.prototype.listAll).toHaveBeenCalledTimes(2);
        });

        it('keeps the displayed history and reports a failed refresh', async () => {
            const user = userEvent.setup();
            const {eventChannel} = await renderHistory([node(1, 'Prüfung')], [], [task(1, 1)]);
            const summary = screen.getByRole('button', {name: /^1\. Prüfung:/});
            await user.click(summary);
            const error = new Error('Refresh failed');
            vi.mocked(ProcessInstanceTaskApiService.prototype.listAllOrdered).mockRejectedValueOnce(error);

            await act(async () => eventChannel.emitEvent('refresh'));
            expect(mocks.dispatch).toHaveBeenCalledWith(showApiErrorSnackbar(error, 'Der Verlauf konnte nicht geladen werden.'));
            expect(screen.getByRole('button', {name: /^1\. Prüfung:/})).toBe(summary);
            expect(summary).toHaveAttribute('aria-expanded', 'true');
            expect(screen.queryByRole('status', {name: 'Verlauf wird geladen'})).not.toBeInTheDocument();
        });

        it('ignores superseded responses and errors when refreshes overlap', async () => {
            const {eventChannel} = await renderHistory([node(1, 'Prüfung')], [], [task(1, 1)]);
            const old = deferred<Page<ProcessInstanceTaskEntity>>();
            const current = deferred<Page<ProcessInstanceTaskEntity>>();
            vi.mocked(ProcessInstanceTaskApiService.prototype.listAllOrdered)
                .mockReturnValueOnce(old.promise).mockReturnValueOnce(current.promise);
            act(() => eventChannel.emitEvent('refresh'));
            act(() => eventChannel.emitEvent('refresh'));
            await act(async () => current.resolve(page([task(1, 1), task(2, 1)])));
            expect(screen.getByRole('button', {name: /^2\. Prüfung:/})).toBeInTheDocument();

            await act(async () => old.resolve(page([task(1, 1)])));
            expect(screen.getByRole('button', {name: /^2\. Prüfung:/})).toBeInTheDocument();

            const failed = deferred<Page<ProcessInstanceTaskEntity>>();
            vi.mocked(ProcessInstanceTaskApiService.prototype.listAllOrdered).mockReturnValueOnce(failed.promise);
            act(() => eventChannel.emitEvent('refresh'));
            act(() => eventChannel.emitEvent('refresh'));
            await waitFor(() => expect(screen.queryByRole('button', {name: /^2\. Prüfung:/})).not.toBeInTheDocument());
            await act(async () => failed.reject(new Error('Obsolete failure')));
            expect(mocks.dispatch).not.toHaveBeenCalled();
        });

        it.each(['success', 'failure'] as const)('ignores late %s after leaving the history', async outcome => {
            const {eventChannel, unmount} = await renderHistory([node(1, 'Prüfung')], [], [task(1, 1)]);
            const pending = deferred<Page<ProcessInstanceTaskEntity>>();
            vi.mocked(ProcessInstanceTaskApiService.prototype.listAllOrdered).mockReturnValueOnce(pending.promise);
            act(() => eventChannel.emitEvent('refresh'));
            expect(ProcessInstanceTaskApiService.prototype.listAllOrdered).toHaveBeenCalledTimes(2);
            unmount();
            eventChannel.emitEvent('refresh');
            expect(ProcessInstanceTaskApiService.prototype.listAllOrdered).toHaveBeenCalledTimes(2);
            await act(async () => {
                if (outcome === 'success') {
                    pending.resolve(page([task(2, 1)]));
                } else {
                    pending.reject(new Error('Late failure'));
                }
            });
            expect(mocks.dispatch).not.toHaveBeenCalled();
        });

        it.each([Permission.PROCESS_INSTANCE_READ, Permission.PROCESS_DEFINITION_READ])(
            'does not fetch history on entry or refresh without %s',
            async permission => {
                mocks.permissions.delete(permission);
                const {eventChannel} = await renderHistory([node(1, 'Prüfung')], [], [task(1, 1)], {waitForLoad: false});
                expect(screen.getByRole('alert')).toHaveTextContent(/keine Berechtigung/);
                await act(async () => eventChannel.emitEvent('refresh'));
                expect(ProcessInstanceTaskApiService.prototype.listAllOrdered).not.toHaveBeenCalled();
                expect(ProcessDefinitionApiService.prototype.retrieve).not.toHaveBeenCalled();
                expect(ProcessInstanceEventApiService.prototype.listAllOrdered).not.toHaveBeenCalled();
            },
        );
    });

    describe('deadline chips', () => {
        const started = '2026-10-01T10:00:00Z';
        const deadline = '2026-10-03T10:00:00Z';

        it.each([
            {now: '2026-10-02T09:59:59.999Z', color: 'Default', label: 'Frist'},
            {now: '2026-10-02T10:00:00Z', color: 'Default', label: 'Frist'},
            {now: '2026-10-02T10:00:00.001Z', color: 'Warning', label: 'Frist'},
            {now: deadline, color: 'Warning', label: 'Frist'},
            {now: '2026-10-03T10:00:00.001Z', color: 'Error', label: 'Abgelaufen'},
        ])('shows $label in $color at $now', async ({now, color, label}) => {
            vi.spyOn(Date, 'now').mockReturnValue(Date.parse(now));
            await renderHistory([node(1, 'Prüfung')], [], [{...task(1, 1), started, deadline}]);

            const summary = screen.getByRole('button', {name: /^1\. Prüfung:/});
            expect(summary).toHaveAccessibleName(new RegExp(`${label}: 03\\.10\\.2026 um 12:00 Uhr`));
            expect(summary.querySelector('.MuiChip-root')).toHaveClass(`MuiChip-color${color}`);
        });

        describe.each([
            ProcessTaskStatus.InProgress,
            ProcessTaskStatus.AwaitingStaff,
            ProcessTaskStatus.Paused,
            ProcessTaskStatus.AwaitingCustomer,
            ProcessTaskStatus.AwaitingPayment,
        ])('open task with status %s', (status) => {
            it.each([
                {now: '2026-10-02T09:59:59.999Z', color: 'Default', label: 'Frist'},
                {now: '2026-10-02T12:00:00Z', color: 'Warning', label: 'Frist'},
                {now: '2026-10-04T10:00:00Z', color: 'Error', label: 'Abgelaufen'},
            ])('shows $label in $color', async ({now, color, label}) => {
                vi.spyOn(Date, 'now').mockReturnValue(Date.parse(now));
                await renderHistory([node(1, 'Prüfung')], [], [{...task(1, 1, status), started, deadline}]);

                const summary = screen.getByRole('button', {name: /^1\. Prüfung:/});
                expect(summary).toHaveAccessibleName(new RegExp(`${label}: 03\\.10\\.2026 um 12:00 Uhr`));
                expect(summary.querySelector('.MuiChip-root')).toHaveClass(`MuiChip-color${color}`);
            });
        });

        describe.each(['2026-10-04T10:00:00Z', '2027-10-04T10:00:00Z'])('completed tasks viewed at %s', (now) => {
            it.each([
                {finished: '2026-10-02T12:00:00Z', color: 'Success', label: 'Frist eingehalten'},
                {finished: deadline, color: 'Success', label: 'Frist eingehalten'},
                {finished: '2026-10-03T10:00:00.001Z', color: 'Error', label: 'Abgelaufen'},
            ])('shows $label without a timestamp when finished at $finished', async ({finished, color, label}) => {
                vi.spyOn(Date, 'now').mockReturnValue(Date.parse(now));
                await renderHistory([node(1, 'Prüfung')], [], [{
                    ...task(1, 1, ProcessTaskStatus.Completed),
                    started,
                    deadline,
                    finished,
                }]);

                const summary = screen.getByRole('button', {name: /^1\. Prüfung:/});
                expect(summary).toHaveAccessibleName(`1. Prüfung: ${label}`);
                const chip = summary.querySelector('.MuiChip-root');
                expect(chip).toHaveTextContent(new RegExp(`^${label}$`));
                expect(chip).toHaveClass(`MuiChip-color${color}`);
            });
        });

        it.each([
            ProcessTaskStatus.Completed,
            ProcessTaskStatus.Aborted,
            ProcessTaskStatus.Failed,
            ProcessTaskStatus.Restarted,
        ])('keeps an inactive %s task without a completion timestamp neutral', async (status) => {
            vi.spyOn(Date, 'now').mockReturnValue(Date.parse('2026-10-04T10:00:00Z'));
            await renderHistory([node(1, 'Prüfung')], [], [{...task(1, 1, status), started, deadline}]);

            const summary = screen.getByRole('button', {name: /^1\. Prüfung:/});
            expect(summary).toHaveAccessibleName(/Frist: 03\.10\.2026 um 12:00 Uhr/);
            expect(summary.querySelector('.MuiChip-root')).toHaveClass('MuiChip-colorDefault');
        });

        it('omits deadline chips for tasks without a deadline and upcoming steps', async () => {
            await renderHistory(
                [node(1, 'Prüfung'), node(2, 'Nächster Schritt')],
                [edge(1, 2)],
                [task(1, 1)],
            );

            for (const name of [/^1\. Prüfung:/, /^Nächster Schritt:/]) {
                const summary = screen.getByRole('button', {name});
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
        vi.mocked(ProcessInstanceEventApiService.prototype.listAllOrdered).mockResolvedValue(page([{
            ...new ProcessInstanceEventApiService().initialize(),
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
        expect(details.getAllByRole('listitem').map(item => item.textContent)).toEqual(['Erhöht', 'Gespeichert']);
        expect(details.getByRole('link', {name: 'Details'})).toHaveAttribute('href', 'https://example.org/details');
        const events = details.getByRole('heading', {name: 'Ereignisse und Zwischenergebnisse für diese Aufgabe'});
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
        expect(details.queryByRole('heading', {name: 'Ereignisse und Zwischenergebnisse für diese Aufgabe'})).not.toBeInTheDocument();
        expect(details.getByRole('button', {name: 'Alle Ereignisse anzeigen'})).toBeVisible();
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
                    node(5, 'Vorgang beenden', ProcessNodeType.Termination),
                ],
                [edge(1, 2), edge(2, 3), edge(3, 4), edge(4, 5)],
                [task(1, 1, status)],
            );

            expect(screen.getByRole('button', {name: /^1\. Festsetzung des Steuersatzes:/})).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: /^Festsetzung des Steuersatzes:/})).not.toBeInTheDocument();
            const preview = screen.getAllByRole('button', {
                name: /^(Zahlungsaufforderung|Bescheiderstellung|Versand des Steuerbescheides|Vorgang beenden):/,
            });
            expect(preview).toHaveLength(4);
            [
                /^Zahlungsaufforderung:/,
                /^Bescheiderstellung:/,
                /^Versand des Steuerbescheides:/,
                /^Vorgang beenden:/,
            ].forEach((name, index) => {
                expect(preview[index]).toHaveAccessibleName(name);
            });
            expect(screen.queryByText('Derzeit sind keine nächsten Schritte vorhersehbar.')).not.toBeInTheDocument();
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
            const preview = screen.getAllByRole('button', {name: /^(Review|Boundary):/});
            expect(preview[0]).toHaveAccessibleName(/^Review:/);
            expect(preview[1]).toHaveAccessibleName(/^Boundary:/);
            expect(screen.queryByRole('button', {name: /^Current:/})).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: /Beyond/})).not.toBeInTheDocument();
            expect(ProcessDefinitionEdgeApiService.prototype.listAll).toHaveBeenCalledWith({processDefinitionId: 10});
        },
    );

    it('does not predict the result of an action with multiple ports even with only one connected port', async () => {
        await renderHistory(
            [node(1, 'Current'), node(2, 'Approval', 'approval'), node(3, 'After approval')],
            [edge(1, 2), edge(2, 3)],
            [task(1, 1)],
        );

        expect(screen.getByRole('button', {name: /^Approval:/})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /After approval/})).not.toBeInTheDocument();
    });

    it('stops a loop without repeating the active node', async () => {
        await renderHistory(
            [node(1, 'Current'), node(2, 'Review')],
            [edge(1, 2), edge(2, 1)],
            [task(1, 1)],
        );

        expect(screen.getAllByRole('button', {name: /Current:/})).toHaveLength(1);
        expect(screen.getAllByRole('button', {name: /^Review:/})).toHaveLength(1);
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

        const preview = screen.getAllByRole('button', {name: /^(Next \d|Shared):/});
        expect(preview).toHaveLength(7);
        [/^Next 0:/, /^Shared:/, /^Next 1:/, /^Next 2:/, /^Next 3:/, /^Next 4:/, /^Next 5:/].forEach((name, index) => {
            expect(preview[index]).toHaveAccessibleName(name);
        });
    });

    it('shows the empty state when all tasks are inactive', async () => {
        const statuses = [ProcessTaskStatus.Completed, ProcessTaskStatus.Aborted, ProcessTaskStatus.Failed, ProcessTaskStatus.Restarted];
        await renderHistory(
            [node(1, 'Past'), node(2, 'Next')],
            [edge(1, 2)],
            statuses.map((status, index) => task(index + 1, 1, status)),
        );

        expect(screen.getByText('Derzeit sind keine nächsten Schritte vorhersehbar.')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /^Next:/})).not.toBeInTheDocument();
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

        expect(screen.getByText('Derzeit sind keine nächsten Schritte vorhersehbar.')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /^(Next|Other):/})).not.toBeInTheDocument();
    });
});
