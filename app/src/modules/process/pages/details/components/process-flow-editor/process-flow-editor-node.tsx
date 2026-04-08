import {Handle, type NodeProps, Position, useUpdateNodeInternals} from '@xyflow/react';
import {Box, Button, Divider, IconButton, Paper, Tooltip, useTheme} from '@mui/material';
import React, {type ReactNode, useEffect, useMemo, useState} from 'react';
import Typography from '@mui/material/Typography';
import {alpha} from '@mui/material/styles';
import {ProcessNodeType} from '../../../../services/process-node-provider-api-service';
import Assignment from '@aivot/mui-material-symbols-400-outlined/dist/assignment/Assignment';
import {ProviderTypeStyles} from '../../../../data/provider-type-styles';
import {KnownProviderIcons} from '../../../../data/known-provider-icons';
import {ProcessFlowEditorNodeHandle} from './process-flow-editor-node-handle';
import {useProcessFlowEditorContext} from './process-flow-editor-context';
import {HANDLE_COLOR, HANDLE_SIZE, INTERACTIVE_HANDLE_SIZE} from './data/process-flow-constants';
import {type FlowNode, getFlowNodeWidth} from './utils/layout-utils';
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
import {ProcessTaskStatus} from '../../../../enums/process-task-status';
import Link from '@mui/icons-material/Link';
import SwapHoriz from '@mui/icons-material/SwapHoriz';
import {ProcessActionMenu, type ProcessActionMenuItem} from '../process-action-menu';
import {ModuleIcons} from '../../../../../../shells/staff/data/module-icons';
import {useNavigate} from 'react-router-dom';

function ProcessFlowEditorNodeComponent(props: NodeProps<FlowNode>): ReactNode {
    const theme = useTheme();
    const confirm = useConfirm();
    const navigate = useNavigate();
    const updateNodeInternals = useUpdateNodeInternals();
    const [showEventsDialog, setShowEventsDialog] = useState(false);
    const [menuAnchorEl, setMenuAnchorEl] = useState<HTMLElement | null>(null);
    const [runtimeActionsMenuAnchorEl, setRuntimeActionsMenuAnchorEl] = useState<HTMLElement | null>(null);

    const {
        data,
    } = props;

    const {
        editable,
        selectedNode,
        onAddFollowUpNode,
        onConnectNodeToExisting,
        onStartReplaceNode,
        onDeleteEdge,
        onDeleteNode,
        showTargetHandles,
        runtimeData,
        nodeValidationResults,
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

    const associatedValidationResult = useMemo(() => {
        return nodeValidationResults.find((result) => result.node.id === node.id) || null;
    }, [node.id, nodeValidationResults]);

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
                outgoingEdge.edge.viaPort,
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
    const runtimeStatusAccentColor = useMemo(() => {
        if (associatedTask == null) {
            return null;
        }

        if (associatedTask.statusOverride != null) {
            return theme.palette.primary.main;
        }

        switch (associatedTask.status) {
            case ProcessTaskStatus.Running:
                return theme.palette.info.main;
            case ProcessTaskStatus.Paused:
                return theme.palette.primary.main;
            case ProcessTaskStatus.Completed:
                return theme.palette.success.main;
            case ProcessTaskStatus.Restarted:
                return theme.palette.warning.main;
            case ProcessTaskStatus.Aborted:
            case ProcessTaskStatus.Failed:
                return theme.palette.error.main;
            default:
                return null;
        }
    }, [associatedTask, theme.palette.error.main, theme.palette.info.main, theme.palette.primary.main, theme.palette.success.main, theme.palette.warning.main]);
    const nodeOutline = useMemo(() => {
        if (selectedNode?.id === node.id) {
            return `2px solid ${theme.palette.primary.light}`;
        }

        if (runtimeStatusAccentColor != null) {
            return `1px solid ${alpha(runtimeStatusAccentColor, 0.48)}`;
        }

        return 'none';
    }, [node.id, runtimeStatusAccentColor, selectedNode?.id, theme.palette.primary.light]);
    const nodeShadow = useMemo(() => {
        const neutralShadow = selectedNode?.id === node.id ?
            '0px 4px 20px rgba(0, 0, 0, 0.15)' :
            '0px 4px 20px rgba(0, 0, 0, 0.1)';

        if (runtimeStatusAccentColor == null) {
            return neutralShadow;
        }

        return selectedNode?.id === node.id ?
            `0 12px 28px ${alpha(runtimeStatusAccentColor, 0.18)}, ${neutralShadow}` :
            `0 10px 24px ${alpha(runtimeStatusAccentColor, 0.16)}, ${neutralShadow}`;
    }, [node.id, runtimeStatusAccentColor, selectedNode?.id]);
    const runtimeActionButtonSx = useMemo(() => ({
        minWidth: 0,
        height: 28,
        px: 0.875,
        py: 0.375,
        borderRadius: '6px',
        bgcolor: 'background.paper',
        borderColor: alpha(theme.palette.primary.main, 0.28),
        color: theme.palette.primary.main,
        fontSize: '0.75rem',
        fontWeight: 500,
        lineHeight: 1,
        textTransform: 'none',
        whiteSpace: 'nowrap',
        '& .MuiButton-startIcon': {
            marginLeft: 0,
            marginRight: 1,
        },
        '&:hover': {
            borderColor: theme.palette.primary.main,
            bgcolor: alpha(theme.palette.primary.main, 0.04),
        },
    }), [theme.palette.primary.main]);
    const handleLayoutSignature = useMemo(() => (
        provider.ports.map((port) => (
            `${port.key}:${outgoingEdges.some((outgoingEdge) => outgoingEdge.port?.key === port.key) ? '1' : '0'}`
        )).join('|')
    ), [outgoingEdges, provider.ports]);
    const shouldRenderMenuButtonSlot = editable || associatedTask == null;
    const availableOutputPorts = useMemo(() => (
        provider.ports.filter((port) => (
            !outgoingEdges.some((outgoingEdge) => outgoingEdge.port?.key === port.key)
        ))
    ), [outgoingEdges, provider.ports]);
    const menuItems = useMemo<ProcessActionMenuItem[]>(() => {
        const actionItems: ProcessActionMenuItem[] = editable ? [
            ...(availableOutputPorts.length > 0 ? [{
                label: 'Mit bestehendem Knoten verbinden',
                icon: <Link/>,
                onClick: () => {
                    onConnectNodeToExisting(node);
                },
            }] : []),
            {
                label: 'Ersetzen',
                icon: <SwapHoriz/>,
                onClick: () => {
                    onStartReplaceNode(node);
                },
            },
        ] : [];

        return [
            ...actionItems,
            ...(actionItems.length > 0 ? ['separator' as const] : []),
            {
                label: 'Löschen',
                icon: <Delete/>,
                isDangerous: true,
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
        ];
    }, [availableOutputPorts.length, confirm, editable, node, nodeName, onConnectNodeToExisting, onDeleteNode, onStartReplaceNode]);

    const runtimeMenuItems: ProcessActionMenuItem[] = useMemo(() => {
        if (associatedTask == null || associatedTask.status != ProcessTaskStatus.Running) {
            return [];
        }
        return [
            {
                label: 'Aufgabe aufrufen',
                icon: ModuleIcons.tasks,
                to: `/tasks/${associatedTask.processInstanceId}/${associatedTask.id}`,
                newTab: true,
                disabled: false,
                visible: true,
                isDangerous: false,
            },
        ];
    }, [associatedTask]);

    // Connecting/disconnecting ports changes the effective handle geometry of the node. React Flow
    // does not always pick that up from pure React re-renders, so force an internal recalculation
    // whenever the port occupancy signature changes.
    useEffect(() => {
        updateNodeInternals(String(node.id));
    }, [handleLayoutSignature, node.id, updateNodeInternals]);

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
                        maxWidth: 'calc(100% - 26px)',
                        minHeight: '26px',
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
                            fontSize: '0.8125rem',
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
                    className="process-flow-editor-node-card"
                    sx={{
                        position: 'relative',
                        display: 'flex',
                        flexDirection: 'column',
                        width: '100%',
                        overflow: 'hidden',
                        borderRadius: '6px',
                        outline: nodeOutline,
                        boxShadow: nodeShadow,
                    }}
                >
                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 1.5,
                            px: 1.5,
                            pt: 1,
                            pb: 1.25,
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
                                display: 'flex',
                                alignItems: 'center',
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

                            {
                                associatedValidationResult != null &&
                                <Tooltip title={
                                    <>
                                        {
                                            associatedValidationResult.problems.map((problem, index) => (
                                                <Typography
                                                    key={problem + index.toString()}
                                                    variant="body2"
                                                    sx={{
                                                        marginBottom: 1,
                                                    }}
                                                >
                                                    {problem}
                                                </Typography>
                                            ))
                                        }
                                    </>
                                }>
                                    <Box
                                        sx={{
                                            marginLeft: 1,
                                            width: '0.5rem',
                                            height: '0.5rem',
                                            borderRadius: '50%',
                                            bgcolor: 'error.main',
                                        }}
                                    />
                                </Tooltip>
                            }
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
                                shouldRenderMenuButtonSlot &&
                                <IconButton
                                    size="small"
                                    aria-hidden={!editable}
                                    disabled={!editable}
                                    sx={{
                                        color: theme.palette.text.secondary,
                                        mt: -0.25,
                                        mr: -0.75,
                                        visibility: editable ? 'visible' : 'hidden',
                                        pointerEvents: editable ? 'auto' : 'none',
                                    }}
                                    onClick={(event) => {
                                        if (!editable) {
                                            return;
                                        }

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
                            p: 1.5,
                            pb: associatedTask != null ? 1 : 2,
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
                        <Box
                            sx={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: 1,
                                px: 1.25,
                                pt: 0.75,
                                pb: 1.25,
                            }}
                        >
                            <Button
                                variant="outlined"
                                size="small"
                                startIcon={<News sx={{fontSize: 16}}/>}
                                sx={runtimeActionButtonSx}
                                onClick={(event) => {
                                    event.stopPropagation();
                                    event.preventDefault();
                                    setShowEventsDialog(true);
                                }}
                            >
                                Ereignisse
                            </Button>

                            <Box sx={{flex: 1}}/>

                            <Button
                                variant="outlined"
                                size="small"
                                startIcon={<DataObject sx={{fontSize: 16}}/>}
                                sx={runtimeActionButtonSx}
                                onClick={(event) => {
                                    event.stopPropagation();
                                    event.preventDefault();

                                    confirm({
                                        title: 'Elementdaten',
                                        width: 'md',
                                        hideCancelButton: true,
                                        confirmButtonText: 'Schließen',
                                        children: (
                                            <>
                                                <Typography variant="h6">
                                                    Die erzeugten Elementdaten
                                                </Typography>
                                                <ExpandableCodeBlock
                                                    value={JSON.stringify(associatedTask?.nodeData, null, 2)}
                                                />
                                            </>
                                        ),
                                    });
                                }}
                            >
                                Daten
                            </Button>

                            <IconButton
                                size="small"
                                color="primary"
                                onClick={(event) => {
                                    event.stopPropagation();
                                    event.preventDefault();
                                    setRuntimeActionsMenuAnchorEl(event.currentTarget);
                                }}
                                disabled={runtimeMenuItems.length === 0}
                            >
                                <MoreVert
                                    sx={{
                                        fontSize: 16,
                                    }}
                                />
                            </IconButton>
                        </Box>
                    }
                </Paper>
            </Box>

            <ProcessActionMenu
                anchorEl={menuAnchorEl}
                onClose={() => {
                    setMenuAnchorEl(null);
                }}
                items={menuItems}
            />

            <ProcessActionMenu
                anchorEl={runtimeActionsMenuAnchorEl}
                onClose={() => {
                    setRuntimeActionsMenuAnchorEl(null);
                }}
                items={runtimeMenuItems}
            />

            {
                provider.type !== ProcessNodeType.Trigger &&
                <Handle
                    type="target"
                    position={Position.Top}
                    style={{
                        opacity: showTargetHandles ? 1 : 0,
                        pointerEvents: showTargetHandles ? 'all' : 'none',
                        width: `${INTERACTIVE_HANDLE_SIZE}px`,
                        height: `${INTERACTIVE_HANDLE_SIZE}px`,
                        top: `${(HANDLE_SIZE / 2) - (INTERACTIVE_HANDLE_SIZE / 2)}px`,
                        backgroundColor: theme.palette.background.paper,
                        border: `2px solid ${HANDLE_COLOR}`,
                        cursor: showTargetHandles ? 'crosshair' : 'default',
                        transition: 'opacity 120ms ease',
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
                                    onConnectToExisting={(port) => {
                                        onConnectNodeToExisting(node, port.key);
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

export const ProcessFlowEditorNode = React.memo(ProcessFlowEditorNodeComponent);
ProcessFlowEditorNode.displayName = 'ProcessFlowEditorNode';
