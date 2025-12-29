import {Edge as ReactFlowEdge, MarkerType, Node as ReactFlowNode,} from "@xyflow/react";
import {ProcessNodeEntity} from "../../../../../entities/process-node-entity";
import {ProcessDefinitionEdgeEntity} from "../../../../../entities/process-definition-edge-entity";
import {ProcessNodeProvider} from "../../../../../services/process-node-provider-api-service";
import {graphlib, layout} from '@dagrejs/dagre';
import {
    DEFAULT_FLOW_EDGE_TYPE,
    DEFAULT_FLOW_NODE_TYPE,
    NODE_HEIGHT,
    NODE_WIDTH,
    PROCESS_FLOW_EDGE_Z_INDEX,
    PROCESS_FLOW_NODE_Z_INDEX
} from "../data/process-flow-constants";
import {processDataToTree, TreeEdge, TreeNode} from "./process-tree-utils";

export type FlowNode = ReactFlowNode<{
    treeNode: TreeNode;
}>;

export type FlowEdge = ReactFlowEdge<{
    treeEdge: TreeEdge;
}>;

export function layoutElements(nodes: ProcessNodeEntity[],
                               edges: ProcessDefinitionEdgeEntity[],
                               providers: ProcessNodeProvider[]): {
    flowNodes: FlowNode[];
    flowEdges: FlowEdge[];
} {
    const treeRoots = processDataToTree(nodes, edges, providers);
    const {
        flowNodes,
        flowEdges,
    } = transformNodes(treeRoots);

    const measuredFlowNodes = measureNodes(flowNodes);

    const g = new graphlib
        .Graph()
        .setDefaultEdgeLabel(() => ({}));

    g.setGraph({
        rankdir: 'TB',
        ranksep: 128,
    });

    flowEdges.forEach((edge) => {
        g.setEdge(edge.source, edge.target);
    });

    measuredFlowNodes.forEach((node) => {
        g.setNode(node.id, {
            ...node,
            width: node.measured?.width ?? NODE_WIDTH,
            height: node.measured?.height ?? NODE_HEIGHT,
        });
    });

    layout(g);

    const positionedFlowNodes = measuredFlowNodes
        .map((node) => {
            const position = g.node(node.id);

            const x = position.x - ((node.measured?.width ?? NODE_WIDTH) / 2);
            const y = position.y - ((node.measured?.height ?? NODE_HEIGHT) / 2);

            return {
                ...node,
                position: {
                    x,
                    y,
                },
            };
        });

    return {
        flowNodes: positionedFlowNodes,
        flowEdges,
    };
}

function transformNodes(treeNodes: TreeNode[]): {
    flowNodes: FlowNode[];
    flowEdges: FlowEdge[];
} {

    const flowNodes: FlowNode[] = [];
    const flowEdges: FlowEdge[] = [];

    function transformTreeNode(treeNode: TreeNode) {
        const {
            node,
            children,
        } = treeNode;

        const nodeAlreadyExists = flowNodes
            .some((n) => n.id === String(node.id));

        if (nodeAlreadyExists) {
            return;
        }

        flowNodes.push({
            id: String(node.id),
            type: DEFAULT_FLOW_NODE_TYPE,
            position: {
                x: 0,
                y: 0,
            },
            measured: {
                width: 0,
                height: 0,
            },
            zIndex: PROCESS_FLOW_NODE_Z_INDEX,
            data: {
                treeNode: treeNode,
            },
        });

        for (const childEdge of children) {
            const edgeAlreadyExists = flowEdges
                .some((e) => e.id === String(childEdge.edge.id));

            if (edgeAlreadyExists) {
                continue;
            }

            const {edge, childNode} = childEdge;

            flowEdges.push({
                id: String(edge.id),
                sourceHandle: edge.viaPort,
                source: String(edge.fromNodeId),
                target: String(edge.toNodeId),
                markerEnd: {
                    type: MarkerType.ArrowClosed,
                },
                type: DEFAULT_FLOW_EDGE_TYPE,
                zIndex: PROCESS_FLOW_EDGE_Z_INDEX,
                data: {
                    treeEdge: childEdge,
                },
            });

            transformTreeNode(childNode);
        }
    }

    treeNodes.forEach(transformTreeNode);

    flowNodes.sort((a, b) => {
        return a.data.treeNode.depth - b.data.treeNode.depth;
    });

    for (const node of flowNodes) {
        console.log(node.data.treeNode.provider.name, node.data.treeNode.depth);
    }

    return {
        flowNodes,
        flowEdges,
    };
}

function measureNodes(nodes: FlowNode[]): FlowNode[] {
    return nodes.map((node) => {
        const elem = document.querySelector(`[data-node-id="${node.id}"]`);
        if (elem == null) {
            return node;
        }

        // Use the clientWidth and clientHeight to get the size without respecting the zoom level.
        // If the getBoundingClientRect is used, the size will be affected by the zoom level of the React Flow viewport.
        // The measurement will be incorrect because we need the actual size of the node.
        const width = elem.clientWidth;
        const height = elem.clientHeight;

        return {
            ...node,
            measured: {
                width,
                height,
            },
        };
    });
}