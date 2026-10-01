import {type ReactNode, useEffect, useId, useState} from 'react';
import {Alert, Box, Button, Skeleton, Stack, Typography} from '@mui/material';
import KeyboardArrowDown from '@aivot/mui-material-symbols-400-n25-outlined/KeyboardArrowDown';
import Schedule from '@aivot/mui-material-symbols-400-n25-outlined/Schedule';
import PlayCircle from '@aivot/mui-material-symbols-400-n25-outlined/PlayCircle';
import {Accordion, AccordionDetails, AccordionSummary} from '../../../../components/accordion/accordion';
import {useGenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {Permission} from '../../../../data/permissions/permission';
import {useHasProcessInstancePermission} from '../../../permissions/hooks/use-permissions';
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
import {type ProcessInstanceHistory} from '../../models/process-instance-history';
import {ProcessInstanceApiService} from '../../services/process-instance-api-service';
import {type ProcessNodeProvider} from '../../services/process-node-provider-api-service';
import {ProviderTypeStyles} from '../../data/provider-type-styles';
import {ProcessInstanceTaskStatusIcon} from '../../components/process-instance-task-status-icon';
import {formatDateTimeWithRelative, ProcessStatusValue} from '../../components/process-detail-values';

export function ProcessInstanceDetailsPageHistory() {
    const {item} = useGenericDetailsPageContext<ProcessInstanceDetails, undefined>();
    return item == null ? (
        <HistorySkeleton />
    ) : (
        <InstanceHistory
            key={item.instance.id}
            item={item}
        />
    );
}

function InstanceHistory({item}: {item: ProcessInstanceDetails}) {
    const canRead = useHasProcessInstancePermission(item.instance.id, Permission.PROCESS_INSTANCE_READ);
    const [reload, setReload] = useState(0);
    const [result, setResult] = useState<{
        item: ProcessInstanceDetails;
        reload: number;
        history: ProcessInstanceHistory | null;
    }>();

    useEffect(() => {
        if (!canRead) {
            setResult(undefined);
            return;
        }
        const controller = new AbortController();
        new ProcessInstanceApiService()
            .retrieveHistory(item.instance.id, {abort: controller.signal})
            .then((history) => {
                if (!controller.signal.aborted) setResult({item, reload, history});
            })
            .catch(() => {
                if (!controller.signal.aborted) setResult({item, reload, history: null});
            });
        return () => controller.abort();
    }, [item, reload, canRead]);

    if (!canRead) {
        return <Alert severity="info">Sie haben keine Berechtigung mehr, diesen Vorgang einzusehen.</Alert>;
    }
    // A refreshed parent item invalidates the history, including responses still in flight.
    if (result == null || result.item !== item || result.reload !== reload) return <HistorySkeleton />;
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
    return <HistoryTimeline history={result.history} />;
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

function HistoryTimeline({history}: {history: ProcessInstanceHistory}) {
    const {processInstance: instance, upcomingTasks} = history;
    const StatusIcon = ProcessInstanceStatusIcons[instance.status];
    const existingTasks = [...history.existingTasks].sort(
        (left, right) => Date.parse(left.task.started) - Date.parse(right.task.started) || left.task.id - right.task.id,
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
                    <StatusIcon color={ProcessInstanceStatusColor[instance.status]} />
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
                        icon={<PlayCircle color="primary" />}
                        label="Vorgang gestartet"
                        timestamp={instance.started}
                    />
                </TimelineItem>
                {existingTasks.map(({task, node, nodeDefinition}) => (
                    <TimelineItem key={task.id}>
                        <HistoryTaskAccordion
                            node={node}
                            nodeDefinition={nodeDefinition}
                            task={task}
                        />
                    </TimelineItem>
                ))}
                {existingTasks.length === 0 && (
                    <TimelineItem>
                        <Typography color="text.secondary">
                            Für diesen Vorgang wurden noch keine Aufgaben angelegt.
                        </Typography>
                    </TimelineItem>
                )}
                {instance.finished != null && (
                    <TimelineItem>
                        <HistoryMarker
                            icon={<StatusIcon color={ProcessInstanceStatusColor[instance.status]} />}
                            label={endLabel}
                            timestamp={instance.finished}
                        />
                    </TimelineItem>
                )}
            </Box>
            {upcomingTasks.length > 0 && (
                <Box
                    component="section"
                    aria-label="Mögliche nächste Schritte"
                >
                    <Typography
                        variant="h6"
                        component="h2"
                        sx={{mb: 2}}
                    >
                        Mögliche nächste Schritte
                    </Typography>
                    <Stack
                        component="ul"
                        spacing={1}
                        sx={{listStyle: 'none', p: 0, m: 0}}
                    >
                        {upcomingTasks.map(({nodeEntity, nodeDefinition}) => (
                            <Box
                                component="li"
                                key={nodeEntity.id}
                            >
                                <HistoryTaskAccordion
                                    node={nodeEntity}
                                    nodeDefinition={nodeDefinition}
                                />
                            </Box>
                        ))}
                    </Stack>
                </Box>
            )}
        </Stack>
    );
}

function TimelineItem({children}: {children: ReactNode}) {
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

function HistoryMarker({icon, label, timestamp}: {icon: ReactNode; label: string; timestamp: string}) {
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

function HistoryTaskAccordion({
    node,
    nodeDefinition,
    task,
}: {
    node: ProcessNodeEntity;
    nodeDefinition: ProcessNodeProvider;
    task?: ProcessInstanceTaskEntity;
}) {
    const id = useId();
    const name = node.name?.trim() || nodeDefinition.name?.trim() || 'Unbenanntes Prozesselement';
    const typeLabel = ProviderTypeStyles[nodeDefinition.type]?.label ?? nodeDefinition.type;

    return (
        <Accordion disableGutters>
            <AccordionSummary
                expandIcon={<KeyboardArrowDown />}
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
                        {task == null ? (
                            <Schedule color="disabled" />
                        ) : (
                            <ProcessInstanceTaskStatusIcon status={task.status} />
                        )}
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
                        {node.description?.trim() && (
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
                            {task == null ? (
                                'Möglicher nächster Schritt'
                            ) : (
                                <ProcessStatusValue
                                    systemLabel={ProcessTaskStatusLabels[task.status]}
                                    statusOverride={task.statusOverride}
                                />
                            )}
                        </Typography>
                        {task != null && (
                            <Typography
                                component="span"
                                variant="body2"
                                color="text.secondary"
                                sx={{display: 'block', mt: 0.5}}
                            >
                                Gestartet am {formatDateTimeWithRelative(task.started)}
                            </Typography>
                        )}
                    </Box>
                    <Typography
                        component="span"
                        variant="body2"
                        color="text.secondary"
                        sx={{flexShrink: 0, pt: 0.25}}
                    >
                        {typeLabel}
                    </Typography>
                </Box>
            </AccordionSummary>
            <AccordionDetails>
                <Typography color="text.secondary">Die Detailansicht ist noch nicht verfügbar.</Typography>
            </AccordionDetails>
        </Accordion>
    );
}
