import {FC} from "react";
import {SvgIconProps} from "@mui/material";
import Start from "@aivot/mui-material-symbols-400-outlined/dist/start/Start";
import Edit from "@aivot/mui-material-symbols-400-outlined/dist/edit/Edit";
import EmergencyHome from "@aivot/mui-material-symbols-400-outlined/dist/emergency-home/EmergencyHome";
import Check from "@aivot/mui-material-symbols-400-outlined/dist/check/Check";
import FramePerson from "@aivot/mui-material-symbols-400-outlined/dist/frame-person/FramePerson";
import Add from "@aivot/mui-material-symbols-400-outlined/dist/add/Add";

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

export const ProcessHistoryEventTypeIcons: Record<ProcessHistoryEventType, FC<SvgIconProps>> = {
    [ProcessHistoryEventType.Create]: Add,
    [ProcessHistoryEventType.Start]: Start,
    [ProcessHistoryEventType.Update]: Edit,
    [ProcessHistoryEventType.Assign]: FramePerson,
    [ProcessHistoryEventType.Error]: EmergencyHome,
    [ProcessHistoryEventType.Complete]: Check,
}