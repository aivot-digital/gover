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

const actorFilters = [
    {label: 'Alle', value: 'all'},
    {label: 'Benutzer', value: 'USER'},
    {label: 'System', value: 'SYSTEM'},
    {label: 'Job', value: 'JOB'},
    {label: 'Integration', value: 'INTEGRATION'},
];

const actionTypeOptions = [
    {label: 'Alle Aktionen', value: 'all'},
    {label: 'Create', value: 'Create'},
    {label: 'List', value: 'List'},
    {label: 'Retrieve', value: 'Retrieve'},
    {label: 'Update', value: 'Update'},
    {label: 'Delete', value: 'Delete'},
    {label: 'Message', value: 'Message'},
];

const resultOptions = [
    {label: 'Alle Ergebnisse', value: 'all'},
    {label: 'Erfolg', value: 'success'},
    {label: 'Fehlgeschlagen', value: 'failed'},
    {label: 'Abgelehnt', value: 'denied'},
];

const changedDataOptions = [
    {label: 'Alle', value: 'all'},
    {label: 'Mit Datenänderung', value: 'true'},
    {label: 'Ohne Datenänderung', value: 'false'},
];

const severityOptions = [
    {label: 'Alle Schweregrade', value: 'all'},
    {label: 'Info', value: 'info'},
    {label: 'Warnung', value: 'warn'},
    {label: 'Fehler', value: 'error'},
    {label: 'Hoch', value: 'high'},
];

function normalizeDate(dateString: string): Date | undefined {
    if (dateString.trim().length === 0) {
        return undefined;
    }

    const normalized = dateString.replace(/(\.\d{3})\d+/, '$1');
    const date = new Date(normalized);

    if (Number.isNaN(date.getTime())) {
        return undefined;
    }

    return date;
}

function formatDateTime(dateString: string): string {
    const date = normalizeDate(dateString);
    if (date == null) {
        return '-';
    }

    return `${new Intl.DateTimeFormat('de-DE', {
        dateStyle: 'medium',
        timeStyle: 'medium',
    }).format(date)} Uhr`;
}

function formatRelative(dateString: string): string {
    const date = normalizeDate(dateString);
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

function shortenClassName(value?: string): string {
    if (value == null || value.trim().length === 0) {
        return '-';
    }

    const parts = value.split('.');
    return parts[parts.length - 1] ?? value;
}

function trimValue(value?: string, maxLength: number = 24): string {
    if (value == null || value.length <= maxLength) {
        return value ?? '-';
    }

    return `${value.slice(0, maxLength - 1)}…`;
}

function actionChipColor(actionType?: string): 'default' | 'primary' | 'success' | 'warning' | 'error' {
    switch (actionType) {
        case 'Create':
            return 'success';
        case 'Update':
            return 'warning';
        case 'Delete':
            return 'error';
        case 'List':
        case 'Retrieve':
            return 'primary';
        default:
            return 'default';
    }
}

function resultChipColor(actionResult?: string): 'default' | 'success' | 'warning' | 'error' {
    switch ((actionResult ?? '').toLowerCase()) {
        case 'success':
            return 'success';
        case 'denied':
            return 'warning';
        case 'failed':
            return 'error';
        default:
            return 'default';
    }
}

function actorLabel(row: AuditLogEntity): string {
    return row.actorLabel ?? row.actorId ?? row.triggeringUserId ?? '-';
}

export function AuditLogsListPage(): ReactNode {
    const permissions = useAppSelector(selectPermissions);

    const hasReadAccess = useMemo(() => {
        return permissions?.systemPermissions
            ?.some((entry) => entry.permissions.includes(AUDIT_LOG_READ_PERMISSION)) ?? false;
    }, [permissions]);

    const [actionType, setActionType] = useState<string | undefined>(undefined);
    const [actionResult, setActionResult] = useState<string | undefined>(undefined);
    const [changedData, setChangedData] = useState<string | undefined>(undefined);
    const [severity, setSeverity] = useState<string | undefined>(undefined);

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
        <PageWrapper
            title="Audit-Logs"
            fullWidth
            background
        >
            <GenericListPage<AuditLogEntity>
                defaultFilter="all"
                filters={actorFilters}
                header={{
                    icon: ModuleIcons.audit,
                    title: 'Audit-Logs',
                    helpDialog: {
                        title: 'Hilfe zu Audit-Logs',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography variant="body1" paragraph>
                                    Die Liste zeigt, wer was wann ausgelöst hat. Die wichtigsten Spalten sind Ereignis, Auslöser und Ergebnis.
                                </Typography>
                                <Typography variant="body1" paragraph>
                                    Für technische Analyse können Sie per Tooltip die vollständigen Klassenpfade und Nachrichten einsehen.
                                </Typography>
                            </>
                        ),
                    },
                }}
                searchLabel="Komponente"
                searchPlaceholder="z.B. ProcessController"
                preSearchElements={[
                    <SelectFieldComponent
                        label="Aktion"
                        value={actionType}
                        onChange={setActionType}
                        options={actionTypeOptions}
                        placeholder="Alle Aktionen"
                        size="small"
                        sx={{minWidth: '10rem'}}
                    />,
                    <SelectFieldComponent
                        label="Ergebnis"
                        value={actionResult}
                        onChange={setActionResult}
                        options={resultOptions}
                        placeholder="Alle Ergebnisse"
                        size="small"
                        sx={{minWidth: '10rem'}}
                    />,
                    <SelectFieldComponent
                        label="Datenänderung"
                        value={changedData}
                        onChange={setChangedData}
                        options={changedDataOptions}
                        placeholder="Alle"
                        size="small"
                        sx={{minWidth: '10rem'}}
                    />,
                    <SelectFieldComponent
                        label="Schweregrad"
                        value={severity}
                        onChange={setSeverity}
                        options={severityOptions}
                        placeholder="Alle"
                        size="small"
                        sx={{minWidth: '9rem'}}
                    />,
                ]}
                fetch={(options) => {
                    const filter: Partial<AuditLogFilter> = {};

                    if (options.search != null && options.search.trim().length > 0) {
                        filter.component = options.search;
                    }

                    if (options.filter != null && options.filter !== 'all') {
                        filter.actorType = options.filter;
                    }

                    if (actionType != null && actionType !== 'all') {
                        filter.actionType = actionType;
                    }

                    if (actionResult != null && actionResult !== 'all') {
                        filter.actionResult = actionResult;
                    }

                    if (changedData != null && changedData !== 'all') {
                        filter.changedData = changedData === 'true';
                    }

                    if (severity != null && severity !== 'all') {
                        filter.severity = severity;
                    }

                    const sortField = (options.sort as string | undefined) ?? 'eventTs';
                    const sortOrder = options.order ?? 'DESC';

                    return new AuditLogsApiService()
                        .list(
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
                        field: 'eventTs',
                        headerName: 'Zeitpunkt',
                        width: 200,
                        renderCell: (params) => (
                            <CellContentWrapper>
                                <Tooltip title={formatDateTime(params.row.eventTs)}>
                                    <Box>
                                        <Typography variant="body2" fontWeight={600}>{formatRelative(params.row.eventTs)}</Typography>
                                        <Typography variant="caption" color="text.secondary">{formatDateTime(params.row.eventTs)}</Typography>
                                    </Box>
                                </Tooltip>
                            </CellContentWrapper>
                        ),
                    },
                    {
                        field: 'event',
                        headerName: 'Ereignis',
                        flex: 1,
                        minWidth: 300,
                        sortable: false,
                        renderCell: (params) => (
                            <CellContentWrapper>
                                <Box sx={{display: 'flex', flexDirection: 'column', gap: 0.4, width: '100%'}}>
                                    <Box sx={{display: 'flex', alignItems: 'center', gap: 0.75}}>
                                        <Chip
                                            size="small"
                                            label={params.row.actionType}
                                            color={actionChipColor(params.row.actionType)}
                                            variant="outlined"
                                        />
                                        <Typography variant="body2" fontWeight={600}>
                                            {shortenClassName(params.row.entityType)}
                                        </Typography>
                                        {
                                            params.row.entityId != null &&
                                            <Typography variant="caption" color="text.secondary">
                                                #{trimValue(params.row.entityId, 14)}
                                            </Typography>
                                        }
                                    </Box>
                                    <Tooltip title={params.row.message}>
                                        <Typography variant="caption" color="text.secondary" noWrap>
                                            {params.row.message}
                                        </Typography>
                                    </Tooltip>
                                </Box>
                            </CellContentWrapper>
                        ),
                    },
                    {
                        field: 'actorType',
                        headerName: 'Auslöser',
                        width: 240,
                        renderCell: (params) => (
                            <CellContentWrapper>
                                <Box sx={{display: 'flex', flexDirection: 'column', gap: 0.5}}>
                                    <Chip label={params.row.actorType} size="small" variant="outlined" sx={{width: 'fit-content'}}/>
                                    <Tooltip title={actorLabel(params.row)}>
                                        <Typography variant="caption" noWrap>
                                            {trimValue(actorLabel(params.row), 30)}
                                        </Typography>
                                    </Tooltip>
                                </Box>
                            </CellContentWrapper>
                        ),
                    },
                    {
                        field: 'component',
                        headerName: 'Komponente',
                        width: 220,
                        renderCell: (params) => (
                            <CellContentWrapper>
                                <Tooltip title={String(params.value)}>
                                    <Typography variant="body2" noWrap>
                                        {shortenClassName(String(params.value))}
                                    </Typography>
                                </Tooltip>
                            </CellContentWrapper>
                        ),
                    },
                    {
                        field: 'actionResult',
                        headerName: 'Ergebnis',
                        width: 130,
                        renderCell: (params) => (
                            <CellContentWrapper>
                                <Chip
                                    size="small"
                                    label={String(params.value)}
                                    color={resultChipColor(String(params.value))}
                                    variant="outlined"
                                />
                            </CellContentWrapper>
                        ),
                    },
                    {
                        field: 'changedData',
                        headerName: 'Änderung',
                        width: 120,
                        renderCell: (params) => (
                            <CellContentWrapper>
                                <Chip
                                    size="small"
                                    label={params.row.changedData ? 'Ja' : 'Nein'}
                                    color={params.row.changedData ? 'warning' : 'default'}
                                    variant={params.row.changedData ? 'filled' : 'outlined'}
                                />
                            </CellContentWrapper>
                        ),
                    },
                ]}
                getRowIdentifier={(row) => row.id.toString()}
                noDataPlaceholder="Keine Audit-Logs vorhanden"
                noSearchResultsPlaceholder="Keine Audit-Logs für diese Filter gefunden"
                dynamicRowHeight={true}
            />
        </PageWrapper>
    );
}
