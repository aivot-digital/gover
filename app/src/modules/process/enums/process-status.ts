import {type SvgIconComponent} from '../../../types/svg-icon-component';
import DraftOrders from '@aivot/mui-material-symbols-400-n25-outlined/DraftOrders';
import Unpublished from '@aivot/mui-material-symbols-400-n25-outlined/Unpublished';
import CheckCircle from '@aivot/mui-material-symbols-400-n25-outlined/CheckCircle';

export enum ProcessStatus {
    Drafted = 'Drafted',
    Published = 'Published',
    Revoked = 'Revoked',
}

export const ProcessStatusColors: Record<ProcessStatus, 'success' | 'warning' | 'error' | 'info'> = {
    Drafted: 'info',
    Published: 'success',
    Revoked: 'warning',
};

export const ProcessStatusIcons: Record<ProcessStatus, SvgIconComponent> = {
    Drafted: DraftOrders,
    Published: CheckCircle,
    Revoked: Unpublished,
};

export const ProcessStatusLabels: Record<ProcessStatus, string> = {
    Drafted: 'Entwurf',
    Published: 'Veröffentlicht',
    Revoked: 'Zurückgezogen',
};