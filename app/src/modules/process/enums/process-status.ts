import {SvgIconComponent} from '@mui/icons-material';
import DraftOrders from '@aivot/mui-material-symbols-400-outlined/dist/draft-orders/DraftOrders';
import Unpublished from '@aivot/mui-material-symbols-400-outlined/dist/unpublished/Unpublished';
import CheckCircle from '@aivot/mui-material-symbols-400-outlined/dist/check-circle/CheckCircle';

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