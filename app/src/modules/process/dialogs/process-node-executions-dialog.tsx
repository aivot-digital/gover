import {useId, useState} from 'react';
import {Link} from 'react-router-dom';
import {
    Alert,
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    Paper,
    Stack,
    Tooltip,
    Typography,
} from '@mui/material';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {Permission} from '../../../data/permissions/permission';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {humanizeMillisecondsDuration} from '../../../utils/duration-utils';
import {useHasProcessInstancePermission, useHasProcessPermission} from '../../permissions/hooks/use-permissions';
import {buildTaskProcessInstanceAttachmentSetItems} from '../components/process-instance-attachment-set-list';
import {Chip} from '../../../components/chip/chip';
import News from '@aivot/mui-material-symbols-400-n25-outlined/News';
import DataObject from '@aivot/mui-material-symbols-400-n25-outlined/DataObject';
import {ProcessInstanceTaskStatusIcon} from '../components/process-instance-task-status-icon';
import {formatDateTimeWithRelative} from '../components/process-detail-values';
import {isProcessTaskEditableByStaff, ProcessTaskStatus, ProcessTaskStatusColors} from '../enums/process-task-status';
import {useDeadlineNow} from '../hooks/use-deadline-now';
import {type ProcessRuntimeData} from '../hooks/use-process-runtime-data';
import {ProcessInstanceTaskApiService} from '../services/process-instance-task-api-service';
import {activeHistoryTaskStatuses} from '../utils/process-history-navigation';
import {getProcessTaskStatusDisplay} from '../utils/process-task-status-label';
import {getExecutionsForNode} from '../utils/process-task-executions';
import {getLatestTaskForNode} from '../pages/details/components/process-flow-editor/utils/runtime-task-utils';
import {ProcessInstanceEventDialog} from './process-instance-event-dialog';
import {ProcessInstanceTaskDataDialog, type ProcessInstanceTaskDataKind} from './process-instance-task-data-dialog';

interface ProcessNodeExecutionsDialogProps {
    nodeId: number;
    nodeName: string;
    runtimeData: ProcessRuntimeData;
    onReload: () => void | Promise<void>;
    onDownloadAttachment?: (attachment: ProcessRuntimeData['attachments'][number]) => void | Promise<void>;
    onClose: () => void;
}

export function ProcessNodeExecutionsDialog({
                                                nodeId,
                                                nodeName,
                                                runtimeData,
                                                onReload,
                                                onDownloadAttachment,
                                                onClose,
                                            }: ProcessNodeExecutionsDialogProps) {
    const titleId = useId();
    const dispatch = useAppDispatch();
    const {
        instance,
        tasks,
    } = runtimeData;
    const canRead = useHasProcessInstancePermission(instance.id, Permission.PROCESS_INSTANCE_READ);
    const canRestart = useHasProcessInstancePermission(instance.id, Permission.PROCESS_INSTANCE_RESTART_FAILED);
    const canViewProcess = useHasProcessPermission(instance.processId, Permission.PROCESS_DEFINITION_READ);
    const [eventTaskId, setEventTaskId] = useState<number | null>(null);
    const [dataTarget, setDataTarget] = useState<{ taskId: number; kind: ProcessInstanceTaskDataKind } | null>(null);
    const [restartingId, setRestartingId] = useState<number | null>(null);
    const executions = getExecutionsForNode(tasks, nodeId);
    const latestTask = getLatestTaskForNode(tasks, nodeId);
    const now = useDeadlineNow(canRead && executions.some(task => task.finished == null && activeHistoryTaskStatuses.has(task.status)));
    const dataTask = tasks.find(task => task.id === dataTarget?.taskId);
    const dataExecutionIndex = executions.findIndex(task => task.id === dataTask?.id);
    const previousData = dataTask?.previousProcessInstanceTaskId == null ? {} : tasks.find(task => task.id === dataTask.previousProcessInstanceTaskId)?.processData ?? null;

    const restart = async (taskId: number) => {
        if (restartingId != null) return;
        setRestartingId(taskId);
        try {
            await new ProcessInstanceTaskApiService().rerunFailedTask(taskId);
            dispatch(showSuccessSnackbar('Die erneute Ausführung der Aufgabe wurde angefordert.'));
            try {
                await onReload();
            } catch (error) {
                dispatch(showApiErrorSnackbar(error, 'Die erneute Ausführung wurde angefordert. Die Ausführungen konnten nicht aktualisiert werden.'));
            }
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Die erneute Ausführung der Aufgabe konnte nicht angefordert werden.'));
        } finally {
            setRestartingId(null);
        }
    };

    return <>
        <Dialog open
                onClose={onClose}
                fullWidth
                maxWidth="lg"
                aria-labelledby={titleId}>
            <DialogTitleWithClose id={titleId}
                                  onClose={onClose}
                                  endAdornment={<Typography variant="caption"
                                                            sx={{color: 'text.secondary'}}>Neueste zuerst</Typography>}>
                Ausführungen von „{nodeName}“
            </DialogTitleWithClose>
            <DialogContent>
                {!canRead ?
                    <Alert severity="info">Sie haben keine Berechtigung mehr, diesen Vorgang einzusehen.</Alert> : (
                        <Stack spacing={2}>
                            {runtimeData.tasksComplete === false &&
                                <Alert severity="info">Die Aufgaben sind nicht vollständig geladen. Die Liste und die
                                                       Nummerierung beziehen sich auf die geladenen
                                                       Ausführungen.</Alert>}
                            {executions.length === 0 ? <Typography sx={{color: 'text.secondary'}}>Keine Ausführungen
                                                                                                  verfügbar.</Typography> : (
                                <Stack component="ol"
                                       spacing={1.5}
                                       sx={{
                                           m: 0,
                                           p: 0,
                                           listStyle: 'none',
                                       }}
                                       aria-label="Ausführungen">
                                    {executions.map((task, index) => ({
                                        task,
                                        number: index + 1,
                                    })).reverse().map(({
                                                           task,
                                                           number,
                                                       }) => {
                                        const status = getProcessTaskStatusDisplay(task, instance.identities);
                                        const elapsed = now - Date.parse(task.started);
                                        const ongoing = task.finished == null && activeHistoryTaskStatuses.has(task.status) && Number.isFinite(elapsed) && elapsed >= 0;
                                        return (
                                            <Paper component="li"
                                                   variant="outlined"
                                                   key={task.id}
                                                   sx={{overflow: 'hidden'}}>
                                                <Box sx={{
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    flexWrap: 'wrap',
                                                    gap: 1,
                                                    px: 2,
                                                    py: 1.5,
                                                    bgcolor: 'background.default',
                                                    borderBottom: '1px solid',
                                                    borderColor: 'divider',
                                                }}>
                                                    <ProcessInstanceTaskStatusIcon {...task} identities={instance.identities}/>
                                                    <Typography variant="h6"
                                                                sx={{mr: 1}}>Ausführung {number}</Typography>
                                                    <Tooltip title={status.tooltip ?? status.label}
                                                             arrow>
                                                        <Chip size="small"
                                                              mode="soft"
                                                              color={task.statusOverride?.trim() ? 'primary' : ProcessTaskStatusColors[task.status]}
                                                              label={status.label}
                                                              sx={{maxWidth: '100%'}}/>
                                                    </Tooltip>
                                                    {task.id === latestTask?.id && (
                                                        <Tooltip title="Im Prozessmodell werden der Status und die Daten dieser Ausführung angezeigt."
                                                                 arrow>
                                                            <Chip size="small"
                                                                  mode="soft"
                                                                  color="primary"
                                                                  label="Neueste Ausführung"/>
                                                        </Tooltip>
                                                    )}
                                                </Box>
                                                <Box component="dl"
                                                     sx={{
                                                         display: 'grid',
                                                         gridTemplateColumns: 'repeat(3, minmax(0, 1fr))',
                                                         gap: 2,
                                                         m: 0,
                                                         px: 2,
                                                         py: 2,
                                                         '& dt': {
                                                             typography: 'caption',
                                                             color: 'text.secondary',
                                                             mb: 0.5,
                                                         },
                                                         '& dd': {
                                                             m: 0,
                                                             typography: 'body2',
                                                         },
                                                     }}>
                                                    <Box>
                                                        <Box component="dt">Beginn</Box>
                                                        <Box component="dd">{formatDateTimeWithRelative(task.started)}</Box>
                                                    </Box>
                                                    {task.finished != null && <Box>
                                                        <Box component="dt">Ende</Box>
                                                        <Box component="dd">{formatDateTimeWithRelative(task.finished)}</Box>
                                                    </Box>}
                                                    {task.runtime != null && <Box>
                                                        <Box component="dt">Dauer</Box>
                                                        <Box component="dd">{humanizeMillisecondsDuration(task.runtime)}</Box>
                                                    </Box>}
                                                    {ongoing && <Box>
                                                        <Box component="dt">Zeit seit Beginn</Box>
                                                        <Box component="dd">{elapsed < 60_000 ? 'Weniger als eine Minute' : humanizeMillisecondsDuration(Math.floor(elapsed / 60_000) * 60_000)}</Box>
                                                    </Box>}
                                                </Box>
                                                <Box sx={{
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    flexWrap: 'wrap',
                                                    gap: 1,
                                                    px: 1.5,
                                                    py: 1,
                                                    borderTop: '1px solid',
                                                    borderColor: 'divider',
                                                }}>

                                                    <Button size="small"
                                                            startIcon={<News/>}
                                                            onClick={() => setEventTaskId(task.id)}>Ereignisprotokoll</Button>
                                                    <Button size="small"
                                                            startIcon={<DataObject/>}
                                                            onClick={() => setDataTarget({
                                                                taskId: task.id,
                                                                kind: 'element-data',
                                                            })}>Elementdaten</Button>
                                                    <Button size="small"
                                                            startIcon={<DataObject/>}
                                                            onClick={() => setDataTarget({
                                                                taskId: task.id,
                                                                kind: 'process-data',
                                                            })}>Vorgangsdaten</Button>
                                                    <Button size="small"
                                                            startIcon={<DataObject/>}
                                                            onClick={() => setDataTarget({
                                                                taskId: task.id,
                                                                kind: 'changes',
                                                            })}>Datenänderungen</Button>
                                                    <Box sx={{
                                                        display: 'flex',
                                                        flexWrap: 'wrap',
                                                        alignItems: 'center',
                                                        gap: 1,
                                                        ml: 'auto',
                                                    }}>
                                                        {canViewProcess && <Button size="small"
                                                                                   component={Link}
                                                                                   to={`/process-instances/${instance.id}/history?taskId=${task.id}`}>Im
                                                                                                                                                      Vorgangsverlauf
                                                                                                                                                      ansehen</Button>}
                                                        {isProcessTaskEditableByStaff(task.status) &&
                                                            <Button size="small"
                                                                    variant="contained"
                                                                    component={Link}
                                                                    to={`/tasks/${instance.id}/${task.id}`}>Aufgabe
                                                                                                            aufrufen</Button>}
                                                        {[ProcessTaskStatus.AwaitingCustomer, ProcessTaskStatus.AwaitingPayment].includes(task.status) && instance.accessKey && task.accessKey && (
                                                            <Button size="small"
                                                                    variant="contained"
                                                                    component="a"
                                                                    href={`/process/${instance.accessKey}/tasks/${task.accessKey}`}
                                                                    target="_blank"
                                                                    rel="noopener noreferrer">Aufgabe für Beteiligte
                                                                                              aufrufen</Button>
                                                        )}
                                                        {task.status === ProcessTaskStatus.Failed && canRestart &&
                                                            <Button size="small"
                                                                    variant="contained"
                                                                    loading={restartingId === task.id}
                                                                    disabled={restartingId != null}
                                                                    onClick={() => void restart(task.id)}>Aufgabe erneut
                                                                                                          ausführen</Button>}
                                                    </Box>
                                                </Box>
                                            </Paper>
                                        );
                                    })}
                                </Stack>
                            )}
                        </Stack>
                    )}
            </DialogContent>
            <DialogActions><Button onClick={onClose}>Schließen</Button></DialogActions>
        </Dialog>
        {canRead && eventTaskId != null && <ProcessInstanceEventDialog key={eventTaskId}
                                                                       open
                                                                       instanceId={instance.id}
                                                                       taskId={eventTaskId}
                                                                       onClose={() => setEventTaskId(null)}/>}
        {canRead && dataTarget != null && dataTask != null && <ProcessInstanceTaskDataDialog
            task={dataTask}
            taskLabel={dataExecutionIndex >= 0 ? `${nodeName} · Ausführung ${dataExecutionIndex + 1}` : nodeName}
            previousProcessData={previousData}
            kind={dataTarget.kind}
            attachmentItems={buildTaskProcessInstanceAttachmentSetItems(instance, dataTask, runtimeData.attachmentSets, runtimeData.attachments)}
            onDownloadAttachment={onDownloadAttachment}
            onClose={() => setDataTarget(null)}
        />}
    </>;
}
