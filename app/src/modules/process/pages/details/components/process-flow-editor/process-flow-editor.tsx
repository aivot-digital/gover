import {type ProcessFlow} from '../../process-details-page';
import {type ProcessNodeProvider} from '../../../../services/process-node-provider-api-service';
import React, {type ReactNode, useCallback, useEffect, useState} from 'react';
import '@xyflow/react/dist/style.css';
import {Background, BackgroundVariant, ReactFlow, useEdgesState, useNodesState, useReactFlow} from '@xyflow/react';
import {type ProcessNodeEntity} from '../../../../entities/process-node-entity';
import {ProcessFlowEditorNode} from './process-flow-editor-node';
import {ProcessFlowEditorEdge} from './process-flow-editor-edge';
import {ProcessFlowEditorContext} from './process-flow-editor-context';
import {DEFAULT_FLOW_EDGE_TYPE, DEFAULT_FLOW_NODE_TYPE} from './data/process-flow-constants';
import {type FlowEdge, type FlowNode, layoutElements} from './utils/layout-utils';
import {Box} from '@mui/material';
import {Actions} from '../../../../../../components/actions/actions';
import MobileLayout from '@aivot/mui-material-symbols-400-outlined/dist/mobile-layout/MobileLayout';
import FitScreen from '@aivot/mui-material-symbols-400-outlined/dist/fit-screen/FitScreen';

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
    } = props;

    const {
        fitView,
    } = useReactFlow();

    const [showTargetHandles, setShowTargetHandles] = useState<boolean>(false);

    const [nodes, setNodes, onNodesChange] = useNodesState<FlowNode>([]);
    const [edges, setEdges, onEdgesChange] = useEdgesState<FlowEdge>([]);

    const layoutNodes = useCallback(() => {
        const {
            flowNodes: laidOutNodes,
            flowEdges: laidOutEdges,
        } = layoutElements(processFlow.nodes, processFlow.edges, nodeProviders);

        setNodes([...laidOutNodes]);
        setEdges([...laidOutEdges]);

        fitView();
    }, [processFlow]);

    useEffect(() => {
        layoutNodes();
    }, [processFlow]);

    return (
        <ProcessFlowEditorContext.Provider
            value={{
                editable,
                showTargetHandles,

                selectedNode: selectedNode ?? null,
                onSelectedNode: onSelectNode ?? (() => {
                }),

                onAddEdge: onAddEdge ?? (() => {
                }),
                onDeleteEdge: onDeleteEdge ?? (() => {
                }),

                onAddFollowUpNode: onAddFollowUpNode ?? (() => {
                }),
                onAddInbetweenNode: onAddInbetweenNode ?? (() => {
                }),
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
                            onClick: layoutNodes,
                            icon: <MobileLayout/>,
                        },
                        {
                            tooltip: 'fit view',
                            onClick: () => fitView(),
                            icon: <FitScreen/>,
                        },
                    ]}
                    tooltipPlacement="right"
                />
            </Box>

            <ReactFlow
                nodes={nodes}
                edges={edges}
                onNodesChange={onNodesChange}
                onEdgesChange={onEdgesChange}
                nodesDraggable={false}
                elementsSelectable={false}
                nodesFocusable={false}
                edgesFocusable={false}
                edgesReconnectable={false}
                fitView
                nodeTypes={NodeTypes}
                edgeTypes={EdgeTypes}
                onNodeClick={(_, node) => {
                    if (onSelectNode == null) {
                        return;
                    }
                    onSelectNode(node.data.treeNode.node);
                }}
                onInit={layoutNodes}
                onConnect={(a) => {
                    if (a.source == null || a.target == null || a.sourceHandle == null || onAddEdge == null) {
                        return;
                    }

                    const sourceId = parseInt(a.source);
                    const targetId = parseInt(a.target);

                    onAddEdge(sourceId, targetId, a.sourceHandle);
                }}
                onConnectStart={() => {
                    setShowTargetHandles(true);
                }}
                onConnectEnd={() => {
                    setShowTargetHandles(false);
                }}
            >
                <Background
                    variant={BackgroundVariant.Dots}
                />
            </ReactFlow>
        </ProcessFlowEditorContext.Provider>
    );
}

