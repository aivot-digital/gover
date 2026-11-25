import {type NodeProps} from '@xyflow/react';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, expect, it, vi} from 'vitest';
import {ProcessTaskStatus} from '../../../../enums/process-task-status';
import {ProcessInstanceApiService} from '../../../../services/process-instance-api-service';
import {ProcessInstanceTaskApiService} from '../../../../services/process-instance-task-api-service';
import {ProcessNodeApiService} from '../../../../services/process-node-api-service';
import {ProcessNodeType, type ProcessNodeProvider} from '../../../../services/process-node-provider-api-service';
import {ProcessFlowEditorNode} from './process-flow-editor-node';
import {ProcessFlowEditorProvider, type ProcessFlowEditorContextType} from './process-flow-editor-context';
import {type FlowNode} from './utils/layout-utils';
import {type ProcessInstanceTaskEntity} from '../../../../entities/process-instance-task-entity';
import {SnackbarSeverity} from '../../../../../../slices/shell-slice';
import {Permission} from '../../../../../../data/permissions/permission';

const mocks = vi.hoisted(() => ({
    dispatch: vi.fn(),
    canRestartFailed: true,
}));

vi.mock('@xyflow/react', async importOriginal => ({
    ...await importOriginal<typeof import('@xyflow/react')>(),
    Handle: () => null,
    useUpdateNodeInternals: () => vi.fn(),
}));
vi.mock('../../../../../../providers/confirm-provider', () => ({useConfirm: () => vi.fn()}));
vi.mock('../../../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => mocks.dispatch}));
vi.mock('../../../../../permissions/hooks/use-permissions', () => ({
    useHasProcessInstancePermission: (_instanceId: number | undefined, permission: Permission) =>
        permission === Permission.PROCESS_INSTANCE_RESTART_FAILED && mocks.canRestartFailed,
}));

beforeEach(() => {
    vi.restoreAllMocks();
    mocks.dispatch.mockReset();
    mocks.canRestartFailed = true;
});

function renderNode(task: Partial<ProcessInstanceTaskEntity>, overrides: Partial<ProcessFlowEditorContextType> = {}) {
    const node = {
        ...ProcessNodeApiService.initialize(),
        id: 1,
        name: 'Unterlagen prüfen',
    };
    const provider: ProcessNodeProvider = {
        key: 'test',
        componentKey: 'test',
        componentType: 'ProcessNodeDefinition',
        componentVersion: '1.0.0',
        deprecationNotice: null,
        majorVersion: 1,
        type: ProcessNodeType.Action,
        executionTypes: [],
        name: 'Prüfung',
        abstractDescription: '',
        description: '',
        documentationUrl: null,
        parentPluginKey: 'test',
        outputs: [],
    };
    const instance = new ProcessInstanceApiService().initialize();
    instance.identities = {
        applicant: {
            identityId: 'applicant',
            title: 'Antragstellende',
            type: 'Email',
            emailAddress: 'person@example.org',
            attributes: {},
            providerKey: null,
            metadataIdentifier: null,
            uniqueIdFromIdentityProvider: null,
            communicationProviderBindingId: null,
            communicationProviderData: {},
        },
    };
    const onReloadRuntimeData = vi.fn();
    const context: ProcessFlowEditorContextType = {
        editable: false,
        showTargetHandles: false,
        selectedNode: null,
        onSelectNode: vi.fn(),
        onAddEdge: vi.fn(),
        onDeleteEdge: vi.fn(),
        onDeleteNode: vi.fn(),
        onConnectNodeToExisting: vi.fn(),
        onStartReplaceNode: vi.fn(),
        onStartCloneNode: vi.fn(),
        onShowNodeProviderDetails: vi.fn(),
        onShowTaskEvents: vi.fn(),
        onReloadRuntimeData,
        onAddFollowUpNode: vi.fn(),
        onAddInbetweenNode: vi.fn(),
        nodeProblems: [],
        showNodeProblemsForNodes: {},
        runtimeData: {
            instance,
            events: [],
            attachments: [],
            attachmentSets: [],
            tasks: [{
                ...new ProcessInstanceTaskApiService().initialize(),
                id: 12,
                processNodeId: node.id, ...task,
            }],
        },
    };
    const props: NodeProps<FlowNode> = {
        id: '1',
        type: 'process',
        dragging: false,
        zIndex: 0,
        selectable: true,
        deletable: false,
        selected: false,
        draggable: false,
        isConnectable: false,
        positionAbsoluteX: 0,
        positionAbsoluteY: 0,
        data: {
            graphNode: {
                node,
                provider,
                displayPorts: [],
                outgoingEdges: [],
                incomingEdges: [],
            },
        },
    };
    const content = (value: ProcessFlowEditorContextType) => (
        <MemoryRouter>
            <ProcessFlowEditorProvider value={value}>
                <ProcessFlowEditorNode {...props} />
            </ProcessFlowEditorProvider>
        </MemoryRouter>
    );
    const value = {...context, ...overrides};
    const view = render(content(value));
    return {
        onReloadRuntimeData,
        rerenderRuntimeData: (runtimeData: ProcessFlowEditorContextType['runtimeData']) => view.rerender(content({
            ...value,
            runtimeData,
        })),
    };
}

function dispatchedSnackbar(message: string, severity: SnackbarSeverity) {
    return expect.objectContaining({
        payload: expect.objectContaining({
            message,
            severity,
        }),
    });
}

it.each([null, 'Unterlagen angefordert'])('shows the runtime identity in the diagram status tooltip (%s)', async statusOverride => {
    renderNode({
        status: ProcessTaskStatus.AwaitingCustomer,
        statusOverride,
        assignedCustomerIdentityId: 'applicant',
    });

    const tooltip = statusOverride
        ? 'Unterlagen angefordert (Systemstatus: Wartet auf Antragstellende)'
        : 'Wartet auf Antragstellende';
    await userEvent.hover(screen.getByLabelText(tooltip));
    expect(await screen.findByRole('tooltip')).toHaveTextContent(tooltip);
});

it('reruns a failed task and reloads the runtime data', async () => {
    const rerun = vi.spyOn(ProcessInstanceTaskApiService.prototype, 'rerunFailedTask')
        .mockResolvedValue(new ProcessInstanceTaskApiService().initialize());
    const {onReloadRuntimeData} = renderNode({status: ProcessTaskStatus.Failed});
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', {name: 'Aktionen zur Aufgabe'}));
    await user.click(await screen.findByRole('menuitem', {name: 'Aufgabe erneut ausführen'}));

    expect(rerun).toHaveBeenCalledWith(12);
    await waitFor(() => expect(onReloadRuntimeData).toHaveBeenCalled());
    expect(mocks.dispatch).toHaveBeenCalledWith(dispatchedSnackbar('Die erneute Ausführung der Aufgabe wurde angefordert.', SnackbarSeverity.Success));
});

it.each([false, true])('keeps the restart status and event target until the new attempt arrives (older loop: %s)', async hasOlderLoop => {
    const restarted = {
        ...new ProcessInstanceTaskApiService().initialize(),
        id: 2,
        processNodeId: 1,
        started: '2026-11-20T10:02:00Z',
        status: ProcessTaskStatus.Restarted,
    };
    const olderLoop = {
        ...restarted,
        id: 1,
        started: '2026-11-20T10:01:00Z',
        status: ProcessTaskStatus.Completed,
    };
    const runtimeData = {
        instance: new ProcessInstanceApiService().initialize(),
        tasks: [restarted, ...(hasOlderLoop ? [olderLoop] : [])],
        events: [],
        attachments: [],
        attachmentSets: [],
    };
    const onShowTaskEvents = vi.fn();
    const {rerenderRuntimeData} = renderNode({}, {
        runtimeData,
        onShowTaskEvents,
    });

    expect(screen.getByLabelText('Neu gestartet')).toBeVisible();
    expect(screen.queryByLabelText('Abgeschlossen')).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', {name: 'Ereignisse'}));
    expect(onShowTaskEvents).toHaveBeenLastCalledWith(restarted.id);

    const replacement = {
        ...restarted,
        id: 3,
        started: '2026-11-20T10:03:00Z',
        status: ProcessTaskStatus.Running,
        restartForTaskId: restarted.id,
    };
    rerenderRuntimeData({
        ...runtimeData,
        tasks: [replacement, ...runtimeData.tasks],
    });
    expect(screen.getByLabelText('Gestartet')).toBeVisible();
    expect(screen.queryByLabelText('Neu gestartet')).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', {name: 'Ereignisse'}));
    expect(onShowTaskEvents).toHaveBeenLastCalledWith(replacement.id);
});

it('reports a rerun the server rejects instead of failing silently', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    vi.spyOn(ProcessInstanceTaskApiService.prototype, 'rerunFailedTask').mockRejectedValue(new Error('Serverfehler'));
    const {onReloadRuntimeData} = renderNode({status: ProcessTaskStatus.Failed});
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', {name: 'Aktionen zur Aufgabe'}));
    await user.click(await screen.findByRole('menuitem', {name: 'Aufgabe erneut ausführen'}));

    await waitFor(() => expect(mocks.dispatch).toHaveBeenCalledWith(
        dispatchedSnackbar('Die erneute Ausführung der Aufgabe konnte nicht angefordert werden.', SnackbarSeverity.Error),
    ));
    expect(onReloadRuntimeData).not.toHaveBeenCalled();
});

it('offers no rerun without the permission to restart failed tasks of the instance', () => {
    mocks.canRestartFailed = false;
    renderNode({status: ProcessTaskStatus.Failed});

    expect(screen.getByRole('button', {name: 'Aktionen zur Aufgabe'})).toBeDisabled();
});


it('opens node execution history without selecting the graph node', async () => {
    const onShowNodeExecutions = vi.fn();
    const onSelectNode = vi.fn();
    const tasks = [1, 2, 3].map(id => ({
        ...new ProcessInstanceTaskApiService().initialize(),
        id,
        processNodeId: 1,
        status: id === 3 ? ProcessTaskStatus.Restarted : ProcessTaskStatus.Completed,
    }));
    renderNode({}, {
        onShowNodeExecutions,
        onSelectNode,
        runtimeData: {
            instance: {
                ...new ProcessInstanceApiService().initialize(),
                id: 17,
            },
            tasks,
            events: [],
            attachments: [],
            attachmentSets: [],
            tasksComplete: true,
        },
    });
    await userEvent.click(screen.getByRole('button', {name: '2 Ausführungen anzeigen'}));
    expect(onShowNodeExecutions).toHaveBeenCalledTimes(1);
    expect(onSelectNode).not.toHaveBeenCalled();
});

it('labels an incomplete execution count as a lower bound', () => {
    const task = {
        ...new ProcessInstanceTaskApiService().initialize(),
        id: 1,
        processNodeId: 1,
    };
    renderNode({}, {
        onShowNodeExecutions: vi.fn(),
        runtimeData: {
            instance: new ProcessInstanceApiService().initialize(),
            tasks: [task],
            events: [],
            attachments: [],
            attachmentSets: [],
            tasksComplete: false,
        },
    });
    expect(screen.getByRole('button', {name: 'Mindestens 1 Ausführung anzeigen'})).toHaveTextContent('≥1×');
});
