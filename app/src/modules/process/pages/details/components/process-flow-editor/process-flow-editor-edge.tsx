import {BaseEdge, EdgeLabelRenderer, type EdgeProps} from '@xyflow/react';
import {Box, IconButton, useTheme} from '@mui/material';
import {Add} from '@mui/icons-material';
import React, {type ReactNode, useMemo} from 'react';
import {useProcessFlowEditorContext} from './process-flow-editor-context';
import {type FlowEdge, type FlowPathPoint} from './utils/layout-utils';
import {ADD_BUTTON_ICON_SIZE, ADD_BUTTON_SIZE, HANDLE_COLOR, HANDLE_WIDTH} from './data/process-flow-constants';
import './process-flow-editor-animations.css';
import DataObject from '@aivot/mui-material-symbols-400-outlined/dist/data-object/DataObject';
import Typography from '@mui/material/Typography';
import {useConfirm} from '../../../../../../providers/confirm-provider';
import {ExpandableCodeBlock} from '../../../../../../components/expandable-code-block/expandable-code-block';
import {getLatestTaskForEdge} from './utils/runtime-task-utils';

function ProcessFlowEditorEdgeComponent(props: EdgeProps<FlowEdge>): ReactNode {
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
        routePoints,
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
        labelPoint,
    } = useMemo(() => {
        const renderedRoutePoints = buildRenderedRoutePoints(routePoints, sourceX, sourceY, targetX, targetY);

        return {
            edgePath: buildOrthogonalPath(renderedRoutePoints),
            labelPoint: getPreferredLabelPoint(renderedRoutePoints),
        };
    }, [routePoints, sourceX, sourceY, targetX, targetY]);

    return (
        <>
            <BaseEdge
                path={edgePath}
                markerEnd={markerEnd}
                style={{
                    ...style,
                    strokeWidth: `${HANDLE_WIDTH}px`,
                    strokeLinecap: 'round',
                    strokeLinejoin: 'round',
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
                        transform: `translate(-50%, -50%) translate(${labelPoint.x}px,${labelPoint.y}px)`,
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
                                'padding': 0,
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
                                    fontSize: ADD_BUTTON_ICON_SIZE,
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
                                'padding': 0,
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
                                    fontSize: ADD_BUTTON_ICON_SIZE,
                                }}
                            />
                        </IconButton>
                    }
                </Box>
            </EdgeLabelRenderer>
        </>
    );
}

export const ProcessFlowEditorEdge = React.memo(ProcessFlowEditorEdgeComponent);
ProcessFlowEditorEdge.displayName = 'ProcessFlowEditorEdge';

interface PathPoint {
    x: number;
    y: number;
}

function buildRenderedRoutePoints(
    routePoints: FlowPathPoint[],
    sourceX: number,
    sourceY: number,
    targetX: number,
    targetY: number,
): PathPoint[] {
    const rawPoints = routePoints.length > 0
        ? routePoints.map((point) => ({...point}))
        : [
            {
                x: sourceX,
                y: sourceY,
            },
            {
                x: targetX,
                y: targetY,
            },
        ];

    if (rawPoints.length === 1) {
        rawPoints.push({
            x: targetX,
            y: targetY,
        });
    }

    rawPoints[0] = {
        x: sourceX,
        y: sourceY,
    };
    rawPoints[rawPoints.length - 1] = {
        x: targetX,
        y: targetY,
    };

    return normalizeOrthogonalPoints(rawPoints);
}

function normalizeOrthogonalPoints(points: PathPoint[]): PathPoint[] {
    const normalizedPoints: PathPoint[] = [];

    points.forEach((point, index) => {
        if (normalizedPoints.length === 0) {
            pushPoint(normalizedPoints, point);
            return;
        }

        const previousPoint = normalizedPoints[normalizedPoints.length - 1];
        const isLastInputPoint = index === points.length - 1;
        const isFirstSegment = normalizedPoints.length === 1;

        if (previousPoint.x !== point.x && previousPoint.y !== point.y) {
            if (isFirstSegment && isLastInputPoint) {
                const bendY = previousPoint.y + ((point.y - previousPoint.y) / 2);
                pushPoint(normalizedPoints, {
                    x: previousPoint.x,
                    y: bendY,
                });
                pushPoint(normalizedPoints, {
                    x: point.x,
                    y: bendY,
                });
            } else if (isFirstSegment) {
                pushPoint(normalizedPoints, {
                    x: previousPoint.x,
                    y: point.y,
                });
            } else {
                pushPoint(normalizedPoints, {
                    x: point.x,
                    y: previousPoint.y,
                });
            }
        }

        pushPoint(normalizedPoints, point);
    });

    return collapseCollinearPoints(normalizedPoints);
}

function pushPoint(points: PathPoint[], point: PathPoint): void {
    const previousPoint = points[points.length - 1];
    if (previousPoint != null && previousPoint.x === point.x && previousPoint.y === point.y) {
        return;
    }

    points.push(point);
}

function collapseCollinearPoints(points: PathPoint[]): PathPoint[] {
    if (points.length <= 2) {
        return points;
    }

    const collapsedPoints: PathPoint[] = [points[0]];

    for (let index = 1; index < points.length - 1; index += 1) {
        const previousPoint = collapsedPoints[collapsedPoints.length - 1];
        const currentPoint = points[index];
        const nextPoint = points[index + 1];

        if (
            (previousPoint.x === currentPoint.x && currentPoint.x === nextPoint.x) ||
            (previousPoint.y === currentPoint.y && currentPoint.y === nextPoint.y)
        ) {
            continue;
        }

        collapsedPoints.push(currentPoint);
    }

    collapsedPoints.push(points[points.length - 1]);
    return collapsedPoints;
}

function getPolylineMidpoint(points: PathPoint[]): PathPoint {
    if (points.length === 0) {
        return {
            x: 0,
            y: 0,
        };
    }

    if (points.length === 1) {
        return points[0];
    }

    const totalLength = points.reduce((length, point, index) => {
        if (index === 0) {
            return length;
        }

        const previousPoint = points[index - 1];
        return length + getSegmentLength(previousPoint, point);
    }, 0);

    if (totalLength === 0) {
        return points[0];
    }

    const midpointDistance = totalLength / 2;
    let travelledDistance = 0;

    for (let index = 1; index < points.length; index += 1) {
        const previousPoint = points[index - 1];
        const currentPoint = points[index];
        const segmentLength = getSegmentLength(previousPoint, currentPoint);

        if (travelledDistance + segmentLength >= midpointDistance) {
            const remainingDistance = midpointDistance - travelledDistance;
            const progress = segmentLength === 0 ? 0 : remainingDistance / segmentLength;

            return {
                x: previousPoint.x + ((currentPoint.x - previousPoint.x) * progress),
                y: previousPoint.y + ((currentPoint.y - previousPoint.y) * progress),
            };
        }

        travelledDistance += segmentLength;
    }

    return points[points.length - 1];
}

function getPreferredLabelPoint(points: PathPoint[]): PathPoint {
    const segments = getLineSegments(points);
    if (segments.length === 0) {
        return getPolylineMidpoint(points);
    }

    const preferredSegments = segments.length > 2
        ? segments.filter((segment) => !segment.isFirst && !segment.isLast)
        : segments;
    const candidateSegments = preferredSegments.length > 0 ? preferredSegments : segments;
    const minimumSegmentLength = (ADD_BUTTON_SIZE * 2) + 10;

    const selectedSegment = [...candidateSegments]
        .sort((a, b) => (
            b.length - a.length ||
            Math.abs(a.index - ((segments.length - 1) / 2)) - Math.abs(b.index - ((segments.length - 1) / 2))
        ))
        .find((segment) => segment.length >= minimumSegmentLength) ?? candidateSegments[0];

    if (selectedSegment == null) {
        return getPolylineMidpoint(points);
    }

    return {
        x: (selectedSegment.start.x + selectedSegment.end.x) / 2,
        y: (selectedSegment.start.y + selectedSegment.end.y) / 2,
    };
}

function getSegmentLength(startPoint: PathPoint, endPoint: PathPoint): number {
    return Math.abs(endPoint.x - startPoint.x) + Math.abs(endPoint.y - startPoint.y);
}

function getLineSegments(points: PathPoint[]): Array<{
    start: PathPoint;
    end: PathPoint;
    length: number;
    index: number;
    isFirst: boolean;
    isLast: boolean;
}> {
    if (points.length < 2) {
        return [];
    }

    return points.slice(1).map((point, index, array) => ({
        start: points[index],
        end: point,
        length: getSegmentLength(points[index], point),
        index,
        isFirst: index === 0,
        isLast: index === array.length - 1,
    }));
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
        const entryDistance = Math.min(radius, previousSegmentLength / 2);
        const exitDistance = Math.min(radius, nextSegmentLength / 2);

        const entryPoint = {
            x: currentPoint.x - (Math.sign(currentPoint.x - previousPoint.x) * entryDistance),
            y: currentPoint.y - (Math.sign(currentPoint.y - previousPoint.y) * entryDistance),
        };
        const exitPoint = {
            x: currentPoint.x + (Math.sign(nextPoint.x - currentPoint.x) * exitDistance),
            y: currentPoint.y + (Math.sign(nextPoint.y - currentPoint.y) * exitDistance),
        };

        path += ` L ${entryPoint.x} ${entryPoint.y}`;
        path += ` Q ${currentPoint.x} ${currentPoint.y} ${exitPoint.x} ${exitPoint.y}`;
    }

    return path;
}
