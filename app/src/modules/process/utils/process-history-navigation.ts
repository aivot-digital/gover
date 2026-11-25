import {type ProcessInstanceTaskEntity} from '../entities/process-instance-task-entity';
import {ProcessTaskStatus} from '../enums/process-task-status';

export const activeHistoryTaskStatuses: ReadonlySet<ProcessTaskStatus> = new Set([
    ProcessTaskStatus.Running, ProcessTaskStatus.InProgress, ProcessTaskStatus.AwaitingStaff,
    ProcessTaskStatus.Paused, ProcessTaskStatus.AwaitingCustomer, ProcessTaskStatus.AwaitingPayment,
]);

export function resolveHistoryTaskTarget(tasks: ProcessInstanceTaskEntity[], target: string): number[] {
    const active = tasks.filter(task => activeHistoryTaskStatuses.has(task.status));
    switch (target) {
        case 'active': return active.map(task => task.id);
        case 'current': return active.length > 0 ? active.map(task => task.id) : tasks.slice(-1).map(task => task.id);
        case 'failed': return tasks.filter(task => task.status === ProcessTaskStatus.Failed).map(task => task.id);
        case 'latest': return tasks.slice(-1).map(task => task.id);
        default: {
            const id = Number(target);
            return /^[1-9]\d*$/.test(target) && Number.isSafeInteger(id) && tasks.some(task => task.id === id) ? [id] : [];
        }
    }
}
