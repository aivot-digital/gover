import {type Edge as ReactFlowEdge, MarkerType, type Node as ReactFlowNode} from '@xyflow/react';
import {graphlib, layout} from '@dagrejs/dagre';
import {type ProcessDefinitionEdgeEntity} from '../../../../../entities/process-definition-edge-entity';
import {type ProcessNodeEntity} from '../../../../../entities/process-node-entity';
import {type ProcessNodeProvider} from '../../../../../services/process-node-provider-api-service';
import {
    DEFAULT_FLOW_EDGE_TYPE,
    DEFAULT_FLOW_NODE_TYPE,
    NODE_HEIGHT,
    NODE_WIDTH,
    PROCESS_FLOW_EDGE_Z_INDEX,
    PROCESS_FLOW_NODE_Z_INDEX,
} from '../data/process-flow-constants';
import {
    buildProcessFlowGraph,
    type ProcessFlowGraph,
    type ProcessFlowGraphEdge,
    type ProcessFlowGraphNode,
} from './process-flow-graph-utils';

export type FlowNode = ReactFlowNode<{
    graphNode: ProcessFlowGraphNode;
}>;

export type FlowEdge = ReactFlowEdge<{
    graphEdge: ProcessFlowGraphEdge;
}>;

type NodeMeasurementMap = Map<string, {
    width?: number;
    height?: number;
}>;

export function getFlowNodeWidth(provider: ProcessNodeProvider): number {
    return Math.max(NODE_WIDTH * 2, NODE_WIDTH * (provider.ports.length + 1));
}

export function createNodeMeasurementMap(flowNodes: FlowNode[]): NodeMeasurementMap {
    return new Map(flowNodes.map((node) => [
        node.id,
        {
            width: node.measured?.width,
            height: node.measured?.height,
        },
    ]));
}

export function layoutElements(
    nodes: ProcessNodeEntity[],
    edges: ProcessDefinitionEdgeEntity[],
    providers: ProcessNodeProvider[],
    nodeMeasurements: NodeMeasurementMap = new Map(),
): {
    flowNodes: FlowNode[];
    flowEdges: FlowEdge[];
} {
    const graph = buildProcessFlowGraph(nodes, edges, providers);
    const {
        flowNodes,
        flowEdges,
    } = transformGraph(graph, nodeMeasurements);

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

    flowNodes.forEach((node) => {
        g.setNode(node.id, {
            ...node,
            width: node.measured?.width ?? getFlowNodeWidth(node.data.graphNode.provider),
            height: node.measured?.height ?? NODE_HEIGHT,
        });
    });

    layout(g);

    const positionedFlowNodes = flowNodes
        .map((node) => {
            const position = g.node(node.id);

            const width = node.measured?.width ?? getFlowNodeWidth(node.data.graphNode.provider);
            const height = node.measured?.height ?? NODE_HEIGHT;

            const x = position.x - (width / 2);
            const y = position.y - (height / 2);

            return {
                ...node,
                measured: {
                    width,
                    height,
                },
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

function transformGraph(
    graph: ProcessFlowGraph,
    nodeMeasurements: NodeMeasurementMap,
): {
    flowNodes: FlowNode[];
    flowEdges: FlowEdge[];
} {
    const flowNodes = graph.nodes.map<FlowNode>((graphNode) => {
        const measurement = nodeMeasurements.get(String(graphNode.node.id));
        const measuredWidth = measurement?.width;
        const measuredHeight = measurement?.height;

        return {
            id: String(graphNode.node.id),
            type: DEFAULT_FLOW_NODE_TYPE,
            position: {
                x: 0,
                y: 0,
            },
            measured: {
                width: measuredWidth != null && measuredWidth > 0 ? measuredWidth : undefined,
                height: measuredHeight != null && measuredHeight > 0 ? measuredHeight : undefined,
            },
            zIndex: PROCESS_FLOW_NODE_Z_INDEX,
            data: {
                graphNode,
            },
        };
    });

    const flowEdges = graph.edges.map<FlowEdge>((graphEdge) => ({
        id: String(graphEdge.edge.id),
        sourceHandle: graphEdge.edge.viaPort,
        source: String(graphEdge.edge.fromNodeId),
        target: String(graphEdge.edge.toNodeId),
        markerEnd: {
            type: MarkerType.ArrowClosed,
        },
        type: DEFAULT_FLOW_EDGE_TYPE,
        zIndex: PROCESS_FLOW_EDGE_Z_INDEX,
        data: {
            graphEdge,
        },
    }));

    return {
        flowNodes,
        flowEdges,
    };
}
