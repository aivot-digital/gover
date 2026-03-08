import {Handle, type NodeProps, Position} from '@xyflow/react';
import {Box, Divider, IconButton, Paper, useTheme} from '@mui/material';
import React, {type ReactNode, useMemo, useState} from 'react';
import Typography from '@mui/material/Typography';
import {ProcessNodeType} from '../../../../services/process-node-provider-api-service';
import Assignment from '@aivot/mui-material-symbols-400-outlined/dist/assignment/Assignment';
import {ProviderTypeStyles} from '../../../../data/provider-type-styles';
import {KnownProviderIcons} from '../../../../data/known-provider-icons';
import {ProcessFlowEditorNodeHandle} from './process-flow-editor-node-handle';
import {useProcessFlowEditorContext} from './process-flow-editor-context';
import {HANDLE_SIZE} from './data/process-flow-constants';
import {getFlowNodeWidth, type FlowNode} from './utils/layout-utils';
import {getNodeDescription, getNodeName} from './utils/node-utils';
import {ProcessInstanceTaskStatusIcon} from '../../../../components/process-instance-task-status-icon';
import DataObject from '@aivot/mui-material-symbols-400-outlined/dist/data-object/DataObject';
import {useConfirm} from '../../../../../../providers/confirm-provider';
import {ExpandableCodeBlock} from '../../../../../../components/expandable-code-block/expandable-code-block';
import {ProcessInstanceEventDialog} from '../../../../dialogs/process-instance-event-dialog';
import News from '@aivot/mui-material-symbols-400-outlined/dist/news/News';
import {getLatestTaskForEdge, getLatestTaskForNode} from './utils/runtime-task-utils';
import MoreVert from '@aivot/mui-material-symbols-400-outlined/dist/more-vert/MoreVert';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import {Menu} from '../../../../../../components/menu/menu';

export function ProcessFlowEditorNode(props: NodeProps<FlowNode>): ReactNode {
    const theme = useTheme();
    const confirm = useConfirm();
    const [showEventsDialog, setShowEventsDialog] = useState(false);
    const [menuAnchorEl, setMenuAnchorEl] = useState<HTMLElement | null>(null);

    const {
        data,
    } = props;

    const {
        editable,
        selectedNode,
        onAddFollowUpNode,
        onDeleteEdge,
        onDeleteNode,
        showTargetHandles,
        runtimeData,
    } = useProcessFlowEditorContext();

    const {
        graphNode,
    } = data;

    const {
        node,
        provider,
        outgoingEdges,
    } = graphNode;

    const associatedTask = useMemo(() => {
        if (runtimeData == null) {
            return null;
        }

        return getLatestTaskForNode(runtimeData.tasks, node.id);
    }, [node.id, runtimeData]);

    const performedPortKeys = useMemo(() => {
        if (runtimeData == null) {
            return new Set<string>();
        }

        const result = new Set<string>();

        for (const outgoingEdge of outgoingEdges) {
            const latestTaskForEdge = getLatestTaskForEdge(
                runtimeData.tasks,
                outgoingEdge.edge.fromNodeId,
                outgoingEdge.edge.toNodeId,
            );

            if (latestTaskForEdge != null && outgoingEdge.port != null) {
                result.add(outgoingEdge.port.key);
            }
        }

        return result;
    }, [node.id, outgoingEdges, runtimeData]);

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

    const nodeName = useMemo(() => getNodeName(node, provider), [node, provider]);
    const nodeDescription = useMemo(() => getNodeDescription(node, provider), [node, provider]);

    return (
        <Box
            data-node-id={node.id}
            sx={{
                position: 'relative',
                width: `${getFlowNodeWidth(provider)}px`,
                minWidth: '280px',
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
                <Paper
                    elevation={0}
                    sx={{
                        bgcolor: typeBgColor,
                        color: typeTextColor,
                        display: 'inline-flex',
                        width: 'fit-content',
                        maxWidth: 'calc(100% - 32px)',
                        minHeight: '32px',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: 0.75,
                        py: 0.5,
                        px: 1.5,
                        borderBottomLeftRadius: 0,
                        borderBottomRightRadius: 0,
                        borderTopLeftRadius: 6,
                        borderTopRightRadius: 6,
                    }}
                >
                    <TypeIcon
                        sx={{
                            fontSize: '18px',
                        }}
                    />

                    <Typography
                        sx={{
                            fontSize: '0.95rem',
                            fontWeight: 600,
                            lineHeight: 1,
                            whiteSpace: 'nowrap',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                        }}
                    >
                        {typeLabel}
                    </Typography>
                </Paper>

                <Paper
                    elevation={0}
                    sx={{
                        position: 'relative',
                        display: 'flex',
                        flexDirection: 'column',
                        width: '100%',
                        overflow: 'hidden',
                        borderRadius: '6px',
                        outline: selectedNode?.id === node.id ? `2px solid ${theme.palette.primary.light}` : 'none',
                        boxShadow: selectedNode?.id === node.id ?
                            '0px 4px 20px rgba(0, 0, 0, 0.15)' :
                            '0px 4px 20px rgba(0, 0, 0, 0.1)',
                    }}
                >
                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 1.5,
                            px: 1.875,
                            pt: 1.5,
                            pb: 1.5,
                            width: '100%',
                        }}
                    >
                        <ProviderIcon
                            sx={{
                                mt: 0.125,
                                color: theme.palette.text.primary,
                                flexShrink: 0,
                                fontSize: '1.5rem',
                            }}
                        />

                        <Box
                            sx={{
                                minWidth: 0,
                                flex: 1,
                            }}
                        >
                            <Typography
                                sx={{
                                    fontSize: '1rem',
                                    fontWeight: 700,
                                    lineHeight: 1.2,
                                    color: theme.palette.text.primary,
                                }}
                            >
                                {nodeName}
                            </Typography>
                        </Box>

                        <Box
                            sx={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: 0.25,
                                ml: 'auto',
                                flexShrink: 0,
                            }}
                        >
                            {
                                associatedTask != null &&
                                <ProcessInstanceTaskStatusIcon
                                    status={associatedTask.status}
                                    statusOverride={associatedTask.statusOverride}
                                />
                            }

                            {
                                editable &&
                                <IconButton
                                    size="small"
                                    sx={{
                                        color: theme.palette.text.secondary,
                                        mt: -0.25,
                                        mr: -0.75,
                                    }}
                                    onClick={(event) => {
                                        event.stopPropagation();
                                        event.preventDefault();
                                        setMenuAnchorEl(event.currentTarget);
                                    }}
                                >
                                    <MoreVert/>
                                </IconButton>
                            }
                        </Box>
                    </Box>

                    <Divider
                        sx={{
                            width: '100%',
                            borderColor: 'rgba(15, 23, 42, 0.12)',
                        }}
                    />

                    <Box
                        sx={{
                            p: 1.875,
                            pb: 2,
                            flex: 1,
                        }}
                    >
                        <Typography
                            sx={{
                                fontSize: '0.875rem',
                                lineHeight: 1.45,
                                color: theme.palette.text.secondary,
                            }}
                        >
                            {nodeDescription}
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
                                zIndex: 9999,
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

                    {
                        associatedTask &&
                        <IconButton
                            sx={{
                                position: 'absolute',
                                bottom: '-1rem',
                                left: '-1rem',
                                padding: 0.5,
                                bgcolor: 'white',
                                border: `2px solid ${theme.palette.primary.main}`,
                                '&:hover': {
                                    bgcolor: '#efefef',
                                },
                                zIndex: 9999,
                            }}
                            size="small"
                            onClick={(event) => {
                                event.stopPropagation();
                                event.preventDefault();
                                setShowEventsDialog(true);
                            }}
                        >
                            <News color="primary"/>
                        </IconButton>
                    }
                </Paper>
            </Box>

            <Menu
                open={menuAnchorEl != null}
                anchorEl={menuAnchorEl}
                onClose={() => {
                    setMenuAnchorEl(null);
                }}
                items={[
                    {
                        label: 'Löschen',
                        icon: <Delete/>,
                        onClick: () => {
                            void confirm({
                                title: 'Prozesselement löschen',
                                children: (
                                    <Typography>
                                        Möchten Sie das Prozesselement <strong>{nodeName}</strong> wirklich löschen?
                                    </Typography>
                                ),
                            })
                                .then((confirmed) => {
                                    if (confirmed) {
                                        void onDeleteNode(node);
                                    }
                                });
                        },
                    },
                ]}
            />

            {
                provider.type !== ProcessNodeType.Trigger &&
                <Handle
                    type="target"
                    position={Position.Top}
                    style={{
                        opacity: showTargetHandles ? 1 : 0,
                        pointerEvents: showTargetHandles ? 'all' : 'none',
                        width: `${HANDLE_SIZE}px`,
                        height: `${HANDLE_SIZE}px`,
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
                        display: 'grid',
                        gridTemplateColumns: `repeat(${Math.max(provider.ports.length, 1)}, minmax(0, 1fr))`,
                        justifyItems: 'center',
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
                                    isConnected={outgoingEdges.some((outgoingEdge) => outgoingEdge.port?.key === port.key)}
                                    port={port}
                                    onClick={() => {
                                        onAddFollowUpNode(node.id, port.key);
                                    }}
                                    onDeleteEdge={(port) => {
                                        const edge = outgoingEdges.find((outgoingEdge) => outgoingEdge.port?.key === port.key);

                                        if (edge != null) {
                                            onDeleteEdge(edge.edge.id);
                                        }
                                    }}
                                />
                            ))
                    }
                </Box>
            }

            {
                runtimeData != null &&
                associatedTask != null &&
                <ProcessInstanceEventDialog
                    open={showEventsDialog}
                    onClose={() => {
                        setShowEventsDialog(false);
                    }}
                    instanceId={runtimeData.instance.id}
                    taskId={associatedTask.id}
                />
            }
        </Box>
    );
}
