import {compareTaskExecutionOrder} from '../../../../../utils/process-task-executions';
import {type ProcessInstanceTaskEntity} from '../../../../../entities/process-instance-task-entity';

function pickLatestTask(
    tasks: ProcessInstanceTaskEntity[],
    predicate: (task: ProcessInstanceTaskEntity) => boolean,
): ProcessInstanceTaskEntity | null {
    let latestTask: ProcessInstanceTaskEntity | null = null;

    for (const task of tasks) {
        if (!predicate(task)) {
            continue;
        }

        if (latestTask == null || compareTaskExecutionOrder(task, latestTask) > 0) {
            latestTask = task;
        }
    }

    return latestTask;
}

export function getLatestTaskForNode(
    tasks: ProcessInstanceTaskEntity[],
    processNodeId: number,
): ProcessInstanceTaskEntity | null {
    // Keep the restart state until the worker creates its replacement; older loop runs must not become current again.
    return pickLatestTask(tasks, (task) => task.processNodeId === processNodeId);
}

export function getLatestTaskForEdge(
    tasks: ProcessInstanceTaskEntity[],
    fromNodeId: number,
    toNodeId: number,
    viaPort: string,
): ProcessInstanceTaskEntity | null {
    return pickLatestTask(tasks, (task) => (
        task.previousProcessNodeId === fromNodeId &&
        task.processNodeId === toNodeId &&
        task.previousProcessNodePortKey === viaPort
    ));
}

interface TransferredProcessDataComparison {
    currentValue: Record<string, any>;
    previousValue: Record<string, any> | null;
}

export function getTransferredProcessDataComparisonForEdge(
    tasks: ProcessInstanceTaskEntity[],
    fromNodeId: number,
    toNodeId: number,
    viaPort: string,
): TransferredProcessDataComparison | null {
    const targetTask = getLatestTaskForEdge(tasks, fromNodeId, toNodeId, viaPort);
    if (targetTask == null) {
        return null;
    }

    const sourceTask = tasks.find(task => task.id === targetTask.previousProcessInstanceTaskId && task.processNodeId === fromNodeId);

    if (sourceTask == null) {
        return null;
    }

    if (sourceTask.previousProcessInstanceTaskId == null) {
        return {
            currentValue: sourceTask.processData,
            previousValue: {},
        };
    }

    const previousTask = tasks.find((task) => task.id === sourceTask.previousProcessInstanceTaskId);

    return {
        currentValue: sourceTask.processData,
        // If permissions or response shaping omit the predecessor, showing no diff is safer than deriving one from
        // the custom and potentially ambiguous processDataDiff representation.
        previousValue: previousTask?.processData ?? null,
    };
}
