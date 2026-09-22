export enum ProcessTaskStatus {
    Running = 'Running',
    Paused = 'Paused',
    AwaitingPayment = 'AwaitingPayment',
    AwaitingCustomer = 'AwaitingCustomer',
    AwaitingCommunication = 'AwaitingCommunication',
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
    [ProcessTaskStatus.AwaitingCommunication]: 'Wartet auf Zustellbestätigung',
    [ProcessTaskStatus.Completed]: 'Abgeschlossen',
    [ProcessTaskStatus.Aborted]: 'Abgebrochen',
    [ProcessTaskStatus.Failed]: 'Fehlgeschlagen',
    [ProcessTaskStatus.Restarted]: 'Neu gestartet',
};
