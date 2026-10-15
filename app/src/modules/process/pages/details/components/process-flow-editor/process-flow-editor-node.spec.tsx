import {type NodeProps} from '@xyflow/react';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {expect, it, vi} from 'vitest';
import {ProcessTaskStatus} from '../../../../enums/process-task-status';
import {ProcessInstanceApiService} from '../../../../services/process-instance-api-service';
import {ProcessInstanceTaskApiService} from '../../../../services/process-instance-task-api-service';
import {ProcessNodeApiService} from '../../../../services/process-node-api-service';
import {ProcessNodeType, type ProcessNodeProvider} from '../../../../services/process-node-provider-api-service';
import {ProcessFlowEditorNode} from './process-flow-editor-node';
import {ProcessFlowEditorProvider, type ProcessFlowEditorContextType} from './process-flow-editor-context';
import {type FlowNode} from './utils/layout-utils';

vi.mock('@xyflow/react', async importOriginal => ({
    ...await importOriginal<typeof import('@xyflow/react')>(),
    Handle: () => null,
    useUpdateNodeInternals: () => vi.fn(),
}));
vi.mock('../../../../../../providers/confirm-provider', () => ({useConfirm: () => vi.fn()}));
vi.mock('../../../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));

it.each([null, 'Unterlagen angefordert'])('shows the runtime identity in the diagram status tooltip (%s)', async statusOverride => {
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
    const context: ProcessFlowEditorContextType = {
        editable: false, showTargetHandles: false, selectedNode: null,
        onSelectNode: vi.fn(), onAddEdge: vi.fn(), onDeleteEdge: vi.fn(), onDeleteNode: vi.fn(),
        onConnectNodeToExisting: vi.fn(), onStartReplaceNode: vi.fn(), onStartCloneNode: vi.fn(),
        onShowNodeProviderDetails: vi.fn(), onShowTaskEvents: vi.fn(), onReloadRuntimeData: vi.fn(),
        onAddFollowUpNode: vi.fn(), onAddInbetweenNode: vi.fn(), nodeProblems: [], showNodeProblemsForNodes: {},
        runtimeData: {
            instance, events: [], attachments: [], attachmentSets: [],
            tasks: [{
                ...new ProcessInstanceTaskApiService().initialize(), id: 12, processNodeId: node.id,
                status: ProcessTaskStatus.AwaitingCustomer, statusOverride, assignedCustomerIdentityId: 'applicant',
            }],
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

    const tooltip = statusOverride
        ? 'Unterlagen angefordert (Systemstatus: Wartet auf Antragstellende)'
        : 'Wartet auf Antragstellende';
    await userEvent.hover(screen.getByLabelText(tooltip));
    expect(await screen.findByRole('tooltip')).toHaveTextContent(tooltip);
});
