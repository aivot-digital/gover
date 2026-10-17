export enum ProcessTaskStatus {
    Running = 'Running',
    Paused = 'Paused',
    AwaitingPayment = 'AwaitingPayment',
    AwaitingCustomer = 'AwaitingCustomer',
    Completed = 'Completed',
    Aborted = 'Aborted',
    Failed = 'Failed',
    Restarted = 'Restarted',
    InProgress = 'InProgress',
    AwaitingStaff = 'AwaitingStaff',
    DeadlineExceeded = 'DeadlineExceeded',
}

// Mirrors the backend staff task view, which rejects tasks outside these statuses.
const StaffEditableProcessTaskStatuses: ReadonlySet<ProcessTaskStatus> = new Set([
    ProcessTaskStatus.Running,
    ProcessTaskStatus.InProgress,
    ProcessTaskStatus.AwaitingStaff,
]);

export function isProcessTaskEditableByStaff(status: ProcessTaskStatus | null | undefined): boolean {
    return status != null && StaffEditableProcessTaskStatuses.has(status);
}

export const ProcessTaskStatusLabels: Record<ProcessTaskStatus, string> = {
    [ProcessTaskStatus.Running]: 'Gestartet',
    [ProcessTaskStatus.Paused]: 'Pausiert',
    [ProcessTaskStatus.AwaitingPayment]: 'Wartet auf Zahlungsbestätigung',
    [ProcessTaskStatus.AwaitingCustomer]: 'Wartet auf Nutzer:in',
    [ProcessTaskStatus.Completed]: 'Abgeschlossen',
    [ProcessTaskStatus.Aborted]: 'Abgebrochen',
    [ProcessTaskStatus.Failed]: 'Fehlgeschlagen',
    [ProcessTaskStatus.Restarted]: 'Neu gestartet',
    [ProcessTaskStatus.InProgress]: 'In Bearbeitung',
    [ProcessTaskStatus.AwaitingStaff]: 'Wartet auf Bearbeitung',
    [ProcessTaskStatus.DeadlineExceeded]: 'Frist überschritten',
};

export const ProcessTaskStatusColors: Record<ProcessTaskStatus, 'default' | 'primary' | 'info' | 'success' | 'warning' | 'error'> = {
    [ProcessTaskStatus.Running]: 'info',
    [ProcessTaskStatus.Paused]: 'warning',
    [ProcessTaskStatus.AwaitingPayment]: 'warning',
    [ProcessTaskStatus.AwaitingCustomer]: 'warning',
    [ProcessTaskStatus.Completed]: 'success',
    [ProcessTaskStatus.Aborted]: 'error',
    [ProcessTaskStatus.Failed]: 'error',
    [ProcessTaskStatus.Restarted]: 'warning',
    [ProcessTaskStatus.InProgress]: 'primary',
    [ProcessTaskStatus.AwaitingStaff]: 'warning',
    [ProcessTaskStatus.DeadlineExceeded]: 'error',
};
