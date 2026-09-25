import {useCallback, useState} from 'react';
import {Button} from '@mui/material';
import AssignmentInd from '@aivot/mui-material-symbols-400-n25-outlined/AssignmentInd';
import {PersonAssignmentDialog} from '../../../components/person-assignment-dialog/person-assignment-dialog';
import {ProcessInstanceApiService} from '../services/process-instance-api-service';
import {ProcessInstanceTaskApiService} from '../services/process-instance-task-api-service';
import {useHasProcessInstancePermission} from '../../permissions/hooks/use-permissions';
import {Permission} from '../../../data/permissions/permission';
import {ProcessTaskStatus} from '../enums/process-task-status';
import {ProcessInstanceStatus} from '../enums/process-instance-status';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {DisabledTooltip} from '../../../components/disabled-tooltip/disabled-tooltip';
import {formatMissingPermissionTooltip} from '../../permissions/utils/permission-utils';

interface ProcessAssignmentButtonProps {
    instanceId: number;
    instanceStatus?: ProcessInstanceStatus;
    taskId?: number;
    taskStatus?: ProcessTaskStatus;
    assignedUserId: string | null;
    onAssigned: () => void;
}

const activeStatuses = new Set([
    ProcessTaskStatus.Running,
    ProcessTaskStatus.Paused,
    ProcessTaskStatus.AwaitingCustomer,
    ProcessTaskStatus.AwaitingPayment,
]);

export function ProcessAssignmentButton({
    instanceId,
    instanceStatus,
    taskId,
    taskStatus,
    assignedUserId,
    onAssigned,
}: ProcessAssignmentButtonProps) {
    const [open, setOpen] = useState(false);
    const isTask = taskId != null;
    const requiredPermission = isTask ? Permission.PROCESS_INSTANCE_EDIT_TASK : Permission.PROCESS_INSTANCE_REASSIGN;
    const allowed = useHasProcessInstancePermission(instanceId, requiredPermission);
    const active = isTask
        ? taskStatus != null && activeStatuses.has(taskStatus)
        : instanceStatus != null &&
          instanceStatus !== ProcessInstanceStatus.Completed &&
          instanceStatus !== ProcessInstanceStatus.Aborted;
    const label = isTask ? 'Aufgabe zuweisen' : 'Vorgang zuweisen';
    const inactiveReason = isTask
        ? 'Nur aktive Aufgaben können zugewiesen werden.'
        : 'Die Zuweisung abgeschlossener oder abgebrochener Vorgänge kann nicht mehr geändert werden.';

    return (
        <>
            <DisabledTooltip
                disabled={!allowed || !active}
                title={
                    !allowed
                        ? formatMissingPermissionTooltip(requiredPermission)
                        : !active
                          ? inactiveReason
                          : undefined
                }
            >
                <Button
                    disabled={!allowed || !active}
                    startIcon={<AssignmentInd />}
                    onClick={() => setOpen(true)}
                >
                    {label}
                </Button>
            </DisabledTooltip>
            {open && allowed && active && (
                <ProcessAssignmentDialog
                    instanceId={instanceId}
                    taskId={taskId}
                    assignedUserId={assignedUserId}
                    onAssigned={onAssigned}
                    onClose={() => setOpen(false)}
                />
            )}
        </>
    );
}

export function ProcessAssignmentDialog({
    instanceId,
    taskId,
    assignedUserId,
    onAssigned,
    onClose,
}: Omit<ProcessAssignmentButtonProps, 'taskStatus' | 'instanceStatus'> & {onClose: () => void}) {
    const dispatch = useAppDispatch();
    const isTask = taskId != null;
    const label = isTask ? 'Aufgabe zuweisen' : 'Vorgang zuweisen';
    const loadOptions = useCallback(async () => {
        const options =
            taskId == null
                ? await new ProcessInstanceApiService().assignmentOptions(instanceId)
                : await new ProcessInstanceTaskApiService().assignmentOptions(taskId);
        return options.map((option) => ({
            value: option.id,
            label: option.name,
            subLabel: option.email ?? undefined,
        }));
    }, [instanceId, taskId]);

    return (
        <PersonAssignmentDialog
            title={label}
            description={
                isTask
                    ? 'Wählen Sie, wer diese Aufgabe bearbeiten soll. Sie können Mitarbeiter:innen mit aktivem Konto auswählen, die den Vorgang einsehen und Aufgaben bearbeiten dürfen. Diese Rechte müssen auch ohne Stellvertretung bestehen.'
                    : 'Wählen Sie, wer für diesen Vorgang zuständig sein soll. Aufgaben werden separat zugewiesen. Sie können Mitarbeiter:innen mit aktivem Konto auswählen, die den Vorgang auch ohne Stellvertretung einsehen dürfen.'
            }
            assignedUserId={assignedUserId}
            allowUnassign={!isTask}
            loadOptions={loadOptions}
            onSave={async (userId) => {
                if (taskId == null) await new ProcessInstanceApiService().reassign(instanceId, userId);
                else await new ProcessInstanceTaskApiService().reassign(taskId, userId);
                dispatch(
                    showSuccessSnackbar(
                        userId == null ? 'Die Zuweisung wurde aufgehoben.' : 'Die Zuweisung wurde gespeichert.',
                    ),
                );
                onAssigned();
            }}
            onClose={onClose}
        />
    );
}
