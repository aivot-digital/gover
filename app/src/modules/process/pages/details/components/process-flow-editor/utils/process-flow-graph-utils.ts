import {type ProcessDefinitionEdgeEntity} from '../../../../../entities/process-definition-edge-entity';
import {type ProcessNodeEntity} from '../../../../../entities/process-node-entity';
import {
    type ProcessNodePort,
    type ProcessNodeProvider,
    ProcessNodeType,
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
    isFeedbackEdge: boolean;
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
            isFeedbackEdge: false,
        };

        sourceNode.outgoingEdges.push(graphEdge);
        targetNode.incomingEdges.push(graphEdge);

        return graphEdge;
    });

    for (const graphNode of graphNodes) {
        graphNode.outgoingEdges.sort((a, b) => (
            getProviderPortIndex(graphNode.provider, a.port?.key) - getProviderPortIndex(graphNode.provider, b.port?.key) ||
            a.edge.toNodeId - b.edge.toNodeId ||
            a.edge.id - b.edge.id
        ));
        graphNode.incomingEdges.sort((a, b) => (
            a.edge.fromNodeId - b.edge.fromNodeId ||
            a.edge.id - b.edge.id
        ));
    }

    markFeedbackEdges(graphNodes);

    return {
        nodes: graphNodes,
        edges: graphEdges,
    };
}

function getProviderPortIndex(provider: ProcessNodeProvider, portKey?: string | null): number {
    if (portKey == null) {
        return Number.MAX_SAFE_INTEGER;
    }

    const portIndex = provider.ports.findIndex((port) => port.key === portKey);
    return portIndex === -1 ? Number.MAX_SAFE_INTEGER : portIndex;
}

function markFeedbackEdges(graphNodes: ProcessFlowGraphNode[]): void {
    const graphNodeById = new Map<number, ProcessFlowGraphNode>(graphNodes.map((graphNode) => [graphNode.node.id, graphNode]));
    const visitStateByNodeId = new Map<number, 'visiting' | 'visited'>();
    const orderedNodes = [
        ...graphNodes.filter((graphNode) => graphNode.provider.type === ProcessNodeType.Trigger),
        ...graphNodes.filter((graphNode) => graphNode.provider.type !== ProcessNodeType.Trigger),
    ];

    // Classify cycle-closing edges before ELK runs. That keeps the main process flow moving
    // downward and treats loop-backs as feedback edges instead of allowing arbitrary layer flips.
    const visit = (graphNode: ProcessFlowGraphNode): void => {
        visitStateByNodeId.set(graphNode.node.id, 'visiting');

        for (const outgoingEdge of graphNode.outgoingEdges) {
            const targetNode = graphNodeById.get(outgoingEdge.edge.toNodeId);
            if (targetNode == null) {
                continue;
            }

            const targetVisitState = visitStateByNodeId.get(targetNode.node.id);
            if (targetVisitState === 'visiting') {
                outgoingEdge.isFeedbackEdge = true;
                continue;
            }

            if (targetVisitState == null) {
                visit(targetNode);
            }
        }

        visitStateByNodeId.set(graphNode.node.id, 'visited');
    };

    for (const graphNode of orderedNodes) {
        if (visitStateByNodeId.has(graphNode.node.id)) {
            continue;
        }

        visit(graphNode);
    }
}
