export enum ProcessTaskStatus {
    Running = 'Running',
    Paused = 'Paused',
    Completed = 'Completed',
    Aborted = 'Aborted',
    Failed = 'Failed',
}

export const ProcessTaskStatusLabels: Record<ProcessTaskStatus, string> = {
    [ProcessTaskStatus.Running]: 'In Bearbeitung',
    [ProcessTaskStatus.Paused]: 'Pausiert',
    [ProcessTaskStatus.Completed]: 'Abgeschlossen',
    [ProcessTaskStatus.Aborted]: 'Abgebrochen',
    [ProcessTaskStatus.Failed]: 'Fehlgeschlagen',
};