import {BaseEdge, EdgeLabelRenderer, type EdgeProps} from '@xyflow/react';
import {Box, IconButton, useTheme} from '@mui/material';
import {Add} from '@mui/icons-material';
import React, {type ReactNode, useMemo} from 'react';
import {useProcessFlowEditorContext} from './process-flow-editor-context';
import {type FlowEdge} from './utils/layout-utils';
import {ADD_BUTTON_SIZE, HANDLE_COLOR, HANDLE_WIDTH} from './data/process-flow-constants';
import './process-flow-editor-animations.css';
import DataObject from '@aivot/mui-material-symbols-400-outlined/dist/data-object/DataObject';
import Typography from '@mui/material/Typography';
import {useConfirm} from '../../../../../../providers/confirm-provider';
import {ExpandableCodeBlock} from '../../../../../../components/expandable-code-block/expandable-code-block';
import {getLatestTaskForEdge} from './utils/runtime-task-utils';

export function ProcessFlowEditorEdge(props: EdgeProps<FlowEdge>): ReactNode {
    const theme = useTheme();
    const confirm = useConfirm();

    const {
        sourceX,
        sourceY,
        targetX,
        targetY,
        markerEnd,
        style,
        data: optData,
    } = props;

    const {
        editable,
        onAddInbetweenNode,
        runtimeData,
    } = useProcessFlowEditorContext();

    const {
        graphEdge,
        route,
    } = useMemo(() => {
        if (optData == null) {
            throw new Error('Edge data is required for ProcessFlowEditorEdge');
        }
        return optData;
    }, [optData]);

    const nextTaskForEdge = useMemo(() => {
        if (runtimeData == null) {
            return null;
        }

        return getLatestTaskForEdge(
            runtimeData.tasks,
            graphEdge.edge.fromNodeId,
            graphEdge.edge.toNodeId,
        );
    }, [
        graphEdge,
        runtimeData,
    ]);

    const wasPerformed = nextTaskForEdge != null;
    const {
        edgePath,
        labelX,
        labelY,
    } = useMemo(() => {
        if (route.kind === 'rail' && route.railX != null) {
            const sourceBendY = sourceY + 28;
            const targetBendY = targetY - 24;

            return {
                edgePath: buildOrthogonalPath([
                    {
                        x: sourceX,
                        y: sourceY,
                    },
                    {
                        x: sourceX,
                        y: sourceBendY,
                    },
                    {
                        x: route.railX,
                        y: sourceBendY,
                    },
                    {
                        x: route.railX,
                        y: targetBendY,
                    },
                    {
                        x: targetX,
                        y: targetBendY,
                    },
                    {
                        x: targetX,
                        y: targetY,
                    },
                ]),
                labelX: route.railX,
                labelY: sourceBendY + ((targetBendY - sourceBendY) / 2),
            };
        }

        if (Math.abs(targetX - sourceX) <= 12) {
            const remainingHeight = targetY - sourceY;
            const targetApproachY = targetY - Math.min(28, Math.max(18, remainingHeight / 3));

            return {
                edgePath: buildOrthogonalPath([
                    {
                        x: sourceX,
                        y: sourceY,
                    },
                    {
                        x: sourceX,
                        y: targetApproachY,
                    },
                    {
                        x: targetX,
                        y: targetApproachY,
                    },
                    {
                        x: targetX,
                        y: targetY,
                    },
                ]),
                labelX: sourceX,
                labelY: sourceY + (remainingHeight / 2),
            };
        }

        const availableHeight = targetY - sourceY;
        const bendY = sourceY + Math.min(36, Math.max(20, availableHeight / 2));

        return {
            edgePath: buildOrthogonalPath([
                {
                    x: sourceX,
                    y: sourceY,
                },
                {
                    x: sourceX,
                    y: bendY,
                },
                {
                    x: targetX,
                    y: bendY,
                },
                {
                    x: targetX,
                    y: targetY,
                },
            ]),
            labelX: (sourceX + targetX) / 2,
            labelY: bendY,
        };
    }, [route, sourceX, sourceY, targetX, targetY]);

    return (
        <>
            <BaseEdge
                path={edgePath}
                markerEnd={markerEnd}
                style={{
                    ...style,
                    strokeWidth: `${HANDLE_WIDTH}px`,
                    strokeLinecap: 'round',
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
                        transform: `translate(-50%, -50%) translate(${labelX}px,${labelY}px)`,
                        zIndex: 100,
                    }}
                >
                    {
                        editable &&
                        nextTaskForEdge == null &&
                        <IconButton
                            sx={{
                                'cursor': 'pointer',
                                'bgcolor': 'background.paper',
                                'border': `${HANDLE_WIDTH}px solid`,
                                'borderColor': HANDLE_COLOR,
                                'width': ADD_BUTTON_SIZE,
                                'height': ADD_BUTTON_SIZE,
                                '&:hover': {
                                    bgcolor: '#efefef',
                                },
                            }}
                            onClick={() => {
                                onAddInbetweenNode(graphEdge.edge.id);
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
                        nextTaskForEdge != null &&
                        <IconButton
                            sx={{
                                'cursor': 'pointer',
                                'bgcolor': 'background.paper',
                                'border': `${HANDLE_WIDTH}px solid`,
                                'borderColor': theme.palette.primary.main,
                                'width': ADD_BUTTON_SIZE,
                                'height': ADD_BUTTON_SIZE,
                                '&:hover': {
                                    bgcolor: '#efefef',
                                },
                            }}
                            onClick={() => {
                                void confirm({
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
                                                value={JSON.stringify(nextTaskForEdge?.processData, null, 2)}
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

interface PathPoint {
    x: number;
    y: number;
}

function buildOrthogonalPath(points: PathPoint[]): string {
    const normalizedPoints = points.filter((point, index) => (
        index === 0 ||
        point.x !== points[index - 1].x ||
        point.y !== points[index - 1].y
    ));

    if (normalizedPoints.length === 0) {
        return '';
    }

    if (normalizedPoints.length === 1) {
        return `M ${normalizedPoints[0].x} ${normalizedPoints[0].y}`;
    }

    const radius = 14;
    let path = `M ${normalizedPoints[0].x} ${normalizedPoints[0].y}`;

    for (let index = 1; index < normalizedPoints.length; index += 1) {
        const previousPoint = normalizedPoints[index - 1];
        const currentPoint = normalizedPoints[index];
        const nextPoint = normalizedPoints[index + 1];

        if (nextPoint == null) {
            path += ` L ${currentPoint.x} ${currentPoint.y}`;
            continue;
        }

        const previousSegmentLength = Math.abs(currentPoint.x - previousPoint.x) + Math.abs(currentPoint.y - previousPoint.y);
        const nextSegmentLength = Math.abs(nextPoint.x - currentPoint.x) + Math.abs(nextPoint.y - currentPoint.y);
        const bendRadius = Math.min(radius, previousSegmentLength / 2, nextSegmentLength / 2);

        const entryPoint = {
            x: currentPoint.x - (Math.sign(currentPoint.x - previousPoint.x) * bendRadius),
            y: currentPoint.y - (Math.sign(currentPoint.y - previousPoint.y) * bendRadius),
        };
        const exitPoint = {
            x: currentPoint.x + (Math.sign(nextPoint.x - currentPoint.x) * bendRadius),
            y: currentPoint.y + (Math.sign(nextPoint.y - currentPoint.y) * bendRadius),
        };

        path += ` L ${entryPoint.x} ${entryPoint.y}`;
        path += ` Q ${currentPoint.x} ${currentPoint.y} ${exitPoint.x} ${exitPoint.y}`;
    }

    return path;
}
