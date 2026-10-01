import {useEffect, useId, useState} from 'react';
import {Alert, Box, Button, Divider, Paper, Skeleton, Stack, Typography} from '@mui/material';
import KeyboardArrowDown from '@aivot/mui-material-symbols-400-n25-outlined/KeyboardArrowDown';
import Schedule from '@aivot/mui-material-symbols-400-n25-outlined/Schedule';
import {Accordion, AccordionDetails, AccordionSummary} from '../../../../components/accordion/accordion';
import {useGenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {Permission} from '../../../../data/permissions/permission';
import {useHasProcessInstancePermission} from '../../../permissions/hooks/use-permissions';
import {type ProcessInstanceDetails} from '../../entities/process-instance-details';
import {type ProcessInstanceTaskEntity} from '../../entities/process-instance-task-entity';
import {type ProcessNodeEntity} from '../../entities/process-node-entity';
import {
    type ProcessNodeProvider,
    ProcessNodeProviderApiService,
    ProcessNodeType,
} from '../../services/process-node-provider-api-service';
import {ProviderTypeStyles} from '../../data/provider-type-styles';
import {ProcessInstanceTaskStatusIcon} from '../../components/process-instance-task-status-icon';
import {ProcessEntity} from "../../entities/process-entity";
import {ProcessInstanceEventEntity} from "../../entities/process-instance-event-entity";
import {ProcessNodeApiService} from "../../services/process-node-api-service";
import {ProcessDefinitionApiService} from "../../services/process-definition-api-service";
import {ProcessInstanceTaskApiService} from "../../services/process-instance-task-api-service";
import {ProcessInstanceEventApiService} from "../../services/process-instance-event-api-service";
import {useAppDispatch} from "../../../../hooks/use-app-dispatch";
import {showApiErrorSnackbar} from "../../../../slices/snackbar-slice";
import {getNodeDescription} from "./components/process-flow-editor/utils/node-utils";
import {Chip} from "../../../../components/chip/chip";
import {formatDateTimeWithRelative} from "../../components/process-detail-values";
import {useNotImplemented} from "../../../../hooks/use-not-implemented";
import {humanizeMillisecondsDuration} from "../../../../utils/duration-utils";
import {ProcessDefinitionEdgeApiService} from '../../services/process-definition-edge-api-service';
import {ProcessTaskStatus} from '../../enums/process-task-status';
import {
    buildProcessFlowGraph,
    type ProcessFlowGraph,
    type ProcessFlowGraphNode,
} from './components/process-flow-editor/utils/process-flow-graph-utils';

export function ProcessInstanceDetailsPageHistory() {
    const {
        item: processInstance,
    } = useGenericDetailsPageContext<ProcessInstanceDetails, undefined>();

    if (processInstance == null) {
        return <HistorySkeleton/>;
    }

    return (
        <InstanceHistory
            processInstanceDetails={processInstance}
        />
    );
}

function HistorySkeleton() {
    return (
        <Stack
            spacing={2}
            role="status"
            aria-label="Verlauf wird geladen"
            aria-busy="true"
        >
            <Skeleton
                width="40%"
                height={32}
            />
            {[0, 1, 2].map((index) => (
                <Skeleton
                    key={index}
                    variant="rounded"
                    height={96}
                />
            ))}
        </Stack>
    );
}

interface InstanceHistoryProps {
    processInstanceDetails: ProcessInstanceDetails;
}


function InstanceHistory(props: InstanceHistoryProps) {
    const {
        processInstanceDetails: processInstanceDetails
    } = props;

    const {
        instance: processInstance,
    } = processInstanceDetails;

    const {
        id: processInstanceId,
        processId: processDefinitionId,
    } = processInstance;

    const dispatch = useAppDispatch();

    const canRead = useHasProcessInstancePermission(processInstanceId, Permission.PROCESS_INSTANCE_READ);
    const canViewProcess = useHasProcessInstancePermission(processInstanceId, Permission.PROCESS_DEFINITION_READ);

    const [result, setResult] = useState<HistoryData>();

    useEffect(() => {
        if (!canRead || !canViewProcess) {
            setResult(undefined);
            return;
        }

        fetchHistory(processDefinitionId, processInstanceId)
            .then(setResult)
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Der Verlauf konnte nicht geladen werden.'));
            });
    }, [canRead, canViewProcess, processInstanceId, processDefinitionId]);

    if (!canRead) {
        return (
            <Alert severity="info">
                Sie haben keine Berechtigung mehr, diesen Vorgang einzusehen.
            </Alert>
        );
    }

    if (!canViewProcess) {
        return (
            <Alert severity="info">
                Sie haben keine Berechtigung, den Prozess zu diesem Vorgang einzusehen.
                Daher kann der Verlauf nicht angezeigt werden.
            </Alert>
        );
    }

    if (result == null) {
        return <HistorySkeleton/>;
    }

    return (
        <Stack spacing={3}>
            <Box>
                <Typography variant="h5">
                    Verlauf des Vorgangs
                </Typography>
                <Box
                    sx={{
                        mt: 1,
                        maxWidth: 850,
                    }}
                >
                    <Typography>
                        Eine Liste aller bereits ausgeführten Verfahrenselemente und ihrer Zwischenergebnisse sowie ein
                        Ausblick auf die nächsten Schritte.
                    </Typography>
                    <Typography sx={{mt: 1.5}}>
                        Bitte beachten Sie, dass nur die Schritte bis zum nächsten Flusselement angezeigt werden.
                    </Typography>
                </Box>
            </Box>

            <Stack
                direction="column"
            >
                {
                    result
                        .tasks
                        .map((task, index) => (
                            <TimelineItem
                                key={task.id}
                                index={index}
                                task={task}
                                node={task.node}
                                nodeDefinition={task.nodeDefinition}
                            />
                        ))
                }
            </Stack>

            <Box>
                <Typography variant="h5">
                    Voraussichtliche nächste Schritte
                </Typography>
                <Box
                    sx={{
                        mt: 1,
                        maxWidth: 850,
                    }}
                >
                    <Typography>
                        Die voraussichtlichen nächsten Verfahrenselemente, die dieser Vorgang durchlaufen wird.
                        Dieser Pfad kann sich durch Verzweigungen in der Modellierung im Verlauf des Vorgangs verändern
                        und dient nur als grober Indikator.
                    </Typography>
                </Box>
            </Box>

            <Stack direction="column">
                {result.upcomingNodes.length > 0 ? (
                    result.upcomingNodes.map(({node, provider}) => (
                        <TimelineItem
                            key={node.id}
                            node={node}
                            nodeDefinition={provider}
                        />
                    ))
                ) : (
                    <Typography>
                        Derzeit sind keine nächsten Schritte vorhersehbar.
                    </Typography>
                )}
            </Stack>
        </Stack>
    );
}

interface HistoryData {
    process: ProcessEntity;
    nodes: ProcessNodeEntity[];
    tasks: TaskWithNodeAndEvents[];
    globalEvents: ProcessInstanceEventEntity[];
    upcomingNodes: ProcessFlowGraphNode[];
}

type TaskWithNodeAndEvents = ProcessInstanceTaskEntity & {
    node: ProcessNodeEntity;
    nodeDefinition: ProcessNodeProvider;
    taskEvents: ProcessInstanceEventEntity[];
};

async function fetchHistory(processId: number, processInstanceId: number): Promise<HistoryData> {
    const [
        process,
        nodes,
        tasks,
        events,
        definitions,
        edges,
    ] = await Promise.all([
        new ProcessDefinitionApiService().retrieve(processId),
        new ProcessNodeApiService().listAll({
            processId: processId,
        }),
        new ProcessInstanceTaskApiService().listAllOrdered('started', 'ASC', {
            processInstanceId: processInstanceId,
        }),
        new ProcessInstanceEventApiService().listAllOrdered('timestamp', 'ASC', {
            processInstanceId: processInstanceId,
            historyRelevant: true,
        }),
        new ProcessNodeProviderApiService()
            .getNodeProviders(),
        new ProcessDefinitionEdgeApiService().listAll({
            processDefinitionId: processId,
        }),
    ]);


    const tasksWithNodes: TaskWithNodeAndEvents[] = tasks
        .content
        .map((task) => {
            const node = nodes
                .content
                .find((node) => node.id === task.processNodeId);

            if (node == null) {
                throw new Error(`Node with ID ${task.processNodeId} not found for task ${task.id}`);
            }

            const nodeDefinition = definitions
                .find((definition) => (
                    definition.key === node.processNodeDefinitionKey &&
                    definition.majorVersion === node.processNodeDefinitionVersion
                ));
            if (nodeDefinition == null) {
                throw new Error(`Node definition with key ${node.processNodeDefinitionKey} and version ${node.processNodeDefinitionVersion} not found for node ${node.id}`);
            }

            const taskEvents = events
                .content
                .filter((event) => event.processInstanceTaskId === task.id);

            return {
                ...task,
                node,
                nodeDefinition,
                taskEvents: taskEvents,
            };
        });

    return {
        process,
        nodes: nodes.content,
        tasks: tasksWithNodes,
        upcomingNodes: getUpcomingNodes(buildProcessFlowGraph(nodes.content, edges.content, definitions), tasks.content),
        globalEvents: events
            .content
            .filter((event) => event.processInstanceTaskId == null)
    };
}

function getUpcomingNodes(graph: ProcessFlowGraph, tasks: ProcessInstanceTaskEntity[]): ProcessFlowGraphNode[] {
    const nodesById = new Map(graph.nodes.map((node) => [node.node.id, node]));
    const upcoming = new Map<number, ProcessFlowGraphNode>();
    const activeStatuses = new Set([
        ProcessTaskStatus.Running,
        ProcessTaskStatus.Paused,
        ProcessTaskStatus.AwaitingCustomer,
        ProcessTaskStatus.AwaitingPayment,
    ]);

    for (const task of tasks) {
        if (!activeStatuses.has(task.status)) {
            continue;
        }

        const visited = new Set([task.processNodeId]);
        let current = nodesById.get(task.processNodeId);

        while (current != null) {
            // Actions can also choose between ports; that choice is only known at runtime.
            if (
                current.provider.type === ProcessNodeType.FlowControl ||
                current.provider.type === ProcessNodeType.Termination ||
                current.provider.ports.length !== 1 ||
                current.outgoingEdges.length !== 1
            ) {
                break;
            }

            const {edge} = current.outgoingEdges[0];
            if (edge.viaPort !== current.provider.ports[0].key) {
                break;
            }

            const next = nodesById.get(edge.toNodeId);
            if (next == null || visited.has(next.node.id)) {
                break;
            }

            visited.add(next.node.id);
            upcoming.set(next.node.id, next);
            current = next;
        }
    }

    return [...upcoming.values()];
}

interface TimelineItemProps {
    index?: number;
    node: ProcessNodeEntity;
    nodeDefinition: ProcessNodeProvider;
    task?: TaskWithNodeAndEvents;
}

function TimelineItem(props: TimelineItemProps) {
    const {
        index,
        node,
        nodeDefinition,
        task,
    } = props;

    const notImplemented = useNotImplemented();

    const nodeDescription = getNodeDescription(node, nodeDefinition);

    const id = useId();
    const name = node.name?.trim() || nodeDefinition.name?.trim() || 'Unbenanntes Prozesselement';

    const {
        label: typeLabel,
        Icon: TypeIcon,
        textColor: typeTextColor,
        bgColor: typeBgColor,
    } = ProviderTypeStyles[nodeDefinition.type];

    return (
        <Accordion
            component={Paper}
            sx={{
                ':not(:first-child):not(:last-child)': {
                    borderRadius: 0,
                },
                ':first-of-type': {
                    borderBottomLeftRadius: 0,
                    borderBottomRightRadius: 0,
                },
                ':last-of-type': {
                    borderTopLeftRadius: 0,
                    borderTopRightRadius: 0,
                },
            }}
        >
            <AccordionSummary
                expandIcon={<KeyboardArrowDown/>}
                id={`${id}-summary`}
                aria-controls={`${id}-details`}
                sx={{
                    display: 'flex',
                    flexDirection: 'row',
                    alignItems: 'center',
                    justifyContent: 'flex-start',
                }}
            >
                {
                    task == null
                        ? <Schedule color="disabled"/>
                        : <ProcessInstanceTaskStatusIcon status={task.status}/>
                }

                <Typography
                    sx={{
                        fontWeight: 600,
                        ml: 2,
                    }}
                >
                    {index != null ? `${index + 1}.` : ''} {name}:
                </Typography>

                <Typography
                    variant="body2"
                    sx={{
                        flex: 1,
                        textOverflow: 'ellipsis',
                        overflow: 'hidden',
                        whiteSpace: 'nowrap',
                        ml: 1,
                    }}
                >
                    {nodeDescription}
                </Typography>

                <Chip
                    mode="soft"
                    icon={<TypeIcon fontSize="small"/>}
                    label={typeLabel}
                    sx={{
                        color: typeTextColor,
                        backgroundColor: typeBgColor,
                        mr: 1,
                    }}
                    size="small"
                />
            </AccordionSummary>
            <AccordionDetails
                sx={{
                    p: 2,
                }}
            >
                {
                    task != null &&
                    <>
                        {
                            task.taskEvents.length > 0
                                ? (
                                    <Stack
                                        direction="column"
                                        spacing={2}
                                    >
                                        <Typography
                                            variant="h6"
                                        >
                                            Ereignisse und Zwischenergebnisse für diese Aufgabe
                                        </Typography>

                                        {
                                            task
                                                .taskEvents
                                                .map((event) => (
                                                    <Paper
                                                        variant="outlined"
                                                    >
                                                        <Box
                                                            sx={{
                                                                p: 1,
                                                            }}
                                                        >
                                                            <Typography
                                                                sx={{
                                                                    fontWeight: 600,
                                                                }}
                                                            >
                                                                {event.title}
                                                            </Typography>
                                                            <Typography
                                                                variant="body2"
                                                            >
                                                                {event.message}
                                                            </Typography>
                                                        </Box>

                                                        <Divider/>

                                                        <Box
                                                            sx={{
                                                                p: 1,
                                                                fontSize: '0.85rem',
                                                            }}
                                                        >
                                                            {formatDateTimeWithRelative(event.timestamp)}
                                                        </Box>
                                                    </Paper>
                                                ))
                                        }
                                    </Stack>
                                )
                                : (
                                    <>
                                        <Typography>
                                            Es existieren keine relevanten Zwischenergebnisse für diese Aufgabe.
                                        </Typography>
                                    </>
                                )
                        }

                        <Stack
                            direction="row"
                            spacing={2}
                            sx={{
                                mt: 2,
                                justifyContent: 'flex-end',
                            }}
                        >
                            {
                                [
                                    {
                                        event: 'showDataChange',
                                        label: 'Datenänderungen anzeigen',
                                        visible: task.finished != null,
                                    },
                                    {
                                        event: 'showAllEvents',
                                        label: 'Alle Ereignisse anzeigen',
                                        visible: true,
                                    },
                                    {
                                        event: 'showElementData',
                                        label: 'Elementdaten anzeigen',
                                        visible: task.finished != null,
                                    },
                                ]
                                    .filter((button) => button.visible)
                                    .map(({event, label}) => (
                                        <Button
                                            variant="outlined"
                                            size="small"
                                            sx={{
                                                fontSize: '0.75rem',
                                            }}
                                            onClick={() => {
                                                notImplemented();
                                            }}
                                        >
                                            {label}
                                        </Button>
                                    ))
                            }
                        </Stack>

                    </>
                }

                <Divider
                    sx={{
                        my: 2,
                    }}
                />

                {
                    task != null &&
                    <>
                        <Typography
                            color="textSecondary"
                            sx={{
                                '& span::before': {
                                    'content': '" • "',
                                },
                                '& span:first-of-type::before': {
                                    'content': '""',
                                }
                            }}
                        >
                            <span>Beginn der Ausführung: {formatDateTimeWithRelative(task.started)}</span>
                            {
                                task.finished != null &&
                                <span>Ende der Ausführung: {formatDateTimeWithRelative(task.finished)}</span>
                            }
                            {
                                task.runtime != null &&
                                <span>Ausführungsdauer: {humanizeMillisecondsDuration(task.runtime)}</span>
                            }
                        </Typography>
                    </>
                }
            </AccordionDetails>
        </Accordion>
    );
}
