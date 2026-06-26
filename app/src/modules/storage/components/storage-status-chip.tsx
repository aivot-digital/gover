import {
    StorageProviderStatus,
    StorageProviderStatusColors,
    StorageProviderStatusLabels,
} from '../enums/storage-provider-status';
import React, { type ReactNode, useEffect, useMemo, useState } from 'react';
import { type SvgIconComponent } from '@mui/icons-material';
import SyncProblem from '@aivot/mui-material-symbols-400-outlined/dist/sync-problem/SyncProblem';
import SyncArrowDown from '@aivot/mui-material-symbols-400-outlined/dist/sync-arrow-down/SyncArrowDown';
import Autoplay from '@aivot/mui-material-symbols-400-outlined/dist/autoplay/Autoplay';
import CheckCircle from '@aivot/mui-material-symbols-400-outlined/dist/check-circle/CheckCircle';
import { Chip } from '../../../components/chip/chip';

import { format, formatDistanceToNowStrict } from 'date-fns';
import { de } from 'date-fns/locale';

interface StorageStatusChipProps {
    status: StorageProviderStatus;
    /** Last successful sync timestamp as ISO string without timezone (local time). */
    lastSync?: string | null;
}

const iconMap: Record<StorageProviderStatus, SvgIconComponent> = {
    [StorageProviderStatus.SyncPending]: Autoplay,
    [StorageProviderStatus.Syncing]: SyncArrowDown,
    [StorageProviderStatus.Synced]: CheckCircle,
    [StorageProviderStatus.SyncFailed]: SyncProblem,
};

function parseIsoLocal(value: string): Date | null {
    // JS Date supports milliseconds only; trim potential microseconds (e.g. .718476 -> .718).
    const normalized = value.replace(/(\.\d{3})\d+/, '$1');
    const d = new Date(normalized);
    return Number.isNaN(d.getTime()) ? null : d;
}

export function StorageStatusChip(props: StorageStatusChipProps): ReactNode {
    const { status, lastSync } = props;

    const Icon = useMemo(() => iconMap[status], [status]);

    const lastSyncDate = useMemo(() => {
        if (!lastSync) return null;
        return parseIsoLocal(lastSync);
    }, [lastSync]);

    const [minuteTick, setMinuteTick] = useState(0);

    useEffect(() => {
        if (status !== StorageProviderStatus.Synced) return;
        if (!lastSyncDate) return;

        const id = window.setInterval(() => setMinuteTick((t) => t + 1), 60_000);
        return () => window.clearInterval(id);
    }, [status, lastSyncDate]);

    const label = useMemo(() => {
        const base = StorageProviderStatusLabels[status];

        if (status !== StorageProviderStatus.Synced) return base;
        if (!lastSyncDate) return base;

        const diffMs = Date.now() - lastSyncDate.getTime();
        const underOneMinute = diffMs >= 0 && diffMs < 60_000;

        if (underOneMinute) {
            return `${base} vor weniger als einer Minute`;
        }

        const rel = formatDistanceToNowStrict(lastSyncDate, {
            addSuffix: true,
            locale: de,
        });

        return `${base} ${rel}`;
    }, [status, lastSyncDate, minuteTick]);

    const formattedLastSync = useMemo(() => {
        if (!lastSyncDate) return undefined;
        return format(lastSyncDate, 'dd.MM.yyyy – HH:mm:ss', { locale: de });
    }, [lastSyncDate]);

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