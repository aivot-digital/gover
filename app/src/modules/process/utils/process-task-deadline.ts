import {ProcessTaskStatus} from '../enums/process-task-status';

export interface ProcessTaskDeadlineSource {
    status: ProcessTaskStatus;
    deadline?: string | null;
    finished?: string | null;
}

const activeStatuses: ReadonlySet<ProcessTaskStatus> = new Set([
    ProcessTaskStatus.Running,
    ProcessTaskStatus.InProgress,
    ProcessTaskStatus.AwaitingStaff,
    ProcessTaskStatus.Paused,
    ProcessTaskStatus.AwaitingCustomer,
    ProcessTaskStatus.AwaitingPayment,
]);

export function hasActiveTaskDeadline(task: ProcessTaskDeadlineSource): boolean {
    return activeStatuses.has(task.status) && task.finished == null && Number.isFinite(Date.parse(task.deadline ?? ''));
}

export function getProcessTaskDeadlineState(
    task: ProcessTaskDeadlineSource,
    now: number = Date.now(),
): 'open' | 'exceeded' | null {
    if (task.status === ProcessTaskStatus.DeadlineExceeded) return 'exceeded';
    const deadline = Date.parse(task.deadline ?? '');
    if (!Number.isFinite(deadline)) return null;
    if (task.finished != null) {
        // Finishing at the deadline is still on time. Later visits must not change the historical result.
        return Date.parse(task.finished) > deadline ? 'exceeded' : null;
    }
    if (!hasActiveTaskDeadline(task)) return null;
    // A phase can start after the task itself. Without its start time, a percentage-based warning would be misleading.
    return now >= deadline ? 'exceeded' : 'open';
}
