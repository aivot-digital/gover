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
    useViewport,
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
const ZOOM_EPSILON = 0.001;

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
        zoomIn,
        zoomOut,
        zoomTo,
    } = useReactFlow<FlowNode, FlowEdge>();
    const {zoom} = useViewport();

    const [showTargetHandles, setShowTargetHandles] = useState<boolean>(false);
    const [needsMeasuredLayout, setNeedsMeasuredLayout] = useState<boolean>(false);
    const [pendingFitView, setPendingFitView] = useState<boolean>(false);
    const [isViewportLocked, setIsViewportLocked] = useState<boolean>(false);

    const [nodes, setNodes, onNodesChange] = useNodesState<FlowNode>([]);
    const [edges, setEdges, onEdgesChange] = useEdgesState<FlowEdge>([]);
    const nodesInitialized = useNodesInitialized({
        includeHiddenNodes: true,
    });
    const hasPerformedInitialFitRef = useRef<boolean>(false);
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
    const canZoomIn = zoom < FLOW_MAX_ZOOM - ZOOM_EPSILON;
    const canZoomOut = zoom > FLOW_MIN_ZOOM + ZOOM_EPSILON;

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
        hasPerformedInitialFitRef.current = false;
    }, [processFlow.definition.id, processFlow.version.processVersion]);

    useEffect(() => {
        if (!hasAllNodeProviders) {
            setNeedsMeasuredLayout(false);
            setPendingFitView(false);
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

        if (!hasPerformedInitialFitRef.current) {
            hasPerformedInitialFitRef.current = true;
            setPendingFitView(true);
        }
    }, [getNodes, hasAllNodeProviders, layoutNodes, processFlow.nodes]);

    useEffect(() => {
        if (!hasAllNodeProviders || !needsMeasuredLayout || !nodesInitialized) {
            return;
        }

        void layoutNodes(createNodeMeasurementMap(getNodes()));
        setNeedsMeasuredLayout(false);
    }, [getNodes, hasAllNodeProviders, layoutNodes, needsMeasuredLayout, nodesInitialized]);

    useEffect(() => {
        if (!pendingFitView || needsMeasuredLayout) {
            return;
        }

        const frameHandle = requestAnimationFrame(() => {
            void fitView(fitViewOptions);
            setPendingFitView(false);
        });

        return () => {
            cancelAnimationFrame(frameHandle);
        };
    }, [edges, fitView, fitViewOptions, needsMeasuredLayout, nodes, pendingFitView]);

    return (
        // TODO: Move the provider upward to avoid unnecessary re-renders of the whole graph when only the context values change
        <ProcessFlowEditorProvider
            value={{
                editable: isEditable,
                showTargetHandles,

                selectedNode: selectedNode ?? null,

                onAddEdge: onAddEdge ?? (() => {
                }),
                onDeleteEdge: onDeleteEdge ?? (() => {
                }),
                onDeleteNode: onDeleteNode ?? (() => {
                }),

                onAddFollowUpNode: onAddFollowUpNode ?? (() => {
                }),
                onAddInbetweenNode: onAddInbetweenNode ?? (() => {
                }),

                runtimeData,
            }}
        >
            <ReactFlow
                nodes={nodes}
                edges={edges}
                onNodesChange={handleNodesChange}
                onEdgesChange={onEdgesChange}
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
                <Controls
                    className="process-flow-editor-controls"
                    position="bottom-left"
                    showZoom={false}
                    showFitView={false}
                    showInteractive={false}
                    style={{
                        bottom: 16,
                        left: 16,
                    }}
                >
                    <ProcessFlowEditorControlButton
                        className="process-flow-editor-control-button"
                        disabled={!canZoomIn}
                        onClick={() => {
                            void zoomIn({duration: 180});
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
                            void zoomOut({duration: 180});
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
                            void zoomTo(1, {duration: 180});
                        }}
                        ariaLabel="Zoom auf Originalgröße (100 %)"
                        tooltip="Zoom auf Originalgröße (100 %)"
                    >
                        <ViewRealSize sx={{fontSize: 18}}/>
                    </ProcessFlowEditorControlButton>

                    <ProcessFlowEditorControlButton
                        className="process-flow-editor-control-button"
                        onClick={() => {
                            setIsViewportLocked((current) => !current);
                        }}
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
                    maskColor="rgba(226, 232, 240, 0.42)"
                    maskStrokeColor="rgba(100, 116, 139, 0.28)"
                    maskStrokeWidth={1}
                    style={{
                        width: 184,
                        height: 116,
                    }}
                />
            </ReactFlow>
        </ProcessFlowEditorProvider>
    );
}
