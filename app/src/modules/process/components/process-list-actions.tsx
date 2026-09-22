import {useState} from 'react';
import {Dialog, DialogContent, Menu, MenuItem, Typography} from '@mui/material';
import {Link} from 'react-router-dom';
import MoreVert from '@aivot/mui-material-symbols-400-n25-outlined/MoreVert';
import Task from '@aivot/mui-material-symbols-400-n25-outlined/Task';
import FolderShared from '@aivot/mui-material-symbols-400-n25-outlined/FolderShared';
import {Actions} from '../../../components/actions/actions';
import {ProcessInstanceListEntry, ProcessTaskListEntry} from '../entities/process-list';
import {useHasProcessInstancePermission, useHasProcessPermission} from '../../permissions/hooks/use-permissions';
import {Permission} from '../../../data/permissions/permission';
import {ProcessInstanceApiService} from '../services/process-instance-api-service';
import {ProcessInstanceTaskApiService} from '../services/process-instance-task-api-service';
import {ProcessTaskStatus} from '../enums/process-task-status';
import {ProcessInstanceStatus} from '../enums/process-instance-status';
import {useConfirm} from '../../../providers/confirm-provider';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {ProcessAssignmentDialog} from './process-assignment-button';
import {ProcessInstanceEventDialog} from '../dialogs/process-instance-event-dialog';
import {ProcessInstanceTaskEntity} from '../entities/process-instance-task-entity';
import {ExpandableCodeBlock} from '../../../components/expandable-code-block/expandable-code-block';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';

export function ProcessListActions({
    item,
    onChanged,
}: {
    item: ProcessInstanceListEntry | ProcessTaskListEntry;
    onChanged: () => void;
}) {
    const task = 'processInstanceId' in item ? item : null;
    const instanceId = task?.processInstanceId ?? item.id;
    const [anchor, setAnchor] = useState<Element | null>(null);
    const [assigning, setAssigning] = useState(false);
    const [events, setEvents] = useState(false);
    const [data, setData] = useState<ProcessInstanceTaskEntity | null>(null);
    const [busy, setBusy] = useState(false);
    const confirm = useConfirm();
    const dispatch = useAppDispatch();
    const canAssign = useHasProcessInstancePermission(
        instanceId,
        task ? Permission.PROCESS_INSTANCE_EDIT_TASK : Permission.PROCESS_INSTANCE_REASSIGN,
    );
    const canRestart = useHasProcessInstancePermission(
        instanceId,
        task ? Permission.PROCESS_INSTANCE_EDIT_TASK : Permission.PROCESS_INSTANCE_UPDATE,
    );
    const canDelete = useHasProcessInstancePermission(instanceId, Permission.PROCESS_INSTANCE_DELETE);
    const canReadModel = useHasProcessPermission(item.processId, Permission.PROCESS_DEFINITION_READ);
    const active =
        task == null ||
        [
            ProcessTaskStatus.Running,
            ProcessTaskStatus.Paused,
            ProcessTaskStatus.AwaitingCustomer,
            ProcessTaskStatus.AwaitingPayment,
        ].includes(task.status);
    const failed = item.status === (task ? ProcessTaskStatus.Failed : ProcessInstanceStatus.Failed);
    const detailPath = task ? `/tasks/${instanceId}/${task.id}` : `/process-instances/${instanceId}`;

    const mutate = async (remove: boolean) => {
        setAnchor(null);
        const label = remove ? 'Vorgang löschen' : task ? 'Aufgabe erneut starten' : 'Vorgang erneut starten';
        if (
            !(await confirm({
                title: label,
                isDestructive: remove,
                children: (
                    <Typography>
                        {remove
                            ? `Möchten Sie den Vorgang „${item.caseNumber}“ löschen? Dies kann nicht rückgängig gemacht werden.`
                            : `Möchten Sie ${task ? 'die fehlgeschlagene Aufgabe' : 'den fehlgeschlagenen Vorgang'} erneut starten?`}
                    </Typography>
                ),
            }))
        )
            return;
        setBusy(true);
        try {
            if (remove) await new ProcessInstanceApiService().destroy(instanceId);
            else if (task) await new ProcessInstanceTaskApiService().rerunFailedTask(task.id);
            else await new ProcessInstanceApiService().restartFailedInstance(instanceId);
            dispatch(showSuccessSnackbar(remove ? 'Der Vorgang wurde gelöscht.' : 'Der Neustart wurde angestoßen.'));
            onChanged();
        } catch (error) {
            dispatch(
                showApiErrorSnackbar(
                    error,
                    remove ? 'Der Vorgang konnte nicht gelöscht werden.' : 'Der Neustart ist fehlgeschlagen.',
                ),
            );
        } finally {
            setBusy(false);
        }
    };
    const loadData = async () => {
        if (!task) return;
        setAnchor(null);
        setBusy(true);
        try {
            setData(await new ProcessInstanceTaskApiService().retrieve(task.id));
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Die Aufgabendaten konnten nicht geladen werden.'));
        } finally {
            setBusy(false);
        }
    };
    return (
        <>
            <Actions
                dense
                isBusy={busy}
                actions={[
                    {
                        icon: task ? <Task /> : <FolderShared />,
                        to: detailPath,
                        tooltip: task ? 'Aufgabe aufrufen' : 'Vorgang aufrufen',
                    },
                    {
                        icon: <MoreVert />,
                        tooltip: 'Weitere Aktionen',
                        onClick: (event) => setAnchor(event.currentTarget),
                    },
                ]}
            />
            <Menu
                anchorEl={anchor}
                open={anchor != null}
                onClose={() => setAnchor(null)}
            >
                {task ? (
                    <MenuItem
                        component={Link}
                        to={`/process-instances/${instanceId}`}
                    >
                        Vorgang aufrufen
                    </MenuItem>
                ) : (
                    <MenuItem
                        component={Link}
                        to={`/processes/${item.processId}/versions/${item.processVersion}/instances/${instanceId}/tasks`}
                    >
                        Alle Aufgaben des Vorgangs
                    </MenuItem>
                )}
                {canAssign && active && (
                    <MenuItem
                        onClick={() => {
                            setAnchor(null);
                            setAssigning(true);
                        }}
                    >
                        {task ? 'Aufgabe zuweisen' : 'Vorgang zuweisen'}
                    </MenuItem>
                )}
                <MenuItem
                    onClick={() => {
                        setAnchor(null);
                        setEvents(true);
                    }}
                >
                    Ereignisse einsehen
                </MenuItem>
                {task && <MenuItem onClick={() => void loadData()}>Technische Aufgabendaten</MenuItem>}
                {canReadModel && (
                    <MenuItem
                        component={Link}
                        to={`/processes/${item.processId}/versions/${item.processVersion}/?instanceId=${instanceId}`}
                    >
                        Im Prozessmodell ansehen
                    </MenuItem>
                )}
                {canRestart && failed && <MenuItem onClick={() => void mutate(false)}>Erneut starten</MenuItem>}
                {!task && canDelete && (
                    <MenuItem
                        sx={{color: 'error.main'}}
                        onClick={() => void mutate(true)}
                    >
                        Vorgang löschen
                    </MenuItem>
                )}
            </Menu>
            {assigning && canAssign && active && (
                <ProcessAssignmentDialog
                    instanceId={instanceId}
                    taskId={task?.id}
                    assignedUserId={item.assignedUserId}
                    onAssigned={onChanged}
                    onClose={() => setAssigning(false)}
                />
            )}
            {events && (
                <ProcessInstanceEventDialog
                    open
                    instanceId={instanceId}
                    taskId={task?.id ?? null}
                    onClose={() => setEvents(false)}
                />
            )}
            <Dialog
                open={data != null}
                onClose={() => setData(null)}
                fullWidth
                maxWidth="md"
            >
                <DialogTitleWithClose onClose={() => setData(null)}>Technische Aufgabendaten</DialogTitleWithClose>
                <DialogContent>
                    {data && (
                        <>
                            <Typography variant="h6">Vorgangsdaten der Aufgabe</Typography>
                            <ExpandableCodeBlock
                                value={JSON.stringify(data.processData, null, 2)}
                                language="json"
                            />
                            <Typography variant="h6">Elementdaten der Aufgabe</Typography>
                            <ExpandableCodeBlock
                                value={JSON.stringify(data.nodeData, null, 2)}
                                language="json"
                            />
                            <Typography variant="h6">Laufzeitdaten der Aufgabe</Typography>
                            <ExpandableCodeBlock
                                value={JSON.stringify(data.runtimeData, null, 2)}
                                language="json"
                            />
                        </>
                    )}
                </DialogContent>
            </Dialog>
        </>
    );
}
