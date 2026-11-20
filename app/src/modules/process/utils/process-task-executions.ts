import {ProcessTaskStatus} from '../enums/process-task-status';
import {type ProcessInstanceTaskEntity} from '../entities/process-instance-task-entity';

function parseTimestamp(value: string | null): number {
    if (value == null) {
        return Number.NEGATIVE_INFINITY;
    }

    const parsed = Date.parse(value);
    return Number.isNaN(parsed) ? Number.NEGATIVE_INFINITY : parsed;
}

export function compareTaskExecutionOrder(left: ProcessInstanceTaskEntity, right: ProcessInstanceTaskEntity): number {
    const leftStarted = parseTimestamp(left.started);
    const rightStarted = parseTimestamp(right.started);
    return (leftStarted === rightStarted ? 0 : leftStarted > rightStarted ? 1 : -1) || left.id - right.id;
}

// Replaced attempts do not add a loop execution, matching the process instance history.
export function getExecutionsForNode(tasks: ProcessInstanceTaskEntity[], processNodeId: number): ProcessInstanceTaskEntity[] {
    return tasks.filter(task => task.processNodeId === processNodeId && task.status !== ProcessTaskStatus.Restarted)
        .sort(compareTaskExecutionOrder);
}
