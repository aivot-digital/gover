import {ProcessTaskStatus} from '../enums/process-task-status';
import React, {type ReactNode} from 'react';
import {Icon, Tooltip} from '@mui/material';
import AppBadging from '@aivot/mui-material-symbols-400-outlined/dist/app-badging/AppBadging';
import PlayCircle from '@aivot/mui-material-symbols-400-outlined/dist/play-circle/PlayCircle';
import PauseCircle from '@aivot/mui-material-symbols-400-outlined/dist/pause-circle/PauseCircle';
import CheckCircle from '@aivot/mui-material-symbols-400-outlined/dist/check-circle/CheckCircle';
import Cancel from '@aivot/mui-material-symbols-400-outlined/dist/cancel/Cancel';

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
        default:
            return null;
    }
}
