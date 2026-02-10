import {BaseEdge, EdgeLabelRenderer, type EdgeProps, getSmoothStepPath} from '@xyflow/react';
import {Box, IconButton, useTheme} from '@mui/material';
import {Add} from '@mui/icons-material';
import React, {type ReactNode, useMemo} from 'react';
import {useProcessFlowEditorContext} from './process-flow-editor-context';
import {type FlowEdge} from './utils/layout-utils';
import {ADD_BUTTON_SIZE, HANDLE_COLOR, HANDLE_WIDTH} from './data/process-flow-constants';
import './process-flow-editor-animations.css';
import DataObject from '@aivot/mui-material-symbols-400-outlined/dist/data-object/DataObject';
import Close from '@aivot/mui-material-symbols-400-outlined/dist/close/Close';
import Chip from '@mui/material/Chip';
import Typography from '@mui/material/Typography';
import {useConfirm} from '../../../../../../providers/confirm-provider';
import {ExpandableCodeBlock} from '../../../../../../components/expandable-code-block/expandable-code-block';

export function ProcessFlowEditorEdge(props: EdgeProps<FlowEdge>): ReactNode {
    const theme = useTheme();
    const confirm = useConfirm();

    const {
        sourceX,
        sourceY,
        sourcePosition,
        targetX,
        targetY,
        targetPosition,
        markerEnd,
        style,
        data: optData,
    } = props;

    const {
        editable,
        onAddInbetweenNode,
        runtimeData,
        onDeleteEdge,
    } = useProcessFlowEditorContext();

    const [edgePath, labelX, labelY] = getSmoothStepPath({
        sourceX,
        sourceY,
        sourcePosition,
        targetX,
        targetY,
        targetPosition,
    });

    const {
        treeEdge,
    } = useMemo(() => {
        if (optData == null) {
            throw new Error('Edge data is required for ProcessFlowEditorEdge');
        }
        return optData;
    }, [optData]);

    const associatedTask = useMemo(() => {
        if (runtimeData == null) {
            return null;
        }

        return runtimeData
            .tasks
            .find((task) => (
                task.processNodeId === treeEdge.edge.fromNodeId
            )) ?? null;
    }, [runtimeData, treeEdge]);

    const wasPerformed = associatedTask != null;

    const handleDeleteEdge = (): void => {
        confirm({
            title: 'Verbindung aufheben',
            children: (
                <Typography>
                    Möchten Sie die Verbindung &quot;{treeEdge.port.label}&quot; wirklich aufheben?
                </Typography>
            ),
        })
            .then((confirmed) => {
                if (confirmed) {
                    onDeleteEdge(treeEdge.edge.id);
                }
            });
    };

    return (
        <>
            <BaseEdge
                path={edgePath}
                markerEnd={markerEnd}
                style={{
                    ...style,
                    strokeWidth: `${HANDLE_WIDTH}px`,
                    stroke: wasPerformed ? theme.palette.primary.main : undefined,
                    strokeDasharray: wasPerformed ? '10 10' : undefined,
                    animation: wasPerformed ? 'active-edge-dash-scroll 2s linear infinite' : undefined,
                }}
            />

            <EdgeLabelRenderer>
                <Box
                    sx={{
                        position: 'absolute',
                        pointerEvents: 'all',
                        transformOrigin: 'center',
                        transform: `translate(-50%, 20%) translate(${sourceX}px,${sourceY}px)`,
                        zIndex: 100,
                    }}
                >
                    <Chip
                        label={treeEdge.port.label}
                        size="small"
                        variant="outlined"
                        sx={{
                            bgcolor: 'background.paper',
                            borderColor: HANDLE_COLOR,
                        }}
                        deleteIcon={editable ? <Close/> : undefined}
                        onDelete={editable ? handleDeleteEdge : undefined}
                    />
                </Box>

                <Box
                    sx={{
                        position: 'absolute',
                        pointerEvents: 'all',
                        transformOrigin: 'center',
                        transform: `translate(-50%, -50%) translate(${labelX}px,${labelY}px)`,
                        zIndex: 100,
                    }}
                >
                    {
                        editable &&
                        associatedTask == null &&
                        <IconButton
                            sx={{
                                cursor: 'pointer',
                                bgcolor: 'background.paper',
                                border: `${HANDLE_WIDTH}px solid`,
                                borderColor: HANDLE_COLOR,
                                width: ADD_BUTTON_SIZE,
                                height: ADD_BUTTON_SIZE,
                            }}
                            onClick={() => {
                                onAddInbetweenNode(treeEdge.edge.id);
                            }}
                        >
                            <Add
                                sx={{
                                    fontSize: ADD_BUTTON_SIZE - 2,
                                }}
                            />
                        </IconButton>
                    }

                    {
                        associatedTask != null &&
                        <IconButton
                            sx={{
                                cursor: 'pointer',
                                bgcolor: 'background.paper',
                                border: `${HANDLE_WIDTH}px solid`,
                                borderColor: theme.palette.primary.main,
                                width: ADD_BUTTON_SIZE,
                                height: ADD_BUTTON_SIZE,
                            }}
                            onClick={() => {
                                confirm({
                                    title: 'Vorgangsdatenebene',
                                    width: 'md',
                                    hideCancelButton: true,
                                    confirmButtonText: 'Schließen',
                                    children: (
                                        <>
                                            <Typography variant="h6">
                                                Die weitergereichte Vorgangsdatenebene
                                            </Typography>
                                            <ExpandableCodeBlock
                                                value={JSON.stringify(associatedTask?.processData, null, 2)}
                                            />
                                        </>
                                    ),
                                });
                            }}
                        >
                            <DataObject
                                sx={{
                                    color: theme.palette.primary.main,
                                    fontSize: ADD_BUTTON_SIZE - 2,
                                }}
                            />
                        </IconButton>
                    }
                </Box>
            </EdgeLabelRenderer>
        </>
    );
}
