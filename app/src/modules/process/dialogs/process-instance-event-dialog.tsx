import {
    Box,
    Collapse,
    Dialog,
    DialogContent, Grid, IconButton,
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
} from '@mui/material';
import {useEffect, useMemo, useState} from 'react';
import {type SvgIconComponent} from '../../../types/svg-icon-component';
import {ProcessInstanceEventEntity, ProcessNodeExecutionLogLevel} from '../entities/process-instance-event-entity';
import {ProcessInstanceEventApiService} from '../services/process-instance-event-api-service';
import {ProcessInstanceApiService} from '../services/process-instance-api-service';
import {ProcessNodeEntity} from '../entities/process-node-entity';
import {ProcessNodeProvider, ProcessNodeProviderApiService} from '../services/process-node-provider-api-service';
import {ProcessNodeApiService} from '../services/process-node-api-service';
import {ProcessInstanceTaskApiService} from '../services/process-instance-task-api-service';
import {ProcessInstanceTaskEntity} from '../entities/process-instance-task-entity';
import {getNodeName} from '../pages/details/components/process-flow-editor/utils/node-utils';
import {UsersApiService} from '../../users/users-api-service';
import {User} from '../../users/models/user';
import {resolveUserName} from '../../users/utils/resolve-user-name';
import {format} from 'date-fns/format';
import Typography from '@mui/material/Typography';
import AccountBox from '@aivot/mui-material-symbols-400-n25-outlined/AccountBox';
import Memory from '@aivot/mui-material-symbols-400-n25-outlined/Memory';
import Info from '@aivot/mui-material-symbols-400-n25-outlined/Info';
import Warning from '@aivot/mui-material-symbols-400-n25-outlined/Warning';
import EmergencyHome from '@aivot/mui-material-symbols-400-n25-outlined/EmergencyHome';
import BugReport from '@aivot/mui-material-symbols-400-n25-outlined/BugReport';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {withDelay} from '../../../utils/with-delay';
import {ExpandableCodeBlock} from '../../../components/expandable-code-block/expandable-code-block';
import ChevronLeft from '@aivot/mui-material-symbols-400-n25-outlined/ChevronLeft';
import {ProcessInstanceEntity} from '../entities/process-instance-entity';
import {StatusTable} from '../../../components/status-table/status-table';
import {StatusTablePropsItem} from '../../../components/status-table/status-table-props';
import {humanizeISO8601Duration} from '../../../utils/duration-utils';

interface ProcessInstanceEventDialogProps {
    open: boolean;
    onClose: () => void;
    instanceId: number;
    taskId: number | null;
}

export function ProcessInstanceEventDialog(props: ProcessInstanceEventDialogProps) {
    const {
        open,
        onClose,
        instanceId,
        taskId,
    } = props;

    const [eventsData, setEventsData] = useState<EventsData | null>(null);

    useEffect(() => {
        if (!open) {
            return;
        }

        withDelay(getEventData(instanceId, taskId), 600)
            .then(setEventsData);
    }, [open, instanceId, taskId]);

    const handleClose = () => {
        onClose();
        setTimeout(() => {
            setEventsData(null);
        }, 300);
    };

    const instanceRuntimeInformation: StatusTablePropsItem[] = useMemo(() => {
        if (eventsData == null) {
            return [];
        }

        const info: StatusTablePropsItem[] = [
            {
                label: 'Start',
                children: format(new Date(eventsData.instance.started), 'dd.MM.yyyy HH:mm:ss') + ' Uhr',
            },
        ];

        if (eventsData.instance.finished != null) {
            info.push({
                label: 'Ende',
                children: format(new Date(eventsData.instance.finished), 'dd.MM.yyyy HH:mm:ss') + ' Uhr',
            });
        } else {
            info.push({
                label: 'Ende',
                children: '-',
            });
        }

        if (eventsData.instance.runtime != null) {
            info.push({
                label: 'Laufzeit',
                children: humanizeISO8601Duration(eventsData.instance.runtime),
            });
        } else {
            info.push({
                label: 'Laufzeit',
                children: '-'
            });
        }

        return info;
    }, [eventsData]);

    const taskRuntimeInformation: StatusTablePropsItem[] = useMemo(() => {
        if (eventsData == null || eventsData.task == null) {
            return [];
        }

        const info: StatusTablePropsItem[] = [
            {
                label: 'Start',
                children: format(new Date(eventsData.task.started), 'dd.MM.yyyy HH:mm:ss') + ' Uhr',
            },
        ];

        if (eventsData.task.finished != null) {
            info.push({
                label: 'Ende',
                children: format(new Date(eventsData.task.finished), 'dd.MM.yyyy HH:mm:ss') + ' Uhr',
            });
        } else {
            info.push({
                label: 'Ende',
                children: '-'
            });
        }

        if (eventsData.task.runtime != null) {
            info.push({
                label: 'Laufzeit',
                children: humanizeISO8601Duration(eventsData.task.runtime),
            });
        } else {
            info.push({
                label: 'Laufzeit',
                children: '-'
            });
        }

        return info;
    }, [eventsData]);

    return (
        <Dialog
            open={open}
            onClose={handleClose}
            fullWidth
            maxWidth="xl"
        >
            <DialogTitleWithClose onClose={handleClose}>
                {
                    taskId != null ?
                        'Prozesselementereignisse' :
                        'Vorgangsereignisse'
                }
            </DialogTitleWithClose>

            <DialogContent>
                <Grid
                    container
                    spacing={2}
                >
                    <Grid
                        size={{
                            xs: 12,
                            md: 6,
                        }}
                    >
                        <Typography
                            variant="h6"
                        >
                            Laufzeitinformation des Vorgangs
                        </Typography>

                        {
                            instanceRuntimeInformation.length === 0 &&
                            <Skeleton height={32}/>
                        }

                        {
                            instanceRuntimeInformation.length > 0 &&
                            <StatusTable
                                cardVariant="outlined"
                                dense={true}
                                sx={{
                                    mt: 1,
                                }}
                                items={instanceRuntimeInformation}
                            />
                        }
                    </Grid>

                    {
                        taskId != null &&
                        <Grid
                            size={{
                                xs: 12,
                                md: 6,
                            }}
                        >
                            <Typography
                                variant="h6"
                            >
                                Laufzeitinformation der Aufgabe
                            </Typography>

                            {
                                taskRuntimeInformation.length === 0 &&
                                <Skeleton height={32}/>
                            }

                            {
                                taskRuntimeInformation.length > 0 &&
                                <StatusTable
                                    cardVariant="outlined"
                                    dense={true}
                                    sx={{
                                        mt: 1,
                                    }}
                                    items={taskRuntimeInformation}
                                />
                            }
                        </Grid>
                    }
                </Grid>
            </DialogContent>

            <DialogContent
                sx={{
                    px: 0,
                    pb: 1,
                }}
            >
                <TableContainer
                    sx={{
                        maxHeight: '720px',
                        overflow: 'auto',
                    }}
                >
                    <Table
                        size="small"
                        stickyHeader={true}
                    >
                        <TableHead>
                            <TableRow>
                                <TableCell>
                                    Ereignistyp
                                </TableCell>
                                <TableCell>
                                    Zeitstempel
                                </TableCell>
                                <TableCell>
                                    Auslöser
                                </TableCell>
                                <TableCell>
                                    Prozesselement
                                </TableCell>
                                <TableCell>
                                    Nachricht
                                </TableCell>
                                <TableCell/>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {
                                eventsData != null &&
                                eventsData.items.length > 0 &&
                                eventsData.items.map((event) => (
                                    <EventTableRow event={event}
                                                   key={event.id}/>
                                ))
                            }
                            {
                                eventsData != null &&
                                eventsData.items.length === 0 &&
                                <TableRow>
                                    <TableCell
                                        colSpan={6}
                                        align="center"
                                        sx={{
                                            py: 2,
                                        }}
                                    >
                                        Keine Ereignisse gefunden.
                                    </TableCell>
                                </TableRow>
                            }
                            {
                                eventsData == null &&
                                <TableRow>
                                    {
                                        new Array(6)
                                            .fill(null)
                                            .map((_, index) => (
                                                <TableCell
                                                    key={index}
                                                >
                                                    <Skeleton
                                                        width="100%"
                                                    />
                                                </TableCell>
                                            ))
                                    }
                                </TableRow>
                            }
                        </TableBody>
                    </Table>
                </TableContainer>
            </DialogContent>
        </Dialog>
    );
}

interface Item extends ProcessInstanceEventEntity {
    scope: string | null; // The name of the node or process instance scope if null
    trigger: string | null; // The name of the user who triggered the event or null if the system triggered it
    startedAt: Date;
    endedAt: Date | null;
}

const ProcessNodeExecutionLogLevelLabels: Record<ProcessNodeExecutionLogLevel, string> = {
    [ProcessNodeExecutionLogLevel.Debug]: 'Debug',
    [ProcessNodeExecutionLogLevel.Info]: 'Info',
    [ProcessNodeExecutionLogLevel.Warn]: 'Warnung',
    [ProcessNodeExecutionLogLevel.Error]: 'Fehler',
};

const ProcessNodeExecutionLogLevelIcons: Record<ProcessNodeExecutionLogLevel, SvgIconComponent> = {
    [ProcessNodeExecutionLogLevel.Debug]: BugReport,
    [ProcessNodeExecutionLogLevel.Info]: Info,
    [ProcessNodeExecutionLogLevel.Warn]: Warning,
    [ProcessNodeExecutionLogLevel.Error]: EmergencyHome,
};

interface EventsData {
    instance: ProcessInstanceEntity;
    task: ProcessInstanceTaskEntity | null;
    items: Item[];
}

async function getEventData(instanceId: number, taskId: number | null): Promise<EventsData> {
    const processInstance = await new ProcessInstanceApiService()
        .retrieve(instanceId);

    const processTasks = await new ProcessInstanceTaskApiService()
        .listAll({
            processInstanceId: instanceId,
        });

    const processDefinitionNodes = await new ProcessNodeApiService()
        .listAll({
            processId: processInstance.processId,
        });

    const providers = await new ProcessNodeProviderApiService()
        .getNodeProviders();

    const events = await new ProcessInstanceEventApiService()
        .listAllOrdered('timestamp', 'ASC', {
            processInstanceId: instanceId,
            processInstanceTaskId: taskId ?? undefined,
        });

    const users = await new UsersApiService()
        .listAll();

    return {
        instance: processInstance,
        task: taskId != null ? (processTasks.content.find(t => t.id === taskId) || null) : null,
        items: events.content.map((event) => {
            const isTechnical = event.isTechnical ?? event.technical ?? false;
            const isAudit = event.isAudit ?? event.audit ?? false;

            let task: ProcessInstanceTaskEntity | null = null;
            if (event.processInstanceTaskId != null) {
                task = processTasks.content.find((t) => t.id === event.processInstanceTaskId)!;
            }

            let node: ProcessNodeEntity | null = null;
            if (task != null) {
                node = processDefinitionNodes.content.find((n) => n.id === task!.processNodeId)!;
            }

            let provider: ProcessNodeProvider | null = null;
            if (node != null) {
                provider = providers.find((p) => p.key === node!.processNodeDefinitionKey)!;
            }

            let user: User | null = null;
            if (event.triggeringUserId != null) {
                user = users.content.find((u) => u.id === event.triggeringUserId)!;
            }

            const item: Item = {
                ...event,
                isTechnical,
                isAudit,
                scope: node != null && provider != null ? getNodeName(node, provider) : null,
                trigger: user != null ? resolveUserName(user) : null,
                startedAt: new Date(task != null ? task.started : processInstance.started),
                endedAt: task != null && task.finished != null ? new Date(task.finished) : (processInstance.finished != null ? new Date(processInstance.finished) : null),
            };

            return item;
        }),
    };
}

function EventTableRow(props: { event: Item }) {
    const {
        event,
    } = props;

    const Icon = useMemo(() => ProcessNodeExecutionLogLevelIcons[event.level], [event.level]);

    const [expanded, setExpanded] = useState(false);

    return (
        <>
            <TableRow>
                <TableCell
                    sx={{
                        borderBottom: 'none',
                    }}
                >
                    <Box
                        display="flex"
                        alignItems="center"
                    >
                        <Icon
                            sx={{
                                mr: 1,
                            }}
                        />
                        {ProcessNodeExecutionLogLevelLabels[event.level]}
                    </Box>
                </TableCell>
                <TableCell
                    sx={{
                        borderBottom: 'none',
                    }}
                >
                    <Box
                        display="flex"
                        alignItems="center"
                        height="100%"
                    >
                        {format(new Date(event.timestamp), 'dd.MM.yyyy HH:mm:ss')}
                    </Box>
                </TableCell>
                <TableCell
                    sx={{
                        borderBottom: 'none',
                    }}
                >
                    <Box
                        display="flex"
                        alignItems="center"
                        height="100%"
                    >
                        {
                            event.trigger != null ?
                                <AccountBox/> :
                                <Memory/>
                        }
                        {event.trigger ?? 'System'}
                    </Box>
                </TableCell>
                <TableCell
                    sx={{
                        borderBottom: 'none',
                    }}
                >
                    <Box
                        display="flex"
                        alignItems="center"
                        height="100%"
                    >
                        {event.scope ?? '-'}
                    </Box>
                </TableCell>
                <TableCell
                    sx={{
                        borderBottom: 'none',
                    }}
                >
                    <Typography
                        sx={{
                            width: '400px',
                            overflow: 'hidden',
                            whiteSpace: 'nowrap',
                            textOverflow: 'ellipsis',
                        }}
                    >
                        {event.title}
                    </Typography>
                </TableCell>
                <TableCell
                    sx={{
                        borderBottom: 'none',
                    }}
                >
                    <IconButton
                        onClick={() => {
                            setExpanded(!expanded);
                        }}
                        size="small"
                        sx={{
                            transform: expanded ? 'rotate(-90deg)' : 'rotate(0deg)',
                            transition: 'transform 0.2s',
                        }}
                    >
                        <ChevronLeft/>
                    </IconButton>
                </TableCell>
            </TableRow>
            <TableRow
                sx={{
                    p: 0,
                }}
            >
                <TableCell
                    colSpan={6}
                    sx={{
                        p: 0,
                    }}
                >
                    <Collapse
                        in={expanded}
                    >
                        <Box
                            sx={{
                                pt: 1,
                                pb: 2,
                                pl: 8,
                                pr: 2,
                            }}
                        >
                            <Typography
                                variant="h6"
                                component="div"
                                gutterBottom={true}
                            >
                                Nachricht
                            </Typography>

                            <Typography>
                                {event.message}
                            </Typography>

                            {
                                event.details != null &&
                                Object.keys(event.details).length > 0 &&
                                <Box
                                    marginTop={2}
                                >
                                    <Typography
                                        variant="h6"
                                        component="div"
                                        gutterBottom={true}
                                    >
                                        Details
                                    </Typography>

                                    <Box
                                        sx={{
                                            overflowX: 'auto',
                                        }}
                                    >
                                        <ExpandableCodeBlock
                                            value={JSON.stringify(event.details, null, 2)}
                                            sx={{
                                                wordWrap: 'normal',
                                                whiteSpace: 'normal',
                                            }}
                                        />
                                    </Box>
                                </Box>
                            }

                            <Box
                                marginTop={2}
                            >
                                <Typography
                                    variant="h6"
                                    component="div"
                                    gutterBottom={true}
                                >
                                    Klassifizierung
                                </Typography>
                                <Typography>
                                    Level: {ProcessNodeExecutionLogLevelLabels[event.level]}
                                </Typography>
                                <Typography>
                                    Technisch: {event.isTechnical ? 'Ja' : 'Nein'}
                                </Typography>
                                <Typography>
                                    Audit-relevant: {event.isAudit ? 'Ja' : 'Nein'}
                                </Typography>
                            </Box>
                            {
                                event.endedAt != null &&
                                <Box
                                    marginTop={2}
                                >
                                    <Typography
                                        variant="h6"
                                        component="div"
                                        gutterBottom={true}
                                    >
                                        Laufzeitinformationen
                                    </Typography>

                                    <Typography>
                                        Zeitstempel: {format(new Date(event.timestamp), 'dd.MM.yyyy HH:mm:ss')} Uhr
                                    </Typography>
                                </Box>
                            }
                        </Box>
                    </Collapse>
                </TableCell>
            </TableRow>
        </>
    );
}
