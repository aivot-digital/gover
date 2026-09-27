import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {ProcessAssignmentButton} from '../../components/process-assignment-button';
import {createStaffPath} from '../../../../utils/url-path-utils';
import {ProcessAssignee} from '../../components/process-assignee';
import React, {type ReactNode, useId, useMemo} from 'react';
import {Box, Button, Tooltip, Typography} from '@mui/material';
import {Link as RouterLink} from 'react-router-dom';
import AccountCircle from '@aivot/mui-material-symbols-400-n25-outlined/AccountCircle';
import Flag from '@aivot/mui-material-symbols-400-n25-outlined/Flag';
import SellOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Sell';
import RouteOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Route';
import EventAvailableOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/EventAvailable';
import ScheduleOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Schedule';
import ScienceOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Science';
import EditOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import AssignmentIndOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/AssignmentInd';
import {StatusTable} from '../../../../components/status-table/status-table';
import {type StatusTablePropsItem} from '../../../../components/status-table/status-table-props';
import {useGenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {
    getProcessTaskDescription,
    getProcessTaskEditPath,
    getProcessTaskName,
    getProcessTaskNodeIcon,
    getProcessTaskProcessPath,
    type ProcessTaskDetailsPageItem,
} from './process-task-view-page';
import Inbox from '@aivot/mui-material-symbols-400-n25-outlined/Inbox';
import MoveToInbox from '@aivot/mui-material-symbols-400-n25-outlined/MoveToInbox';
import Acute from '@aivot/mui-material-symbols-400-n25-outlined/Acute';
import Task from '@aivot/mui-material-symbols-400-n25-outlined/Task';
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

import {ProcessTaskStatus, ProcessTaskStatusLabels} from '../../enums/process-task-status';
import {type IdentityDataMap} from '../../../identity/models/identity-data';
import {useHasProcessPermission} from '../../../permissions/hooks/use-permissions';
import {Permission} from '../../../../data/permissions/permission';

function getExternalAssigneeLabel(identityId: string, identities?: IdentityDataMap): ReactNode {
    const identity = Object.values(identities ?? {}).find((value) => value.identityId === identityId);
    if (identity == null) return <ProcessEmptyValue>Zugewiesen · Angaben nicht verfügbar</ProcessEmptyValue>;

    const attributes = identity.attributes;
    const name =
        attributes.name?.trim() ||
        [attributes.given_name, attributes.family_name]
            .map((part) => part?.trim())
            .filter(Boolean)
            .join(' ');
    return (
        name ||
        identity.emailAddress?.trim() || <ProcessEmptyValue>Zugewiesen · Name nicht hinterlegt</ProcessEmptyValue>
    );
}

export function ProcessTaskViewPageIndex(): ReactNode {
    const sectionId = useId();
    const {item, refresh} = useGenericDetailsPageContext<ProcessTaskDetailsPageItem, undefined>();
    const canReadProcess = useHasProcessPermission(item?.task.processId, Permission.PROCESS_DEFINITION_READ);

    const instanceInfoItems = useMemo<StatusTablePropsItem[]>(() => {
        if (item == null) {
            return [];
        }

        const processPath = canReadProcess ? getProcessTaskProcessPath(item) : null;
        const processLabel = renderProcessLabel(
            item.process?.internalTitle ?? `Prozess #${item.task.processId}`,
            item.task.processVersion,
        );
        const entries: StatusTablePropsItem[] = [
            {
                label: 'Vorgangskennung',
                icon: <Inbox />,
                children: (
                    <CopyableProcessValue
                        value={item.instance?.caseNumber}
                        label="Vorgangskennung"
                    >
                        {renderLinkedValue(
                            item.instance?.caseNumber,
                            item.instance == null ? null : createStaffPath(`/process-instances/${item.instance.id}`),
                        )}
                    </CopyableProcessValue>
                ),
            },
            {
                label: 'Aktenzeichen',
                icon: <SellOutlinedIcon />,
                children: <ProcessFileNumbers values={item.instance?.assignedFileNumbers} />,
            },
            {
                label: 'Prozess',
                icon: <RouteOutlinedIcon />,
                children: renderLinkedValue(processLabel, processPath),
            },
        ];
        return entries;
    }, [item, canReadProcess]);

    const taskInfoItems = useMemo<StatusTablePropsItem[]>(() => {
        if (item == null) {
            return [];
        }

        const entries: StatusTablePropsItem[] = [
            {
                label: 'Prozesselement',
                icon: getProcessTaskNodeIcon(item),
                children: (
                    <ProcessNodeLabel
                        name={getProcessTaskName(item)}
                        typeLabel={item.provider?.name}
                    />
                ),
            },
            {
                label: 'Kurzbeschreibung',
                icon: <Task />,
                alignTop: true,
                children:
                    item.node?.description?.trim() || item.provider?.abstractDescription?.trim() ? (
                        getProcessTaskDescription(item)
                    ) : (
                        <ProcessEmptyValue>{getProcessTaskDescription(item)}</ProcessEmptyValue>
                    ),
            },
            {
                label: 'Aufgabe erhalten',
                icon: <MoveToInbox />,
                children: formatDateTimeWithRelative(item.task.started),
            },
        ];
        if (item.instance?.createdForTestClaimId != null) {
            entries.push({
                label: 'Test-Aufgabe',
                icon: <ScienceOutlinedIcon sx={{color: 'warning.main'}} />,
                alignTop: true,
                children: (
                    <>
                        Diese Aufgabe gehört zu einem Vorgang, der über den Testmodus des Prozesses gestartet wurde.
                    </>
                ),
            });
        }
        return entries;
    }, [item]);

    const taskStatusItems = useMemo<StatusTablePropsItem[]>(() => {
        if (item == null) return [];
        const entries: StatusTablePropsItem[] = [
            {
                label: 'Aufgabenstatus',
                icon: <Flag />,
                children: (
                    <ProcessStatusValue
                        systemLabel={ProcessTaskStatusLabels[item.task.status]}
                        statusOverride={item.task.statusOverride}
                    />
                ),
            },
            {
                label: 'Aufgabe zugewiesen an',
                icon: <AssignmentIndOutlinedIcon />,
                children: <ProcessAssignee userId={item.task.assignedUserId} />,
            },
            {
                label: 'Fälligkeit (spätestens)',
                icon: <Acute />,
                children: formatDateTimeWithRelative(item.task.deadline, 'Nicht festgelegt'),
            },
            {
                label: 'Zuletzt aktualisiert',
                icon: <ScheduleOutlinedIcon />,
                children: formatDateTimeWithRelative(item.task.updated),
            },
        ];

        if (item.task.assignedCustomerIdentityId != null) {
            entries.push({
                label: 'Externe Beteiligung',
                icon: <AccountCircle />,
                children: getExternalAssigneeLabel(item.task.assignedCustomerIdentityId, item.instance?.identities),
            });
        }

        if (item.task.finished != null) {
            entries.push({
                label:
                    item.task.status === ProcessTaskStatus.Completed
                        ? 'Abgeschlossen am'
                        : item.task.status === ProcessTaskStatus.Aborted
                          ? 'Abgebrochen am'
                          : item.task.status === ProcessTaskStatus.Failed
                            ? 'Fehlgeschlagen am'
                            : 'Beendet am',
                icon: <EventAvailableOutlinedIcon />,
                children: formatDateTimeWithRelative(item.task.finished),
            });
        }

        return entries;
    }, [item]);

    if (item == null) return <GenericDetailsSkeleton />;

    return (
        <Box
            sx={{
                pt: 1,
            }}
        >
            <Box
                component="section"
                aria-labelledby={`${sectionId}-instance`}
            >
                <Typography
                    id={`${sectionId}-instance`}
                    component="h2"
                    variant="h5"
                >
                    Angaben zum Vorgang
                </Typography>
                <StatusTable
                    sx={{mt: 2}}
                    cardVariant="outlined"
                    items={instanceInfoItems}
                />
            </Box>
            <Box
                component="section"
                aria-labelledby={`${sectionId}-task`}
                sx={{mt: 4}}
            >
                <Typography
                    id={`${sectionId}-task`}
                    component="h2"
                    variant="h5"
                >
                    Angaben zur Aufgabe
                </Typography>
                <StatusTable
                    sx={{mt: 2}}
                    cardVariant="outlined"
                    items={taskInfoItems}
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
                    Stand der Aufgabe
                </Typography>
                <StatusTable
                    sx={{mt: 2}}
                    cardVariant="outlined"
                    items={taskStatusItems}
                />
            </Box>
            <Box
                sx={{
                    mt: 4,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: 2,
                    flexWrap: 'wrap',
                }}
            >
                <Box
                    sx={{
                        display: 'flex',
                        gap: 2,
                        flexWrap: 'wrap',
                    }}
                >
                    <Button
                        component={RouterLink}
                        to={getProcessTaskEditPath(item.task.processInstanceId, item.task.id)}
                        variant="contained"
                        startIcon={<EditOutlinedIcon />}
                    >
                        Aufgabe bearbeiten
                    </Button>

                    <Tooltip
                        title="Diese Funktion ist noch nicht verfügbar."
                        arrow
                    >
                        <span>
                            <Button
                                disabled
                                startIcon={<ScheduleOutlinedIcon />}
                            >
                                Wiedervorlage einrichten
                            </Button>
                        </span>
                    </Tooltip>
                </Box>

                <ProcessAssignmentButton
                    instanceId={item.task.processInstanceId}
                    taskId={item.task.id}
                    taskStatus={item.task.status}
                    assignedUserId={item.task.assignedUserId}
                    onAssigned={refresh}
                />
            </Box>
        </Box>
    );
}
