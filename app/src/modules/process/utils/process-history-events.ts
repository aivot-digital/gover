import {type ProcessInstanceTaskEntity} from '../entities/process-instance-task-entity';
import {ProcessTaskStatus} from '../enums/process-task-status';
import {type ProcessInstanceHistoryEvent} from '../entities/process-instance-event-entity';

export function isManualHistoryIntervention(event: ProcessInstanceHistoryEvent): boolean {
    return event.details.manualIntervention === true;
}

export function getHistoryEventActor(event: ProcessInstanceHistoryEvent): string {
    return event.triggeringUserName?.trim() || (event.triggeringUserId ? 'Unbekannte Person' : 'System');
}

export type TaskHistoryEvent = ProcessInstanceHistoryEvent & {fromPreviousAttempt: boolean};

/** Include manual interventions from replaced attempts without mixing their execution results into this task. */
export function getTaskHistoryEvents(
    task: ProcessInstanceTaskEntity,
    tasksById: ReadonlyMap<number, ProcessInstanceTaskEntity>,
    events: readonly ProcessInstanceHistoryEvent[],
): TaskHistoryEvent[] {
    const previousAttemptIds = new Set<number>();
    const visited = new Set([task.id]);
    let previousId = task.restartForTaskId;
    while (previousId != null && !visited.has(previousId)) {
        const previous = tasksById.get(previousId);
        if (previous == null || previous.processInstanceId !== task.processInstanceId ||
            previous.processNodeId !== task.processNodeId || previous.status !== ProcessTaskStatus.Restarted) {
            break;
        }
        visited.add(previousId);
        previousAttemptIds.add(previousId);
        previousId = previous.restartForTaskId;
    }

    const seen = new Set<number>();
    return events.flatMap(event => {
        if (event.processInstanceId !== task.processInstanceId || seen.has(event.id)) return [];
        const fromPreviousAttempt = event.processInstanceTaskId != null && previousAttemptIds.has(event.processInstanceTaskId);
        if (event.processInstanceTaskId !== task.id && !(fromPreviousAttempt && isManualHistoryIntervention(event))) return [];
        seen.add(event.id);
        return [{...event, fromPreviousAttempt}];
    }).sort((left, right) => (Date.parse(left.timestamp) || 0) - (Date.parse(right.timestamp) || 0) || left.id - right.id);
}
