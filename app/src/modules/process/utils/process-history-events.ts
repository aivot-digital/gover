import {type ProcessInstanceHistoryEvent} from '../entities/process-instance-event-entity';

export function isManualHistoryIntervention(event: ProcessInstanceHistoryEvent): boolean {
    return event.details.manualIntervention === true;
}

export function getHistoryEventActor(event: ProcessInstanceHistoryEvent): string {
    return event.triggeringUserName?.trim() || (event.triggeringUserId ? 'Unbekannte Person' : 'System');
}

export type TaskHistoryEvent = ProcessInstanceHistoryEvent & {fromPreviousAttempt: boolean};
