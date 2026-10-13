import {ProcessInstanceStatus} from '../enums/process-instance-status';

export type ProcessInstanceDeadlineState = 'open' | 'warning' | 'exceeded';

interface ProcessInstanceDeadlineSource {
    status: ProcessInstanceStatus;
    started: string;
    deadline: string | null;
}

// Matches the "Laufende Vorgänge" list view and the dashboard panel.
const ACTIVE_INSTANCE_STATUSES: ProcessInstanceStatus[] = [
    ProcessInstanceStatus.Created,
    ProcessInstanceStatus.Running,
    ProcessInstanceStatus.Paused,
];

/**
 * Evaluates the soft processing deadline of a process instance. It only signals urgency and never affects the
 * process flow. Returns null when no deadline is set or the instance is no longer in progress.
 */
export function getProcessInstanceDeadlineState(
    instance: ProcessInstanceDeadlineSource,
    now: number = Date.now(),
): ProcessInstanceDeadlineState | null {
    if (!ACTIVE_INSTANCE_STATUSES.includes(instance.status)) {
        return null;
    }
    return getProcessInstanceDeadlineProgress(instance.started, instance.deadline, now);
}

/** Time-based part of the deadline state, for sources that only contain instances still in progress. */
export function getProcessInstanceDeadlineProgress(
    startedValue: string,
    deadlineValue: string | null,
    now: number = Date.now(),
): ProcessInstanceDeadlineState | null {
    const deadline = deadlineValue == null ? NaN : Date.parse(deadlineValue);
    if (Number.isNaN(deadline)) {
        return null;
    }
    if (now >= deadline) {
        return 'exceeded';
    }

    const started = Date.parse(startedValue);
    // Warn once half of the period between creation and deadline has passed.
    if (!Number.isNaN(started) && now >= started + (deadline - started) / 2) {
        return 'warning';
    }

    return 'open';
}

export const ProcessInstanceDeadlineStateLabels: Record<ProcessInstanceDeadlineState, string> = {
    open: 'Frist läuft',
    warning: 'Bald fällig',
    exceeded: 'Frist überschritten',
};

export const ProcessInstanceDeadlineStateColors: Record<ProcessInstanceDeadlineState, 'warning' | 'error' | undefined> = {
    open: undefined,
    warning: 'warning',
    exceeded: 'error',
};
