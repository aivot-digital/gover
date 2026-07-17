import React, {ReactNode, useEffect, useMemo, useState} from 'react';
import {Chip} from '../../../components/chip/chip';
import {CodeListStatus, CodeListStatusColors, CodeListStatusLabels} from '../enums/code-list-status';
import SyncProblem from '@aivot/mui-material-symbols-400-n25-outlined/SyncProblem';
import SyncArrowDown from '@aivot/mui-material-symbols-400-n25-outlined/SyncArrowDown';
import Autoplay from '@aivot/mui-material-symbols-400-n25-outlined/Autoplay';
import CheckCircle from '@aivot/mui-material-symbols-400-n25-outlined/CheckCircle';
import {format, formatDistanceToNowStrict} from 'date-fns';
import {de} from 'date-fns/locale';
import {SvgIconComponent} from '../../../types/svg-icon-component';
import {CodeListSourceType, CodeListSourceTypeLabels, isCodeListSyncable} from '../enums/code-list-source-type';

interface CodeListStatusChipProps {
    status: CodeListStatus;
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

function parseIsoLocal(value: string): Date | null {
    // JS Date supports milliseconds only; trim potential microseconds (e.g. .718476 -> .718).
    const normalized = value.replace(/(\.\d{3})\d+/, '$1');
    const date = new Date(normalized);
    return Number.isNaN(date.getTime()) ? null : date;
}

export function CodeListStatusChip(props: CodeListStatusChipProps): ReactNode {
    const {
        status,
        sourceType,
        statusMessage,
        lastSync,
    } = props;

    const Icon = useMemo(() => iconMap[status], [status]);
    const isSyncable = sourceType == null || isCodeListSyncable(sourceType);

    const lastSyncDate = useMemo(() => {
        if (!lastSync) return null;
        return parseIsoLocal(lastSync);
    }, [lastSync]);

    const [minuteTick, setMinuteTick] = useState(0);

    useEffect(() => {
        if (status !== CodeListStatus.Synced) return;
        if (!lastSyncDate) return;

        const id = window.setInterval(() => setMinuteTick((current) => current + 1), 60_000);
        return () => window.clearInterval(id);
    }, [status, lastSyncDate]);

    const label = useMemo(() => {
        if (!isSyncable && sourceType != null) {
            return CodeListSourceTypeLabels[sourceType];
        }

        const base = CodeListStatusLabels[status];

        if (status !== CodeListStatus.Synced) return base;
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
    }, [isSyncable, lastSyncDate, minuteTick, sourceType, status]);

    const formattedLastSync = useMemo(() => {
        if (!lastSyncDate) return undefined;
        return format(lastSyncDate, 'dd.MM.yyyy – HH:mm:ss', {locale: de});
    }, [lastSyncDate]);

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
            color={isSyncable ? CodeListStatusColors[status] : 'default'}
            size="small"
            icon={isSyncable ? <Icon fontSize="small" /> : undefined}
            mode="soft"
        />
    );
}
