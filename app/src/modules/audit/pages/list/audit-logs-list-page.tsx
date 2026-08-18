import React, {type ReactNode, useCallback, useEffect, useMemo, useState} from 'react';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {Box, Chip, Tooltip, Typography} from '@mui/material';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {
    GenericListPage,
    type GenericListPagePermissionConfig,
} from '../../../../components/generic-list-page/generic-list-page';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import {AuditLogEntity} from '../../models/audit-log-entity';
import {AuditLogFilter, AuditLogFilterOptions, AuditLogsApiService} from '../../audit-logs-api-service';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {AUDIT_LOG_READ_PERMISSION} from '../../constants/audit-permissions';
import {useConfirm} from '../../../../providers/confirm-provider';
import MoreVert from '@aivot/mui-material-symbols-400-n25-outlined/MoreVert';
import {getTriggerTypeColor, getTriggerTypeIcon, getTriggerTypeLabel} from '../../data/trigger-type';
import {getActorTypeColor, getActorTypeIcon, getActorTypeLabel} from '../../data/actor-type';
import {AuditLogDetailsDialogContent} from './audit-log-details-dialog-content';
import {ChipInputFieldComponent} from '../../../../components/chip-input-field/chip-input-field-component';
import {
    formatInstantInApplicationTimeZone,
    instantToEpochMillis,
} from '../../../../utils/temporal-utils';

const actorFilters = [
    {label: 'Alle', value: 'all'},
    {label: 'User', value: 'User'},
    {label: 'System', value: 'System'},
    {label: 'Process', value: 'Process'},
];

const auditLogsListPermissionCheck: GenericListPagePermissionConfig<AuditLogEntity> = {
    scope: {
        type: 'system',
    },
    read: AUDIT_LOG_READ_PERMISSION,
};

function formatDateTime(value: string): string {
    const formatted = formatInstantInApplicationTimeZone(value, 'dd.MM.yyyy, HH:mm:ss');
    return formatted != null ? `${formatted} Uhr` : '-';
}

function formatRelative(value: string): string {
    const epochMillis = instantToEpochMillis(value);
    if (epochMillis == null) {
        return '-';
    }

    const diffMs = Date.now() - epochMillis;
    const diffMin = Math.floor(diffMs / 60000);

    if (diffMin < 1) {
        return 'gerade eben';
    }
    if (diffMin < 60) {
        return `vor ${diffMin} Min.`;
    }

    const diffHours = Math.floor(diffMin / 60);
    if (diffHours < 24) {
        return `vor ${diffHours} Std.`;
    }

    const diffDays = Math.floor(diffHours / 24);
    return `vor ${diffDays} Tag${diffDays === 1 ? '' : 'en'}`;
}

function trimValue(value: string | undefined, maxLength: number = 28): string {
    if (value == null || value.length <= maxLength) {
        return value ?? '-';
    }

    return `${value.slice(0, maxLength - 1)}…`;
}

export function AuditLogsListPage(): ReactNode {
    const confirm = useConfirm();

    const [filterOptions, setFilterOptions] = useState<AuditLogFilterOptions>({
        modules: [],
        triggerTypes: [],
        actors: [],
    });
    const [selectedModules, setSelectedModules] = useState<string[] | undefined>(undefined);
    const [selectedTriggerTypes, setSelectedTriggerTypes] = useState<string[] | undefined>(undefined);
    const [selectedActors, setSelectedActors] = useState<string[] | undefined>(undefined);

    useEffect(() => {
        let cancelled = false;
        new AuditLogsApiService()
            .getFilterOptions()
            .then((result) => {
                if (!cancelled) {
                    setFilterOptions(result);
                }
            })
            .catch((error) => {
                console.error(error);
            });
        return () => {
            cancelled = true;
        };
    }, []);

    const actorLabelToValue = useMemo(() => {
        return Object.fromEntries(
            filterOptions.actors.map((entry) => [entry.label, entry.value]),
        );
    }, [filterOptions.actors]);

    const actorValueToLabel = useMemo(() => {
        return Object.fromEntries(
            filterOptions.actors.map((entry) => [entry.value, entry.label]),
        );
    }, [filterOptions.actors]);

    const handleModuleChange = useCallback((value: string[] | null | undefined) => {
        setSelectedModules(value ?? undefined);
    }, []);

    const handleTriggerTypeChange = useCallback((value: string[] | null | undefined) => {
        setSelectedTriggerTypes(value ?? undefined);
    }, []);

    const handleActorChange = useCallback((value: string[] | null | undefined) => {
        setSelectedActors(value ?? undefined);
    }, []);

    const header = useMemo(() => ({
        icon: ModuleIcons.audit,
        title: 'Audit-Log',
    }), []);

    const preSearchElements = useMemo(() => [
        <ChipInputFieldComponent
            key="modules"
            label="Modul"
            value={selectedModules}
            onChange={handleModuleChange}
            size="small"
            placeholder="z.B. Prozess"
            suggestions={filterOptions.modules}
        />,
        <ChipInputFieldComponent
            key="triggerTypes"
            label="Auslösende Aktion"
            value={selectedTriggerTypes}
            onChange={handleTriggerTypeChange}
            size="small"
            placeholder="z.B. Update"
            suggestions={filterOptions.triggerTypes}
        />,
        <ChipInputFieldComponent
            key="actors"
            label="Akteur"
            value={selectedActors}
            onChange={handleActorChange}
            size="small"
            placeholder="Akteur-ID oder Name"
            suggestions={filterOptions.actors.map((entry) => entry.label)}
        />,
    ], [
        filterOptions.actors,
        filterOptions.modules,
        filterOptions.triggerTypes,
        handleActorChange,
        handleModuleChange,
        handleTriggerTypeChange,
        selectedActors,
        selectedModules,
        selectedTriggerTypes,
    ]);

    const fetchAuditLogs = useCallback((options: {
        page: number;
        size: number;
        sort?: string;
        order?: 'ASC' | 'DESC';
        filter?: string;
    }) => {
        const filter: Partial<AuditLogFilter> = {};

        if (selectedModules != null && selectedModules.length > 0) {
            filter.modules = selectedModules;
        }

        if (options.filter != null && options.filter !== 'all') {
            filter.actorType = options.filter;
        }

        if (selectedTriggerTypes != null && selectedTriggerTypes.length > 0) {
            filter.triggerTypes = selectedTriggerTypes;
        }

        if (selectedActors != null && selectedActors.length > 0) {
            filter.actors = selectedActors
                .map((entry) => entry.trim())
                .filter((entry) => entry.length > 0)
                .map((entry) => actorLabelToValue[entry] ?? entry);
        }

        const sortField = options.sort ?? 'timestamp';
        const sortOrder = options.order ?? 'DESC';

        return new AuditLogsApiService().list(
            options.page,
            options.size,
            sortField,
            sortOrder,
            filter,
        );
    }, [actorLabelToValue, selectedActors, selectedModules, selectedTriggerTypes]);

    const columnDefinitions = useMemo(() => [
        {
            field: 'timestamp',
            headerName: 'Zeitpunkt',
            width: 200,
            renderCell: (params: any) => (
                <CellContentWrapper>
                    <Tooltip title={formatDateTime(params.row.timestamp)} arrow>
                        <Box>
                            <Typography variant="body2"
                                        fontWeight={500}>
                                {formatRelative(params.row.timestamp)}
                            </Typography>
                            <Typography variant="caption"
                                        color="text.secondary">
                                {formatDateTime(params.row.timestamp)}
                            </Typography>
                        </Box>
                    </Tooltip>
                </CellContentWrapper>
            ),
        },
        {
            field: 'module',
            headerName: 'Modul',
            width: 190,
            renderCell: (params: any) => (
                <CellContentWrapper>
                    <Typography variant="body2"
                                noWrap>
                        {String(params.value)}
                    </Typography>
                </CellContentWrapper>
            ),
        },
        {
            field: 'triggerType',
            headerName: 'Auslösende Aktion',
            width: 180,
            renderCell: (params: any) => {
                const Icon = getTriggerTypeIcon(params.row.triggerType);
                return (
                    <CellContentWrapper>
                        <Chip
                            icon={Icon != null ? <Icon/> : undefined}
                            size="small"
                            label={getTriggerTypeLabel(params.row.triggerType)}
                            color={getTriggerTypeColor(params.row.triggerType)}
                            variant="outlined"
                        />
                    </CellContentWrapper>
                );
            },
        },
        {
            field: 'actorType',
            headerName: 'Auslösender Akteur',
            width: 220,
            renderCell: (params: any) => {
                const actorType = params.row.actorType;
                const actorId = params.row.actorId?.trim() || undefined;
                const isUser = actorType === 'User';
                const actorLabel = isUser
                    ? ((actorId != null ? actorValueToLabel[actorId] : undefined) || actorId || getActorTypeLabel(actorType))
                    : getActorTypeLabel(actorType);
                const Icon = getActorTypeIcon(actorType);

                return (
                    <CellContentWrapper>
                        <Tooltip title={isUser ? (actorId ?? '-') : actorLabel} arrow>
                            <Chip
                                size="small"
                                variant="outlined"
                                icon={Icon != null ? <Icon/> : undefined}
                                label={trimValue(actorLabel, 32)}
                                color={getActorTypeColor(actorType)}
                            />
                        </Tooltip>
                    </CellContentWrapper>
                );
            },
        },
        {
            field: 'message',
            headerName: 'Nachricht',
            flex: 1,
            renderCell: (params: any) => (
                <CellContentWrapper>
                    <Typography variant="body2"
                                noWrap>
                        {String(params.value)}
                    </Typography>
                </CellContentWrapper>
            ),
        },
    ], [actorValueToLabel]);

    const getRowIdentifier = useCallback((row: AuditLogEntity) => row.id.toString(), []);

    const rowActions = useCallback((row: AuditLogEntity) => [
        {
            icon: <MoreVert/>,
            tooltip: 'Mehr Informationen',
            onClick: () => {
                confirm({
                    title: 'Weitere Informationen',
                    hideCancelButton: true,
                    confirmButtonText: 'Schließen',
                    children: (
                        <AuditLogDetailsDialogContent
                            row={row}
                            actorLabelsById={actorValueToLabel}
                        />
                    ),
                });
            },
        },
    ], [actorValueToLabel, confirm]);

    return (
        <PageWrapper title="Audit-Log"
                     fullWidth
                     background>
            <GenericListPage<AuditLogEntity>
                defaultFilter="all"
                filters={actorFilters}
                header={header}
                permissionCheck={auditLogsListPermissionCheck}
                preSearchElements={preSearchElements}
                fetch={fetchAuditLogs}
                columnIcon={ModuleIcons.audit}
                columnDefinitions={columnDefinitions}
                getRowIdentifier={getRowIdentifier}
                noDataPlaceholder={
                    <EmptyDataListPlaceholder
                        title="Noch keine Audit-Log-Einträge vorhanden"
                        description="Audit-Log-Einträge protokollieren sicherheits- und verwaltungsrelevante Aktionen im System."
                    />
                }
                noSearchResultsPlaceholder="Keine Audit-Log-Einträge gefunden"
                dynamicRowHeight={true}
                rowActionsCount={1}
                rowActions={rowActions}
            />
        </PageWrapper>
    );
}
