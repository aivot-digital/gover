import ELK, {type ElkEdgeSection, type ElkExtendedEdge, type ElkNode, type ElkPoint, type ElkPort} from 'elkjs/lib/elk.bundled.js';
import {type Edge as ReactFlowEdge, MarkerType, type Node as ReactFlowNode} from '@xyflow/react';
import {type ProcessDefinitionEdgeEntity} from '../../../../../entities/process-definition-edge-entity';
import {type ProcessNodeEntity} from '../../../../../entities/process-node-entity';
import {type ProcessNodeProvider, ProcessNodeType} from '../../../../../services/process-node-provider-api-service';
import {
    DEFAULT_FLOW_EDGE_TYPE,
    DEFAULT_FLOW_NODE_TYPE,
    MIN_NODE_WIDTH,
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

const elk = new ELK();

const ELK_TARGET_PORT_KEY = '__target__';
const INCLUDED_PORTS_IN_MIN_WIDTH = 2;
const ADDITIONAL_PORT_WIDTH = 72;
export const FLOW_HORIZONTAL_NODE_SPACING = 100;
const ELK_LAYOUT_OPTIONS = {
    'elk.algorithm': 'layered',
    'elk.direction': 'DOWN',
    'elk.edgeRouting': 'ORTHOGONAL',
    'elk.padding': '[top=32,left=32,bottom=32,right=32]',
    'elk.spacing.nodeNode': String(FLOW_HORIZONTAL_NODE_SPACING),
    'org.eclipse.elk.spacing.componentComponent': String(FLOW_HORIZONTAL_NODE_SPACING),
    'elk.layered.spacing.nodeNodeBetweenLayers': '52',
    'elk.layered.considerModelOrder.strategy': 'PREFER_EDGES',
    'elk.layered.considerModelOrder.longEdgeStrategy': 'DUMMY_NODE_UNDER',
    'elk.layered.nodePlacement.strategy': 'BRANDES_KOEPF',
    'elk.layered.nodePlacement.bk.fixedAlignment': 'BALANCED',
    'elk.layered.nodePlacement.favorStraightEdges': 'true',
} as const;
const ELK_NODE_LAYOUT_OPTIONS = {
    'org.eclipse.elk.portConstraints': 'FIXED_POS',
} as const;

export interface FlowPathPoint {
    x: number;
    y: number;
}

export type FlowNode = ReactFlowNode<{
    graphNode: ProcessFlowGraphNode;
}>;

export type FlowEdge = ReactFlowEdge<{
    graphEdge: ProcessFlowGraphEdge;
    routePoints: FlowPathPoint[];
}>;

type NodeMeasurementMap = Map<string, {
    width?: number;
    height?: number;
}>;

interface LayoutNodeMeta {
    graphNode: ProcessFlowGraphNode;
    width: number;
    height: number;
}

interface ResolvedLayoutNode {
    x: number;
    y: number;
    width: number;
    height: number;
}

export function getFlowNodeWidth(provider: ProcessNodeProvider): number {
    return MIN_NODE_WIDTH + (Math.max(provider.ports.length - INCLUDED_PORTS_IN_MIN_WIDTH, 0) * ADDITIONAL_PORT_WIDTH);
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

export async function layoutElements(
    nodes: ProcessNodeEntity[],
    edges: ProcessDefinitionEdgeEntity[],
    providers: ProcessNodeProvider[],
    nodeMeasurements: NodeMeasurementMap = new Map(),
): Promise<{
    flowNodes: FlowNode[];
    flowEdges: FlowEdge[];
}> {
    const graph = buildProcessFlowGraph(nodes, edges, providers);
    const layoutMetaByNodeId = createLayoutMeta(graph, nodeMeasurements);
    const elkGraph = createElkGraph(graph, layoutMetaByNodeId);
    const laidOutGraph = await elk.layout(elkGraph);
    const resolvedLayoutNodes = createResolvedLayoutNodeMap(laidOutGraph, layoutMetaByNodeId);

    return {
        flowNodes: transformNodes(graph, resolvedLayoutNodes),
        flowEdges: transformEdges(graph, laidOutGraph.edges ?? []),
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
            },
        ];
    }));
}

function createElkGraph(
    graph: ProcessFlowGraph,
    layoutMetaByNodeId: Map<number, LayoutNodeMeta>,
): ElkNode {
    return {
        id: 'process-flow-root',
        layoutOptions: ELK_LAYOUT_OPTIONS,
        children: graph.nodes.map((graphNode) => {
            const layoutNode = layoutMetaByNodeId.get(graphNode.node.id);
            if (layoutNode == null) {
                throw new Error(`Missing layout meta for node ${graphNode.node.id}`);
            }

            return createElkNode(graphNode, layoutNode);
        }),
        edges: graph.edges.map((graphEdge) => ({
            id: String(graphEdge.edge.id),
            sources: [createSourcePortId(graphEdge.edge.fromNodeId, graphEdge.edge.viaPort)],
            targets: [createTargetPortId(graphEdge.edge.toNodeId)],
        })),
    };
}

function createElkNode(graphNode: ProcessFlowGraphNode, layoutNode: LayoutNodeMeta): ElkNode {
    const sourcePorts = graphNode.provider.ports.map<ElkPort>((port, index) => ({
        id: createSourcePortId(graphNode.node.id, port.key),
        x: getSourcePortX(layoutNode.width, graphNode.provider.ports.length, index),
        y: layoutNode.height,
        width: 0,
        height: 0,
        layoutOptions: {
            'org.eclipse.elk.port.side': 'SOUTH',
        },
    }));

    return {
        id: String(graphNode.node.id),
        width: layoutNode.width,
        height: layoutNode.height,
        layoutOptions: getElkNodeLayoutOptions(graphNode),
        ports: [
            {
                id: createTargetPortId(graphNode.node.id),
                x: layoutNode.width / 2,
                y: 0,
                width: 0,
                height: 0,
                layoutOptions: {
                    'org.eclipse.elk.port.side': 'NORTH',
                },
            },
            ...sourcePorts,
        ],
    };
}

function getElkNodeLayoutOptions(graphNode: ProcessFlowGraphNode): Record<string, string> {
    return {
        ...ELK_NODE_LAYOUT_OPTIONS,
        ...(graphNode.provider.type === ProcessNodeType.Trigger ? {
            'org.eclipse.elk.layered.layering.layerConstraint': 'FIRST_SEPARATE',
        } : {}),
    };
}

function createResolvedLayoutNodeMap(
    laidOutGraph: ElkNode,
    layoutMetaByNodeId: Map<number, LayoutNodeMeta>,
): Map<number, ResolvedLayoutNode> {
    const resolvedLayoutNodeMap = new Map<number, ResolvedLayoutNode>();

    for (const child of laidOutGraph.children ?? []) {
        const nodeId = Number.parseInt(child.id, 10);
        if (Number.isNaN(nodeId)) {
            continue;
        }

        const fallbackLayoutNode = layoutMetaByNodeId.get(nodeId);
        resolvedLayoutNodeMap.set(nodeId, {
            x: child.x ?? 0,
            y: child.y ?? 0,
            width: child.width ?? fallbackLayoutNode?.width ?? NODE_WIDTH,
            height: child.height ?? fallbackLayoutNode?.height ?? NODE_HEIGHT,
        });
    }

    layoutMetaByNodeId.forEach((layoutNode, nodeId) => {
        if (!resolvedLayoutNodeMap.has(nodeId)) {
            resolvedLayoutNodeMap.set(nodeId, {
                x: 0,
                y: 0,
                width: layoutNode.width,
                height: layoutNode.height,
            });
        }
    });

    return resolvedLayoutNodeMap;
}

function transformNodes(
    graph: ProcessFlowGraph,
    resolvedLayoutNodes: Map<number, ResolvedLayoutNode>,
): FlowNode[] {
    return graph.nodes.map<FlowNode>((graphNode) => {
        const resolvedLayoutNode = resolvedLayoutNodes.get(graphNode.node.id);
        if (resolvedLayoutNode == null) {
            throw new Error(`Missing resolved layout for node ${graphNode.node.id}`);
        }

        return {
            id: String(graphNode.node.id),
            type: DEFAULT_FLOW_NODE_TYPE,
            position: {
                x: resolvedLayoutNode.x,
                y: resolvedLayoutNode.y,
            },
            measured: {
                width: resolvedLayoutNode.width,
                height: resolvedLayoutNode.height,
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
    elkEdges: ElkExtendedEdge[],
): FlowEdge[] {
    const routePointsByEdgeId = new Map<number, FlowPathPoint[]>();

    for (const elkEdge of elkEdges) {
        const edgeId = Number.parseInt(elkEdge.id ?? '', 10);
        if (Number.isNaN(edgeId)) {
            continue;
        }

        routePointsByEdgeId.set(edgeId, extractEdgeRoutePoints(elkEdge));
    }

    return graph.edges.map<FlowEdge>((graphEdge) => ({
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
            routePoints: routePointsByEdgeId.get(graphEdge.edge.id) ?? [],
        },
    }));
}

function extractEdgeRoutePoints(edge: ElkExtendedEdge): FlowPathPoint[] {
    const orderedSections = getOrderedSections(edge.sections ?? []);
    const points: FlowPathPoint[] = [];

    for (const section of orderedSections) {
        appendUniquePoint(points, section.startPoint);
        for (const bendPoint of section.bendPoints ?? []) {
            appendUniquePoint(points, bendPoint);
        }
        appendUniquePoint(points, section.endPoint);
    }

    return points;
}

function getOrderedSections(sections: ElkEdgeSection[]): ElkEdgeSection[] {
    if (sections.length <= 1) {
        return sections;
    }

    const sectionById = new Map(sections.map((section) => [section.id, section]));
    const orderedSections: ElkEdgeSection[] = [];
    const seenSectionIds = new Set<string>();
    let currentSection: ElkEdgeSection | null = sections.find((section) => (section.incomingSections?.length ?? 0) === 0) ?? sections[0] ?? null;

    while (currentSection != null && !seenSectionIds.has(currentSection.id)) {
        orderedSections.push(currentSection);
        seenSectionIds.add(currentSection.id);

        const nextSectionId: string | undefined = currentSection.outgoingSections?.find((sectionId) => (
            sectionById.has(sectionId) &&
            !seenSectionIds.has(sectionId)
        ));

        currentSection = nextSectionId == null ? null : sectionById.get(nextSectionId) ?? null;
    }

    if (orderedSections.length === sections.length) {
        return orderedSections;
    }

    return [
        ...orderedSections,
        ...sections.filter((section) => !seenSectionIds.has(section.id)),
    ];
}

function appendUniquePoint(points: FlowPathPoint[], point: ElkPoint): void {
    const nextPoint = {
        x: roundCoordinate(point.x),
        y: roundCoordinate(point.y),
    };
    const previousPoint = points[points.length - 1];

    if (previousPoint != null && previousPoint.x === nextPoint.x && previousPoint.y === nextPoint.y) {
        return;
    }

    points.push(nextPoint);
}

function roundCoordinate(value: number): number {
    return Math.round(value * 100) / 100;
}

function createSourcePortId(nodeId: number, portKey: string): string {
    return `${nodeId}:${portKey}`;
}

function createTargetPortId(nodeId: number): string {
    return `${nodeId}:${ELK_TARGET_PORT_KEY}`;
}

function getSourcePortX(nodeWidth: number, portCount: number, portIndex: number): number {
    if (portCount <= 0) {
        return nodeWidth / 2;
    }

    return ((portIndex + 0.5) * nodeWidth) / portCount;
}
