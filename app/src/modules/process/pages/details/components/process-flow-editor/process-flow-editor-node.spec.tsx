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

const mocks = vi.hoisted(() => ({
    dispatch: vi.fn(),
    canEditTask: true,
}));

vi.mock('@xyflow/react', async importOriginal => ({
    ...await importOriginal<typeof import('@xyflow/react')>(),
    Handle: () => null,
    useUpdateNodeInternals: () => vi.fn(),
}));
vi.mock('../../../../../../providers/confirm-provider', () => ({useConfirm: () => vi.fn()}));
vi.mock('../../../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => mocks.dispatch}));
vi.mock('../../../../../permissions/hooks/use-permissions', () => ({
    useHasProcessInstancePermission: () => mocks.canEditTask,
}));

beforeEach(() => {
    vi.restoreAllMocks();
    mocks.dispatch.mockReset();
    mocks.canEditTask = true;
});

function renderNode(task: Partial<ProcessInstanceTaskEntity>) {
    const node = {...ProcessNodeApiService.initialize(), id: 1, name: 'Unterlagen prüfen'};
    const provider: ProcessNodeProvider = {
        key: 'test', componentKey: 'test', componentType: 'ProcessNodeDefinition', componentVersion: '1.0.0',
        deprecationNotice: null, majorVersion: 1, type: ProcessNodeType.Action, executionTypes: [],
        name: 'Prüfung', abstractDescription: '', description: '', documentationUrl: null,
        parentPluginKey: 'test', outputs: [],
    };
    const instance = new ProcessInstanceApiService().initialize();
    instance.identities = {
        applicant: {
            identityId: 'applicant', title: 'Antragstellende', type: 'Email',
            emailAddress: 'person@example.org', attributes: {}, providerKey: null, metadataIdentifier: null,
            uniqueIdFromIdentityProvider: null, communicationProviderBindingId: null, communicationProviderData: {},
        },
    };
    const onReloadRuntimeData = vi.fn();
    const context: ProcessFlowEditorContextType = {
        editable: false, showTargetHandles: false, selectedNode: null,
        onSelectNode: vi.fn(), onAddEdge: vi.fn(), onDeleteEdge: vi.fn(), onDeleteNode: vi.fn(),
        onConnectNodeToExisting: vi.fn(), onStartReplaceNode: vi.fn(), onStartCloneNode: vi.fn(),
        onShowNodeProviderDetails: vi.fn(), onShowTaskEvents: vi.fn(), onReloadRuntimeData,
        onAddFollowUpNode: vi.fn(), onAddInbetweenNode: vi.fn(), nodeProblems: [], showNodeProblemsForNodes: {},
        runtimeData: {
            instance, events: [], attachments: [], attachmentSets: [],
            tasks: [{...new ProcessInstanceTaskApiService().initialize(), id: 12, processNodeId: node.id, ...task}],
        },
    };
    const props: NodeProps<FlowNode> = {
        id: '1', type: 'process', dragging: false, zIndex: 0, selectable: true,
        deletable: false, selected: false, draggable: false, isConnectable: false,
        positionAbsoluteX: 0, positionAbsoluteY: 0,
        data: {graphNode: {node, provider, displayPorts: [], outgoingEdges: [], incomingEdges: []}},
    };
    render(
        <MemoryRouter>
            <ProcessFlowEditorProvider value={context}>
                <ProcessFlowEditorNode {...props} />
            </ProcessFlowEditorProvider>
        </MemoryRouter>,
    );
    return {onReloadRuntimeData};
}

function dispatchedSnackbar(message: string, severity: SnackbarSeverity) {
    return expect.objectContaining({payload: expect.objectContaining({message, severity})});
}

it.each([null, 'Unterlagen angefordert'])('shows the runtime identity in the diagram status tooltip (%s)', async statusOverride => {
    renderNode({status: ProcessTaskStatus.AwaitingCustomer, statusOverride, assignedCustomerIdentityId: 'applicant'});

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
    await user.click(await screen.findByRole('menuitem', {name: 'Aufgabe neu starten'}));

    expect(rerun).toHaveBeenCalledWith(12);
    await waitFor(() => expect(onReloadRuntimeData).toHaveBeenCalled());
    expect(mocks.dispatch).toHaveBeenCalledWith(dispatchedSnackbar('Der Neustart wurde angestoßen.', SnackbarSeverity.Success));
});

it('reports a rerun the server rejects instead of failing silently', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    vi.spyOn(ProcessInstanceTaskApiService.prototype, 'rerunFailedTask').mockRejectedValue(new Error('Serverfehler'));
    const {onReloadRuntimeData} = renderNode({status: ProcessTaskStatus.Failed});
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', {name: 'Aktionen zur Aufgabe'}));
    await user.click(await screen.findByRole('menuitem', {name: 'Aufgabe neu starten'}));

    await waitFor(() => expect(mocks.dispatch).toHaveBeenCalledWith(
        dispatchedSnackbar('Der Neustart ist fehlgeschlagen.', SnackbarSeverity.Error),
    ));
    expect(onReloadRuntimeData).not.toHaveBeenCalled();
});

it('offers no rerun without the permission to edit tasks of the instance', () => {
    mocks.canEditTask = false;
    renderNode({status: ProcessTaskStatus.Failed});

    expect(screen.getByRole('button', {name: 'Aktionen zur Aufgabe'})).toBeDisabled();
});
