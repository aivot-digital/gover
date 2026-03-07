import {Handle, type NodeProps, Position} from '@xyflow/react';
import {Box, Divider, IconButton, Paper, useTheme} from '@mui/material';
import React, {type ReactNode, useMemo} from 'react';
import Typography from '@mui/material/Typography';
import {ProcessNodeType} from '../../../../services/process-node-provider-api-service';
import Assignment from '@aivot/mui-material-symbols-400-outlined/dist/assignment/Assignment';
import {ProviderTypeStyles} from '../../../../data/provider-type-styles';
import {KnownProviderIcons} from '../../../../data/known-provider-icons';
import {ProcessFlowEditorNodeHandle} from './process-flow-editor-node-handle';
import {useProcessFlowEditorContext} from './process-flow-editor-context';
import {HANDLE_SIZE, NODE_WIDTH} from './data/process-flow-constants';
import {type FlowNode} from './utils/layout-utils';
import {getNodeDescription, getNodeName} from './utils/node-utils';
import {ProcessTaskStatus} from '../../../../enums/process-task-status';
import {ProcessInstanceTaskStatusIcon} from '../../../../components/process-instance-task-status-icon';
import DataObject from '@aivot/mui-material-symbols-400-outlined/dist/data-object/DataObject';
import {useConfirm} from '../../../../../../providers/confirm-provider';
import {ExpandableCodeBlock} from '../../../../../../components/expandable-code-block/expandable-code-block';

export function ProcessFlowEditorNode(props: NodeProps<FlowNode>): ReactNode {
    const theme = useTheme();
    const confirm = useConfirm();

    const {
        data,
    } = props;

    const {
        editable,
        selectedNode,
        onAddFollowUpNode,
        onDeleteEdge,
        showTargetHandles,
        runtimeData,
    } = useProcessFlowEditorContext();

    const {
        treeNode,
    } = data;

    const {
        node,
        provider,
    } = treeNode;

    const associatedTask = useMemo(() => {
        if (runtimeData == null) {
            return null;
        }

        const treeNode = data?.treeNode;
        if (treeNode == null) {
            return null;
        }

        return runtimeData
            .tasks
            .find((task) => (
                task.processNodeId === treeNode.node.id
            )) ?? null;
    }, [runtimeData]);

    const performedPortKeys = useMemo(() => {
        if (runtimeData == null) {
            return new Set<string>();
        }

        const result = new Set<string>();

        for (const child of data.treeNode.children) {
            const hasMatchingNextTask = runtimeData.tasks.some((task) => (
                task.previousProcessNodeId === node.id &&
                task.processNodeId === child.childNode.node.id
            ));

            if (hasMatchingNextTask) {
                result.add(child.port.key);
            }
        }

        return result;
    }, [runtimeData, data.treeNode.children, node.id]);

    const {
        Icon: TypeIcon,
        label: typeLabel,
        bgColor: typeBgColor,
        textColor: typeTextColor,
    } = useMemo(() => {
        return ProviderTypeStyles[provider.type];
    }, [provider.type]);

    const ProviderIcon = useMemo(() => {
        return (
            KnownProviderIcons[provider.componentKey] ||
            KnownProviderIcons[provider.key] ||
            Assignment
        );
    }, [provider.componentKey, provider.key]);

    return (
        <Box
            data-node-id={node.id}
            sx={{
                position: 'relative',
                width: `${Math.max(NODE_WIDTH * 2, NODE_WIDTH * (provider.ports.length + 1))}px`,
            }}
        >
            <Box
                sx={{
                    width: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                }}
            >
                {/* Type Card */}
                <Paper
                    sx={{
                        bgcolor: typeBgColor,
                        color: typeTextColor,
                        display: 'flex',
                        width: '66%',
                        height: '24px',
                        alignItems: 'center',
                        justifyContent: 'center',
                        py: 0.25,
                        px: 0.5,
                        borderBottomLeftRadius: 0,
                        borderBottomRightRadius: 0,
                    }}
                >
                    <TypeIcon
                        sx={{
                            fontSize: '16px',
                            mr: 0.5,
                        }}
                    />

                    <Typography
                        sx={{
                            fontSize: '12px',
                            fontWeight: 200,
                        }}
                    >
                        {typeLabel}
                    </Typography>
                </Paper>

                {/* Content Card */}
                <Paper
                    sx={{
                        position: 'relative',
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'flex-start',
                        width: '100%',
                        height: 'calc(100% - 24px)',
                        outline: selectedNode?.id === node.id ? `2px solid ${theme.palette.primary.light}` : 'none',
                    }}
                >
                    {/* Title */}
                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                            padding: 1,
                            width: '100%',
                        }}
                    >
                        <ProviderIcon
                            sx={{
                                mr: 1,
                            }}
                        />

                        <Typography>
                            {getNodeName(node, provider)}
                        </Typography>

                        {
                            associatedTask != null &&
                            <Box
                                sx={{
                                    ml: 'auto',
                                }}
                            >
                                <ProcessInstanceTaskStatusIcon
                                    status={associatedTask.status}
                                    statusOverride={associatedTask.statusOverride}
                                />
                            </Box>
                        }
                    </Box>

                    {/* Divider */}
                    <Divider
                        sx={{
                            mb: 0.25,
                            width: '100%',
                        }}
                    />

                    {/* Description */}
                    <Box
                        sx={{
                            padding: 1,
                            flex: 1,
                        }}
                    >
                        <Typography>
                            {getNodeDescription(node, provider)}
                        </Typography>
                    </Box>

                    {
                        associatedTask != null &&
                        <IconButton
                            sx={{
                                position: 'absolute',
                                bottom: '-1rem',
                                right: '-1rem',
                                padding: 0.5,
                                bgcolor: 'white',
                                border: `2px solid ${theme.palette.primary.main}`,
                                '&:hover': {
                                    bgcolor: '#efefef',
                                },
                            }}
                            size="small"
                            onClick={(event) => {
                                event.stopPropagation();
                                event.preventDefault();

                                confirm({
                                    title: 'Prozesselementdaten',
                                    width: 'md',
                                    hideCancelButton: true,
                                    confirmButtonText: 'Schließen',
                                    children: (
                                        <>
                                            <Typography variant="h6">
                                                Die erzeugten Prozesselementdaten
                                            </Typography>
                                            <ExpandableCodeBlock
                                                value={JSON.stringify(associatedTask?.nodeData, null, 2)}
                                            />
                                        </>
                                    ),
                                });
                            }}
                        >
                            <DataObject color="primary"/>
                        </IconButton>
                    }
                </Paper>
            </Box>

            {
                provider.type !== ProcessNodeType.Trigger &&
                <Handle
                    type="target"
                    position={Position.Top}
                    style={{
                        visibility: showTargetHandles ? 'visible' : 'hidden',
                        width: showTargetHandles ? `${HANDLE_SIZE}px` : 0,
                        height: showTargetHandles ? `${HANDLE_SIZE}px` : 0,
                        backgroundColor: 'var(--xy-edge-stroke, var(--xy-edge-stroke-default))',
                        border: 'none',
                    }}
                />
            }

            {
                provider.type !== ProcessNodeType.Termination &&
                <Box
                    sx={{
                        position: 'relative',
                        display: 'flex',
                        justifyContent: 'space-evenly',
                        alignItems: 'stretch',
                        width: '100%',
                    }}
                >
                    {
                        provider
                            .ports
                            .map((port) => (
                                <ProcessFlowEditorNodeHandle
                                    key={port.key}
                                    editable={editable}
                                    wasPerformed={performedPortKeys.has(port.key)}
                                    isConnected={data.treeNode.children.some((c) => c.port.key === port.key)}
                                    port={port}
                                    onClick={() => {
                                        onAddFollowUpNode(node.id, port.key);
                                    }}
                                    onDeleteEdge={(port) => {
                                        const edge = data
                                            .treeNode
                                            .children
                                            .find((c) => c.port.key === port.key);

                                        if (edge != null) {
                                            onDeleteEdge(edge.edge.id);
                                        }
                                    }}
                                />
                            ))
                    }
                </Box>
            }
        </Box>
    );
}
