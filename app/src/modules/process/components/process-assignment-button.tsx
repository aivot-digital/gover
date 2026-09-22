import {useCallback, useState} from 'react';
import {Button} from '@mui/material';
import AssignmentInd from '@aivot/mui-material-symbols-400-n25-outlined/AssignmentInd';
import {PersonAssignmentDialog} from '../../../components/person-assignment-dialog/person-assignment-dialog';
import {ProcessInstanceApiService} from '../services/process-instance-api-service';
import {ProcessInstanceTaskApiService} from '../services/process-instance-task-api-service';
import {useHasProcessInstancePermission} from '../../permissions/hooks/use-permissions';
import {Permission} from '../../../data/permissions/permission';
import {ProcessTaskStatus} from '../enums/process-task-status';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {DisabledTooltip} from '../../../components/disabled-tooltip/disabled-tooltip';
import {formatMissingPermissionTooltip} from '../../permissions/utils/permission-utils';

interface ProcessAssignmentButtonProps {
    instanceId: number;
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
    taskId,
    taskStatus,
    assignedUserId,
    onAssigned,
}: ProcessAssignmentButtonProps) {
    const [open, setOpen] = useState(false);
    const isTask = taskId != null;
    const requiredPermission = isTask ? Permission.PROCESS_INSTANCE_EDIT_TASK : Permission.PROCESS_INSTANCE_REASSIGN;
    const allowed = useHasProcessInstancePermission(instanceId, requiredPermission);
    const active = !isTask || (taskStatus != null && activeStatuses.has(taskStatus));
    const label = isTask ? 'Aufgabe zuweisen' : 'Vorgang zuweisen';

    return (
        <>
            <DisabledTooltip
                disabled={!allowed || !active}
                title={
                    !allowed
                        ? formatMissingPermissionTooltip(requiredPermission)
                        : !active
                          ? 'Nur aktive Aufgaben können zugewiesen werden.'
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
}: Omit<ProcessAssignmentButtonProps, 'taskStatus'> & {onClose: () => void}) {
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
                    ? 'Mit der Zuweisung legen Sie fest, wer für die Bearbeitung dieser Aufgabe zuständig ist. Zur Auswahl stehen Mitarbeiter:innen mit aktivem Benutzerkonto, die den Vorgang anzeigen und seine Aufgaben bearbeiten dürfen. Die Zuständigkeit für den gesamten Vorgang und die Zuweisungen anderer Aufgaben bleiben unverändert.'
                    : 'Mit der Zuweisung legen Sie fest, wer für den gesamten Vorgang zuständig ist. Einzelne Aufgaben können weiterhin von anderen Personen bearbeitet werden; ihre Zuweisungen bleiben unverändert. Zur Auswahl stehen Mitarbeiter:innen mit aktivem Benutzerkonto, die den Vorgang anzeigen dürfen.'
            }
            assignedUserId={assignedUserId}
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
