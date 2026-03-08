import {type ProcessDefinitionEdgeEntity} from '../../../../../entities/process-definition-edge-entity';
import {type ProcessNodeEntity} from '../../../../../entities/process-node-entity';
import {
    type ProcessNodePort,
    type ProcessNodeProvider,
} from '../../../../../services/process-node-provider-api-service';

export interface ProcessFlowGraphNode {
    node: ProcessNodeEntity;
    provider: ProcessNodeProvider;
    outgoingEdges: ProcessFlowGraphEdge[];
    incomingEdges: ProcessFlowGraphEdge[];
}

export interface ProcessFlowGraphEdge {
    edge: ProcessDefinitionEdgeEntity;
    port: ProcessNodePort | null;
}

export interface ProcessFlowGraph {
    nodes: ProcessFlowGraphNode[];
    edges: ProcessFlowGraphEdge[];
}

export function getProcessNodeProviderKey(key: string, version: number): string {
    return `${key}:${version}`;
}

export function buildProcessFlowGraph(
    nodes: ProcessNodeEntity[],
    edges: ProcessDefinitionEdgeEntity[],
    providers: ProcessNodeProvider[],
): ProcessFlowGraph {
    const providerMap = new Map<string, ProcessNodeProvider>();
    for (const provider of providers) {
        providerMap.set(getProcessNodeProviderKey(provider.key, provider.majorVersion), provider);
    }

    const graphNodes = nodes.map<ProcessFlowGraphNode>((node) => {
        const provider = providerMap.get(getProcessNodeProviderKey(node.processNodeDefinitionKey, node.processNodeDefinitionVersion));
        if (provider == null) {
            throw new Error(`Missing node provider ${node.processNodeDefinitionKey}@${node.processNodeDefinitionVersion} for process node ${node.id}`);
        }

        return {
            node,
            provider,
            outgoingEdges: [],
            incomingEdges: [],
        };
    });

    const graphNodeById = new Map<number, ProcessFlowGraphNode>(graphNodes.map((graphNode) => [graphNode.node.id, graphNode]));
    const graphEdges = edges.map<ProcessFlowGraphEdge>((edge) => {
        const sourceNode = graphNodeById.get(edge.fromNodeId);
        const targetNode = graphNodeById.get(edge.toNodeId);

        if (sourceNode == null || targetNode == null) {
            throw new Error(`Missing source or target node for edge ${edge.id}`);
        }

        const graphEdge: ProcessFlowGraphEdge = {
            edge,
            port: sourceNode.provider.ports.find((port) => port.key === edge.viaPort) ?? null,
        };

        sourceNode.outgoingEdges.push(graphEdge);
        targetNode.incomingEdges.push(graphEdge);

        return graphEdge;
    });

    for (const graphNode of graphNodes) {
        graphNode.outgoingEdges.sort((a, b) => a.edge.id - b.edge.id);
        graphNode.incomingEdges.sort((a, b) => a.edge.id - b.edge.id);
    }

    graphNodes.sort((a, b) => a.node.id - b.node.id);
    graphEdges.sort((a, b) => a.edge.id - b.edge.id);

    return {
        nodes: graphNodes,
        edges: graphEdges,
    };
}
