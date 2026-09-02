import {type SvgIconComponent} from '../../../types/svg-icon-component';
import Download from '@aivot/mui-material-symbols-400-n25-outlined/Download';
import PlayCircle from '@aivot/mui-material-symbols-400-n25-outlined/PlayCircle';
import PauseCircle from '@aivot/mui-material-symbols-400-n25-outlined/PauseCircle';
import CheckCircle from '@aivot/mui-material-symbols-400-n25-outlined/CheckCircle';
import StopCircle from '@aivot/mui-material-symbols-400-n25-outlined/StopCircle';
import Cancel from '@aivot/mui-material-symbols-400-n25-outlined/Cancel';
import AppBadging from '@aivot/mui-material-symbols-400-n25-outlined/AppBadging';

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

type ProcessIconColor = 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning';

export const ProcessInstanceStatusColor: Record<ProcessInstanceStatus, ProcessIconColor> = {
    [ProcessInstanceStatus.Created]: 'info',
    [ProcessInstanceStatus.Running]: 'primary',
    [ProcessInstanceStatus.Paused]: 'info',
    [ProcessInstanceStatus.Completed]: 'success',
    [ProcessInstanceStatus.Aborted]: 'warning',
    [ProcessInstanceStatus.Failed]: 'error',
};

export const ProcessInstanceStatusIcons: Record<ProcessInstanceStatus, SvgIconComponent> = {
    [ProcessInstanceStatus.Created]: Download,
    [ProcessInstanceStatus.Running]: PlayCircle,
    [ProcessInstanceStatus.Paused]: PauseCircle,
    [ProcessInstanceStatus.Completed]: CheckCircle,
    [ProcessInstanceStatus.Aborted]: StopCircle,
    [ProcessInstanceStatus.Failed]: Cancel,
};

export const ProcessInstanceOverrideStatusColor: ProcessIconColor = 'primary';
export const ProcessInstanceOverrideStatusIcon: SvgIconComponent = AppBadging;
