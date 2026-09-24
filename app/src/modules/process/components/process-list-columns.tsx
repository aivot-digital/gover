import {Box, Tooltip} from '@mui/material';
import {GridColDef} from '@mui/x-data-grid';
import {CellLink} from '../../../components/cell-link/cell-link';
import {Chip} from '../../../components/chip/chip';
import {ProcessInstanceListEntry, ProcessTaskListEntry} from '../entities/process-list';
import {
    ProcessInstanceStatus,
    ProcessInstanceStatusColor,
    ProcessInstanceStatusLabels,
} from '../enums/process-instance-status';
import {ProcessTaskStatus, ProcessTaskStatusLabels, ProcessTaskStatusColors} from '../enums/process-task-status';
import {
    formatInstantInApplicationTimeZone,
    formatRelativeInstantInApplicationTimeZone,
} from '../../../utils/temporal-utils';
import {ProcessEmptyValue, ProcessNodeLabel} from './process-detail-values';
import {ProcessListActions} from './process-list-actions';

type Entry = ProcessInstanceListEntry | ProcessTaskListEntry;

function ListDate({value, overdue = false}: {value: string | null; overdue?: boolean}) {
    if (!value) return <ProcessEmptyValue>—</ProcessEmptyValue>;
    return (
        <Tooltip
            arrow
            title={formatRelativeInstantInApplicationTimeZone(value) ?? ''}
        >
            <Box
                component="span"
                sx={{color: overdue ? 'error.main' : undefined}}
            >
                {formatInstantInApplicationTimeZone(value, 'dd.MM.yyyy – HH:mm')}
            </Box>
        </Tooltip>
    );
}

export function processListColumns<T extends Entry>(tasks: boolean, onChanged: () => void): GridColDef<T>[] {
    const columns: GridColDef<T>[] = [
        {
            field: 'caseNumber',
            headerName: 'Vorgangskennung',
            minWidth: 155,
            flex: 1,
            renderCell: ({row}) => (
                <CellLink
                    to={
                        tasks
                            ? `/tasks/${(row as ProcessTaskListEntry).processInstanceId}/${row.id}`
                            : `/process-instances/${row.id}`
                    }
                >
                    {row.caseNumber}
                </CellLink>
            ),
        },
        {
            field: 'assignedFileNumbers',
            headerName: 'Aktenzeichen',
            width: 190,
            sortable: false,
            valueGetter: (_, row) => row.assignedFileNumbers.join(', '),
            renderCell: ({value}) => value || <ProcessEmptyValue>Nicht hinterlegt</ProcessEmptyValue>,
        },
        {
            field: 'processName',
            headerName: 'Prozess',
            minWidth: 160,
            flex: 1,
        },
    ];
    if (tasks)
        columns.push(
            {
                field: 'taskName',
                headerName: 'Aufgabe',
                minWidth: 180,
                flex: 1.3,
                sortable: false,
                renderCell: ({row}) => (
                    <CellLink to={`/tasks/${(row as ProcessTaskListEntry).processInstanceId}/${row.id}`}>
                        <ProcessNodeLabel
                            name={(row as ProcessTaskListEntry).taskName}
                            typeLabel={(row as ProcessTaskListEntry).taskType}
                        />
                    </CellLink>
                ),
            },
            {
                field: 'description',
                headerName: 'Kurzbeschreibung',
                width: 300,
                sortable: false,
                renderCell: ({value}) => value || <ProcessEmptyValue>Nicht hinterlegt</ProcessEmptyValue>,
            },
        );
    columns.push(
        {
            field: 'assignedUserName',
            headerName: 'Zugewiesen an',
            width: 165,
            sortable: false,
            renderCell: ({row}) =>
                row.assignedUserId ? (
                    (row.assignedUserName ?? 'Name nicht verfügbar')
                ) : (
                    <ProcessEmptyValue>Nicht zugewiesen</ProcessEmptyValue>
                ),
        },
        {
            field: 'status',
            headerName: tasks ? 'Aufgabenstatus' : 'Vorgangsstatus',
            width: 165,
            renderCell: ({row}) => {
                const label = tasks
                    ? ProcessTaskStatusLabels[row.status as ProcessTaskStatus]
                    : ProcessInstanceStatusLabels[row.status as ProcessInstanceStatus];
                return (
                    <Tooltip
                        arrow
                        title={
                            row.statusOverride?.trim() ? `${row.statusOverride.trim()} (Systemstatus: ${label})` : label
                        }
                    >
                        <Box
                            component="span"
                            sx={{maxWidth: '100%'}}
                        >
                            <Chip
                                sx={{maxWidth: '100%'}}
                                size="small"
                                mode="soft"
                                label={row.statusOverride?.trim() || label}
                                color={
                                    tasks
                                        ? ProcessTaskStatusColors[row.status as ProcessTaskStatus]
                                        : ProcessInstanceStatusColor[row.status as ProcessInstanceStatus]
                                }
                            />
                        </Box>
                    </Tooltip>
                );
            },
        },
    );
    if (tasks)
        columns.push({
            field: 'deadline',
            headerName: 'Fällig am',
            width: 170,
            renderCell: ({row}) => {
                const task = row as ProcessTaskListEntry;
                return (
                    <ListDate
                        value={task.deadline}
                        overdue={
                            task.deadline != null &&
                            Date.parse(task.deadline) < Date.now() &&
                            [
                                ProcessTaskStatus.Running,
                                ProcessTaskStatus.Paused,
                                ProcessTaskStatus.AwaitingCustomer,
                                ProcessTaskStatus.AwaitingPayment,
                            ].includes(task.status)
                        }
                    />
                );
            },
        });
    columns.push(
        {
            field: 'started',
            headerName: tasks ? 'Aufgabe erhalten' : 'Eingegangen',
            width: 170,
            renderCell: ({row}) => <ListDate value={row.started} />,
        },
        {
            field: 'finished',
            headerName: 'Beendet am',
            width: 170,
            renderCell: ({row}) => <ListDate value={row.finished} />,
        },
        {
            field: 'processVersion',
            headerName: 'Prozessversion',
            width: 130,
            sortable: false,
        },
        {
            field: 'test',
            headerName: tasks ? 'Test-Aufgabe' : 'Test-Vorgang',
            type: 'boolean',
            width: 130,
            sortable: false,
        },
        {
            field: 'actions',
            headerName: 'Aktionen',
            width: 100,
            sortable: false,
            hideable: false,
            resizable: false,
            renderCell: ({row}) => (
                <ProcessListActions
                    item={row}
                    onChanged={onChanged}
                />
            ),
        },
    );
    return columns;
}
