import {useEffect, useId, useRef, useState} from 'react';
import {Link, useLocation, useSearchParams} from 'react-router-dom';
import Task from '@aivot/mui-material-symbols-400-n25-outlined/Task';
import OpenInNew from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import {activeHistoryTaskStatuses, resolveHistoryTaskTarget} from '../../utils/process-history-navigation';
import {getHistoryEventActor, getTaskHistoryEvents, isManualHistoryIntervention, type TaskHistoryEvent} from '../../utils/process-history-events';
import {ProcessHistoryEventDetailsDialog} from '../../dialogs/process-history-event-details-dialog';
import UnfoldMore from '@aivot/mui-material-symbols-400-n25-outlined/UnfoldMore';
import UnfoldLess from '@aivot/mui-material-symbols-400-n25-outlined/UnfoldLess';
import News from '@aivot/mui-material-symbols-400-n25-outlined/News';
import DataObject from '@aivot/mui-material-symbols-400-n25-outlined/DataObject';
import Replay from '@aivot/mui-material-symbols-400-n25-outlined/Replay';
import AccountTree from '@aivot/mui-material-symbols-400-n25-outlined/AccountTree';
import {Actions} from '../../../../components/actions/actions';
import {ProcessInstanceEventDialog} from '../../dialogs/process-instance-event-dialog';
import {ProcessInstanceTaskDataDialog, type ProcessInstanceTaskDataKind} from '../../dialogs/process-instance-task-data-dialog';
import {Alert, Box, Button, Paper, Skeleton, Stack, AlertTitle, Typography} from '@mui/material';
import KeyboardArrowDown from '@aivot/mui-material-symbols-400-n25-outlined/KeyboardArrowDown';
import Schedule from '@aivot/mui-material-symbols-400-n25-outlined/Schedule';
import Person from '@aivot/mui-material-symbols-400-n25-outlined/Person';
import {Accordion, AccordionDetails, AccordionSummary} from '../../../../components/accordion/accordion';
import {ProcessExecutionSummary} from '../../components/process-execution-summary';
import {useGenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {useGenericDetailsPageEvent} from '../../../../components/generic-details-page/use-generic-details-page-event';
import {Permission} from '../../../../data/permissions/permission';
import {useHasProcessInstancePermission, useHasProcessPermission} from '../../../permissions/hooks/use-permissions';
import {type ProcessInstanceDetails} from '../../entities/process-instance-details';
import {type ProcessInstanceTaskEntity} from '../../entities/process-instance-task-entity';
import {type ProcessNodeEntity} from '../../entities/process-node-entity';
import {
    type ProcessNodeProvider,
    ProcessNodeProviderApiService,
    ProcessNodeType,
} from '../../services/process-node-provider-api-service';
import {ProcessInstanceTaskStatusIcon} from '../../components/process-instance-task-status-icon';
import {ProcessInstanceEventEntity, type ProcessInstanceHistoryEvent, ProcessNodeExecutionLogLevel} from "../../entities/process-instance-event-entity";
import {ProcessNodeApiService} from "../../services/process-node-api-service";
import {ProcessInstanceTaskApiService} from "../../services/process-instance-task-api-service";
import {ProcessInstanceEventApiService} from "../../services/process-instance-event-api-service";
import {useAppDispatch} from "../../../../hooks/use-app-dispatch";
import {showApiErrorSnackbar, showSuccessSnackbar} from "../../../../slices/snackbar-slice";
import {getNodeDescription} from "./components/process-flow-editor/utils/node-utils";
import {type ChipProps} from '../../../../components/chip/chip';
import {ProcessDeadlineChip} from '../../components/process-deadline-value';
import {getProcessTaskDeadlineState, hasActiveTaskDeadline} from '../../utils/process-task-deadline';
import {useDeadlineNow} from '../../hooks/use-deadline-now';
import {formatDateTimeWithRelative} from "../../components/process-detail-values";
import {humanizeMillisecondsDuration} from "../../../../utils/duration-utils";
import {ProcessDefinitionEdgeApiService} from '../../services/process-definition-edge-api-service';
import {isProcessTaskEditableByStaff, ProcessTaskStatus} from '../../enums/process-task-status';
import {getProcessTaskStatusDisplay} from '../../utils/process-task-status-label';
import {type IdentityDataMap} from '../../../identity/models/identity-data';
import {isProcessInstanceFinished} from '../../enums/process-instance-status';
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
            key={processInstance.instance.id}
            processInstanceDetails={processInstance}
        />
    );
}

function HistorySkeleton({showUpcomingSteps = true}: {showUpcomingSteps?: boolean}) {
    return (
        <Stack
            spacing={3}
            role="status"
            aria-label="Verlauf wird geladen"
            aria-busy="true"
        >
            <Box aria-hidden="true">
                <Stack direction="row" spacing={2} sx={{alignItems: 'center', justifyContent: 'space-between'}}>
                    <Skeleton width={240} height={32}/>
                    <Stack direction="row" spacing={2} sx={{alignItems: 'center'}}>
                        <Skeleton variant="rounded" width={200} height={36}/>
                        <Skeleton variant="circular" width={32} height={32}/>
                        <Skeleton variant="circular" width={32} height={32}/>
                    </Stack>
                </Stack>
                <Skeleton width="100%" height={24} sx={{mt: 1, maxWidth: 850}}/>
            </Box>

            <HistoryRowsSkeleton widths={['85%', '70%', '80%']} expandable/>

            {showUpcomingSteps && (
                <Box aria-hidden="true">
                    <Skeleton width={320} height={32}/>
                    <Skeleton width="100%" height={24} sx={{mt: 1, mb: 3, maxWidth: 850}}/>
                    <HistoryRowsSkeleton widths={['75%', '60%']}/>
                </Box>
            )}

            <Box aria-hidden="true">
                <Skeleton width={220} height={32}/>
                <Skeleton width="100%" height={24} sx={{mt: 1, mb: 2, maxWidth: 820}}/>
                <HistoryRowsSkeleton widths={['65%', '55%']} metadata/>
            </Box>

            <Box aria-hidden="true" sx={{display: 'flex', justifyContent: 'flex-end'}}>
                <Skeleton variant="rounded" width={240} height={36}/>
            </Box>
        </Stack>
    );
}

function HistoryRowsSkeleton({widths, expandable = false, metadata = false}: {widths: string[]; expandable?: boolean; metadata?: boolean}) {
    return (
        <Paper variant="outlined" aria-hidden="true" sx={{overflow: 'hidden'}}>
            {widths.map((width, index) => (
                <Box
                    key={index}
                    sx={{
                        display: 'flex', alignItems: 'center', gap: 2, px: 2, py: 1.5,
                        '& + div': {borderTop: '1px solid', borderColor: 'divider'},
                    }}
                >
                    <Skeleton variant="circular" width={24} height={24} sx={{flexShrink: 0}}/>
                    <Box sx={{flex: 1, minWidth: 0}}>
                        <Skeleton width={width} height={24}/>
                        {metadata && <Skeleton width="40%" height={18}/>}
                    </Box>
                    {expandable && <Skeleton width={24} height={24} sx={{flexShrink: 0}}/>}
                </Box>
            ))}
        </Paper>
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
    const [searchParams] = useSearchParams();
    const location = useLocation();
    const requestedTask = searchParams.get('taskId');
    const handledTaskTarget = useRef<string | null>(null);
    const [loadFailed, setLoadFailed] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [jumpNotice, setJumpNotice] = useState<string>();
    const [scrollTarget, setScrollTarget] = useState<string>();
    const [selectedEventId, setSelectedEventId] = useState<number>();

    const canRead = useHasProcessInstancePermission(processInstanceId, Permission.PROCESS_INSTANCE_READ);
    const canViewProcess = useHasProcessPermission(processDefinitionId, Permission.PROCESS_DEFINITION_READ);

    const [result, setResult] = useState<HistoryData>();
    const [refreshCounter, setRefreshCounter] = useState(0);
    const [expandedTaskIds, setExpandedTaskIds] = useState<Set<number>>(() => new Set());

    const [eventTarget, setEventTarget] = useState<{taskId: number | null} | null>(null);
    const [dataTarget, setDataTarget] = useState<{taskId: number; kind: ProcessInstanceTaskDataKind} | null>(null);

    useGenericDetailsPageEvent('refresh', () => {
        setRefreshCounter(counter => counter + 1);
    });

    useEffect(() => {
        if (!canRead || !canViewProcess) {
            setResult(undefined);
            return;
        }

        let isActive = true;
        setLoadFailed(false);
        setIsLoading(true);

        // Keep the displayed timeline mounted during refreshes to preserve expanded entries.
        fetchHistory(processDefinitionId, processInstanceId, processInstance.initialProcessVersion)
            .then(history => {
                if (isActive) {
                    setResult(history);
                }
            })
            .catch((error) => {
                if (isActive) {
                    setLoadFailed(true);
                    dispatch(showApiErrorSnackbar(error, 'Der Verlauf konnte nicht geladen werden.'));
                }
            })
            .finally(() => { if (isActive) setIsLoading(false); });

        return () => {
            isActive = false;
        };
    }, [canRead, canViewProcess, processInstanceId, processDefinitionId, processInstance.initialProcessVersion, dispatch, refreshCounter]);

    useEffect(() => {
        if (!canRead || !canViewProcess || result == null) return;
        if (requestedTask == null) {
            handledTaskTarget.current = null;
            setJumpNotice(undefined);
            return;
        }
        const requestKey = `${location.key}:${requestedTask}`;
        if (handledTaskTarget.current === requestKey) return;
        const ids = resolveHistoryTaskTarget(result.tasks, requestedTask);
        if (ids.length === 0) {
            setJumpNotice('Das Sprungziel ist im angezeigten Verlauf nicht verfügbar.');
            return;
        }
        // A target can become available after a refresh, for example while a task is restarting.
        handledTaskTarget.current = requestKey;
        setExpandedTaskIds(previous => new Set([...previous, ...ids]));
        setJumpNotice(undefined);
        setScrollTarget(`history-${processInstanceId}-task-${ids[0]}`);
    }, [requestedTask, location.key, result, processInstanceId, canRead, canViewProcess]);

    useEffect(() => {
        if (scrollTarget == null || !canRead || !canViewProcess) return;
        const element = document.getElementById(scrollTarget);
        const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
        element?.scrollIntoView?.({block: 'start', behavior: reduceMotion ? 'auto' : 'smooth'});
        const focusTarget = element?.querySelector<HTMLElement>('.MuiAccordionSummary-root') ?? element;
        focusTarget?.focus({preventScroll: true});
        setScrollTarget(undefined);
    }, [scrollTarget, canRead, canViewProcess]);

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

    if (result == null && loadFailed) {
        return (
            <Alert severity="error" action={<Button color="inherit" loading={isLoading} onClick={() => setRefreshCounter(counter => counter + 1)}>Erneut versuchen</Button>}>
                Der Verlauf konnte nicht geladen werden.
            </Alert>
        );
    }

    if (result == null) {
        return <HistorySkeleton showUpcomingSteps={!isProcessInstanceFinished(processInstance.status)}/>;
    }

    const dataTask = result.tasks.find(task => task.id === dataTarget?.taskId);
    const interventions = result.events.filter(isManualHistoryIntervention);
    const instanceErrors = result.events.filter(event => event.processInstanceTaskId == null &&
        event.level === ProcessNodeExecutionLogLevel.Error && !isManualHistoryIntervention(event));
    const selectedEvent = result.events.find(event => event.id === selectedEventId);

    return (
        <Stack spacing={3}>
            <Box>
                <Stack direction="row" spacing={2} sx={{alignItems: 'center', justifyContent: 'space-between'}}>
                    <Typography variant="h5">
                        Verlauf des Vorgangs
                    </Typography>
                    <Stack direction="row" spacing={2} sx={{alignItems: 'center'}}>
                        <Button startIcon={<News/>} onClick={() => setEventTarget({taskId: null})}>
                            Ereignisprotokoll öffnen
                        </Button>
                        {result.tasks.length > 0 && (
                            <Actions dense actions={[
                                {
                                    icon: <UnfoldMore/>,
                                    tooltip: 'Alle ausklappen',
                                    disabled: result.tasks.every(task => expandedTaskIds.has(task.id)),
                                    onClick: () => setExpandedTaskIds(new Set(result.tasks.map(task => task.id))),
                                },
                                {
                                    icon: <UnfoldLess/>,
                                    tooltip: 'Alle einklappen',
                                    disabled: !result.tasks.some(task => expandedTaskIds.has(task.id)),
                                    onClick: () => setExpandedTaskIds(new Set()),
                                },
                            ]}/>
                        )}
                    </Stack>
                </Stack>
                <Box
                    sx={{
                        mt: 1,
                        maxWidth: 850,
                    }}
                >
                    <Typography>
                        Chronologischer Verlauf der Aufgaben mit ihren Ereignissen und Ergebnissen.
                    </Typography>
                </Box>
            </Box>

            {loadFailed && <Alert severity="error" action={<Button color="inherit" loading={isLoading} onClick={() => setRefreshCounter(counter => counter + 1)}>Erneut versuchen</Button>}>Der angezeigte Verlauf konnte nicht aktualisiert werden.</Alert>}
            {jumpNotice != null && <Alert severity="info">{jumpNotice}</Alert>}

            {instanceErrors.length > 0 && (
                <Box component="section" aria-labelledby={`history-${processInstanceId}-errors`}>
                    <Typography variant="h6" id={`history-${processInstanceId}-errors`} sx={{mb: 1}}>Fehler auf Vorgangsebene</Typography>
                    <Stack spacing={1}>
                        {instanceErrors.map(event => (
                            <Alert key={event.id} severity="error" action={<Button color="inherit" size="small" onClick={() => setSelectedEventId(event.id)}>Details anzeigen</Button>}>
                                <AlertTitle>{event.title}</AlertTitle>
                                <Typography sx={{whiteSpace: 'pre-wrap', overflowWrap: 'anywhere'}}>{event.message}</Typography>
                                <Typography component="time" dateTime={event.timestamp} variant="caption" sx={{display: 'block', mt: 0.5}}>{formatDateTimeWithRelative(event.timestamp)}</Typography>
                            </Alert>
                        ))}
                    </Stack>
                </Box>
            )}

            {result.tasks.length === 0 ? (
                <Box sx={{border: '1px dashed', borderColor: 'divider', borderRadius: 1, p: 3, textAlign: 'center'}}>
                    <Typography color="text.secondary">
                        Derzeit sind keine Aufgaben im Verlauf sichtbar.
                    </Typography>
                </Box>
            ) : (
                <Stack
                    direction="column"
                >
                    {result.tasks.map((task, index) => (
                        <TimelineItem
                            key={task.id}
                            index={index}
                            task={task}
                            identities={processInstance.identities}
                            processInstanceAccessKey={processInstance.accessKey}
                            node={task.node}
                            nodeDefinition={task.nodeDefinition}
                            onRestarted={() => setRefreshCounter(counter => counter + 1)}
                            onShowEvents={() => setEventTarget({taskId: task.id})}
                            onShowData={(kind) => setDataTarget({taskId: task.id, kind})}
                            expanded={expandedTaskIds.has(task.id)}
                            onExpandedChange={(expanded) => setExpandedTaskIds(previous => {
                                const next = new Set(previous);
                                if (expanded) next.add(task.id);
                                else next.delete(task.id);
                                return next;
                            })}
                        />
                    ))}
                </Stack>
            )}

            {!isProcessInstanceFinished(processInstance.status) && <Box>
                <Typography variant="h5">
                    Voraussichtliche nächste Schritte
                </Typography>
                <Typography color="text.secondary" sx={{mt: 1}}>
                    Vorschau auf mögliche nächste Schritte in diesem Vorgang. Der weitere Ablauf kann sich durch Verzweigungen ändern.
                </Typography>
                {result.preview.emptyReason === 'unavailable' ? (
                    <Alert severity="warning" sx={{mt: 2}} action={<Button color="inherit" loading={isLoading} onClick={() => setRefreshCounter(counter => counter + 1)}>Erneut versuchen</Button>}>
                        Die nächsten Schritte konnten nicht ermittelt werden. Der bisherige Verlauf bleibt verfügbar.
                    </Alert>
                ) : result.preview.nodes.length > 0 ? (
                    <Paper
                        component="ol"
                        variant="outlined"
                        aria-label="Voraussichtliche nächste Schritte"
                        sx={{listStyle: 'none', p: 0, mb: 0, mt: 3, overflow: 'hidden'}}
                    >
                        {result.preview.nodes.map(({node, provider}) => (
                            <Box
                                key={node.id}
                                component="li"
                                sx={{
                                    display: 'flex', alignItems: 'center', gap: 2, px: 2, py: 1.5, minWidth: 0,
                                    '& + li': {borderTop: '1px solid', borderColor: 'divider'},
                                }}
                            >
                                <Schedule color="disabled" sx={{flexShrink: 0}}/>
                                <TimelineLabel node={node} nodeDefinition={provider}/>
                            </Box>
                        ))}
                    </Paper>
                ) : (
                    <Box sx={{border: '1px dashed', borderColor: 'divider', borderRadius: 1, p: 3, mt: 2, textAlign: 'center'}}>
                        <Typography color="text.secondary">
                            {result.preview.emptyReason === 'multiple-paths'
                                ? 'Nächste Schritte hängen vom weiteren Prozesspfad ab.'
                                : 'Keine nächsten Schritte ableitbar.'}
                        </Typography>
                    </Box>
                )}
            </Box>}
            <Box component="section" aria-labelledby={`history-${processInstanceId}-interventions`}>
                <Typography variant="h5" id={`history-${processInstanceId}-interventions`}>Manuelle Eingriffe</Typography>
                <Typography sx={{mt: 1, mb: 2, maxWidth: 820, color: 'text.secondary'}}>Hier finden Sie manuelle Aktionen außerhalb des modellierten Ablaufs, zum Beispiel Neuzuweisungen und erneute Ausführungen. Eingriffe in eine Aufgabe erscheinen zusätzlich bei deren Ereignissen.</Typography>
                {interventions.length === 0 ? (
                    <Box sx={{border: '1px dashed', borderColor: 'divider', borderRadius: 1, p: 3, textAlign: 'center'}}>
                        <Typography sx={{color: 'text.secondary'}}>Keine manuellen Eingriffe protokolliert.</Typography>
                    </Box>
                ) : (
                    <Paper component="ol" aria-label="Manuelle Eingriffe" variant="outlined" sx={{listStyle: 'none', m: 0, p: 0, overflow: 'hidden'}}>
                        {interventions.map(event => (
                            <Box key={event.id} component="li" sx={{display: 'flex', alignItems: 'center', gap: 2, px: 2, py: 1.5, minWidth: 0, '& + li': {borderTop: '1px solid', borderColor: 'divider'}}}>
                                <Person color="action" sx={{flexShrink: 0}}/>
                                <Box sx={{flex: 1, minWidth: 0}}>
                                    <Typography title={event.title} sx={{fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis'}}>{event.title}</Typography>
                                    <Typography variant="caption" component="div" sx={{color: 'text.secondary'}}>{getHistoryEventActor(event)} · {formatDateTimeWithRelative(event.timestamp)}</Typography>
                                </Box>
                                <Button size="small" sx={{flexShrink: 0}} onClick={() => setSelectedEventId(event.id)}>Details anzeigen</Button>
                            </Box>
                        ))}
                    </Paper>
                )}
            </Box>
            {selectedEvent != null && <ProcessHistoryEventDetailsDialog event={selectedEvent} onClose={() => setSelectedEventId(undefined)}/>}
            <Box sx={{display: 'flex', justifyContent: 'flex-end'}}>
                <Button
                    component={Link}
                    to={`/processes/${processDefinitionId}/versions/${processInstance.initialProcessVersion}/?instanceId=${processInstanceId}`}
                    variant="outlined"
                    startIcon={<AccountTree/>}
                >
                    Im Prozessmodell ansehen
                </Button>
            </Box>
            {eventTarget != null && (
                <ProcessInstanceEventDialog
                    key={eventTarget.taskId ?? 'instance'}
                    open
                    instanceId={processInstanceId}
                    taskId={eventTarget.taskId}
                    onClose={() => setEventTarget(null)}
                />
            )}
            {dataTarget != null && dataTask != null && (
                <ProcessInstanceTaskDataDialog
                    task={dataTask}
                    taskLabel={`${result.tasks.indexOf(dataTask) + 1}. ${getTimelineNodeName(dataTask.node, dataTask.nodeDefinition)}`}
                    previousProcessData={dataTask.previousProcessData}
                    kind={dataTarget.kind}
                    onClose={() => setDataTarget(null)}
                />
            )}
        </Stack>
    );
}

interface HistoryData {
    events: ProcessInstanceHistoryEvent[];
    tasks: TaskWithNodeAndEvents[];
    preview: UpcomingStepsPreview;
}

interface UpcomingStepsPreview {
    nodes: ProcessFlowGraphNode[];
    emptyReason: 'multiple-paths' | 'unknown' | 'unavailable' | null;
}

type TaskWithNodeAndEvents = ProcessInstanceTaskEntity & {
    node: ProcessNodeEntity | null;
    nodeDefinition: ProcessNodeProvider | null;
    taskEvents: TaskHistoryEvent[];
    previousProcessData: Record<string, unknown> | null;
    failureEvent: ProcessInstanceEventEntity | null;
    failureLoadFailed: boolean;
};

async function fetchHistory(processId: number, processInstanceId: number, initialProcessVersion: number): Promise<HistoryData> {
    const [tasks, events] = await Promise.all([
        new ProcessInstanceTaskApiService().listAllOrdered(['started', 'id'], 'ASC', {processInstanceId}),
        new ProcessInstanceEventApiService().listHistory(processInstanceId),
    ]);
    const versions = [...new Set([initialProcessVersion, ...tasks.content.map(task => task.processVersion)])];
    const [nodeResults, providerResult, edgeResults] = await Promise.all([
        Promise.allSettled(versions.map(processVersion => new ProcessNodeApiService().listAll({processId, processVersion}))),
        new ProcessNodeProviderApiService().getNodeProviders().then(value => ({value})).catch(() => ({value: null})),
        Promise.allSettled(versions.map(processDefinitionVersion => new ProcessDefinitionEdgeApiService().listAll({processDefinitionId: processId, processDefinitionVersion}))),
    ]);
    const nodes = [...new Map(nodeResults.flatMap(result => result.status === 'fulfilled' ? result.value.content : []).map(node => [node.id, node])).values()];
    const definitions = providerResult.value ?? [];
    const edges = [...new Map(edgeResults.flatMap(result => result.status === 'fulfilled' ? result.value.content : []).map(edge => [edge.id, edge])).values()];
    let preview: UpcomingStepsPreview = {nodes: [], emptyReason: 'unavailable'};
    if (providerResult.value != null && nodeResults.every(result => result.status === 'fulfilled') && edgeResults.every(result => result.status === 'fulfilled')) {
        try {
            preview = getUpcomingNodes(buildProcessFlowGraph(nodes, edges, definitions), tasks.content);
        } catch {
            // Missing model metadata must not hide the tasks, their data or their execution summaries.
        }
    }

    const failureEvents = new Map<number, {event: ProcessInstanceEventEntity | null; loadFailed: boolean}>(await Promise.all(tasks.content
        .filter(task => task.status === ProcessTaskStatus.Failed)
        .map(async task => {
            try {
                // Execution failures may be technical events excluded by the history-relevant filter.
                const errors = await new ProcessInstanceEventApiService().list(0, 1, ['timestamp', 'id'], 'DESC', {
                    processInstanceId,
                    processInstanceTaskId: task.id,
                    level: ProcessNodeExecutionLogLevel.Error,
                });
                return [task.id, {event: errors.content[0] ?? null, loadFailed: false}] as const;
            } catch {
                return [task.id, {event: null, loadFailed: true}] as const;
            }
        })));
    const tasksById = new Map(tasks.content.map(task => [task.id, task]));
    const tasksWithNodes: TaskWithNodeAndEvents[] = tasks.content
        .filter(task => task.status !== ProcessTaskStatus.Restarted)
        .map(task => {
            const node = nodes.find(node => node.id === task.processNodeId) ?? null;
            const nodeDefinition = definitions.find(definition => definition.key === node?.processNodeDefinitionKey && definition.majorVersion === node?.processNodeDefinitionVersion) ?? null;
            return {
                ...task, node, nodeDefinition,
                taskEvents: getTaskHistoryEvents(task, tasksById, events.content),
                failureEvent: failureEvents.get(task.id)?.event ?? null,
                failureLoadFailed: failureEvents.get(task.id)?.loadFailed ?? false,
                previousProcessData: task.previousProcessInstanceTaskId == null ? {} : tasksById.get(task.previousProcessInstanceTaskId)?.processData ?? null,
            };
        });
    return {tasks: tasksWithNodes, events: events.content, preview};
}

function getUpcomingNodes(graph: ProcessFlowGraph, tasks: ProcessInstanceTaskEntity[]): UpcomingStepsPreview {
    const nodesById = new Map(graph.nodes.map((node) => [node.node.id, node]));
    const upcoming = new Map<number, ProcessFlowGraphNode>();
    let hasOpenBranch = false;

    for (const task of tasks) {
        if (!activeHistoryTaskStatuses.has(task.status)) {
            continue;
        }

        const visited = new Set([task.processNodeId]);
        let current = nodesById.get(task.processNodeId);

        while (current != null) {
            if (current.node.ports.length > 1 || current.outgoingEdges.length > 1) {
                hasOpenBranch = true;
                break;
            }
            // Actions can also choose between ports; that choice is only known at runtime.
            if (
                current.provider.type === ProcessNodeType.FlowControl ||
                current.provider.type === ProcessNodeType.Termination ||
                current.node.ports.length !== 1 ||
                current.outgoingEdges.length !== 1
            ) {
                break;
            }

            const {edge} = current.outgoingEdges[0];
            if (edge.viaPort !== current.node.ports[0].key) {
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

    return {
        nodes: [...upcoming.values()],
        emptyReason: upcoming.size > 0 ? null : hasOpenBranch ? 'multiple-paths' : 'unknown',
    };
}

function getTaskDeadlineChipProps(task: ProcessInstanceTaskEntity, now: number): Pick<ChipProps, 'label'> & {color: 'default' | 'error'} | null {
    const state = getProcessTaskDeadlineState(task, now);
    if (state === 'exceeded') {
        return {color: 'error', label: 'Frist überschritten'};
    }
    // Successful completion needs no extra badge. Keep upcoming deadlines visible while the task is active.
    if (task.finished != null || task.deadline == null || !Number.isFinite(Date.parse(task.deadline))) return null;
    return {color: 'default', label: <>Frist: {formatDateTimeWithRelative(task.deadline)}</>};
}

interface TimelineLabelProps {
    index?: number;
    node: ProcessNodeEntity | null;
    nodeDefinition: ProcessNodeProvider | null;
}

function getTimelineNodeName(node: ProcessNodeEntity | null, provider: ProcessNodeProvider | null): string {
    return node?.name?.trim() || provider?.name?.trim() ||
        (node != null && provider != null ? 'Unbenanntes Prozesselement' : 'Prozesselement nicht verfügbar');
}

function TimelineLabel({index, node, nodeDefinition}: TimelineLabelProps) {
    const name = getTimelineNodeName(node, nodeDefinition);
    const description = node != null && nodeDefinition != null ? getNodeDescription(node, nodeDefinition) : 'Die Beschreibung dieses Prozesselements ist nicht verfügbar.';
    const title = `${index != null ? `${index + 1}. ` : ''}${name}:`;

    return (
        <Typography
            variant="body2"
            component="div"
            title={`${title} ${description}`}
            sx={{flex: '1 1 0%', minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap'}}
        >
            <Box component="span" sx={{fontWeight: 600}}>{title}</Box> {description}
        </Typography>
    );
}

const activeTaskNotices: Partial<Record<ProcessTaskStatus, string>> = {
    [ProcessTaskStatus.Running]: 'Dieses Prozesselement wird derzeit ausgeführt.',
    [ProcessTaskStatus.InProgress]: 'Dieses Prozesselement wird derzeit bearbeitet.',
    [ProcessTaskStatus.AwaitingStaff]: 'Dieses Prozesselement wartet auf Bearbeitung durch Mitarbeiter:innen.',
    [ProcessTaskStatus.Paused]: 'Die Ausführung dieses Prozesselements ist pausiert.',
    [ProcessTaskStatus.AwaitingCustomer]: 'Dieses Prozesselement wartet auf eine Rückmeldung der zugewiesenen Person.',
    [ProcessTaskStatus.AwaitingPayment]: 'Dieses Prozesselement wartet auf die Zahlungsbestätigung.',
};

interface TimelineItemProps extends TimelineLabelProps {
    task: TaskWithNodeAndEvents;
    identities: IdentityDataMap;
    processInstanceAccessKey: string;
    expanded: boolean;
    onExpandedChange: (expanded: boolean) => void;
    onRestarted: () => void;
    onShowEvents: () => void;
    onShowData: (kind: ProcessInstanceTaskDataKind) => void;
}

function TimelineItem({index, node, nodeDefinition, task, identities, processInstanceAccessKey, expanded, onExpandedChange, onRestarted, onShowEvents, onShowData}: TimelineItemProps) {
    const id = useId();
    const dispatch = useAppDispatch();
    const canRestart = useHasProcessInstancePermission(task.processInstanceId, Permission.PROCESS_INSTANCE_RESTART_FAILED);
    const [isRestarting, setIsRestarting] = useState(false);
    const restart = async () => {
        setIsRestarting(true);
        try {
            await new ProcessInstanceTaskApiService().rerunFailedTask(task.id);
            dispatch(showSuccessSnackbar('Die erneute Ausführung der Aufgabe wurde angefordert.'));
            onRestarted();
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Die erneute Ausführung der Aufgabe konnte nicht angefordert werden.'));
        } finally {
            setIsRestarting(false);
        }
    };
    const startedAt = Date.parse(task.started);
    const isOngoing = task.finished == null && activeHistoryTaskStatuses.has(task.status) && Number.isFinite(startedAt);
    const now = useDeadlineNow(hasActiveTaskDeadline(task) || (expanded && isOngoing));
    const elapsedSinceStart = isOngoing && startedAt <= now ? now - startedAt : null;
    const deadlineChipProps = getTaskDeadlineChipProps(task, now);
    const hasSummary = Boolean(task.executionSummaryMarkdown?.trim());
    const status = getProcessTaskStatusDisplay(task, identities);
    const activeNotice = status.identityLabel
        ? `Dieses Prozesselement wartet auf eine Rückmeldung der Identität „${status.identityLabel}“.`
        : activeTaskNotices[task.status];
    const notice = task.status !== ProcessTaskStatus.Failed && !hasSummary
        ? activeNotice ?? (task.taskEvents.length === 0
            ? 'Für diese Aufgabe liegen keine Zusammenfassung und keine verlaufsrelevanten Ereignisse vor.' : undefined)
        : undefined;

    return (
        <Accordion
            id={`history-${task.processInstanceId}-task-${task.id}`}
            component={Paper}
            expanded={expanded}
            onChange={(_event, nextExpanded) => onExpandedChange(nextExpanded)}
            sx={{
                minWidth: 0,
                scrollMarginTop: 16,
                ':not(:first-child):not(:last-child)': {borderRadius: 0},
                ':first-of-type': {borderBottomLeftRadius: 0, borderBottomRightRadius: 0},
                ':last-of-type': {borderTopLeftRadius: 0, borderTopRightRadius: 0},
            }}
        >
            <AccordionSummary
                expandIcon={<KeyboardArrowDown/>}
                id={`${id}-summary`}
                aria-controls={`${id}-details`}
                sx={{
                    minWidth: 0,
                    '& .MuiAccordionSummary-content': {minWidth: 0, alignItems: 'center', gap: 2},
                    '& .MuiSvgIcon-root, & .MuiAccordionSummary-expandIconWrapper': {flexShrink: 0},
                }}
            >
                <ProcessInstanceTaskStatusIcon
                    status={task.status}
                    statusOverride={task.statusOverride}
                    assignedCustomerIdentityLabel={status.identityLabel}
                />
                <TimelineLabel index={index} node={node} nodeDefinition={nodeDefinition}/>
                {deadlineChipProps != null && (
                    <ProcessDeadlineChip {...deadlineChipProps} size="small" sx={{mx: 1, flexShrink: 0}}/>
                )}
            </AccordionSummary>
            <AccordionDetails sx={{p: 2}}>
                <Box sx={{display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) 16rem', gap: 3, alignItems: 'start'}}>
                    <Stack spacing={3} sx={{minWidth: 0}}>
                        {(node == null || nodeDefinition == null) && <Alert severity="info">Informationen zum Prozesselement sind nicht verfügbar. Die gespeicherten Aufgabendaten und Ergebnisse können weiterhin eingesehen werden.</Alert>}
                        {task.status === ProcessTaskStatus.Failed && (
                            <Alert severity="error">
                                <AlertTitle>{task.failureEvent?.title.trim() || 'Aufgabe fehlgeschlagen'}</AlertTitle>
                                {task.failureEvent != null ? (
                                    <Typography sx={{whiteSpace: 'pre-wrap', overflowWrap: 'anywhere'}}>{task.failureEvent.message}</Typography>
                                ) : task.failureLoadFailed
                                    ? 'Die Fehlermeldung konnte nicht geladen werden. Öffnen Sie das Ereignisprotokoll, um weitere Details einzusehen.'
                                    : 'Für diese Aufgabe ist keine Fehlermeldung gespeichert. Weitere Details finden Sie gegebenenfalls im Ereignisprotokoll.'}
                            </Alert>
                        )}
                        {notice != null && <Alert severity="info">{notice}</Alert>}
                        {hasSummary && (
                            <Box>
                                <Typography variant="h6" sx={{mb: 1}}>
                                    Zusammenfassung der Ausführung
                                </Typography>
                                <ProcessExecutionSummary markdown={task.executionSummaryMarkdown!}/>
                            </Box>
                        )}
                        {task.taskEvents.length > 0 && (
                            <Box>
                                <Stack direction="row" spacing={2} sx={{alignItems: 'baseline', justifyContent: 'space-between', mb: 1}}>
                                    <Typography variant="h6">
                                        Ereignisse und Zwischenergebnisse
                                    </Typography>
                                    <Typography variant="caption" sx={{color: 'text.secondary', flexShrink: 0}}>
                                        Älteste zuerst
                                    </Typography>
                                </Stack>
                                <Paper variant="outlined" component="ol" aria-label="Ereignisse und Zwischenergebnisse" sx={{listStyle: 'none', m: 0, p: 0}}>
                                    {task.taskEvents.map(event => (
                                        <Box key={event.id} component="li" sx={{p: 1.5, '& + li': {borderTop: '1px solid', borderColor: 'divider'}}}>
                                            {isManualHistoryIntervention(event) && <Typography variant="caption" sx={{color: 'text.secondary'}}>{event.fromPreviousAttempt ? 'Manueller Eingriff · vorheriger Ausführungsversuch' : 'Manueller Eingriff'}</Typography>}
                                            <Stack direction="row" spacing={2} sx={{alignItems: 'baseline', justifyContent: 'space-between', flexWrap: 'wrap'}}>
                                                <Typography variant="body2" sx={{fontWeight: 600, minWidth: 0, overflowWrap: 'anywhere'}}>{event.title}</Typography>
                                                <Typography component="time" dateTime={event.timestamp} variant="caption" color="text.secondary" sx={{flexShrink: 0}}>
                                                    {formatDateTimeWithRelative(event.timestamp)}
                                                </Typography>
                                            </Stack>
                                            <Typography variant="body2" sx={{mt: 0.5, overflowWrap: 'anywhere'}}>{event.message}</Typography>
                                        </Box>
                                    ))}
                                </Paper>
                            </Box>
                        )}
                    </Stack>
                    <Box component="aside" aria-label="Aufgabendetails" sx={{minWidth: 0, borderLeft: '1px solid', borderColor: 'divider', pl: 2.5}}>
                        <Typography variant="h6">Aufgabendetails</Typography>
                        <Box component="dl" sx={{m: 0, mt: 1.5, '& dt': {typography: 'caption', color: 'text.secondary'}, '& dd': {m: 0, mb: 1.5, typography: 'body2'}}}>
                            <Box component="dt">Beginn</Box>
                            <Box component="dd">{formatDateTimeWithRelative(task.started)}</Box>
                            {elapsedSinceStart != null && <>
                                <Box component="dt">Zeit seit Beginn</Box>
                                <Box component="dd">{elapsedSinceStart < 60_000 ? 'Weniger als eine Minute' : humanizeMillisecondsDuration(Math.floor(elapsedSinceStart / 60_000) * 60_000)}</Box>
                            </>}
                            {task.finished != null && <>
                                <Box component="dt">Ende</Box>
                                <Box component="dd">{formatDateTimeWithRelative(task.finished)}</Box>
                            </>}
                            {task.runtime != null && <>
                                <Box component="dt">Dauer</Box>
                                <Box component="dd">{humanizeMillisecondsDuration(task.runtime)}</Box>
                            </>}
                            {task.deadline != null && <>
                                <Box component="dt">Frist</Box>
                                <Box component="dd">{formatDateTimeWithRelative(task.deadline)}</Box>
                            </>}
                        </Box>
                    </Box>
                </Box>
                <Stack direction="row" sx={{borderTop: '1px solid', borderColor: 'divider', mt: 2.5, pt: 1.5, gap: 1, flexWrap: 'wrap', alignItems: 'center'}}>
                    <Button size="small" startIcon={<News/>} aria-label="Ereignisprotokoll öffnen" onClick={onShowEvents}>
                        Ereignisprotokoll
                    </Button>
                    <Button size="small" startIcon={<DataObject/>} aria-label="Datenänderungen anzeigen" onClick={() => onShowData('changes')}>
                        Datenänderungen
                    </Button>
                    <Button size="small" startIcon={<DataObject/>} aria-label="Elementdaten anzeigen" onClick={() => onShowData('element-data')}>
                        Elementdaten
                    </Button>
                    {isProcessTaskEditableByStaff(task.status) && (
                        <Button size="small" component={Link} to={`/tasks/${task.processInstanceId}/${task.id}`} variant="contained" startIcon={<Task/>} sx={{ml: 'auto'}}>
                            Aufgabe aufrufen
                        </Button>
                    )}
                    {[ProcessTaskStatus.AwaitingCustomer, ProcessTaskStatus.AwaitingPayment].includes(task.status) && processInstanceAccessKey && task.accessKey && (
                        <Button
                            size="small"
                            component="a"
                            href={`/process/${processInstanceAccessKey}/tasks/${task.accessKey}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            variant="contained"
                            startIcon={<Task/>}
                            endIcon={<OpenInNew/>}
                            sx={{ml: 'auto'}}
                        >
                            Aufgabe für Beteiligte aufrufen
                        </Button>
                    )}
                    {task.status === ProcessTaskStatus.Failed && canRestart && (
                        <Button size="small" variant="contained" startIcon={<Replay/>} loading={isRestarting} onClick={restart} sx={{ml: 'auto'}}>
                            Aufgabe erneut ausführen
                        </Button>
                    )}
                </Stack>
            </AccordionDetails>
        </Accordion>
    );
}
