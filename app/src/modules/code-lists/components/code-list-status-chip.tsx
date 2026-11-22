import React, {ReactNode, useEffect, useMemo, useState} from 'react';
import {Chip} from '../../../components/chip/chip';
import {CodeListStatus, CodeListStatusColors, CodeListStatusLabels} from '../enums/code-list-status';
import SyncProblem from '@aivot/mui-material-symbols-400-n25-outlined/SyncProblem';
import SyncArrowDown from '@aivot/mui-material-symbols-400-n25-outlined/SyncArrowDown';
import Autoplay from '@aivot/mui-material-symbols-400-n25-outlined/Autoplay';
import CheckCircle from '@aivot/mui-material-symbols-400-n25-outlined/CheckCircle';
import Warning from '@aivot/mui-material-symbols-400-n25-outlined/Warning';
import {SvgIconComponent} from '../../../types/svg-icon-component';
import {CodeListSourceType, CodeListSourceTypeLabels, isCodeListSyncable} from '../enums/code-list-source-type';
import {type CodeListColumnMapping} from '../models/code-list';
import {
    formatInstantInApplicationTimeZone,
    formatRelativeEpochMillisInApplicationTimeZone,
    instantToEpochMillis,
} from '../../../utils/temporal-utils';

interface CodeListStatusChipProps {
    status: CodeListStatus;
    columnMappings: CodeListColumnMapping[];
    sourceType?: CodeListSourceType;
    statusMessage?: string | null;
    lastSync?: string | null;
}

const iconMap: Record<CodeListStatus, SvgIconComponent> = {
    [CodeListStatus.SyncPending]: Autoplay,
    [CodeListStatus.Syncing]: SyncArrowDown,
    [CodeListStatus.Synced]: CheckCircle,
    [CodeListStatus.SyncFailed]: SyncProblem,
};

export function CodeListStatusChip(props: CodeListStatusChipProps): ReactNode {
    const {
        status,
        columnMappings,
        sourceType,
        statusMessage,
        lastSync,
    } = props;

    const isSyncable = sourceType == null || isCodeListSyncable(sourceType);
    const hasWarning = isSyncable && status === CodeListStatus.Synced &&
        columnMappings.some(mapping => mapping.index == null && mapping.formerSourceColumn != null);
    const Icon = hasWarning ? Warning : iconMap[status];

    const lastSyncEpochMillis = useMemo(() => {
        if (!lastSync) return null;
        return instantToEpochMillis(lastSync);
    }, [lastSync]);

    const [minuteTick, setMinuteTick] = useState(0);

    useEffect(() => {
        if (status !== CodeListStatus.Synced) return;
        if (lastSyncEpochMillis == null) return;

        const id = window.setInterval(() => setMinuteTick((current) => current + 1), 60_000);
        return () => window.clearInterval(id);
    }, [status, lastSyncEpochMillis]);

    const label = useMemo(() => {
        if (!isSyncable && sourceType != null) {
            return CodeListSourceTypeLabels[sourceType];
        }
        if (hasWarning) return 'Synchronisiert mit Warnung';

        const base = CodeListStatusLabels[status];

        if (status !== CodeListStatus.Synced) return base;
        if (lastSyncEpochMillis == null) return base;

        const now = Date.now();
        const diffMs = now - lastSyncEpochMillis;
        const underOneMinute = diffMs >= 0 && diffMs < 60_000;

        if (underOneMinute) {
            return `${base} vor weniger als einer Minute`;
        }

        const relative = formatRelativeEpochMillisInApplicationTimeZone(lastSyncEpochMillis, now);

        return relative == null ? base : `${base} ${relative}`;
    }, [hasWarning, isSyncable, lastSyncEpochMillis, minuteTick, sourceType, status]);

    const formattedLastSync = useMemo(() => {
        return formatInstantInApplicationTimeZone(lastSync, 'dd.MM.yyyy – HH:mm:ss') ?? undefined;
    }, [lastSync]);

    const title = useMemo(() => {
        if (!isSyncable) {
            return 'Diese Codeliste wird manuell gepflegt und nicht synchronisiert.';
        }

        const lastSyncTitle = formattedLastSync == null
            ? undefined
            : status === CodeListStatus.Synced
                ? `Zuletzt synchronisiert: ${formattedLastSync} Uhr`
                : `Letzte erfolgreiche Synchronisierung: ${formattedLastSync} Uhr`;

        if (status === CodeListStatus.SyncFailed && statusMessage != null && statusMessage.length > 0) {
            return lastSyncTitle == null
                ? statusMessage
                : `${statusMessage}\n${lastSyncTitle}`;
        }

        return lastSyncTitle;
    }, [formattedLastSync, isSyncable, status, statusMessage]);

    return (
        <Chip
            label={label}
            title={title}
            color={hasWarning ? 'warning' : isSyncable ? CodeListStatusColors[status] : 'default'}
            size="small"
            icon={isSyncable ? <Icon fontSize="small" /> : undefined}
            mode="soft"
        />
    );
}
