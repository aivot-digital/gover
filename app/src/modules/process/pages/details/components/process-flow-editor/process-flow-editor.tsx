import {type ProcessFlow} from '../../process-details-page';
import {type ProcessNodeProvider} from '../../../../services/process-node-provider-api-service';
import React, {type ReactNode, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import '@xyflow/react/dist/style.css';
import './process-flow-editor.css';
import {
    Background,
    BackgroundVariant,
    ControlButton,
    Controls,
    MiniMap,
    type NodeChange,
    ReactFlow,
    useEdgesState,
    useNodesInitialized,
    useNodesState,
    useReactFlow,
    useStore,
} from '@xyflow/react';
import {type ProcessNodeEntity} from '../../../../entities/process-node-entity';
import {ProcessFlowEditorNode} from './process-flow-editor-node';
import {ProcessFlowEditorEdge} from './process-flow-editor-edge';
import {ProcessFlowEditorProvider} from './process-flow-editor-context';
import {DEFAULT_FLOW_EDGE_TYPE, DEFAULT_FLOW_NODE_TYPE} from './data/process-flow-constants';
import {
    createNodeMeasurementMap,
    type FlowEdge,
    type FlowNode,
    layoutElements,
} from './utils/layout-utils';
import {Box, Tooltip} from '@mui/material';
import {type ProcessInstanceEntity} from '../../../../entities/process-instance-entity';
import {type ProcessInstanceTaskEntity} from '../../../../entities/process-instance-task-entity';
import {type ProcessInstanceEventEntity} from '../../../../entities/process-instance-event-entity';
import Add from '@mui/icons-material/Add';
import Remove from '@mui/icons-material/Remove';
import CropFree from '@mui/icons-material/CropFree';
import Lock from '@mui/icons-material/Lock';
import LockOpen from '@mui/icons-material/LockOpen';
import ViewRealSize from '@aivot/mui-material-symbols-400-outlined/dist/view-real-size/ViewRealSize';

const FLOW_MIN_ZOOM = 0.25;
const FLOW_MAX_ZOOM = 2;
const INITIAL_VIEWPORT_ZOOM = 1;
const ZOOM_EPSILON = 0.001;
const NOOP_ADD_EDGE = (_fromNodeId: number, _toNodeId: number, _viaPortKey: string): void => {
};
const NOOP_DELETE_EDGE = (_edgeId: number): void => {
};
const NOOP_DELETE_NODE = (_node: ProcessNodeEntity): void => {
};
const NOOP_ADD_FOLLOW_UP_NODE = (_fromNodeId: number, _viaPortKey: string): void => {
};
const NOOP_ADD_INBETWEEN_NODE = (_forEdgeId: number): void => {
};

interface ProcessFlowEditorProps {
    editable: boolean;

    processFlow: ProcessFlow;
    nodeProviders: ProcessNodeProvider[];

    selectedNode?: ProcessNodeEntity | null;
    onSelectNode?: (node: ProcessNodeEntity | null) => void;

    onAddEdge?: (fromNodeId: number, toNodeId: number, viaPortKey: string) => void;
    onDeleteEdge?: (edgeId: number) => void;
    onDeleteNode?: (node: ProcessNodeEntity) => void | Promise<void>;

    onAddFollowUpNode?: (fromNodeId: number, viaPortKey: string) => void;
    onAddInbetweenNode?: (forEdgeId: number) => void;

    runtimeData: {
        instance: ProcessInstanceEntity;
        tasks: ProcessInstanceTaskEntity[];
        events: ProcessInstanceEventEntity[];
    } | null;
}

const NodeTypes = {
    [DEFAULT_FLOW_NODE_TYPE]: ProcessFlowEditorNode,
};
const EdgeTypes = {
    [DEFAULT_FLOW_EDGE_TYPE]: ProcessFlowEditorEdge,
};

interface ProcessFlowEditorControlButtonProps {
    ariaLabel: string;
    children: ReactNode;
    className?: string;
    disabled?: boolean;
    onClick: () => void;
    tooltip: string;
}

function ProcessFlowEditorControlButton(props: ProcessFlowEditorControlButtonProps): ReactNode {
    const {
        ariaLabel,
        children,
        className,
        disabled = false,
        onClick,
        tooltip,
    } = props;

    return (
        <Tooltip
            title={tooltip}
            arrow
            placement="right"
        >
            <span className="process-flow-editor-control-tooltip-anchor">
                <ControlButton
                    className={className}
                    disabled={disabled}
                    onClick={onClick}
                    aria-label={ariaLabel}
                >
                    {children}
                </ControlButton>
            </span>
        </Tooltip>
    );
}

interface ProcessFlowEditorViewportControlsProps {
    fitViewOptions: {
        padding: number;
        duration: number;
    };
    isViewportLocked: boolean;
    onToggleViewportLock: () => void;
}

function ProcessFlowEditorViewportControls(props: ProcessFlowEditorViewportControlsProps): ReactNode {
    const {
        fitViewOptions,
        isViewportLocked,
        onToggleViewportLock,
    } = props;

    const {
        fitView,
        zoomIn,
        zoomOut,
        zoomTo,
    } = useReactFlow<FlowNode, FlowEdge>();
    const zoom = useStore((store) => store.transform[2]);
    const canZoomIn = zoom < FLOW_MAX_ZOOM - ZOOM_EPSILON;
    const canZoomOut = zoom > FLOW_MIN_ZOOM + ZOOM_EPSILON;

    return (
        <Controls
            className="process-flow-editor-controls"
            position="bottom-left"
            showZoom={false}
            showFitView={false}
            showInteractive={false}
        >
            <ProcessFlowEditorControlButton
                className="process-flow-editor-control-button"
                disabled={!canZoomIn}
                onClick={() => {
                    void zoomIn();
                }}
                ariaLabel="Vergrößern"
                tooltip="Vergrößern"
            >
                <Add sx={{fontSize: 20}}/>
            </ProcessFlowEditorControlButton>

            <ProcessFlowEditorControlButton
                className="process-flow-editor-control-button"
                disabled={!canZoomOut}
                onClick={() => {
                    void zoomOut();
                }}
                ariaLabel="Verkleinern"
                tooltip="Verkleinern"
            >
                <Remove sx={{fontSize: 20}}/>
            </ProcessFlowEditorControlButton>

            <ProcessFlowEditorControlButton
                className="process-flow-editor-control-button"
                onClick={() => {
                    void fitView(fitViewOptions);
                }}
                ariaLabel="Ansicht einpassen"
                tooltip="Ansicht einpassen"
            >
                <CropFree sx={{fontSize: 18}}/>
            </ProcessFlowEditorControlButton>

            <ProcessFlowEditorControlButton
                className="process-flow-editor-control-button process-flow-editor-control-button-zoom-reset"
                onClick={() => {
                    void zoomTo(1);
                }}
                ariaLabel="Zoom auf Originalgröße (100 %)"
                tooltip="Zoom auf Originalgröße (100 %)"
            >
                <ViewRealSize sx={{fontSize: 18}}/>
            </ProcessFlowEditorControlButton>

            <ProcessFlowEditorControlButton
                className="process-flow-editor-control-button"
                onClick={onToggleViewportLock}
                ariaLabel={isViewportLocked ? 'Viewport entsperren' : 'Viewport sperren'}
                tooltip={isViewportLocked ? 'Viewport entsperren' : 'Viewport sperren'}
            >
                {
                    isViewportLocked ?
                        <Lock sx={{fontSize: 18}}/> :
                        <LockOpen sx={{fontSize: 18}}/>
                }
            </ProcessFlowEditorControlButton>
        </Controls>
    );
}

export function ProcessFlowEditor(props: ProcessFlowEditorProps): ReactNode {
    const {
        editable,

        processFlow,
        nodeProviders,

        selectedNode,
        onSelectNode,

        onAddEdge,
        onDeleteEdge,
        onDeleteNode,

        onAddFollowUpNode,
        onAddInbetweenNode,

        runtimeData,
    } = props;

    const {
        fitView,
        getNodes,
        setCenter,
    } = useReactFlow<FlowNode, FlowEdge>();

    const [showTargetHandles, setShowTargetHandles] = useState<boolean>(false);
    const [needsMeasuredLayout, setNeedsMeasuredLayout] = useState<boolean>(false);
    const [pendingInitialViewport, setPendingInitialViewport] = useState<boolean>(false);
    const [isViewportLocked, setIsViewportLocked] = useState<boolean>(false);

    const [nodes, setNodes, onNodesChange] = useNodesState<FlowNode>([]);
    const [edges, setEdges, onEdgesChange] = useEdgesState<FlowEdge>([]);
    const nodesInitialized = useNodesInitialized({
        includeHiddenNodes: true,
    });
    const hasResolvedInitialViewportRef = useRef<boolean>(false);
    const initialViewportNodeIdRef = useRef<number | null>(null);
    const layoutRequestIdRef = useRef<number>(0);

    const isEditable = runtimeData == null && editable;
    const hasAllNodeProviders = useMemo(() => (
        processFlow.nodes.every((node) => (
            nodeProviders.some((provider) => (
                provider.key === node.processNodeDefinitionKey &&
                provider.majorVersion === node.processNodeDefinitionVersion
            ))
        ))
    ), [nodeProviders, processFlow.nodes]);
    const fitViewOptions = useMemo(() => ({
        padding: 0.16,
        duration: 200,
    }), []);
    const contextValue = useMemo(() => ({
        editable: isEditable,
        showTargetHandles,

        selectedNode: selectedNode ?? null,

        onAddEdge: onAddEdge ?? NOOP_ADD_EDGE,
        onDeleteEdge: onDeleteEdge ?? NOOP_DELETE_EDGE,
        onDeleteNode: onDeleteNode ?? NOOP_DELETE_NODE,

        onAddFollowUpNode: onAddFollowUpNode ?? NOOP_ADD_FOLLOW_UP_NODE,
        onAddInbetweenNode: onAddInbetweenNode ?? NOOP_ADD_INBETWEEN_NODE,

        runtimeData,
    }), [
        isEditable,
        onAddEdge,
        onAddFollowUpNode,
        onAddInbetweenNode,
        onDeleteEdge,
        onDeleteNode,
        runtimeData,
        selectedNode,
        showTargetHandles,
    ]);
    const handleToggleViewportLock = useCallback(() => {
        setIsViewportLocked((current) => !current);
    }, []);
    const focusInitialViewport = useCallback(async (): Promise<boolean> => {
        const initialViewportNodeId = initialViewportNodeIdRef.current;
        if (nodes.length === 0 || initialViewportNodeId == null) {
            return false;
        }

        const initialZoom = Math.min(INITIAL_VIEWPORT_ZOOM, FLOW_MAX_ZOOM);
        const selectedFlowNode = nodes.find((node) => node.id === String(initialViewportNodeId));
        if (selectedFlowNode == null) {
            return false;
        }

        const selectedNodeWidth = selectedFlowNode.measured?.width ?? 0;
        const selectedNodeHeight = selectedFlowNode.measured?.height ?? 0;

        await setCenter(
            selectedFlowNode.position.x + (selectedNodeWidth / 2),
            selectedFlowNode.position.y + (selectedNodeHeight / 2),
            {
                zoom: initialZoom,
                duration: 200,
            },
        );

        return true;
    }, [nodes, setCenter]);

    const resolveInitialViewportDecision = useCallback(() => {
        if (hasResolvedInitialViewportRef.current) {
            return;
        }

        hasResolvedInitialViewportRef.current = true;
        if (initialViewportNodeIdRef.current != null) {
            setPendingInitialViewport(true);
        }
    }, []);

    const layoutNodes = useCallback(async (nodeMeasurements?: ReturnType<typeof createNodeMeasurementMap>) => {
        if (!hasAllNodeProviders) {
            return;
        }

        const layoutRequestId = layoutRequestIdRef.current + 1;
        layoutRequestIdRef.current = layoutRequestId;

        try {
            const {
                flowNodes: laidOutNodes,
                flowEdges: laidOutEdges,
            } = await layoutElements(
                processFlow.nodes,
                processFlow.edges,
                nodeProviders,
                nodeMeasurements,
            );

            if (layoutRequestIdRef.current !== layoutRequestId) {
                return;
            }

            setNodes([...laidOutNodes]);
            setEdges([...laidOutEdges]);
        } catch (error) {
            if (layoutRequestIdRef.current !== layoutRequestId) {
                return;
            }

            console.error('Failed to layout process flow', error);
        }
    }, [hasAllNodeProviders, nodeProviders, processFlow.edges, processFlow.nodes, setEdges, setNodes]);

    const handleNodesChange = useCallback((changes: NodeChange<FlowNode>[]) => {
        onNodesChange(changes);

        if (changes.some((change) => change.type === 'dimensions')) {
            setNeedsMeasuredLayout(true);
        }
    }, [onNodesChange]);

    useEffect(() => {
        hasResolvedInitialViewportRef.current = false;
        initialViewportNodeIdRef.current = selectedNode?.id ?? null;
        setPendingInitialViewport(false);
    }, [processFlow.definition.id, processFlow.version.processVersion]);

    useEffect(() => {
        if (!hasAllNodeProviders) {
            setNeedsMeasuredLayout(false);
            setPendingInitialViewport(false);
            return;
        }

        const currentNodeMeasurements = createNodeMeasurementMap(getNodes());
        const hasAnyMeasuredNodes = currentNodeMeasurements.size > 0;
        const hasMeasurementsForAllNodes = processFlow.nodes.every((node) => {
            const measurement = currentNodeMeasurements.get(String(node.id));

            return (
                measurement?.width != null &&
                measurement.width > 0 &&
                measurement?.height != null &&
                measurement.height > 0
            );
        });

        void layoutNodes(hasAnyMeasuredNodes ? currentNodeMeasurements : undefined);
        setNeedsMeasuredLayout(!hasMeasurementsForAllNodes);

        resolveInitialViewportDecision();
    }, [getNodes, hasAllNodeProviders, layoutNodes, processFlow.nodes, resolveInitialViewportDecision]);

    useEffect(() => {
        if (!hasAllNodeProviders || !needsMeasuredLayout || !nodesInitialized) {
            return;
        }

        void layoutNodes(createNodeMeasurementMap(getNodes()));
        setNeedsMeasuredLayout(false);
    }, [getNodes, hasAllNodeProviders, layoutNodes, needsMeasuredLayout, nodesInitialized]);

    useEffect(() => {
        if (!pendingInitialViewport || needsMeasuredLayout) {
            return;
        }

        const frameHandle = requestAnimationFrame(() => {
            void focusInitialViewport()
                .then((wasApplied) => {
                    if (wasApplied) {
                        setPendingInitialViewport(false);
                    }
                });
        });

        return () => {
            cancelAnimationFrame(frameHandle);
        };
    }, [focusInitialViewport, needsMeasuredLayout, pendingInitialViewport]);

    return (
        <ProcessFlowEditorProvider
            value={contextValue}
        >
            <ReactFlow
                className="process-flow-editor"
                nodes={nodes}
                edges={edges}
                onNodesChange={handleNodesChange}
                onEdgesChange={onEdgesChange}
                onlyRenderVisibleElements
                nodesDraggable={false}
                nodesConnectable={isEditable}
                elementsSelectable={false}
                nodesFocusable={false}
                edgesFocusable={false}
                edgesReconnectable={false}
                nodeTypes={NodeTypes}
                edgeTypes={EdgeTypes}
                minZoom={FLOW_MIN_ZOOM}
                maxZoom={FLOW_MAX_ZOOM}
                panOnDrag={!isViewportLocked}
                zoomOnScroll={!isViewportLocked}
                zoomOnPinch={!isViewportLocked}
                zoomOnDoubleClick={!isViewportLocked}
                preventScrolling={!isViewportLocked}
                proOptions={{
                    hideAttribution: true,
                }}
                onNodeClick={(_, node) => {
                    if (onSelectNode == null) {
                        return;
                    }
                    onSelectNode(node.data.graphNode.node);
                }}
                onConnect={(a) => {
                    if (a.source == null || a.target == null || a.sourceHandle == null || onAddEdge == null) {
                        return;
                    }

                    const sourceId = Number.parseInt(a.source, 10);
                    const targetId = Number.parseInt(a.target, 10);
                    if (Number.isNaN(sourceId) || Number.isNaN(targetId)) {
                        return;
                    }

                    onAddEdge(sourceId, targetId, a.sourceHandle);
                }}
                onConnectStart={() => {
                    if (!isEditable) {
                        return;
                    }
                    setShowTargetHandles(true);
                }}
                onConnectEnd={() => {
                    if (!isEditable) {
                        return;
                    }
                    setShowTargetHandles(false);
                }}
            >
                <Background
                    variant={BackgroundVariant.Dots}
                />
                <ProcessFlowEditorViewportControls
                    fitViewOptions={fitViewOptions}
                    isViewportLocked={isViewportLocked}
                    onToggleViewportLock={handleToggleViewportLock}
                />
                <MiniMap
                    className="process-flow-editor-minimap"
                    position="bottom-right"
                    pannable={true}
                    zoomable={true}
                    zoomStep={0.8}
                    nodeBorderRadius={12}
                    nodeStrokeWidth={1.5}
                    nodeColor={() => 'rgba(148, 163, 184, 0.34)'}
                    nodeStrokeColor={() => 'rgba(100, 116, 139, 0.28)'}
                    maskColor="rgba(248, 250, 252, 0.8)"
                    maskStrokeColor="rgba(100, 116, 139, 0.28)"
                    maskStrokeWidth={1}
                    style={{
                        backgroundColor: '#ffffff',
                        width: 184,
                        height: 116,
                    }}
                />
            </ReactFlow>
        </ProcessFlowEditorProvider>
    );
}
