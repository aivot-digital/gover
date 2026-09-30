import {type ReactNode, useEffect, useId, useState} from 'react';
import {Alert, Box, Button, Skeleton, Stack, Typography} from '@mui/material';
import KeyboardArrowDown from '@aivot/mui-material-symbols-400-n25-outlined/KeyboardArrowDown';
import PlayCircle from '@aivot/mui-material-symbols-400-n25-outlined/PlayCircle';
import {Accordion, AccordionDetails, AccordionSummary} from '../../../../components/accordion/accordion';
import {useGenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {Permission} from '../../../../data/permissions/permission';
import {useHasProcessInstancePermission, useHasProcessPermission} from '../../../permissions/hooks/use-permissions';
import {type ProcessInstanceDetails} from '../../entities/process-instance-details';
import {type ProcessInstanceTaskEntity} from '../../entities/process-instance-task-entity';
import {type ProcessNodeEntity} from '../../entities/process-node-entity';
import {
    ProcessInstanceStatus,
    ProcessInstanceStatusColor,
    ProcessInstanceStatusIcons,
    ProcessInstanceStatusLabels,
} from '../../enums/process-instance-status';
import {ProcessTaskStatusLabels} from '../../enums/process-task-status';
import {type ProcessNodeProvider, ProcessNodeProviderApiService} from '../../services/process-node-provider-api-service';
import {ProviderTypeStyles} from '../../data/provider-type-styles';
import {ProcessInstanceTaskStatusIcon} from '../../components/process-instance-task-status-icon';
import {formatDateTimeWithRelative, ProcessStatusValue} from '../../components/process-detail-values';
import {ProcessInstanceEventApiService} from '../../services/process-instance-event-api-service';
import {type ProcessInstanceEventEntity} from '../../entities/process-instance-event-entity';
import {ProcessInstanceTaskApiService} from '../../services/process-instance-task-api-service';
import {ProcessNodeApiService} from '../../services/process-node-api-service';
import {type Page} from '../../../../models/dtos/page';

export function ProcessInstanceDetailsPageHistory() {
    const {
        item,
    } = useGenericDetailsPageContext<ProcessInstanceDetails, undefined>();

    return item == null ? (
        <HistorySkeleton/>
    ) : (
        <InstanceHistory
            key={item.instance.id}
            item={item}
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

function InstanceHistory({item}: {item: ProcessInstanceDetails}) {
    const canRead = useHasProcessInstancePermission(item.instance.id, Permission.PROCESS_INSTANCE_READ);
    const canReadDefinition = useHasProcessPermission(item.instance.processId, Permission.PROCESS_DEFINITION_READ);
    const [reload, setReload] = useState(0);
    const [result, setResult] = useState<{
        item: ProcessInstanceDetails;
        reload: number;
        canReadDefinition: boolean;
        history: ProcessInstanceHistoryData | null;
    }>();

    useEffect(() => {
        if (!canRead) {
            setResult(undefined);
            return;
        }

        let active = true;
        fetchHistoryData(item.instance.id, item.instance.processId, canReadDefinition, () => active)
            .then((history) => {
                if (active) setResult({item, reload, canReadDefinition, history});
            })
            .catch(() => {
                if (active) setResult({item, reload, canReadDefinition, history: null});
            });
        return () => {
            active = false;
        };
    }, [item, reload, canRead, canReadDefinition]);

    if (!canRead) {
        return <Alert severity="info">Sie haben keine Berechtigung mehr, diesen Vorgang einzusehen.</Alert>;
    }

    // Invalidate rendered data immediately on refresh or permission changes, before the next effect runs.
    if (result == null || result.item !== item || result.reload !== reload || result.canReadDefinition !== canReadDefinition) {
        return <HistorySkeleton />;
    }
    if (result.history == null) {
        return (
            <Alert
                severity="error"
                action={
                    <Button
                        color="inherit"
                        onClick={() => setReload((value) => value + 1)}
                    >
                        Erneut laden
                    </Button>
                }
            >
                Der Verlauf konnte nicht geladen werden.
            </Alert>
        );
    }

    return <HistoryTimeline item={item} history={result.history} />;
}

interface ProcessInstanceHistoryData {
    events: ProcessInstanceEventEntity[];
    tasks: ProcessInstanceTaskEntity[];
    nodes: ProcessNodeEntity[];
    providers: ProcessNodeProvider[];
}

// BaseReadApiService.listAll only returns the first page, which can truncate long-running histories.
async function loadHistoryPages<T>(loadPage: (page: number) => Promise<Page<T>>, isActive: () => boolean): Promise<T[]> {
    const entries: T[] = [];
    for (let page = 0; isActive(); page++) {
        const result = await loadPage(page);
        entries.push(...result.content);
        if (page + 1 >= result.page.totalPages) break;
    }
    return entries;
}

async function fetchHistoryData(
    instanceId: number,
    processId: number,
    canReadDefinition: boolean,
    isActive: () => boolean,
): Promise<ProcessInstanceHistoryData> {
    const eventService = new ProcessInstanceEventApiService();
    const taskService = new ProcessInstanceTaskApiService();
    const nodeService = new ProcessNodeApiService();
    const [events, tasks, nodes, providers] = await Promise.all([
        loadHistoryPages((page) => eventService.list(page, 999, ['timestamp', 'id'], 'ASC', {
            processInstanceId: instanceId,
            historyRelevant: true,
        }), isActive),
        loadHistoryPages((page) => taskService.list(page, 999, ['started', 'id'], 'ASC', {
            processInstanceId: instanceId,
        }), isActive),
        canReadDefinition
            ? loadHistoryPages((page) => nodeService.list(page, 999, 'id', 'ASC', {processId}), isActive).catch(() => [])
            : Promise.resolve([]),
        canReadDefinition
            ? new ProcessNodeProviderApiService().getNodeProviders().catch(() => [])
            : Promise.resolve([]),
    ]);
    return {events, tasks, nodes, providers};
}

type HistoryEntry =
    | {kind: 'task'; id: number; timestamp: string; task: ProcessInstanceTaskEntity}
    | {kind: 'event'; id: number; timestamp: string; event: ProcessInstanceEventEntity};

function HistoryTimeline({item, history}: {item: ProcessInstanceDetails; history: ProcessInstanceHistoryData}) {
    const {instance} = item;
    const StatusIcon = ProcessInstanceStatusIcons[instance.status];
    const nodes = new Map(history.nodes.map((node) => [node.id, node]));
    const providers = new Map(history.providers.map((provider) => [
        JSON.stringify([provider.key, provider.majorVersion]), provider,
    ]));
    const activeTaskNames = new Map(item.activeTasks.map((task) => [task.id, task.name]));
    const taskEvents = new Map<number, ProcessInstanceEventEntity[]>(history.tasks.map((task) => [task.id, []]));
    const entries: HistoryEntry[] = history.tasks.map((task) => ({kind: 'task', id: task.id, timestamp: task.started, task}));
    const events = [...history.events].sort(
        (left, right) => Date.parse(left.timestamp) - Date.parse(right.timestamp) || left.id - right.id,
    );
    for (const event of events) {
        const associatedEvents = event.processInstanceTaskId == null ? undefined : taskEvents.get(event.processInstanceTaskId);
        if (associatedEvents != null) {
            associatedEvents.push(event);
        } else {
            entries.push({kind: 'event', id: event.id, timestamp: event.timestamp, event});
        }
    }
    entries.sort((left, right) =>
        Date.parse(left.timestamp) - Date.parse(right.timestamp) || left.id - right.id || left.kind.localeCompare(right.kind),
    );
    const endLabel =
        instance.status === ProcessInstanceStatus.Completed
            ? 'Vorgang abgeschlossen'
            : instance.status === ProcessInstanceStatus.Aborted
                ? 'Vorgang abgebrochen'
                : instance.status === ProcessInstanceStatus.Failed
                    ? 'Vorgang fehlgeschlagen'
                    : 'Vorgang beendet';

    return (
        <Stack spacing={3}>
            <Box>
                <Typography
                    variant="h6"
                    component="h2"
                >
                    Aktueller Vorgangsstatus
                </Typography>
                <Stack
                    direction="row"
                    spacing={1}
                    sx={{mt: 1, alignItems: 'center'}}
                >
                    <StatusIcon color={ProcessInstanceStatusColor[instance.status]}/>
                    <Typography>
                        <ProcessStatusValue
                            systemLabel={ProcessInstanceStatusLabels[instance.status]}
                            statusOverride={instance.statusOverride}
                        />
                    </Typography>
                </Stack>
            </Box>
            <Box
                component="ol"
                aria-label="Bisheriger Verlauf"
                sx={{listStyle: 'none', m: 0, p: 0}}
            >
                <TimelineItem>
                    <HistoryMarker
                        icon={<PlayCircle color="primary"/>}
                        label="Vorgang gestartet"
                        timestamp={instance.started}
                    />
                </TimelineItem>
                {entries.map((entry) => {
                    if (entry.kind === 'event') {
                        return (
                            <TimelineItem key={`event-${entry.id}`}>
                                <HistoryEvent event={entry.event} />
                            </TimelineItem>
                        );
                    }
                    const node = nodes.get(entry.task.processNodeId);
                    const provider = node == null ? undefined : providers.get(JSON.stringify([
                        node.processNodeDefinitionKey, node.processNodeDefinitionVersion,
                    ]));
                    return (
                        <TimelineItem key={`task-${entry.id}`}>
                            <HistoryTaskAccordion
                                node={node}
                                nodeDefinition={provider}
                                task={entry.task}
                                fallbackName={activeTaskNames.get(entry.id)}
                                events={taskEvents.get(entry.id) ?? []}
                            />
                        </TimelineItem>
                    );
                })}
                {history.tasks.length === 0 && (
                    <TimelineItem>
                        <Typography color="text.secondary">
                            Für diesen Vorgang wurden noch keine Aufgaben angelegt.
                        </Typography>
                    </TimelineItem>
                )}
                {instance.finished != null && (
                    <TimelineItem>
                        <HistoryMarker
                            icon={<StatusIcon color={ProcessInstanceStatusColor[instance.status]}/>}
                            label={endLabel}
                            timestamp={instance.finished}
                        />
                    </TimelineItem>
                )}
            </Box>
        </Stack>
    );
}

function TimelineItem({children}: { children: ReactNode }) {
    return (
        <Box
            component="li"
            sx={{
                position: 'relative',
                ml: 1,
                pl: 3,
                pb: 2,
                borderLeft: 2,
                borderColor: 'divider',
                '&:last-child': {pb: 0},
                '&::before': {
                    content: '""',
                    position: 'absolute',
                    left: -6,
                    top: 20,
                    width: 10,
                    height: 10,
                    borderRadius: '50%',
                    bgcolor: 'text.secondary',
                },
            }}
        >
            {children}
        </Box>
    );
}

function HistoryMarker({icon, label, timestamp}: { icon: ReactNode; label: string; timestamp: string }) {
    return (
        <Stack
            direction="row"
            spacing={1.5}
            sx={{py: 1, alignItems: 'center'}}
        >
            {icon}
            <Box>
                <Typography sx={{fontWeight: 600}}>{label}</Typography>
                <Typography
                    variant="body2"
                    color="text.secondary"
                >
                    {formatDateTimeWithRelative(timestamp)}
                </Typography>
            </Box>
        </Stack>
    );
}

function HistoryEvent({event}: {event: ProcessInstanceEventEntity}) {
    return (
        <Box sx={{py: 1, overflowWrap: 'anywhere'}}>
            <Typography sx={{fontWeight: 600}}>{event.title}</Typography>
            <Typography variant="body2" color="text.secondary">
                {formatDateTimeWithRelative(event.timestamp)}
            </Typography>
            {event.message.trim() && <Typography sx={{whiteSpace: 'pre-line', mt: 0.5}}>{event.message}</Typography>}
        </Box>
    );
}

function HistoryTaskAccordion({node, nodeDefinition, task, fallbackName, events}: {
    node?: ProcessNodeEntity;
    nodeDefinition?: ProcessNodeProvider;
    task: ProcessInstanceTaskEntity;
    fallbackName?: string;
    events: ProcessInstanceEventEntity[];
}) {
    const id = useId();
    const name = node?.name?.trim() || nodeDefinition?.name?.trim() || fallbackName?.trim() || `Aufgabe #${task.id}`;
    const typeLabel = nodeDefinition == null ? undefined : ProviderTypeStyles[nodeDefinition.type]?.label ?? nodeDefinition.type;

    return (
        <Accordion disableGutters>
            <AccordionSummary
                expandIcon={<KeyboardArrowDown/>}
                id={`${id}-summary`}
                aria-controls={`${id}-details`}
            >
                <Box
                    component="span"
                    sx={{display: 'flex', alignItems: 'flex-start', gap: 1.5, width: '100%', minWidth: 0, pr: 2}}
                >
                    <Box
                        component="span"
                        sx={{display: 'flex', pt: 0.25}}
                    >
                        <ProcessInstanceTaskStatusIcon status={task.status} />
                    </Box>
                    <Box
                        component="span"
                        sx={{flex: 1, minWidth: 0, overflowWrap: 'anywhere'}}
                    >
                        <Typography
                            component="span"
                            sx={{display: 'block', fontWeight: 600}}
                        >
                            {name}
                        </Typography>
                        {node?.description?.trim() && (
                            <Typography
                                component="span"
                                variant="body2"
                                sx={{display: 'block', whiteSpace: 'pre-line', mt: 0.5}}
                            >
                                {node.description}
                            </Typography>
                        )}
                        <Typography
                            component="span"
                            variant="body2"
                            color="text.secondary"
                            sx={{display: 'block', mt: 0.5}}
                        >
                            <ProcessStatusValue
                                systemLabel={ProcessTaskStatusLabels[task.status]}
                                statusOverride={task.statusOverride}
                            />
                        </Typography>
                        <Typography
                            component="span"
                            variant="body2"
                            color="text.secondary"
                            sx={{display: 'block', mt: 0.5}}
                        >
                            Gestartet am {formatDateTimeWithRelative(task.started)}
                        </Typography>
                    </Box>
                    {typeLabel != null && (
                        <Typography
                            component="span"
                            variant="body2"
                            color="text.secondary"
                            sx={{flexShrink: 0, pt: 0.25}}
                        >
                            {typeLabel}
                        </Typography>
                    )}
                </Box>
            </AccordionSummary>
            <AccordionDetails>
                {events.length === 0 ? (
                    <Typography color="text.secondary">Keine verlaufsrelevanten Ereignisse vorhanden.</Typography>
                ) : (
                    <Box component="ol" aria-label={`Ereignisse zu ${name}`} sx={{listStyle: 'none', m: 0, p: 0}}>
                        {events.map((event) => (
                            <Box component="li" key={event.id}>
                                <HistoryEvent event={event} />
                            </Box>
                        ))}
                    </Box>
                )}
            </AccordionDetails>
        </Accordion>
    );
}
