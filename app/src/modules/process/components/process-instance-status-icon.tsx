import React, {type ReactNode} from 'react';
import {Tooltip} from '@mui/material';
import {
    ProcessInstanceOverrideStatusColor,
    ProcessInstanceOverrideStatusIcon,
    ProcessInstanceStatus,
    ProcessInstanceStatusColor,
    ProcessInstanceStatusIcons,
    ProcessInstanceStatusLabels,
} from '../enums/process-instance-status';

interface ProcessInstanceStatusIconProps {
    status: ProcessInstanceStatus;
    statusOverride?: string | null;
}

export function ProcessInstanceStatusIcon(props: ProcessInstanceStatusIconProps): ReactNode {
    const {
        status,
        statusOverride,
    } = props;


    if (statusOverride != null) {
        return (
            <Tooltip
                title={statusOverride}
            >
                <ProcessInstanceOverrideStatusIcon
                    color={ProcessInstanceOverrideStatusColor}
                />
            </Tooltip>
        );
    }

    const title = ProcessInstanceStatusLabels[status];
    const color = ProcessInstanceStatusColor[status];
    const Icon = ProcessInstanceStatusIcons[status];

    return (
        <Tooltip
            title={title}
        >
            <Icon color={color}/>
        </Tooltip>
    );
}
