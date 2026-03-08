import {type ProcessInstanceTaskEntity} from '../../../../../entities/process-instance-task-entity';

function parseTimestamp(value: string | null): number {
    if (value == null) {
        return Number.NEGATIVE_INFINITY;
    }

    const parsed = Date.parse(value);
    return Number.isNaN(parsed) ? Number.NEGATIVE_INFINITY : parsed;
}

function isNewerTask(candidate: ProcessInstanceTaskEntity, current: ProcessInstanceTaskEntity): boolean {
    const candidateUpdated = parseTimestamp(candidate.updated);
    const currentUpdated = parseTimestamp(current.updated);
    if (candidateUpdated !== currentUpdated) {
        return candidateUpdated > currentUpdated;
    }

    const candidateStarted = parseTimestamp(candidate.started);
    const currentStarted = parseTimestamp(current.started);
    if (candidateStarted !== currentStarted) {
        return candidateStarted > currentStarted;
    }

    return candidate.id > current.id;
}

function pickLatestTask(
    tasks: ProcessInstanceTaskEntity[],
    predicate: (task: ProcessInstanceTaskEntity) => boolean,
): ProcessInstanceTaskEntity | null {
    let latestTask: ProcessInstanceTaskEntity | null = null;

    for (const task of tasks) {
        if (!predicate(task)) {
            continue;
        }

        if (latestTask == null || isNewerTask(task, latestTask)) {
            latestTask = task;
        }
    }

    return latestTask;
}

export function getLatestTaskForNode(
    tasks: ProcessInstanceTaskEntity[],
    processNodeId: number,
): ProcessInstanceTaskEntity | null {
    return pickLatestTask(tasks, (task) => task.processNodeId === processNodeId);
}

export function getLatestTaskForEdge(
    tasks: ProcessInstanceTaskEntity[],
    fromNodeId: number,
    toNodeId: number,
): ProcessInstanceTaskEntity | null {
    return pickLatestTask(tasks, (task) => (
        task.previousProcessNodeId === fromNodeId &&
        task.processNodeId === toNodeId
    ));
}
