import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {ProcessAssignmentButton} from '../../components/process-assignment-button';
import {useEffect, useId, useState} from 'react';
import {Box, Button, Tooltip, Typography} from '@mui/material';
import {Link as RouterLink} from 'react-router-dom';
import Lock from '@aivot/mui-material-symbols-400-n25-outlined/Lock';
import Cancel from '@aivot/mui-material-symbols-400-n25-outlined/Cancel';
import Inbox from '@aivot/mui-material-symbols-400-n25-outlined/Inbox';
import Sell from '@aivot/mui-material-symbols-400-n25-outlined/Sell';
import Route from '@aivot/mui-material-symbols-400-n25-outlined/Route';
import Schedule from '@aivot/mui-material-symbols-400-n25-outlined/Schedule';
import Science from '@aivot/mui-material-symbols-400-n25-outlined/Science';
import Flag from '@aivot/mui-material-symbols-400-n25-outlined/Flag';
import Acute from '@aivot/mui-material-symbols-400-n25-outlined/Acute';
import Assignment from '@aivot/mui-material-symbols-400-n25-outlined/Assignment';
import AssignmentInd from '@aivot/mui-material-symbols-400-n25-outlined/AssignmentInd';
import News from '@aivot/mui-material-symbols-400-n25-outlined/News';
import {StatusTable} from '../../../../components/status-table/status-table';
import {type StatusTablePropsItem} from '../../../../components/status-table/status-table-props';
import {useGenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {type ProcessInstanceDetails} from '../../entities/process-instance-details';
import {ProcessInstanceStatusLabels} from '../../enums/process-instance-status';
import {ProcessTaskStatusLabels} from '../../enums/process-task-status';
import {ProcessAssignee} from '../../components/process-assignee';
import {ProcessInstanceEventApiService} from '../../services/process-instance-event-api-service';
import {type ProcessInstanceEventEntity} from '../../entities/process-instance-event-entity';
import {createStaffPath} from '../../../../utils/url-path-utils';
import {
    ProcessNodeLabel,
    ProcessEmptyValue,
    ProcessStatusValue,
    CopyableProcessValue,
    ProcessFileNumbers,
    formatDateTimeWithRelative,
    renderLinkedValue,
    renderProcessLabel,
} from '../../components/process-detail-values';
import EventAvailable from '@aivot/mui-material-symbols-400-n25-outlined/EventAvailable';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {useHasProcessPermission} from '../../../permissions/hooks/use-permissions';
import {Permission} from '../../../../data/permissions/permission';

export function ProcessInstanceDetailsPageIndex() {
    const sectionId = useId();
    const {item, refresh} = useGenericDetailsPageContext<ProcessInstanceDetails, undefined>();
    const canReadProcess = useHasProcessPermission(item?.instance.processId, Permission.PROCESS_DEFINITION_READ);
    const [eventState, setEventState] = useState<{
        item: ProcessInstanceDetails;
        event?: ProcessInstanceEventEntity;
        failed?: boolean;
    } | null>(null);
    useEffect(() => {
        if (item == null) return;
        let cancelled = false;
        new ProcessInstanceEventApiService()
            .list(0, 1, ['timestamp', 'id'], 'DESC', {processInstanceId: item.instance.id})
            .then(({content}) => {
                if (!cancelled)
                    setEventState({
                        item,
                        event: content[0],
                    });
            })
            .catch(() => {
                if (!cancelled)
                    setEventState({
                        item,
                        failed: true,
                    });
            });
        return () => {
            cancelled = true;
        };
    }, [item]);

    if (item == null) return <GenericDetailsSkeleton />;
    const {instance} = item;
    const processPath = createStaffPath(
        `/processes/${instance.processId}/versions/${instance.initialProcessVersion}/?instanceId=${instance.id}`,
    );
    const processLabel = renderProcessLabel(item.processName, instance.initialProcessVersion);
    const generalItems: StatusTablePropsItem[] = [
        {
            label: 'Vorgangskennung',
            icon: <Inbox />,
            children: (
                <CopyableProcessValue
                    value={instance.caseNumber}
                    label="Vorgangskennung"
                />
            ),
        },
        {
            label: 'Aktenzeichen',
            icon: <Sell />,
            children: <ProcessFileNumbers values={instance.assignedFileNumbers} />,
        },
        {
            label: 'Prozess',
            icon: <Route />,
            children: renderLinkedValue(processLabel, canReadProcess ? processPath : null),
        },
        {
            label: 'Gestartet am',
            icon: <Schedule />,
            children: formatDateTimeWithRelative(instance.started),
        },
        {
            label: 'Auslösendes Prozesselement',
            icon: <Assignment />,
            children: (
                <ProcessNodeLabel
                    name={item.triggerName}
                    typeLabel={item.triggerType}
                />
            ),
        },
        {
            label: 'Verwaltende Org.-Einheit',
            icon: ModuleIcons.departments,
            children: item.departmentName ?? `Organisationseinheit #${item.departmentId}`,
        },
    ];
    if (instance.createdForTestClaimId != null) {
        generalItems.push({
            label: 'Test-Vorgang',
            icon: <Science sx={{color: 'warning.main'}} />,
            alignTop: true,
            children: <>Es handelt sich bei diesem Vorgang um einen Test.</>,
        });
    }

    const statusItems: StatusTablePropsItem[] = [
        {
            label: 'Vorgangsstatus',
            icon: <Flag />,
            children: (
                <ProcessStatusValue
                    systemLabel={ProcessInstanceStatusLabels[instance.status]}
                    statusOverride={instance.statusOverride}
                />
            ),
        },
        {
            label: 'Vorgang zugewiesen an',
            icon: <AssignmentInd />,
            children: <ProcessAssignee userId={instance.assignedUserId} />,
        },
        {
            label: 'Letztes Ereignis',
            icon: <News />,
            children:
                eventState?.item !== item ? (
                    <ProcessEmptyValue>Wird geladen …</ProcessEmptyValue>
                ) : eventState.failed ? (
                    'Das letzte Ereignis konnte nicht geladen werden.'
                ) : eventState.event == null ? (
                    <ProcessEmptyValue>Noch kein Ereignis vorhanden</ProcessEmptyValue>
                ) : (
                    <>
                        {eventState.event.title}
                        <Typography
                            variant="body2"
                            color="text.secondary"
                        >
                            {formatDateTimeWithRelative(eventState.event.timestamp)}
                        </Typography>
                    </>
                ),
        },
        {
            label: 'Zuletzt aktualisiert',
            icon: <Schedule />,
            children: formatDateTimeWithRelative(instance.updated),
        },
    ];
    if (instance.finished != null) {
        statusItems.push({
            label: 'Beendet am',
            icon: <EventAvailable />,
            children: formatDateTimeWithRelative(instance.finished),
        });
    }

    return (
        <Box sx={{pt: 1}}>
            <Box
                component="section"
                aria-labelledby={`${sectionId}-general`}
            >
                <Typography
                    id={`${sectionId}-general`}
                    component="h2"
                    variant="h5"
                >
                    Angaben zum Vorgang
                </Typography>
                <StatusTable
                    sx={{mt: 2}}
                    cardVariant="outlined"
                    items={generalItems}
                />
            </Box>
            <Box
                component="section"
                aria-labelledby={`${sectionId}-status`}
                sx={{mt: 4}}
            >
                <Typography
                    id={`${sectionId}-status`}
                    component="h2"
                    variant="h5"
                >
                    Stand des Vorgangs
                </Typography>
                <StatusTable
                    sx={{mt: 2}}
                    cardVariant="outlined"
                    items={statusItems}
                />
            </Box>
            <Box
                component="section"
                aria-labelledby={`${sectionId}-tasks`}
                sx={{mt: 4}}
            >
                <Typography
                    id={`${sectionId}-tasks`}
                    component="h2"
                    variant="h5"
                >
                    {item.activeTasks.length > 1 ? 'Aktive Aufgaben' : 'Aktive Aufgabe'}
                </Typography>
                {item.activeTasks.length === 0 ? (
                    <Box sx={{mt: 2}}>
                        <ProcessEmptyValue>Keine aktive Aufgabe</ProcessEmptyValue>
                    </Box>
                ) : (
                    item.activeTasks.map((task) => (
                        <Box
                            key={task.id}
                            role="group"
                            aria-label={`Aufgabe ${task.name}`}
                        >
                            <StatusTable
                                sx={{mt: 2}}
                                cardVariant="outlined"
                                items={[
                                    {
                                        label: 'Aufgabe',
                                        icon: <Assignment />,
                                        children: renderLinkedValue(
                                            task.name,
                                            createStaffPath(`/tasks/${instance.id}/${task.id}`),
                                        ),
                                    },
                                    {
                                        label: 'Aufgabenstatus',
                                        icon: <Flag />,
                                        children: (
                                            <ProcessStatusValue
                                                systemLabel={ProcessTaskStatusLabels[task.status]}
                                                statusOverride={task.statusOverride}
                                            />
                                        ),
                                    },
                                    {
                                        label: 'Aufgabe zugewiesen an',
                                        icon: <AssignmentInd />,
                                        children: <ProcessAssignee userId={task.assignedUserId} />,
                                    },
                                    {
                                        label: 'Fälligkeit (spätestens)',
                                        icon: <Acute />,
                                        children: formatDateTimeWithRelative(task.deadline, 'Nicht festgelegt'),
                                    },
                                ]}
                            />
                        </Box>
                    ))
                )}
            </Box>
            <Box
                sx={{
                    mt: 4,
                    display: 'flex',
                    justifyContent: 'space-between',
                    gap: 2,
                    flexWrap: 'wrap',
                }}
            >
                <Button
                    component={RouterLink}
                    to={`/processes/${instance.processId}/versions/${instance.initialProcessVersion}/instances/${instance.id}/tasks`}
                    startIcon={ModuleIcons.tasks}
                >
                    Alle Aufgaben aufrufen
                </Button>
                <Box
                    sx={{
                        display: 'flex',
                        gap: 2,
                        flexWrap: 'wrap',
                    }}
                >
                    <ProcessAssignmentButton
                        instanceId={instance.id}
                        assignedUserId={instance.assignedUserId}
                        onAssigned={refresh}
                    />
                    <Tooltip
                        title="Diese Funktion ist noch nicht verfügbar."
                        arrow
                    >
                        <span>
                            <Button
                                disabled
                                color="error"
                                startIcon={<Lock />}
                            >
                                Vorgang sperren
                            </Button>
                        </span>
                    </Tooltip>
                    <Tooltip
                        title="Diese Funktion ist noch nicht verfügbar."
                        arrow
                    >
                        <span>
                            <Button
                                disabled
                                color="error"
                                startIcon={<Cancel />}
                            >
                                Vorgang beenden
                            </Button>
                        </span>
                    </Tooltip>
                </Box>
            </Box>
        </Box>
    );
}
