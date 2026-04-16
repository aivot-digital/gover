import {SvgIconComponent} from '@mui/icons-material';
import Download from '@aivot/mui-material-symbols-400-outlined/dist/download/Download';
import PlayCircle from '@aivot/mui-material-symbols-400-outlined/dist/play-circle/PlayCircle';
import PauseCircle from '@aivot/mui-material-symbols-400-outlined/dist/pause-circle/PauseCircle';
import CheckCircle from '@aivot/mui-material-symbols-400-outlined/dist/check-circle/CheckCircle';
import StopCircle from '@aivot/mui-material-symbols-400-outlined/dist/stop-circle/StopCircle';
import Cancel from '@aivot/mui-material-symbols-400-outlined/dist/cancel/Cancel';
import AppBadging from '@aivot/mui-material-symbols-400-outlined/dist/app-badging/AppBadging';

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
