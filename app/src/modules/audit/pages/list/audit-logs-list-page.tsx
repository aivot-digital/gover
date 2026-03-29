import React, {type ReactNode, useEffect, useMemo, useState} from 'react';
import {Box, Chip, Tooltip, Typography} from '@mui/material';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import {AuditLogEntity} from '../../models/audit-log-entity';
import {AuditLogFilter, AuditLogFilterOptions, AuditLogsApiService} from '../../audit-logs-api-service';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectPermissions} from '../../../../slices/user-slice';
import {AUDIT_LOG_READ_PERMISSION} from '../../constants/audit-permissions';
import {useConfirm} from '../../../../providers/confirm-provider';
import MoreVert from '@aivot/mui-material-symbols-400-outlined/dist/more-vert/MoreVert';
import {getTriggerTypeColor, getTriggerTypeIcon, getTriggerTypeLabel} from '../../data/trigger-type';
import {getActorTypeColor, getActorTypeIcon, getActorTypeLabel} from '../../data/actor-type';
import {AuditLogDetailsDialogContent} from './audit-log-details-dialog-content';
import {ChipInputFieldComponent} from '../../../../components/chip-input-field/chip-input-field-component';


const actorFilters = [
    {label: 'Alle', value: 'all'},
    {label: 'User', value: 'User'},
    {label: 'System', value: 'System'},
    {label: 'Process', value: 'Process'},
];

function parseDate(value: string): Date | undefined {
    if (value.trim().length === 0) {
        return undefined;
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return undefined;
    }

    return date;
}

function formatDateTime(value: string): string {
    const date = parseDate(value);
    if (date == null) {
        return '-';
    }

    return `${new Intl.DateTimeFormat('de-DE', {
        dateStyle: 'medium',
        timeStyle: 'medium',
    }).format(date)} Uhr`;
}

function formatRelative(value: string): string {
    const date = parseDate(value);
    if (date == null) {
        return '-';
    }

    const diffMs = Date.now() - date.getTime();
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
    const permissions = useAppSelector(selectPermissions);

    const confirm = useConfirm();

    const hasReadAccess = useMemo(() => {
        return permissions?.systemPermissions
            ?.some((entry) => entry.permissions.includes(AUDIT_LOG_READ_PERMISSION)) ?? false;
    }, [permissions]);

    const [filterOptions, setFilterOptions] = useState<AuditLogFilterOptions>({
        modules: [],
        triggerTypes: [],
        actors: [],
    });
    const [selectedModules, setSelectedModules] = useState<string[] | undefined>(undefined);
    const [selectedTriggerTypes, setSelectedTriggerTypes] = useState<string[] | undefined>(undefined);
    const [selectedActors, setSelectedActors] = useState<string[] | undefined>(undefined);

    useEffect(() => {
        if (!hasReadAccess) {
            return;
        }

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
    }, [hasReadAccess]);

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

    const handleModuleChange = (value: string[] | null | undefined) => {
        setSelectedModules(value ?? undefined);
    };

    const handleTriggerTypeChange = (value: string[] | null | undefined) => {
        setSelectedTriggerTypes(value ?? undefined);
    };

    const handleActorChange = (value: string[] | null | undefined) => {
        setSelectedActors(value ?? undefined);
    };

    if (!hasReadAccess) {
        return (
            <PageWrapper
                title="Audit-Log"
                background
                error="Zugriff verweigert. Ihnen fehlt die Berechtigung audit_log.read."
            />
        );
    }

    return (
        <PageWrapper title="Audit-Log"
                     fullWidth
                     background>
            <GenericListPage<AuditLogEntity>
                defaultFilter="all"
                filters={actorFilters}
                header={{
                    icon: ModuleIcons.audit,
                    title: 'Audit-Log',
                }}
                preSearchElements={[
                    <ChipInputFieldComponent
                        label="Modul"
                        value={selectedModules}
                        onChange={handleModuleChange}
                        size="small"
                        placeholder="z.B. Prozess"
                        suggestions={filterOptions.modules}
                    />,
                    <ChipInputFieldComponent
                        label="Auslösende Aktion"
                        value={selectedTriggerTypes}
                        onChange={handleTriggerTypeChange}
                        size="small"
                        placeholder="z.B. Update"
                        suggestions={filterOptions.triggerTypes}
                    />,
                    <ChipInputFieldComponent
                        label="Akteur"
                        value={selectedActors}
                        onChange={handleActorChange}
                        size="small"
                        placeholder="Akteur-ID oder Name"
                        suggestions={filterOptions.actors.map((entry) => entry.label)}
                    />,
                ]}
                fetch={(options) => {
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

                    const sortField = (options.sort as string | undefined) ?? 'timestamp';
                    const sortOrder = options.order ?? 'DESC';

                    return new AuditLogsApiService().list(
                        options.page,
                        options.size,
                        sortField,
                        sortOrder,
                        filter,
                    );
                }}
                columnIcon={ModuleIcons.audit}
                columnDefinitions={[
                    {
                        field: 'timestamp',
                        headerName: 'Zeitpunkt',
                        width: 200,
                        renderCell: (params) => (
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
                        renderCell: (params) => (
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
                        renderCell: (params) => {
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
                        renderCell: (params) => {
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
                        renderCell: (params) => (
                            <CellContentWrapper>
                                <Typography variant="body2"
                                            noWrap>
                                    {String(params.value)}
                                </Typography>
                            </CellContentWrapper>
                        ),
                    },
                ]}
                getRowIdentifier={(row) => row.id.toString()}
                noDataPlaceholder="Kein Audit-Log vorhanden"
                noSearchResultsPlaceholder="Kein Audit-Log für diese Filter gefunden"
                dynamicRowHeight={true}
                rowActionsCount={1}
                rowActions={(row) => [
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
                ]}
            />
        </PageWrapper>
    );
}
