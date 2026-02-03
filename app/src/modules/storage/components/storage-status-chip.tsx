import {
    StorageProviderStatus,
    StorageProviderStatusColors,
    StorageProviderStatusLabels,
} from '../enums/storage-provider-status';
import Chip from '@mui/material/Chip';
import React, {type ReactNode, useMemo} from 'react';
import {type SvgIconComponent} from '@mui/icons-material';
import SyncProblem from '@aivot/mui-material-symbols-400-outlined/dist/sync-problem/SyncProblem';
import SyncArrowDown from '@aivot/mui-material-symbols-400-outlined/dist/sync-arrow-down/SyncArrowDown';
import Autoplay from '@aivot/mui-material-symbols-400-outlined/dist/autoplay/Autoplay';
import CheckCircle from '@aivot/mui-material-symbols-400-outlined/dist/check-circle/CheckCircle';

interface StorageStatusChipProps {
    status: StorageProviderStatus;
}

const iconMap: Record<StorageProviderStatus, SvgIconComponent> = {
    [StorageProviderStatus.SyncPending]: Autoplay,
    [StorageProviderStatus.Syncing]: SyncArrowDown,
    [StorageProviderStatus.Synced]: CheckCircle,
    [StorageProviderStatus.SyncFailed]: SyncProblem,
};

export function StorageStatusChip(props: StorageStatusChipProps): ReactNode {
    const {
        status,
    } = props;

    const Icon = useMemo(() => {
        return iconMap[status];
    }, [status]);

    return (
        <Chip
            label={StorageProviderStatusLabels[status]}
            color={StorageProviderStatusColors[status]}
            size="small"
            icon={<Icon fontSize="small"/>}
        />
    );
}
