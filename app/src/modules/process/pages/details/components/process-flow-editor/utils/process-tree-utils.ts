import {ProcessNodeEntity} from "../../../../../entities/process-node-entity";
import {ProcessDefinitionEdgeEntity} from "../../../../../entities/process-definition-edge-entity";
import {ProcessNodePort, ProcessNodeProvider} from "../../../../../services/process-node-provider-api-service";

export interface TreeNode {
    node: ProcessNodeEntity;
    provider: ProcessNodeProvider;
    children: TreeEdge[];
    depth: number;
    order: number;
}

export interface TreeEdge {
    edge: ProcessDefinitionEdgeEntity;
    port: ProcessNodePort;
    childNode: TreeNode;
}

export function processDataToTree(nodes: ProcessNodeEntity[],
                                  edges: ProcessDefinitionEdgeEntity[],
                                  providers: ProcessNodeProvider[]): TreeNode[] {
    const nodeMap = new Map<number, ProcessNodeEntity>();
    const providerMap = new Map<string, ProcessNodeProvider>();
    const incomingEdgeCount = new Map<number, number>();
    const edgesByFromNode = new Map<number, ProcessDefinitionEdgeEntity[]>();
    const parentMap = new Map<number, number[]>(); // nodeId -> parent nodeIds
    const childMap = new Map<number, number[]>(); // nodeId -> child nodeIds

    nodes.forEach(node => {
        nodeMap.set(node.id, node);
        incomingEdgeCount.set(node.id, 0);
        parentMap.set(node.id, []);
        childMap.set(node.id, []);
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
        // Build parent/child maps
        parentMap.get(edge.toNodeId)!.push(edge.fromNodeId);
        childMap.get(edge.fromNodeId)!.push(edge.toNodeId);
    });

    // Compute rank (depth) for each node using topological sort
    const rankMap = new Map<number, number>();
    const queue: number[] = [];
    // Start with nodes with no incoming edges (roots)
    nodes.forEach(node => {
        if ((incomingEdgeCount.get(node.id) || 0) === 0) {
            queue.push(node.id);
            rankMap.set(node.id, 0);
        }
    });
    while (queue.length > 0) {
        const nodeId = queue.shift()!;
        const children = childMap.get(nodeId) || [];
        for (const childId of children) {
            // For each child, compute its rank as max(rank of all parents) + 1
            const parents = parentMap.get(childId) || [];
            let maxParentRank = -1;
            for (const parentId of parents) {
                maxParentRank = Math.max(maxParentRank, rankMap.get(parentId) ?? 0);
            }
            const newRank = maxParentRank + 1;
            if (!rankMap.has(childId) || rankMap.get(childId)! < newRank) {
                rankMap.set(childId, newRank);
            }
            // Decrement incoming edge count and add to queue if all parents processed
            incomingEdgeCount.set(childId, (incomingEdgeCount.get(childId) || 1) - 1);
            if (incomingEdgeCount.get(childId) === 0) {
                queue.push(childId);
            }
        }
    }

    function buildTree(nodeId: number, order: number, visited: Set<number>): TreeNode {
        if (visited.has(nodeId)) {
            // Cycle detected, return node without children to prevent infinite recursion
            const node = nodeMap.get(nodeId)!;
            const provider = providerMap.get(node.processNodeDefinitionKey)!;
            return {
                node,
                provider,
                children: [],
                depth: rankMap.get(nodeId) ?? 0,
                order,
            };
        }
        visited.add(nodeId);
        const node = nodeMap.get(nodeId)!;
        const provider = providerMap.get(node.processNodeDefinitionKey)!;
        const children: TreeEdge[] = [];
        const outgoingEdges = edgesByFromNode.get(nodeId) || [];
        provider.ports.forEach((port, index) => {
            const edge = outgoingEdges.find(e => e.viaPort === port.key);
            if (edge == null) {
                // No edge for this port, could handle differently if needed
            } else {
                const childNode = buildTree(edge.toNodeId, index, new Set(visited));
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
            depth: rankMap.get(nodeId) ?? 0,
            order,
        };
    }

    // Find root nodes (no incoming edges)
    const rootNodeIds = nodes
        .filter(node => (parentMap.get(node.id)?.length || 0) === 0)
        .map(node => node.id);

    return rootNodeIds.map(nodeId => buildTree(nodeId, 0, new Set<number>()));
}
