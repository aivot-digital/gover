import {type ComponentProps, type PropsWithChildren, useState} from 'react';
import {act, render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {expect, it, vi} from 'vitest';
import {ElkLoadError} from '../../../../../../utils/elk-loader';
import {ProcessFlowEditor} from './process-flow-editor';
import {type FlowNode, layoutElements} from './utils/layout-utils';

const flow = vi.hoisted(() => ({
    nodes: [] as FlowNode[],
    getNodes: vi.fn((): FlowNode[] => flow.nodes),
    setViewport: vi.fn(async () => true),
}));

vi.mock('./utils/layout-utils', async (importOriginal) => ({
    ...await importOriginal<typeof import('./utils/layout-utils')>(),
    layoutElements: vi.fn(),
}));
vi.mock('./process-flow-editor-node', () => ({ProcessFlowEditorNode: () => null}));
vi.mock('./process-flow-editor-edge', () => ({ProcessFlowEditorEdge: () => null}));
vi.mock('../../../../components/process-node-provider-details', () => ({ProcessNodeProviderDetailsDialog: () => null}));
vi.mock('../../../../dialogs/process-instance-event-dialog', () => ({ProcessInstanceEventDialog: () => null}));
vi.mock('@xyflow/react', async (importOriginal) => ({
    ...await importOriginal<typeof import('@xyflow/react')>(),
    ReactFlow: ({children}: PropsWithChildren) => <div aria-label="Prozessfluss">{children}</div>,
    Panel: ({children}: PropsWithChildren) => children,
    Controls: () => null,
    Background: () => null,
    MiniMap: () => null,
    useNodesState: () => {
        const [nodes, setNodes] = useState<FlowNode[]>([]);
        flow.nodes = nodes;
        return [nodes, setNodes, vi.fn()];
    },
    useNodesInitialized: () => flow.nodes.length > 0,
    useReactFlow: () => ({getNodes: flow.getNodes, setViewport: flow.setViewport}),
    useStore: (selector: (state: unknown) => unknown) => selector({domNode: {clientWidth: 1280}, transform: [0, 0, 1]}),
}));

it('retries a failed runtime download and restores the initial diagram viewport without discarding inputs', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    const node = {id: 1, processNodeDefinitionKey: 'test', processNodeDefinitionVersion: 1};
    const provider = {key: 'test', majorVersion: 1, type: 'action'};
    const result = {
        flowNodes: [{id: '1', position: {x: 32, y: 144}, width: 420, height: 200, data: {graphNode: {node, provider}}}],
        flowEdges: [],
    } as unknown as Awaited<ReturnType<typeof layoutElements>>;
    vi.mocked(layoutElements).mockRejectedValueOnce(new ElkLoadError(new Error('offline'))).mockResolvedValue(result);
    const props = {
        editable: false,
        processFlow: {definition: {id: 1}, version: {processVersion: 1}, nodes: [node], edges: []},
        nodeProviders: [provider], runtimeData: null, onReloadRuntimeData: vi.fn(),
        nodeProblems: [], showNodeProblemsForNodes: {},
    } as unknown as ComponentProps<typeof ProcessFlowEditor>;
    render(<><input aria-label="Ungespeicherte Notiz" defaultValue="Entwurf"/><ProcessFlowEditor {...props}/></>);

    expect(await screen.findByRole('alert')).toHaveTextContent('Die Darstellung des Prozessflusses konnte nicht geladen werden.');
    expect(flow.setViewport).not.toHaveBeenCalled();
    await userEvent.click(screen.getByRole('button', {name: 'Erneut versuchen'}));

    await waitFor(() => expect(flow.setViewport).toHaveBeenCalledWith({x: 398, y: -96, zoom: 1}, {duration: 0}));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(screen.getByRole('textbox', {name: 'Ungespeicherte Notiz'})).toHaveValue('Entwurf');
    expect(flow.nodes.map((item) => item.id)).toEqual(['1']);
});

it('ignores a pending layout after switching to a process whose providers are unavailable', async () => {
    let finish!: (result: Awaited<ReturnType<typeof layoutElements>>) => void;
    vi.mocked(layoutElements).mockReset().mockReturnValue(new Promise((resolve) => { finish = resolve; }));
    const props = {
        editable: false,
        processFlow: {definition: {id: 1}, version: {processVersion: 1}, nodes: [{id: 1, processNodeDefinitionKey: 'test', processNodeDefinitionVersion: 1}], edges: []},
        nodeProviders: [{key: 'test', majorVersion: 1}], runtimeData: null, onReloadRuntimeData: vi.fn(),
        nodeProblems: [], showNodeProblemsForNodes: {},
    } as unknown as ComponentProps<typeof ProcessFlowEditor>;
    const {rerender} = render(<ProcessFlowEditor {...props}/>);
    await waitFor(() => expect(layoutElements).toHaveBeenCalledTimes(1));

    rerender(<ProcessFlowEditor {...props} nodeProviders={[]}/>);
    await act(async () => {
        finish({flowNodes: [{id: 'obsolete', position: {x: 0, y: 0}, data: {}}], flowEdges: []} as unknown as Awaited<ReturnType<typeof layoutElements>>);
    });

    expect(flow.nodes).toEqual([]);
    expect(flow.setViewport).not.toHaveBeenCalled();
});
