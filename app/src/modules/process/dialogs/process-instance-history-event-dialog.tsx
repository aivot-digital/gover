import {
    Box,
    Button,
    Collapse,
    Dialog,
    DialogContent, IconButton,
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow
} from "@mui/material";
import {useEffect, useMemo, useState} from "react";
import {ProcessInstanceHistoryEventEntity} from "../entities/process-instance-history-event-entity";
import {ProcessInstanceHistoryEventApiService} from "../services/process-instance-history-event-api-service";
import {ProcessInstanceApiService} from "../services/process-instance-api-service";
import {ProcessDefinitionNodeEntity} from "../entities/process-definition-node-entity";
import {ProcessNodeProvider, ProcessNodeProviderApiService} from "../services/process-node-provider-api-service";
import {ProcessDefinitionApiService} from "../services/process-definition-api-service";
import {ProcessDefinitionNodeApiService} from "../services/process-definition-node-api-service";
import {ProcessInstanceTaskApiService} from "../services/process-instance-task-api-service";
import {ProcessInstanceTaskEntity} from "../entities/process-instance-task-entity";
import {getNodeName} from "../pages/details/components/process-flow-editor/utils/node-utils";
import {UsersApiService} from "../../users/users-api-service";
import {User} from "../../users/models/user";
import {resolveUserName} from "../../users/utils/resolve-user-name";
import {
    ProcessHistoryEventType,
    ProcessHistoryEventTypeIcons,
    ProcessHistoryEventTypeLabels
} from "../enums/process-history-event-type";
import {format} from "date-fns/format";
import Typography from "@mui/material/Typography";
import AccountBox from "@aivot/mui-material-symbols-400-outlined/dist/account-box/AccountBox";
import Memory from "@aivot/mui-material-symbols-400-outlined/dist/memory/Memory";
import {DialogTitleWithClose} from "../../../components/dialog-title-with-close/dialog-title-with-close";
import {withDelay} from "../../../utils/with-delay";
import {ExpandableCodeBlock} from "../../../components/expandable-code-block/expandable-code-block";
import ChevronLeft from "@aivot/mui-material-symbols-400-outlined/dist/chevron-left/ChevronLeft";

interface ProcessInstanceHistoryEventDialogProps {
    open: boolean;
    onClose: () => void;
    instanceId: number;
    taskId: number | null;
}

export function ProcessInstanceHistoryEventDialog(props: ProcessInstanceHistoryEventDialogProps) {
    const {
        open,
        onClose,
        instanceId,
        taskId,
    } = props;

    const [items, setItems] = useState<Item[] | null>(null);

    useEffect(() => {
        if (!open) {
            return;
        }

        withDelay(getItems(instanceId, taskId), 600)
            .then(setItems);
    }, [open, instanceId, taskId]);

    const handleClose = () => {
        onClose();
        setTimeout(() => {
            setItems(null);
        }, 300);
    };

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
                                items != null &&
                                items.length > 0 &&
                                items.map((event) => (
                                    <EventTableRow event={event}
                                                   key={event.id}/>
                                ))
                            }
                            {
                                items != null &&
                                items.length === 0 &&
                                <TableRow>
                                    <TableCell
                                        colSpan={5}
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
                                items == null &&
                                <TableRow>
                                    {
                                        new Array(5)
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

interface Item extends ProcessInstanceHistoryEventEntity {
    scope: string | null; // The name of the node or process instance scope if null
    trigger: string | null; // The name of the user who triggered the event or null if the system triggered it
    startedAt: Date;
    endedAt: Date | null;
}

async function getItems(instanceId: number, taskId: number | null): Promise<Item[]> {
    const processInstance = await new ProcessInstanceApiService()
        .retrieve(instanceId);

    const processTasks = await new ProcessInstanceTaskApiService()
        .listAll({
            processInstanceId: instanceId,
        });

    const processDefinition = await new ProcessDefinitionApiService()
        .retrieve(processInstance.processDefinitionId);

    const processDefinitionNodes = await new ProcessDefinitionNodeApiService()
        .listAll({
            processDefinitionId: processInstance.processDefinitionId,
        });

    const providers = await new ProcessNodeProviderApiService()
        .getNodeProviders();

    const events = await new ProcessInstanceHistoryEventApiService()
        .listAllOrdered('timestamp', 'ASC', {
            processInstanceId: instanceId,
            processInstanceTaskId: taskId ?? undefined,
        });

    const users = await new UsersApiService()
        .listAll();

    return events.content.map((event) => {
        let task: ProcessInstanceTaskEntity | null = null;
        if (event.processInstanceTaskId != null) {
            task = processTasks.content.find((t) => t.id === event.processInstanceTaskId)!;
        }

        let node: ProcessDefinitionNodeEntity | null = null;
        if (task != null) {
            node = processDefinitionNodes.content.find((n) => n.id === task!.processDefinitionNodeId)!;
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
            scope: node != null && provider != null ? getNodeName(node, provider) : null,
            trigger: user != null ? resolveUserName(user) : null,
            startedAt: new Date(task != null ? task.started : processInstance.started),
            endedAt: task != null && task.finished != null ? new Date(task.finished) : (processInstance.finished != null ? new Date(processInstance.finished) : null),
        };

        return item;
    });
}

function EventTableRow(props: { event: Item }) {
    const {
        event,
    } = props;

    const Icon = useMemo(() => ProcessHistoryEventTypeIcons[event.type], [event.type]);

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
                        {ProcessHistoryEventTypeLabels[event.type]}
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

                            {
                                event.type === ProcessHistoryEventType.Complete &&
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
                                        Gestartet: {format(event.startedAt, 'dd.MM.yyyy HH:mm:ss')}
                                    </Typography>
                                    <Typography>
                                        Beendet: {event.endedAt != null ? format(event.endedAt, 'dd.MM.yyyy HH:mm:ss') : 'Läuft noch'}
                                    </Typography>
                                    <Typography>
                                        Dauer: {event.endedAt != null ? `${((event.endedAt.getTime() - event.startedAt.getTime()) / 1000).toFixed(2)} Sekunden` : 'Läuft noch'}
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