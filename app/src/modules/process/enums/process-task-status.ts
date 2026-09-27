export enum ProcessTaskStatus {
    Running = 'Running',
    Paused = 'Paused',
    AwaitingPayment = 'AwaitingPayment',
    AwaitingCustomer = 'AwaitingCustomer',
    Completed = 'Completed',
    Aborted = 'Aborted',
    Failed = 'Failed',
    Restarted = 'Restarted',
}

export const ProcessTaskStatusLabels: Record<ProcessTaskStatus, string> = {
    [ProcessTaskStatus.Running]: 'Läuft',
    [ProcessTaskStatus.Paused]: 'Pausiert',
    [ProcessTaskStatus.AwaitingPayment]: 'Wartet auf Zahlungsbestätigung',
    [ProcessTaskStatus.AwaitingCustomer]: 'Wartet auf Nutzer:in',
    [ProcessTaskStatus.Completed]: 'Abgeschlossen',
    [ProcessTaskStatus.Aborted]: 'Abgebrochen',
    [ProcessTaskStatus.Failed]: 'Fehlgeschlagen',
    [ProcessTaskStatus.Restarted]: 'Neu gestartet',
};

export const ProcessTaskStatusColors: Record<ProcessTaskStatus, 'default' | 'info' | 'success' | 'warning' | 'error'> = {
    [ProcessTaskStatus.Running]: 'info',
    [ProcessTaskStatus.Paused]: 'warning',
    [ProcessTaskStatus.AwaitingPayment]: 'warning',
    [ProcessTaskStatus.AwaitingCustomer]: 'warning',
    [ProcessTaskStatus.Completed]: 'success',
    [ProcessTaskStatus.Aborted]: 'error',
    [ProcessTaskStatus.Failed]: 'error',
    [ProcessTaskStatus.Restarted]: 'warning',
};
