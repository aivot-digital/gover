import React, {type ReactNode, useMemo, useState} from 'react';
import {Box, Chip, Tooltip, Typography} from '@mui/material';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {SelectFieldComponent} from '../../../../components/select-field/select-field-component';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import {AuditLogEntity} from '../../models/audit-log-entity';
import {AuditLogsApiService, AuditLogFilter} from '../../audit-logs-api-service';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectPermissions} from '../../../../slices/user-slice';
import {AUDIT_LOG_READ_PERMISSION} from '../../constants/audit-permissions';
import {useConfirm} from '../../../../providers/confirm-provider';
import MoreVert from '@aivot/mui-material-symbols-400-outlined/dist/more-vert/MoreVert';

const actorFilters = [
    {label: 'Alle', value: 'all'},
    {label: 'User', value: 'User'},
    {label: 'System', value: 'System'},
    {label: 'Process', value: 'Process'},
];

const triggerTypeOptions = [
    {label: 'Alle Trigger', value: 'all'},
    {label: 'Create', value: 'Create'},
    {label: 'Update', value: 'Update'},
    {label: 'Delete', value: 'Delete'},
    {label: 'Error', value: 'Error'},
    {label: 'PermissionDenied', value: 'PermissionDenied'},
    {label: 'Export', value: 'Export'},
    {label: 'Import', value: 'Import'},
    {label: 'Message', value: 'Message'},
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

function hasEntries(value: Record<string, unknown> | undefined): boolean {
    return value != null && Object.keys(value).length > 0;
}

function previewJson(value: Record<string, unknown> | undefined): string {
    if (!hasEntries(value)) {
        return '-';
    }

    const entries = Object.entries(value!);
    const preview = entries
        .slice(0, 2)
        .map(([key, entryValue]) => `${key}: ${String(entryValue)}`)
        .join(', ');

    return entries.length > 2 ? `${preview}…` : preview;
}

function prettyJson(value: Record<string, unknown> | undefined): string {
    if (value == null) {
        return '{}';
    }

    return JSON.stringify(value, null, 2);
}

function triggerColor(triggerType: string): 'default' | 'success' | 'warning' | 'error' | 'info' {
    switch (triggerType) {
        case 'Create':
            return 'success';
        case 'Update':
            return 'warning';
        case 'Delete':
        case 'Error':
            return 'error';
        case 'PermissionDenied':
            return 'warning';
        case 'Import':
        case 'Export':
            return 'info';
        default:
            return 'default';
    }
}

export function AuditLogsListPage(): ReactNode {
    const permissions = useAppSelector(selectPermissions);

    const confirm = useConfirm();

    const hasReadAccess = useMemo(() => {
        return permissions?.systemPermissions
            ?.some((entry) => entry.permissions.includes(AUDIT_LOG_READ_PERMISSION)) ?? false;
    }, [permissions]);

    const [triggerType, setTriggerType] = useState<string | undefined>(undefined);

    if (!hasReadAccess) {
        return (
            <PageWrapper
                title="Audit-Logs"
                background
                error="Zugriff verweigert. Ihnen fehlt die Berechtigung audit_log.read."
            />
        );
    }

    return (
        <PageWrapper title="Audit-Logs"
                     fullWidth
                     background>
            <GenericListPage<AuditLogEntity>
                defaultFilter="all"
                filters={actorFilters}
                header={{
                    icon: ModuleIcons.audit,
                    title: 'Audit-Logs',
                }}
                searchLabel="Modul"
                searchPlaceholder="z.B. Assets, Process, UserRole"
                preSearchElements={[
                    <SelectFieldComponent
                        label="Trigger"
                        value={triggerType}
                        onChange={setTriggerType}
                        options={triggerTypeOptions}
                        placeholder="Alle Trigger"
                        size="small"
                        sx={{minWidth: '11rem'}}
                    />,
                ]}
                fetch={(options) => {
                    const filter: Partial<AuditLogFilter> = {};

                    if (options.search != null && options.search.trim().length > 0) {
                        filter.module = options.search;
                    }

                    if (options.filter != null && options.filter !== 'all') {
                        filter.actorType = options.filter;
                    }

                    if (triggerType != null && triggerType !== 'all') {
                        filter.triggerType = triggerType;
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
                                <Tooltip title={formatDateTime(params.row.timestamp)}>
                                    <Box>
                                        <Typography variant="body2"
                                                    fontWeight={600}>{formatRelative(params.row.timestamp)}</Typography>
                                        <Typography variant="caption"
                                                    color="text.secondary">{formatDateTime(params.row.timestamp)}</Typography>
                                    </Box>
                                </Tooltip>
                            </CellContentWrapper>
                        ),
                    },
                    {
                        field: 'triggerType',
                        headerName: 'Trigger',
                        width: 180,
                        renderCell: (params) => (
                            <CellContentWrapper>
                                <Chip
                                    size="small"
                                    label={params.row.triggerType}
                                    color={triggerColor(params.row.triggerType)}
                                    variant="outlined"
                                />
                            </CellContentWrapper>
                        ),
                    },
                    {
                        field: 'actorType',
                        headerName: 'Akteur',
                        width: 220,
                        renderCell: (params) => (
                            <CellContentWrapper>
                                <Box sx={{display: 'flex', flexDirection: 'column', gap: 0.5}}>
                                    <Chip label={params.row.actorType}
                                          size="small"
                                          variant="outlined"
                                          sx={{width: 'fit-content'}}/>
                                    <Tooltip title={params.row.actorId ?? '-'}>
                                        <Typography variant="caption"
                                                    noWrap>
                                            {trimValue(params.row.actorId, 28)}
                                        </Typography>
                                    </Tooltip>
                                </Box>
                            </CellContentWrapper>
                        ),
                    },
                    {
                        field: 'reference',
                        headerName: 'Referenz',
                        width: 250,
                        sortable: false,
                        renderCell: (params) => (
                            <CellContentWrapper>
                                <Box sx={{display: 'flex', flexDirection: 'column', gap: 0.4}}>
                                    <Typography variant="body2">{trimValue(params.row.triggerRef, 32)}</Typography>
                                    <Typography variant="caption"
                                                color="text.secondary">
                                        {params.row.triggerRefType ?? '-'}
                                    </Typography>
                                </Box>
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
                ]}
                getRowIdentifier={(row) => row.id.toString()}
                noDataPlaceholder="Keine Audit-Logs vorhanden"
                noSearchResultsPlaceholder="Keine Audit-Logs für diese Filter gefunden"
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
                                    <Box sx={{display: 'flex', flexDirection: 'column', gap: 2}}>
                                        <Box>
                                            <Typography variant="subtitle2">Basisdaten</Typography>
                                            <Typography variant="body2">Zeitpunkt: {formatDateTime(row.timestamp)}</Typography>
                                            <Typography variant="body2">Trigger: {row.triggerType}</Typography>
                                            <Typography variant="body2">Akteur: {row.actorType} {row.actorId != null ? `(${row.actorId})` : ''}</Typography>
                                            <Typography variant="body2">Referenz: {row.triggerRef ?? '-'}</Typography>
                                            <Typography variant="body2">Referenz-Typ: {row.triggerRefType ?? '-'}</Typography>
                                            <Typography variant="body2">Modul: {row.module}</Typography>
                                            <Typography variant="body2">IP-Adresse: {row.ipAddress ?? '-'}</Typography>
                                        </Box>

                                        <Box>
                                            <Typography variant="subtitle2">Diff</Typography>
                                            <Box
                                                component="pre"
                                                sx={{
                                                    m: 0,
                                                    p: 1.25,
                                                    borderRadius: 1,
                                                    bgcolor: 'background.default',
                                                    border: '1px solid',
                                                    borderColor: 'divider',
                                                    overflowX: 'auto',
                                                    fontSize: '0.75rem',
                                                    lineHeight: 1.5,
                                                }}
                                            >
                                                {prettyJson(row.diff)}
                                            </Box>
                                        </Box>

                                        <Box>
                                            <Typography variant="subtitle2">Metadata</Typography>
                                            <Box
                                                component="pre"
                                                sx={{
                                                    m: 0,
                                                    p: 1.25,
                                                    borderRadius: 1,
                                                    bgcolor: 'background.default',
                                                    border: '1px solid',
                                                    borderColor: 'divider',
                                                    overflowX: 'auto',
                                                    fontSize: '0.75rem',
                                                    lineHeight: 1.5,
                                                }}
                                            >
                                                {prettyJson(row.metadata)}
                                            </Box>
                                        </Box>
                                    </Box>
                                ),
                            });
                        },
                    },
                ]}
            />
        </PageWrapper>
    );
}
