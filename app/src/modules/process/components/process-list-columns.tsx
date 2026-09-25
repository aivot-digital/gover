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

function ListText({value}: {value: string}) {
    return (
        <Box
            component="span"
            title={value}
            sx={{display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap'}}
        >
            {value}
        </Box>
    );
}

function ListDate({value, overdue = false}: {value: string | null; overdue?: boolean}) {
    if (!value) return <ProcessEmptyValue>—</ProcessEmptyValue>;
    const formatted = formatInstantInApplicationTimeZone(value, 'dd.MM.yyyy – HH:mm');
    if (formatted == null) return <ProcessEmptyValue>—</ProcessEmptyValue>;
    const label = `${formatted} Uhr`;
    const relative = formatRelativeInstantInApplicationTimeZone(value);
    return (
        <Tooltip
            arrow
            title={relative ? `${label} (${relative})` : label}
        >
            <Box
                component="span"
                sx={{
                    color: overdue ? 'error.main' : undefined,
                    display: 'block',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                }}
            >
                {label}
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
                    title={row.caseNumber}
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
            renderCell: ({value}) =>
                value ? <ListText value={value} /> : <ProcessEmptyValue>Nicht hinterlegt</ProcessEmptyValue>,
        },
        {
            field: 'processName',
            headerName: 'Prozess',
            minWidth: 160,
            flex: 1,
            renderCell: ({row}) => <ListText value={row.processName} />,
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
                renderCell: ({row}) => {
                    const task = row as ProcessTaskListEntry;
                    const name = task.taskName.trim();
                    const type = task.taskType?.trim();
                    const title = name
                        ? type && type !== name
                            ? `„${name}“ · ${type}`
                            : name
                        : type || 'Nicht verfügbar';
                    return (
                        <CellLink
                            to={`/tasks/${task.processInstanceId}/${row.id}`}
                            title={title}
                        >
                            <ProcessNodeLabel
                                name={task.taskName}
                                typeLabel={task.taskType}
                            />
                        </CellLink>
                    );
                },
            },
            {
                field: 'description',
                headerName: 'Kurzbeschreibung',
                width: 300,
                sortable: false,
                renderCell: ({value}) =>
                    value ? <ListText value={value} /> : <ProcessEmptyValue>Nicht hinterlegt</ProcessEmptyValue>,
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
                    <ListText value={row.assignedUserName ?? 'Name nicht verfügbar'} />
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
                const customLabel = row.statusOverride?.trim();
                return (
                    <Tooltip
                        arrow
                        title={
                            customLabel && customLabel !== label ? `${customLabel} (Systemstatus: ${label})` : ''
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
                                label={customLabel || label}
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
