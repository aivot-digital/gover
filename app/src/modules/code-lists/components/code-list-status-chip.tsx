import React, {ReactNode, useMemo} from 'react';
import {Chip} from '../../../components/chip/chip';
import {CodeListStatus, CodeListStatusColors, CodeListStatusLabels} from '../enums/code-list-status';
import SyncProblem from '@aivot/mui-material-symbols-400-n25-outlined/SyncProblem';
import SyncArrowDown from '@aivot/mui-material-symbols-400-n25-outlined/SyncArrowDown';
import Autoplay from '@aivot/mui-material-symbols-400-n25-outlined/Autoplay';
import CheckCircle from '@aivot/mui-material-symbols-400-n25-outlined/CheckCircle';
import {format} from 'date-fns';
import {de} from 'date-fns/locale';
import {SvgIconComponent} from '../../../types/svg-icon-component';

interface CodeListStatusChipProps {
    status: CodeListStatus;
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
        statusMessage,
        lastSync,
    } = props;

    const Icon = useMemo(() => iconMap[status], [status]);

    const title = useMemo(() => {
        if (status === CodeListStatus.SyncFailed && statusMessage != null && statusMessage.length > 0) {
            return statusMessage;
        }

        if (lastSync == null || lastSync.length === 0) {
            return undefined;
        }

        return `Zuletzt synchronisiert: ${format(new Date(lastSync), 'dd.MM.yyyy - HH:mm:ss', {locale: de})} Uhr`;
    }, [lastSync, status, statusMessage]);

    return (
        <Chip
            label={CodeListStatusLabels[status]}
            title={title}
            color={CodeListStatusColors[status]}
            size="small"
            icon={<Icon fontSize="small" />}
            mode="soft"
        />
    );
}
