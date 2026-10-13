import {ProcessTaskStatus} from '../enums/process-task-status';
import React, {type ReactNode} from 'react';
import {Tooltip} from '@mui/material';
import AppBadging from '@aivot/mui-material-symbols-400-n25-outlined/AppBadging';
import PlayCircle from '@aivot/mui-material-symbols-400-n25-outlined/PlayCircle';
import PauseCircle from '@aivot/mui-material-symbols-400-n25-outlined/PauseCircle';
import CheckCircle from '@aivot/mui-material-symbols-400-n25-outlined/CheckCircle';
import Cancel from '@aivot/mui-material-symbols-400-n25-outlined/Cancel';
import StopCircle from '@aivot/mui-material-symbols-400-n25-outlined/StopCircle';
import Replay from '@aivot/mui-material-symbols-400-n25-outlined/Replay';
import PaymentArrowDown from '@aivot/mui-material-symbols-400-n25-outlined/PaymentArrowDown';
import ContractEdit from '@aivot/mui-material-symbols-400-n25-outlined/ContractEdit';
import Edit from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import AccountCircle from '@aivot/mui-material-symbols-400-n25-outlined/AccountCircle';
import {type IdentityDataMap} from '../../identity/models/identity-data';
import {getProcessTaskStatusDisplay, type ProcessTaskStatusLabelSource} from '../utils/process-task-status-label';

interface ProcessInstanceTaskStatusIconProps extends ProcessTaskStatusLabelSource {
    identities?: IdentityDataMap | null;
}

export function ProcessInstanceTaskStatusIcon(props: ProcessInstanceTaskStatusIconProps): ReactNode {
    const display = getProcessTaskStatusDisplay(props, props.identities);
    let icon;
    if (props.statusOverride?.trim()) {
        icon = <AppBadging color="primary"/>;
    } else {
        switch (props.status) {
            case ProcessTaskStatus.Running:
                icon = <PlayCircle color="info"/>;
                break;
            case ProcessTaskStatus.InProgress:
                icon = <Edit color="primary"/>;
                break;
            case ProcessTaskStatus.AwaitingStaff:
                icon = <AccountCircle color="warning"/>;
                break;
            case ProcessTaskStatus.AwaitingPayment:
                icon = <PaymentArrowDown color="info"/>;
                break;
            case ProcessTaskStatus.AwaitingCustomer:
                icon = <ContractEdit color="info"/>;
                break;
            case ProcessTaskStatus.Paused:
                icon = <PauseCircle color="primary"/>;
                break;
            case ProcessTaskStatus.Completed:
                icon = <CheckCircle color="success"/>;
                break;
            case ProcessTaskStatus.Aborted:
                icon = <StopCircle color="error"/>;
                break;
            case ProcessTaskStatus.Failed:
                icon = <Cancel color="error"/>;
                break;
            case ProcessTaskStatus.Restarted:
                icon = <Replay color="warning"/>;
                break;
            default:
                return null;
        }
    }

    return <Tooltip title={display.tooltip ?? display.label}>{icon}</Tooltip>;
}
