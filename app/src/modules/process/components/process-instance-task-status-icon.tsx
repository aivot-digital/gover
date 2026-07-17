import {ProcessTaskStatus} from '../enums/process-task-status';
import React, {type ReactNode} from 'react';
import {Tooltip} from '@mui/material';
import AppBadging from '@aivot/mui-material-symbols-400-n25-outlined/AppBadging';
import PlayCircle from '@aivot/mui-material-symbols-400-n25-outlined/PlayCircle';
import PauseCircle from '@aivot/mui-material-symbols-400-n25-outlined/PauseCircle';
import CheckCircle from '@aivot/mui-material-symbols-400-n25-outlined/CheckCircle';
import Cancel from '@aivot/mui-material-symbols-400-n25-outlined/Cancel';
import Replay from '@aivot/mui-material-symbols-400-n25-outlined/Replay';

interface ProcessInstanceTaskStatusIconProps {
    status: ProcessTaskStatus;
    statusOverride?: string | null;
}

export function ProcessInstanceTaskStatusIcon(props: ProcessInstanceTaskStatusIconProps): ReactNode {
    const {
        status,
        statusOverride,
    } = props;


    if (statusOverride != null) {
        return (
            <Tooltip
                title={statusOverride}
            >
                <AppBadging color="primary"/>
            </Tooltip>
        );
    }

    switch (status) {
        case ProcessTaskStatus.Running:
            return (
                <Tooltip
                    title="Wird ausgeführt"
                >
                    <PlayCircle color="info"/>
                </Tooltip>
            );
        case ProcessTaskStatus.Paused:
            return (
                <Tooltip
                    title="Pausiert"
                >
                    <PauseCircle color="primary"/>
                </Tooltip>
            );
        case ProcessTaskStatus.Completed:
            return (
                <Tooltip
                    title="Abgeschlossen"
                >
                    <CheckCircle color="success"/>
                </Tooltip>
            );
        case ProcessTaskStatus.Failed:
            return (
                <Tooltip
                    title="Fehlgeschlagen"
                >
                    <Cancel color="error"/>
                </Tooltip>
            );
        case ProcessTaskStatus.Restarted:
            return (
                <Tooltip
                    title="Neu gestartet"
                >
                    <Replay color="warning"/>
                </Tooltip>
            );
        default:
            return null;
    }
}
