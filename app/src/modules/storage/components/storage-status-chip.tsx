import {
    StorageProviderStatus,
    StorageProviderStatusColors,
    StorageProviderStatusLabels,
} from '../enums/storage-provider-status';
import React, { type ReactNode, useEffect, useMemo, useState } from 'react';
import {type SvgIconComponent} from '../../../types/svg-icon-component';
import SyncProblem from '@aivot/mui-material-symbols-400-n25-outlined/SyncProblem';
import SyncArrowDown from '@aivot/mui-material-symbols-400-n25-outlined/SyncArrowDown';
import Autoplay from '@aivot/mui-material-symbols-400-n25-outlined/Autoplay';
import CheckCircle from '@aivot/mui-material-symbols-400-n25-outlined/CheckCircle';
import { Chip } from '../../../components/chip/chip';

import {
    formatInstantInApplicationTimeZone,
    formatRelativeEpochMillisInApplicationTimeZone,
    instantToEpochMillis,
} from '../../../utils/temporal-utils';

interface StorageStatusChipProps {
    status: StorageProviderStatus;
    /** Last successful sync as an ISO instant with `Z` or an explicit offset. */
    lastSync?: string | null;
}

const iconMap: Record<StorageProviderStatus, SvgIconComponent> = {
    [StorageProviderStatus.SyncPending]: Autoplay,
    [StorageProviderStatus.Syncing]: SyncArrowDown,
    [StorageProviderStatus.Synced]: CheckCircle,
    [StorageProviderStatus.SyncFailed]: SyncProblem,
};

export function StorageStatusChip(props: StorageStatusChipProps): ReactNode {
    const { status, lastSync } = props;

    const Icon = useMemo(() => iconMap[status], [status]);

    const lastSyncEpochMillis = useMemo(() => {
        if (!lastSync) return null;
        return instantToEpochMillis(lastSync);
    }, [lastSync]);

    const [minuteTick, setMinuteTick] = useState(0);

    useEffect(() => {
        if (status !== StorageProviderStatus.Synced) return;
        if (lastSyncEpochMillis == null) return;

        const id = window.setInterval(() => setMinuteTick((t) => t + 1), 60_000);
        return () => window.clearInterval(id);
    }, [status, lastSyncEpochMillis]);

    const label = useMemo(() => {
        const base = StorageProviderStatusLabels[status];

        if (status !== StorageProviderStatus.Synced) return base;
        if (lastSyncEpochMillis == null) return base;

        const now = Date.now();
        const diffMs = now - lastSyncEpochMillis;
        const underOneMinute = diffMs >= 0 && diffMs < 60_000;

        if (underOneMinute) {
            return `${base} vor weniger als einer Minute`;
        }

        const relative = formatRelativeEpochMillisInApplicationTimeZone(lastSyncEpochMillis, now);

        return relative == null ? base : `${base} ${relative}`;
    }, [status, lastSyncEpochMillis, minuteTick]);

    const formattedLastSync = useMemo(() => {
        return formatInstantInApplicationTimeZone(lastSync, 'dd.MM.yyyy – HH:mm:ss') ?? undefined;
    }, [lastSync]);

    const title = useMemo(() => {
        if (!formattedLastSync) return undefined;

        if (status === StorageProviderStatus.SyncFailed) {
            return `Letzte erfolgreiche Synchronisierung: ${formattedLastSync} Uhr`;
        }

        if (status === StorageProviderStatus.Synced) {
            return `Zuletzt synchronisiert: ${formattedLastSync} Uhr`;
        }

        return undefined;
    }, [status, formattedLastSync]);

    return (
        <Chip
            label={label}
            title={title}
            color={StorageProviderStatusColors[status]}
            size="small"
            icon={<Icon fontSize="small" />}
            mode="soft"
        />
    );
}
