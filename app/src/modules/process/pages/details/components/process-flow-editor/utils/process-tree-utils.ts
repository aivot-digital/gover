import {ProcessDefinitionNodeEntity} from "../../../../../entities/process-definition-node-entity";
import {ProcessDefinitionEdgeEntity} from "../../../../../entities/process-definition-edge-entity";
import {ProcessNodePort, ProcessNodeProvider} from "../../../../../services/process-node-provider-api-service";

export interface TreeNode {
    node: ProcessDefinitionNodeEntity;
    provider: ProcessNodeProvider;
    children: TreeEdge[];
    depth: number;
}

export interface TreeEdge {
    edge: ProcessDefinitionEdgeEntity;
    port: ProcessNodePort;
    childNode: TreeNode;
}

export function processDataToTree(nodes: ProcessDefinitionNodeEntity[],
                                  edges: ProcessDefinitionEdgeEntity[],
                                  providers: ProcessNodeProvider[]): TreeNode[] {
    const nodeMap = new Map<number, ProcessDefinitionNodeEntity>();
    const providerMap = new Map<string, ProcessNodeProvider>();
    const incomingEdgeCount = new Map<number, number>();
    const edgesByFromNode = new Map<number, ProcessDefinitionEdgeEntity[]>();

    nodes.forEach(node => {
        nodeMap.set(node.id, node);
        incomingEdgeCount.set(node.id, 0);
    });

    providers.forEach(provider => {
        providerMap.set(provider.key, provider);
    });

    edges.forEach(edge => {
        incomingEdgeCount.set(edge.toNodeId, (incomingEdgeCount.get(edge.toNodeId) || 0) + 1);
        if (!edgesByFromNode.has(edge.fromNodeId)) {
            edgesByFromNode.set(edge.fromNodeId, []);
        }
        edgesByFromNode.get(edge.fromNodeId)!.push(edge);
    });

    function buildTree(nodeId: number, depth: number): TreeNode {
        const node = nodeMap.get(nodeId)!;
        const provider = providerMap.get(node.codeKey)!;
        const children: TreeEdge[] = [];

        const outgoingEdges = edgesByFromNode.get(nodeId) || [];
        provider.ports.forEach(port => {
            const edge = outgoingEdges
                .find(e => e.viaPort === port.key);
            if (edge == null) {
                // No edge for this port, could handle differently if needed
            } else {
                const childNode = buildTree(edge.toNodeId, depth + 1);
                children.push({
                    edge,
                    port,
                    childNode,
                });
            }
        });

        return {
            node,
            provider,
            children,
            depth,
        };
    }

    // Find root nodes (no incoming edges)
    const rootNodeIds = nodes
        .filter(node => (incomingEdgeCount.get(node.id) || 0) === 0)
        .map(node => node.id);

    return rootNodeIds.map(nodeId => buildTree(nodeId, 0));
}

