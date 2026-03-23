export enum ProcessInstanceStatus {
    Created = 'Created',
    Running = 'Running',
    Paused = 'Paused',
    Completed = 'Completed',
    Aborted = 'Aborted',
    Failed = 'Failed',
}

export const ProcessInstanceStatusLabels: Record<ProcessInstanceStatus, string> = {
    [ProcessInstanceStatus.Created]: 'Erstellt',
    [ProcessInstanceStatus.Running]: 'In Bearbeitung',
    [ProcessInstanceStatus.Paused]: 'Pausiert',
    [ProcessInstanceStatus.Completed]: 'Abgeschlossen',
    [ProcessInstanceStatus.Aborted]: 'Abgebrochen',
    [ProcessInstanceStatus.Failed]: 'Fehlgeschlagen',
};