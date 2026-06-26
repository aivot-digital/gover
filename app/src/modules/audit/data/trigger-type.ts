import Add from '@aivot/mui-material-symbols-400-outlined/dist/add/Add';
import {DoNotDisturb, Message, SvgIconComponent} from '@mui/icons-material';
import Edit from '@aivot/mui-material-symbols-400-outlined/dist/edit/Edit';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import Error from '@aivot/mui-material-symbols-400-outlined/dist/error/Error';
import ArrowDownward from '@aivot/mui-material-symbols-400-outlined/dist/arrow-downward/ArrowDownward';
import ArrowUpward from '@aivot/mui-material-symbols-400-outlined/dist/arrow-upward/ArrowUpward';

type TriggerDefault = 'Create' |
    'Update' |
    'Delete' |
    'Error' |
    'PermissionDenied' |
    'Export' |
    'Import' |
    'Message';

const triggerTypeDefaultLabels: Record<TriggerDefault, string> = {
    'Create': 'Erstellt',
    'Update': 'Aktualisiert',
    'Delete': 'Gelöscht',
    'Error': 'Fehler',
    'PermissionDenied': 'Zugriff verweigert',
    'Export': 'Datenexport',
    'Import': 'Datenimport',
    'Message': 'Nachricht',
};

export function getTriggerTypeLabel(triggerType: TriggerDefault | string | null | undefined): string {
    if (!triggerType) {
        return '';
    }
    return triggerTypeDefaultLabels[triggerType as TriggerDefault] ?? triggerType;
}

const triggerDefaultColors: Record<TriggerDefault, 'default' | 'success' | 'warning' | 'error' | 'info'> = {
    'Create': 'success',
    'Update': 'warning',
    'Delete': 'error',
    'Error': 'error',
    'PermissionDenied': 'info',
    'Export': 'default',
    'Import': 'default',
    'Message': 'default',
};

export function getTriggerTypeColor(triggerType: TriggerDefault | string | null | undefined): 'default' | 'success' | 'warning' | 'error' | 'info' {
    if (!triggerType) {
        return 'default';
    }
    return triggerDefaultColors[triggerType as TriggerDefault] ?? 'default';
}

const triggerDefaultIcons: Record<TriggerDefault, SvgIconComponent> = {
    'Create': Add,
    'Update': Edit,
    'Delete': Delete,
    'Error': Error,
    'PermissionDenied': DoNotDisturb,
    'Export': ArrowDownward,
    'Import': ArrowUpward,
    'Message': Message,
};

export function getTriggerTypeIcon(triggerType: TriggerDefault | string | null | undefined): SvgIconComponent | null {
    if (!triggerType) {
        return null;
    }
    return triggerDefaultIcons[triggerType as TriggerDefault] ?? null;
}