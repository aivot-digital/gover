import {BaseEdge, EdgeLabelRenderer, type EdgeProps, getSmoothStepPath} from '@xyflow/react';
import {Box, IconButton} from '@mui/material';
import {Add} from '@mui/icons-material';
import React, {type ReactNode, useContext} from 'react';
import {ProcessFlowEditorContext} from './process-flow-editor-context';
import {type FlowEdge} from './utils/layout-utils';
import {ADD_BUTTON_SIZE, HANDLE_COLOR, HANDLE_WIDTH} from './data/process-flow-constants';

export function ProcessFlowEditorEdge(props: EdgeProps<FlowEdge>): ReactNode {
    const {
        sourceX,
        sourceY,
        sourcePosition,
        targetX,
        targetY,
        targetPosition,
        markerEnd,
        style,
        data,
    } = props;

    const {
        editable,
        onAddInbetweenNode,

    } = useContext(ProcessFlowEditorContext);

    const [edgePath, labelX, labelY] = getSmoothStepPath({
        sourceX,
        sourceY,
        sourcePosition,
        targetX,
        targetY,
        targetPosition,
    });

    return (
        <>
            <BaseEdge
                path={edgePath}
                markerEnd={markerEnd}
                style={{
                    ...style,
                    strokeWidth: `${HANDLE_WIDTH}px`,
                }}
            />
            {
                editable &&
                <EdgeLabelRenderer>
                    <Box
                        sx={{
                            position: 'absolute',
                            pointerEvents: 'all',
                            transformOrigin: 'center',
                            transform: `translate(-50%, -50%) translate(${labelX}px,${labelY}px)`,
                            zIndex: 100,
                        }}
                    >
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
                                if (data == null) {
                                    return;
                                }

                                onAddInbetweenNode(data.treeEdge.edge.id);
                            }}
                        >
                            <Add
                                sx={{
                                    fontSize: ADD_BUTTON_SIZE - 2,
                                }}
                            />
                        </IconButton>
                    </Box>
                </EdgeLabelRenderer>
            }
        </>
    );
}
