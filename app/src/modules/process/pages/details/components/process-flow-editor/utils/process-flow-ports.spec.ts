import {describe, expect, it} from 'vitest';
import {ProcessNodeApiService} from '../../../../../services/process-node-api-service';
import {type ProcessNodePort, type ProcessNodeProvider, ProcessNodeType, ProcessNodeExecutionType} from '../../../../../services/process-node-provider-api-service';
import {buildProcessFlowGraph} from './process-flow-graph-utils';
import {layoutElements} from './layout-utils';

const provider: ProcessNodeProvider = {
    key: 'test.node', componentKey: 'node', componentType: 'ProcessNodeDefinition', componentVersion: '1.0.0',
    majorVersion: 1, parentPluginKey: 'test', name: 'Test', abstractDescription: '', description: '',
    deprecationNotice: null, documentationUrl: null, type: ProcessNodeType.Action,
    executionTypes: [ProcessNodeExecutionType.Automatic], outputs: [],
};
const port = (key: string): ProcessNodePort => ({key, label: key, description: 'Fortsetzen'});
const node = (id: number, ports: ProcessNodePort[]) => ({
    ...ProcessNodeApiService.initialize(), id, processNodeDefinitionKey: provider.key,
    processNodeDefinitionVersion: 1, ports,
});

describe('node-specific outgoing ports', () => {
    it('uses different ordered ports for nodes of the same provider and refreshes from saved nodes', () => {
        const first = node(1, [port('a'), port('b')]);
        const second = node(2, [port('c')]);
        const edge = {id: 1, fromNodeId: 1, toNodeId: 2, viaPort: 'b', processId: 1, processVersion: 1};
        const graph = buildProcessFlowGraph([first, second], [edge], [provider]);
        expect(graph.nodes.map((item) => item.displayPorts.map((item) => item.key))).toEqual([['a', 'b'], ['c']]);
        expect(graph.edges[0].port?.label).toBe('b');

        const saved = {...first, ports: [port('a'), port('__deadline_exceeded')]};
        const updated = buildProcessFlowGraph([saved, second], [edge], [provider]);
        expect(updated.edges[0].port).toBeNull();
        expect(updated.nodes[0].displayPorts.at(-1)).toMatchObject({key: 'b', unavailable: true});
        expect(saved.ports.map((item) => item.key)).toEqual(['a', '__deadline_exceeded']);
        expect(edge.viaPort).toBe('b');
    });

    it('preserves the unavailable deadline edge after a saved switch from strict to soft', async () => {
        const strict = {...node(1, [port('__deadline_exceeded')]), timeLimitDays: 3, timeLimitStrict: true};
        const target = node(2, []);
        const edges = [{id: 5, fromNodeId: 1, toNodeId: 2, viaPort: '__deadline_exceeded', processId: 1, processVersion: 1}];
        expect(buildProcessFlowGraph([strict, target], edges, [provider]).edges[0].port).not.toBeNull();
        const nodes = [{...strict, timeLimitStrict: false, ports: []}, target];
        const result = await layoutElements(nodes, edges, [provider]);
        expect(result.flowNodes).toHaveLength(2);
        expect(result.flowEdges).toHaveLength(1);
        expect(result.flowEdges[0]).toMatchObject({id: '5', sourceHandle: '__deadline_exceeded'});
        expect(result.flowEdges[0].data?.graphEdge.port).toBeNull();
        expect(result.flowNodes[0].data.graphNode.displayPorts[0].unavailable).toBe(true);
    });

    it('still rejects missing endpoints and duplicate node IDs', async () => {
        const edge = {id: 5, fromNodeId: 1, toNodeId: 3, viaPort: 'a', processId: 1, processVersion: 1};
        await expect(layoutElements([node(1, [port('a')])], [edge], [provider])).rejects.toThrow();
        await expect(layoutElements([node(1, []), node(1, [])], [], [provider])).rejects.toThrow();
    });
});
