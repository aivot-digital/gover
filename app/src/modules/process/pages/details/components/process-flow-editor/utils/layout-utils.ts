import {type Edge as ReactFlowEdge, MarkerType, type Node as ReactFlowNode} from '@xyflow/react';
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

const FLOW_ROW_GAP = 112;
const FLOW_NODE_GAP = 64;
const FLOW_ROOT_GAP = 128;
const FLOW_RAIL_GUTTER = 112;
const FLOW_RAIL_SPACING = 40;
const FLOW_LAYOUT_PASSES = 4;

export interface FlowEdgeRoute {
    kind: 'direct' | 'rail';
    railX?: number;
}

export type FlowNode = ReactFlowNode<{
    graphNode: ProcessFlowGraphNode;
}>;

export type FlowEdge = ReactFlowEdge<{
    graphEdge: ProcessFlowGraphEdge;
    route: FlowEdgeRoute;
}>;

type NodeMeasurementMap = Map<string, {
    width?: number;
    height?: number;
}>;

interface LayoutNodeMeta {
    graphNode: ProcessFlowGraphNode;
    width: number;
    height: number;
    depth: number;
    centerX: number;
    x: number;
    y: number;
    subtreeWidth: number;
    structuralParentId: number | null;
    structuralChildrenIds: number[];
}

interface GraphBounds {
    minLeft: number;
    maxRight: number;
    centerX: number;
}

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
    const layoutMetaByNodeId = createLayoutMeta(graph, nodeMeasurements);

    assignNodeDepths(layoutMetaByNodeId);
    assignStructuralParents(layoutMetaByNodeId);
    computeSubtreeWidths(layoutMetaByNodeId);
    assignInitialHorizontalPositions(layoutMetaByNodeId);
    relaxRows(layoutMetaByNodeId);
    assignVerticalPositions(layoutMetaByNodeId);
    normalizeHorizontalPositions(layoutMetaByNodeId);

    const graphBounds = getGraphBounds(layoutMetaByNodeId);

    return {
        flowNodes: transformNodes(graph, layoutMetaByNodeId),
        flowEdges: transformEdges(graph, layoutMetaByNodeId, graphBounds),
    };
}

function createLayoutMeta(
    graph: ProcessFlowGraph,
    nodeMeasurements: NodeMeasurementMap,
): Map<number, LayoutNodeMeta> {
    return new Map(graph.nodes.map((graphNode) => {
        const measurement = nodeMeasurements.get(String(graphNode.node.id));
        const measuredWidth = measurement?.width;
        const measuredHeight = measurement?.height;

        return [
            graphNode.node.id,
            {
                graphNode,
                width: measuredWidth != null && measuredWidth > 0 ? measuredWidth : getFlowNodeWidth(graphNode.provider),
                height: measuredHeight != null && measuredHeight > 0 ? measuredHeight : NODE_HEIGHT,
                depth: 0,
                centerX: 0,
                x: 0,
                y: 0,
                subtreeWidth: 0,
                structuralParentId: null,
                structuralChildrenIds: [],
            },
        ];
    }));
}

function assignNodeDepths(layoutMetaByNodeId: Map<number, LayoutNodeMeta>): void {
    const allNodes = Array.from(layoutMetaByNodeId.values())
        .sort((a, b) => a.graphNode.node.id - b.graphNode.node.id);

    const queue = allNodes
        .filter((layoutNode) => layoutNode.graphNode.incomingEdges.length === 0);

    if (queue.length === 0 && allNodes.length > 0) {
        queue.push(allNodes[0]);
    }

    const seenNodeIds = new Set<number>();

    queue.forEach((layoutNode) => {
        layoutNode.depth = 0;
        seenNodeIds.add(layoutNode.graphNode.node.id);
    });

    while (queue.length > 0) {
        const currentNode = queue.shift();

        if (currentNode == null) {
            continue;
        }

        for (const outgoingEdge of currentNode.graphNode.outgoingEdges) {
            const targetNode = layoutMetaByNodeId.get(outgoingEdge.edge.toNodeId);
            if (targetNode == null) {
                continue;
            }

            const nextDepth = currentNode.depth + 1;
            if (!seenNodeIds.has(targetNode.graphNode.node.id)) {
                targetNode.depth = nextDepth;
                seenNodeIds.add(targetNode.graphNode.node.id);
                queue.push(targetNode);
                continue;
            }

            if (nextDepth < targetNode.depth) {
                targetNode.depth = nextDepth;
                queue.push(targetNode);
            }
        }
    }

    let fallbackDepth = allNodes.reduce((maxDepth, layoutNode) => Math.max(maxDepth, layoutNode.depth), 0) + 1;
    allNodes.forEach((layoutNode) => {
        if (!seenNodeIds.has(layoutNode.graphNode.node.id)) {
            layoutNode.depth = fallbackDepth;
            fallbackDepth += 1;
        }
    });
}

function assignStructuralParents(layoutMetaByNodeId: Map<number, LayoutNodeMeta>): void {
    layoutMetaByNodeId.forEach((layoutNode) => {
        layoutNode.structuralParentId = null;
        layoutNode.structuralChildrenIds = [];
    });

    const orderedNodes = Array.from(layoutMetaByNodeId.values())
        .sort((a, b) => (
            a.depth - b.depth ||
            a.graphNode.node.id - b.graphNode.node.id
        ));

    orderedNodes.forEach((layoutNode) => {
        const parentCandidate = selectStructuralParent(layoutNode, layoutMetaByNodeId);

        if (parentCandidate == null) {
            return;
        }

        layoutNode.structuralParentId = parentCandidate.graphNode.node.id;
        parentCandidate.structuralChildrenIds.push(layoutNode.graphNode.node.id);
    });

    layoutMetaByNodeId.forEach((layoutNode) => {
        if (layoutNode.structuralChildrenIds.length === 0) {
            return;
        }

        const outgoingOrder = new Map<number, number>();
        layoutNode.graphNode.outgoingEdges.forEach((outgoingEdge, index) => {
            outgoingOrder.set(outgoingEdge.edge.toNodeId, index);
        });

        layoutNode.structuralChildrenIds.sort((a, b) => (
            (outgoingOrder.get(a) ?? Number.MAX_SAFE_INTEGER) - (outgoingOrder.get(b) ?? Number.MAX_SAFE_INTEGER) ||
            a - b
        ));
    });
}

function selectStructuralParent(
    layoutNode: LayoutNodeMeta,
    layoutMetaByNodeId: Map<number, LayoutNodeMeta>,
): LayoutNodeMeta | null {
    const directParents = layoutNode.graphNode.incomingEdges
        .map((incomingEdge) => layoutMetaByNodeId.get(incomingEdge.edge.fromNodeId))
        .filter((parentNode): parentNode is LayoutNodeMeta => parentNode != null && parentNode.depth === layoutNode.depth - 1)
        .sort((a, b) => (
            b.depth - a.depth ||
            a.graphNode.node.id - b.graphNode.node.id
        ));

    if (directParents.length > 0) {
        return directParents[0];
    }

    const anyForwardParent = layoutNode.graphNode.incomingEdges
        .map((incomingEdge) => layoutMetaByNodeId.get(incomingEdge.edge.fromNodeId))
        .filter((parentNode): parentNode is LayoutNodeMeta => parentNode != null && parentNode.depth < layoutNode.depth)
        .sort((a, b) => (
            b.depth - a.depth ||
            a.graphNode.node.id - b.graphNode.node.id
        ));

    return anyForwardParent[0] ?? null;
}

function computeSubtreeWidths(layoutMetaByNodeId: Map<number, LayoutNodeMeta>): void {
    const subtreeWidthByNodeId = new Map<number, number>();

    const measureSubtree = (nodeId: number): number => {
        const layoutNode = layoutMetaByNodeId.get(nodeId);
        if (layoutNode == null) {
            return 0;
        }

        const cachedWidth = subtreeWidthByNodeId.get(nodeId);
        if (cachedWidth != null) {
            return cachedWidth;
        }

        const structuralChildren = layoutNode.structuralChildrenIds
            .map((childNodeId) => layoutMetaByNodeId.get(childNodeId))
            .filter((childNode): childNode is LayoutNodeMeta => childNode != null);

        const childrenWidth = structuralChildren.reduce((sum, childNode, index) => (
            sum + measureSubtree(childNode.graphNode.node.id) + (index === 0 ? 0 : FLOW_NODE_GAP)
        ), 0);

        const subtreeWidth = Math.max(layoutNode.width, childrenWidth);
        layoutNode.subtreeWidth = subtreeWidth;
        subtreeWidthByNodeId.set(nodeId, subtreeWidth);
        return subtreeWidth;
    };

    layoutMetaByNodeId.forEach((layoutNode) => {
        measureSubtree(layoutNode.graphNode.node.id);
    });
}

function assignInitialHorizontalPositions(layoutMetaByNodeId: Map<number, LayoutNodeMeta>): void {
    const roots = Array.from(layoutMetaByNodeId.values())
        .filter((layoutNode) => layoutNode.structuralParentId == null)
        .sort((a, b) => (
            a.depth - b.depth ||
            a.graphNode.node.id - b.graphNode.node.id
        ));

    let cursorX = 0;

    roots.forEach((rootNode, index) => {
        assignSubtreePosition(rootNode.graphNode.node.id, cursorX, layoutMetaByNodeId);
        cursorX += rootNode.subtreeWidth + (index === roots.length - 1 ? 0 : FLOW_ROOT_GAP);
    });
}

function assignSubtreePosition(
    nodeId: number,
    startX: number,
    layoutMetaByNodeId: Map<number, LayoutNodeMeta>,
): void {
    const layoutNode = layoutMetaByNodeId.get(nodeId);
    if (layoutNode == null) {
        return;
    }

    const structuralChildren = layoutNode.structuralChildrenIds
        .map((childNodeId) => layoutMetaByNodeId.get(childNodeId))
        .filter((childNode): childNode is LayoutNodeMeta => childNode != null);

    if (structuralChildren.length === 0) {
        layoutNode.centerX = startX + (layoutNode.subtreeWidth / 2);
        return;
    }

    const childrenWidth = structuralChildren.reduce((sum, childNode, index) => (
        sum + childNode.subtreeWidth + (index === 0 ? 0 : FLOW_NODE_GAP)
    ), 0);
    let childCursorX = startX + ((layoutNode.subtreeWidth - childrenWidth) / 2);

    structuralChildren.forEach((childNode, index) => {
        assignSubtreePosition(childNode.graphNode.node.id, childCursorX, layoutMetaByNodeId);
        childCursorX += childNode.subtreeWidth + (index === structuralChildren.length - 1 ? 0 : FLOW_NODE_GAP);
    });

    layoutNode.centerX = startX + (layoutNode.subtreeWidth / 2);
}

function relaxRows(layoutMetaByNodeId: Map<number, LayoutNodeMeta>): void {
    const rowsByDepth = groupRowsByDepth(layoutMetaByNodeId);
    const maxDepth = Math.max(...Array.from(rowsByDepth.keys()), 0);

    for (let pass = 0; pass < FLOW_LAYOUT_PASSES; pass += 1) {
        for (let depth = 1; depth <= maxDepth; depth += 1) {
            const rowNodes = rowsByDepth.get(depth);
            if (rowNodes == null) {
                continue;
            }

            relaxRow(rowNodes, layoutMetaByNodeId, 'incoming');
        }

        for (let depth = maxDepth - 1; depth >= 0; depth -= 1) {
            const rowNodes = rowsByDepth.get(depth);
            if (rowNodes == null) {
                continue;
            }

            relaxRow(rowNodes, layoutMetaByNodeId, 'outgoing');
        }
    }
}

function relaxRow(
    rowNodes: LayoutNodeMeta[],
    layoutMetaByNodeId: Map<number, LayoutNodeMeta>,
    direction: 'incoming' | 'outgoing',
): void {
    const orderedNodes = [...rowNodes]
        .sort((a, b) => a.centerX - b.centerX || a.graphNode.node.id - b.graphNode.node.id);

    const targetCenters = orderedNodes.map((layoutNode) => {
        const neighboringCenters = direction === 'incoming'
            ? getDirectIncomingCenters(layoutNode, layoutMetaByNodeId)
            : getDirectOutgoingCenters(layoutNode, layoutMetaByNodeId);

        if (neighboringCenters.length === 0) {
            return layoutNode.centerX;
        }

        return (layoutNode.centerX * 0.35) + (average(neighboringCenters) * 0.65);
    });

    const adjustedCenters: number[] = [];

    orderedNodes.forEach((layoutNode, index) => {
        if (index === 0) {
            adjustedCenters.push(targetCenters[index]);
            return;
        }

        const previousNode = orderedNodes[index - 1];
        const minimumCenter = adjustedCenters[index - 1] + (previousNode.width / 2) + (layoutNode.width / 2) + FLOW_NODE_GAP;
        adjustedCenters.push(Math.max(targetCenters[index], minimumCenter));
    });

    for (let index = adjustedCenters.length - 2; index >= 0; index -= 1) {
        const currentNode = orderedNodes[index];
        const nextNode = orderedNodes[index + 1];
        const maximumCenter = adjustedCenters[index + 1] - (currentNode.width / 2) - (nextNode.width / 2) - FLOW_NODE_GAP;
        adjustedCenters[index] = Math.min(adjustedCenters[index], maximumCenter);
    }

    const shift = average(targetCenters) - average(adjustedCenters);
    orderedNodes.forEach((layoutNode, index) => {
        layoutNode.centerX = adjustedCenters[index] + shift;
    });
}

function getDirectIncomingCenters(
    layoutNode: LayoutNodeMeta,
    layoutMetaByNodeId: Map<number, LayoutNodeMeta>,
): number[] {
    return layoutNode.graphNode.incomingEdges
        .map((incomingEdge) => layoutMetaByNodeId.get(incomingEdge.edge.fromNodeId))
        .filter((parentNode): parentNode is LayoutNodeMeta => parentNode != null && parentNode.depth === layoutNode.depth - 1)
        .map((parentNode) => parentNode.centerX);
}

function getDirectOutgoingCenters(
    layoutNode: LayoutNodeMeta,
    layoutMetaByNodeId: Map<number, LayoutNodeMeta>,
): number[] {
    return layoutNode.graphNode.outgoingEdges
        .map((outgoingEdge) => layoutMetaByNodeId.get(outgoingEdge.edge.toNodeId))
        .filter((childNode): childNode is LayoutNodeMeta => childNode != null && childNode.depth === layoutNode.depth + 1)
        .map((childNode) => childNode.centerX);
}

function assignVerticalPositions(layoutMetaByNodeId: Map<number, LayoutNodeMeta>): void {
    const rowsByDepth = groupRowsByDepth(layoutMetaByNodeId);
    const orderedDepths = Array.from(rowsByDepth.keys())
        .sort((a, b) => a - b);

    let currentY = 0;

    orderedDepths.forEach((depth) => {
        const rowNodes = rowsByDepth.get(depth) ?? [];
        const rowHeight = rowNodes.reduce((maxHeight, layoutNode) => Math.max(maxHeight, layoutNode.height), 0);

        rowNodes.forEach((layoutNode) => {
            layoutNode.y = currentY;
            layoutNode.x = layoutNode.centerX - (layoutNode.width / 2);
        });

        currentY += rowHeight + FLOW_ROW_GAP;
    });
}

function normalizeHorizontalPositions(layoutMetaByNodeId: Map<number, LayoutNodeMeta>): void {
    const graphBounds = getGraphBounds(layoutMetaByNodeId);
    const shiftX = -graphBounds.centerX;

    layoutMetaByNodeId.forEach((layoutNode) => {
        layoutNode.centerX += shiftX;
        layoutNode.x = layoutNode.centerX - (layoutNode.width / 2);
    });
}

function transformNodes(
    graph: ProcessFlowGraph,
    layoutMetaByNodeId: Map<number, LayoutNodeMeta>,
): FlowNode[] {
    return graph.nodes.map<FlowNode>((graphNode) => {
        const layoutNode = layoutMetaByNodeId.get(graphNode.node.id);
        if (layoutNode == null) {
            throw new Error(`Missing layout meta for node ${graphNode.node.id}`);
        }

        return {
            id: String(graphNode.node.id),
            type: DEFAULT_FLOW_NODE_TYPE,
            position: {
                x: layoutNode.x,
                y: layoutNode.y,
            },
            measured: {
                width: layoutNode.width,
                height: layoutNode.height,
            },
            zIndex: PROCESS_FLOW_NODE_Z_INDEX,
            data: {
                graphNode,
            },
        };
    });
}

function transformEdges(
    graph: ProcessFlowGraph,
    layoutMetaByNodeId: Map<number, LayoutNodeMeta>,
    graphBounds: GraphBounds,
): FlowEdge[] {
    const railRouteByEdgeId = createRailRouteMap(graph, layoutMetaByNodeId, graphBounds);

    return graph.edges.map<FlowEdge>((graphEdge) => {
        const sourceNode = layoutMetaByNodeId.get(graphEdge.edge.fromNodeId);
        const targetNode = layoutMetaByNodeId.get(graphEdge.edge.toNodeId);

        if (sourceNode == null || targetNode == null) {
            throw new Error(`Missing source or target layout meta for edge ${graphEdge.edge.id}`);
        }

        const route = railRouteByEdgeId.get(graphEdge.edge.id) ?? createDirectRoute(sourceNode, targetNode);

        return {
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
                route,
            },
        };
    });
}

function createRailRouteMap(
    graph: ProcessFlowGraph,
    layoutMetaByNodeId: Map<number, LayoutNodeMeta>,
    graphBounds: GraphBounds,
): Map<number, FlowEdgeRoute> {
    const railCandidates = graph.edges
        .map((graphEdge) => {
            const sourceNode = layoutMetaByNodeId.get(graphEdge.edge.fromNodeId);
            const targetNode = layoutMetaByNodeId.get(graphEdge.edge.toNodeId);

            if (sourceNode == null || targetNode == null) {
                return null;
            }

            const depthDifference = targetNode.depth - sourceNode.depth;
            if (depthDifference === 1) {
                return null;
            }

            return {
                graphEdge,
                sourceNode,
                targetNode,
                side: getRailSide(sourceNode, targetNode, graphBounds.centerX),
                span: Math.abs(depthDifference),
            };
        })
        .filter((railCandidate): railCandidate is {
            graphEdge: ProcessFlowGraphEdge;
            sourceNode: LayoutNodeMeta;
            targetNode: LayoutNodeMeta;
            side: 'left' | 'right';
            span: number;
        } => railCandidate != null)
        .sort((a, b) => (
            a.side.localeCompare(b.side) ||
            a.span - b.span ||
            a.graphEdge.edge.id - b.graphEdge.edge.id
        ));

    const railCountBySide = new Map<'left' | 'right', number>([
        ['left', 0],
        ['right', 0],
    ]);
    const railRouteByEdgeId = new Map<number, FlowEdgeRoute>();

    railCandidates.forEach((railCandidate) => {
        const railIndex = railCountBySide.get(railCandidate.side) ?? 0;
        railCountBySide.set(railCandidate.side, railIndex + 1);

        const railX = railCandidate.side === 'right'
            ? graphBounds.maxRight + FLOW_RAIL_GUTTER + (railIndex * FLOW_RAIL_SPACING)
            : graphBounds.minLeft - FLOW_RAIL_GUTTER - (railIndex * FLOW_RAIL_SPACING);

        railRouteByEdgeId.set(railCandidate.graphEdge.edge.id, {
            kind: 'rail',
            railX,
        });
    });

    return railRouteByEdgeId;
}

function createDirectRoute(sourceNode: LayoutNodeMeta, targetNode: LayoutNodeMeta): FlowEdgeRoute {
    return {
        kind: 'direct',
    };
}

function getRailSide(
    sourceNode: LayoutNodeMeta,
    targetNode: LayoutNodeMeta,
    graphCenterX: number,
): 'left' | 'right' {
    if (sourceNode.centerX <= graphCenterX && targetNode.centerX <= graphCenterX) {
        return 'left';
    }

    if (sourceNode.centerX >= graphCenterX && targetNode.centerX >= graphCenterX) {
        return 'right';
    }

    return sourceNode.centerX <= targetNode.centerX ? 'right' : 'left';
}

function groupRowsByDepth(layoutMetaByNodeId: Map<number, LayoutNodeMeta>): Map<number, LayoutNodeMeta[]> {
    const rowsByDepth = new Map<number, LayoutNodeMeta[]>();

    layoutMetaByNodeId.forEach((layoutNode) => {
        const rowNodes = rowsByDepth.get(layoutNode.depth) ?? [];
        rowNodes.push(layoutNode);
        rowsByDepth.set(layoutNode.depth, rowNodes);
    });

    return rowsByDepth;
}

function getGraphBounds(layoutMetaByNodeId: Map<number, LayoutNodeMeta>): GraphBounds {
    const nodes = Array.from(layoutMetaByNodeId.values());
    const minLeft = Math.min(...nodes.map((layoutNode) => layoutNode.centerX - (layoutNode.width / 2)));
    const maxRight = Math.max(...nodes.map((layoutNode) => layoutNode.centerX + (layoutNode.width / 2)));

    return {
        minLeft,
        maxRight,
        centerX: (minLeft + maxRight) / 2,
    };
}

function average(values: number[]): number {
    if (values.length === 0) {
        return 0;
    }

    return values.reduce((sum, value) => sum + value, 0) / values.length;
}
