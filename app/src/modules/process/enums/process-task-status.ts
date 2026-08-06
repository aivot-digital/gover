export enum ProcessTaskStatus {
    Running = 'Running',
    Paused = 'Paused',
    AwaitingPayment = 'AwaitingPayment',
    AwaitingCitizen = 'AwaitingCitizen',
    Completed = 'Completed',
    Aborted = 'Aborted',
    Failed = 'Failed',
    Restarted = 'Restarted',
}

export const ProcessTaskStatusLabels: Record<ProcessTaskStatus, string> = {
    [ProcessTaskStatus.Running]: 'Läuft',
    [ProcessTaskStatus.Paused]: 'Pausiert',
    [ProcessTaskStatus.AwaitingPayment]: 'Wartet auf Zahlungsbestätigung',
    [ProcessTaskStatus.AwaitingCitizen]: 'Wartet auf Bürger',
    [ProcessTaskStatus.Completed]: 'Abgeschlossen',
    [ProcessTaskStatus.Aborted]: 'Abgebrochen',
    [ProcessTaskStatus.Failed]: 'Fehlgeschlagen',
    [ProcessTaskStatus.Restarted]: 'Neu gestartet',
};
