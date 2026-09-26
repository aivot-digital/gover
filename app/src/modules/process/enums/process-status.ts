import {type SvgIconComponent} from '../../../types/svg-icon-component';
import DraftOrders from '@aivot/mui-material-symbols-400-n25-outlined/DraftOrders';
import Inventory2 from '@aivot/mui-material-symbols-400-n25-outlined/Inventory2';
import Route from '@aivot/mui-material-symbols-400-n25-outlined/Route';

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
    Published: Route,
    Revoked: Inventory2,
};

export const ProcessStatusLabels: Record<ProcessStatus, string> = {
    Drafted: 'Entwurf',
    Published: 'Veröffentlicht',
    Revoked: 'Zurückgezogen',
};
