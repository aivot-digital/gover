import {type SvgIconComponent} from '../../../types/svg-icon-component';
import Start from '@aivot/mui-material-symbols-400-n25-outlined/Start';
import Edit from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import EmergencyHome from '@aivot/mui-material-symbols-400-n25-outlined/EmergencyHome';
import Check from '@aivot/mui-material-symbols-400-n25-outlined/Check';
import FramePerson from '@aivot/mui-material-symbols-400-n25-outlined/FramePerson';
import Add from '@aivot/mui-material-symbols-400-n25-outlined/Add';

export enum ProcessHistoryEventType {
    Create = 'Create',
    Start = 'Start',
    Update = 'Update',
    Assign = 'Assign',
    Error = 'Error',
    Complete = 'Complete',
}

export const ProcessHistoryEventTypeLabels: Record<ProcessHistoryEventType, string> = {
    [ProcessHistoryEventType.Create]: 'Erstellt',
    [ProcessHistoryEventType.Start]: 'Start',
    [ProcessHistoryEventType.Update]: 'Aktualisierung',
    [ProcessHistoryEventType.Assign]: 'Zuweisung',
    [ProcessHistoryEventType.Error]: 'Fehler',
    [ProcessHistoryEventType.Complete]: 'Abschluss',
};

export const ProcessHistoryEventTypeIcons: Record<ProcessHistoryEventType, SvgIconComponent> = {
    [ProcessHistoryEventType.Create]: Add,
    [ProcessHistoryEventType.Start]: Start,
    [ProcessHistoryEventType.Update]: Edit,
    [ProcessHistoryEventType.Assign]: FramePerson,
    [ProcessHistoryEventType.Error]: EmergencyHome,
    [ProcessHistoryEventType.Complete]: Check,
}
