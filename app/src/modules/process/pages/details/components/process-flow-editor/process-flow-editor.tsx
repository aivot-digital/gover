import {type ProcessFlow} from '../../process-details-page';
import {type ProcessNodeProvider} from '../../../../services/process-node-provider-api-service';
import React, {type ReactNode, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import '@xyflow/react/dist/style.css';
import {
    Background,
    BackgroundVariant,
    type NodeChange,
    ReactFlow,
    useEdgesState,
    useNodesInitialized,
    useNodesState,
    useReactFlow,
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
import {Box} from '@mui/material';
import {Actions} from '../../../../../../components/actions/actions';
import MobileLayout from '@aivot/mui-material-symbols-400-outlined/dist/mobile-layout/MobileLayout';
import FitScreen from '@aivot/mui-material-symbols-400-outlined/dist/fit-screen/FitScreen';
import {type ProcessInstanceEntity} from '../../../../entities/process-instance-entity';
import {type ProcessInstanceTaskEntity} from '../../../../entities/process-instance-task-entity';
import {type ProcessInstanceEventEntity} from '../../../../entities/process-instance-event-entity';

interface ProcessFlowEditorProps {
    editable: boolean;

    processFlow: ProcessFlow;
    nodeProviders: ProcessNodeProvider[];

    selectedNode?: ProcessNodeEntity | null;
    onSelectNode?: (node: ProcessNodeEntity | null) => void;

    onAddEdge?: (fromNodeId: number, toNodeId: number, viaPortKey: string) => void;
    onDeleteEdge?: (edgeId: number) => void;

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

export function ProcessFlowEditor(props: ProcessFlowEditorProps): ReactNode {
    const {
        editable,

        processFlow,
        nodeProviders,

        selectedNode,
        onSelectNode,

        onAddEdge,
        onDeleteEdge,

        onAddFollowUpNode,
        onAddInbetweenNode,

        runtimeData,
    } = props;

    const {
        fitView,
        getNodes,
    } = useReactFlow<FlowNode, FlowEdge>();

    const [showTargetHandles, setShowTargetHandles] = useState<boolean>(false);
    const [needsMeasuredLayout, setNeedsMeasuredLayout] = useState<boolean>(false);
    const [pendingFitView, setPendingFitView] = useState<boolean>(false);

    const [nodes, setNodes, onNodesChange] = useNodesState<FlowNode>([]);
    const [edges, setEdges, onEdgesChange] = useEdgesState<FlowEdge>([]);
    const nodesInitialized = useNodesInitialized({
        includeHiddenNodes: true,
    });
    const hasPerformedInitialFitRef = useRef<boolean>(false);

    const isEditable = runtimeData == null && editable;
    const hasAllNodeProviders = useMemo(() => (
        processFlow.nodes.every((node) => (
            nodeProviders.some((provider) => (
                provider.key === node.processNodeDefinitionKey &&
                provider.majorVersion === node.processNodeDefinitionVersion
            ))
        ))
    ), [nodeProviders, processFlow.nodes]);

    const layoutNodes = useCallback((nodeMeasurements?: ReturnType<typeof createNodeMeasurementMap>) => {
        if (!hasAllNodeProviders) {
            return;
        }

        const {
            flowNodes: laidOutNodes,
            flowEdges: laidOutEdges,
        } = layoutElements(
            processFlow.nodes,
            processFlow.edges,
            nodeProviders,
            nodeMeasurements,
        );

        setNodes([...laidOutNodes]);
        setEdges([...laidOutEdges]);
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

        layoutNodes(hasAnyMeasuredNodes ? currentNodeMeasurements : undefined);
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

        layoutNodes(createNodeMeasurementMap(getNodes()));
        setNeedsMeasuredLayout(false);
    }, [getNodes, hasAllNodeProviders, layoutNodes, needsMeasuredLayout, nodesInitialized]);

    useEffect(() => {
        if (!pendingFitView || needsMeasuredLayout) {
            return;
        }

        const frameHandle = requestAnimationFrame(() => {
            void fitView();
            setPendingFitView(false);
        });

        return () => {
            cancelAnimationFrame(frameHandle);
        };
    }, [edges, fitView, needsMeasuredLayout, nodes, pendingFitView]);

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

                onAddFollowUpNode: onAddFollowUpNode ?? (() => {
                }),
                onAddInbetweenNode: onAddInbetweenNode ?? (() => {
                }),

                runtimeData,
            }}
        >
            <Box
                sx={{
                    position: 'absolute',
                    zIndex: 10,
                }}
            >
                <Actions
                    size="small"
                    direction="column"
                    dense={true}
                    actions={[
                        {
                            tooltip: 'layout',
                            onClick: () => {
                                if (nodesInitialized) {
                                    layoutNodes(createNodeMeasurementMap(getNodes()));
                                } else {
                                    layoutNodes();
                                    setNeedsMeasuredLayout(true);
                                }

                                setPendingFitView(true);
                            },
                            icon: <MobileLayout/>,
                        },
                        {
                            tooltip: 'fit view',
                            onClick: () => {
                                void fitView();
                            },
                            icon: <FitScreen/>,
                        },
                    ]}
                    tooltipPlacement="right"
                />
            </Box>

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
            </ReactFlow>
        </ProcessFlowEditorProvider>
    );
}
