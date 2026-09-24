import {useState} from 'react';
import {Menu, MenuItem, Typography} from '@mui/material';
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

    const restart = async () => {
        setAnchor(null);
        const label = task ? 'Aufgabe erneut starten' : 'Vorgang erneut starten';
        if (
            !(await confirm({
                title: label,
                isDestructive: false,
                children: (
                    <Typography>
                        {`Möchten Sie ${task ? 'die fehlgeschlagene Aufgabe' : 'den fehlgeschlagenen Vorgang'} erneut starten?`}
                    </Typography>
                ),
            }))
        )
            return;
        setBusy(true);
        try {
            if (task) await new ProcessInstanceTaskApiService().rerunFailedTask(task.id);
            else await new ProcessInstanceApiService().restartFailedInstance(instanceId);
            dispatch(showSuccessSnackbar('Der Neustart wurde angestoßen.'));
            onChanged();
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Der Neustart ist fehlgeschlagen.'));
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
                {canReadModel && (
                    <MenuItem
                        component={Link}
                        to={`/processes/${item.processId}/versions/${item.processVersion}/?instanceId=${instanceId}`}
                    >
                        Im Prozessmodell ansehen
                    </MenuItem>
                )}
                {canRestart && failed && <MenuItem onClick={() => void restart()}>Erneut starten</MenuItem>}
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
        </>
    );
}
