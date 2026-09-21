import React, {type ReactNode, useEffect, useMemo, useState} from 'react';
import {Box, Button, Link, Skeleton, Tooltip, Typography} from '@mui/material';
import {Link as RouterLink, useSearchParams} from 'react-router-dom';
import AccountCircle from '@aivot/mui-material-symbols-400-n25-outlined/AccountCircle';
import Flag from '@aivot/mui-material-symbols-400-n25-outlined/Flag';
import SellOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Sell';
import RouteOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Route';
import EventAvailableOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/EventAvailable';
import ScheduleOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Schedule';
import ScienceOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Science';
import EditOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import AssignmentIndOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/AssignmentInd';
import OpenInNewIcon from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
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
    formatInstantInApplicationTimeZone,
    formatRelativeInstantInApplicationTimeZone,
} from '../../../../utils/temporal-utils';

import {ProcessTaskStatus, ProcessTaskStatusLabels} from '../../enums/process-task-status';
import {UsersApiService} from '../../../users/users-api-service';
import {resolveUserName} from '../../../users/utils/resolve-user-name';
import {type User} from '../../../users/models/user';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectUser} from '../../../../slices/user-slice';
import {useHasSystemPermission} from '../../../permissions/hooks/use-permissions';
import {Permission} from '../../../../data/permissions/permission';
import {type IdentityDataMap} from '../../../identity/models/identity-data';

function TaskAssignee({userId}: {userId: string | null}): ReactNode {
    const currentUser = useAppSelector(selectUser);
    const canReadUsers = useHasSystemPermission(Permission.USER_READ);
    const [lookup, setLookup] = useState<{id: string; user: User | null} | null>(null);
    const isSelf = userId != null && currentUser?.id === userId;

    useEffect(() => {
        let cancelled = false;
        setLookup(null);
        if (userId != null && !isSelf && canReadUsers) {
            new UsersApiService().retrieve(userId).then(
                (user) => {
                    if (!cancelled) {
                        setLookup({id: userId, user});
                    }
                },
                () => {
                    if (!cancelled) {
                        setLookup({id: userId, user: null});
                    }
                },
            );
        }
        return () => {
            cancelled = true;
        };
    }, [userId, isSelf, canReadUsers]);

    if (userId == null) return 'Nicht zugewiesen';
    if (isSelf) return resolveUserName(currentUser);
    if (!canReadUsers) return 'Zugewiesen · Name nicht verfügbar';
    if (lookup?.id !== userId) return 'Name wird geladen …';
    return lookup.user == null ? 'Zugewiesen · Name nicht verfügbar' : resolveUserName(lookup.user);
}

function getExternalAssigneeLabel(identityId: string, identities?: IdentityDataMap): string {
    const identity = Object.values(identities ?? {}).find((value) => value.identityId === identityId);
    if (identity == null) return 'Zugewiesen · Angaben nicht verfügbar';

    const attributes = identity.attributes;
    const name = attributes.name?.trim() || [attributes.given_name, attributes.family_name]
        .map((part) => part?.trim()).filter(Boolean).join(' ');
    return name || identity.emailAddress?.trim() || 'Zugewiesen · Name nicht hinterlegt';
}

function formatDateTimeWithRelative(value?: string | null, fallback = 'Nicht hinterlegt'): ReactNode {
    if (value == null || value.trim().length === 0) {
        return fallback;
    }

    const formatted = formatInstantInApplicationTimeZone(value, 'dd.MM.yyyy – HH:mm');
    const relative = formatRelativeInstantInApplicationTimeZone(value);
    if (formatted == null || relative == null) {
        return fallback;
    }

    return (
        <Box component="span">
            {formatted} Uhr{' '}
            <Box
                component="span"
                sx={{
                    color: 'text.secondary',
                }}
            >
                ({relative})
            </Box>
        </Box>
    );
}

function renderLinkedValue(label: ReactNode, to: string | null): ReactNode {
    if (to == null) {
        return label;
    }

    return (
        <Link
            href={to}
            target="_blank"
            rel="noopener noreferrer"
            underline="hover"
            color="inherit"
            sx={{
                display: 'inline-flex',
                alignItems: 'center',
                flexWrap: 'wrap',
                columnGap: 0.75,
                rowGap: 0.25,
            }}
        >
            {label}
            <OpenInNewIcon
                fontSize="inherit"
                sx={{
                    fontSize: 16,
                    color: 'text.secondary',
                }}
            />
        </Link>
    );
}

function renderProcessLabel(name: string, version: number): ReactNode {
    return (
        <Box component="span">
            <Box component="span">{name}</Box>{' '}
            <Box
                component="span"
                sx={{
                    color: 'text.secondary',
                }}
            >
                (v{version})
            </Box>
        </Box>
    );
}

export function ProcessTaskViewPageIndex(): ReactNode {
    const {
        item,
    } = useGenericDetailsPageContext<ProcessTaskDetailsPageItem, undefined>();

    const [searchParams] = useSearchParams();
    // Temporary display-only preview; never changes the task or persists example data.
    const previewMetadata = import.meta.env.DEV && searchParams.get('previewTaskMetadata') === '1';
    const previewFinished = useMemo(() => new Date(Date.now() - 60 * 60 * 1000).toISOString(), []);

    const generalInfoItems = useMemo<StatusTablePropsItem[]>(() => {
        if (item == null) {
            return [];
        }

        const processPath = getProcessTaskProcessPath(item);
        const processLabel = renderProcessLabel(
            item.process?.internalTitle ?? `Prozess #${item.task.processId}`,
            item.task.processVersion,
        );
        const fileNumbers = item.instance?.assignedFileNumbers?.filter((value) => value.trim().length > 0) ?? [];
        const entries: StatusTablePropsItem[] = [
            {
                label: 'Vorgangskennung',
                icon: <Inbox />,
                children: renderLinkedValue(item.instance?.caseNumber ?? 'Nicht hinterlegt', processPath),
            },
            {
                label: 'Aktenzeichen',
                icon: <SellOutlinedIcon />,
                children: fileNumbers.length > 0 ? fileNumbers.join(', ') : 'Kein Aktenzeichen hinterlegt',
            },
            {
                label: 'Prozess',
                icon: <RouteOutlinedIcon />,
                children: renderLinkedValue(processLabel, processPath),
            },
            {
                label: 'Aufgabe erhalten',
                icon: <MoveToInbox />,
                children: formatDateTimeWithRelative(item.task.started),
            },
            {
                label: 'Fälligkeit (spätestens)',
                icon: <Acute/>,
                children: formatDateTimeWithRelative(item.task.deadline, 'Nicht festgelegt'),
            },
        ];

        if (item.instance?.createdForTestClaimId != null) {
            entries.push({
                label: 'Test-Aufgabe',
                icon: <ScienceOutlinedIcon sx={{color: 'warning.main'}} />,
                alignTop: true,
                children: (
                    <>
                        Es handelt sich bei dieser Aufgabe um einen Test.
                        <br />
                        Der zugehörige Vorgang wurde im Testmodus ausgelöst.
                    </>
                ),
            });
        }

        return entries;
    }, [item]);

    const taskInfoItems = useMemo<StatusTablePropsItem[]>(() => {
        if (item == null) {
            return [];
        }

        const entries: StatusTablePropsItem[] = [
            {
                label: 'Prozesselement',
                icon: getProcessTaskNodeIcon(item),
                children: getProcessTaskName(item),
            },
            {
                label: 'Kurzbeschreibung',
                icon: <Task />,
                alignTop: true,
                children: getProcessTaskDescription(item),
            },
            {
                label: 'Aufgabenstatus',
                icon: <Flag />,
                children: item.task.statusOverride?.trim() || ProcessTaskStatusLabels[item.task.status],
            },
            {
                label: 'Zuständige Person',
                icon: <AssignmentIndOutlinedIcon />,
                children: <TaskAssignee userId={item.task.assignedUserId} />,
            },
            {
                label: 'Zuletzt aktualisiert',
                icon: <ScheduleOutlinedIcon />,
                children: formatDateTimeWithRelative(item.task.updated),
            },
        ];

        if (previewMetadata || item.task.assignedCustomerIdentityId != null) {
            entries.push({
                label: 'Externe Beteiligung',
                icon: <AccountCircle />,
                children: previewMetadata
                    ? 'Erika Muster (Beispiel)'
                    : getExternalAssigneeLabel(item.task.assignedCustomerIdentityId!, item.instance?.identities),
            });
        }

        if (previewMetadata || item.task.finished != null) {
            entries.push({
                label: previewMetadata || item.task.status === ProcessTaskStatus.Completed
                    ? 'Abgeschlossen am'
                    : item.task.status === ProcessTaskStatus.Aborted
                        ? 'Abgebrochen am'
                        : item.task.status === ProcessTaskStatus.Failed
                            ? 'Fehlgeschlagen am'
                            : 'Beendet am',
                icon: <EventAvailableOutlinedIcon />,
                children: formatDateTimeWithRelative(previewMetadata ? previewFinished : item.task.finished),
            });
        }

        return entries;
    }, [item, previewMetadata, previewFinished]);

    if (item == null) {
        return (
            <Box
                sx={{
                    pt: 1,
                    pb: 2,
                }}
            >
                <Typography variant="h5">
                    Allgemeine Informationen
                </Typography>
                <Skeleton
                    sx={{mt: 3}}
                    height={280}
                />
            </Box>
        );
    }

    return (
        <Box
            sx={{
                pt: 1,
            }}
        >
            <Typography variant="h5">
                Allgemeine Informationen
            </Typography>

            <StatusTable
                sx={{mt: 2}}
                cardVariant="outlined"
                items={generalInfoItems}
            />

            <Typography
                variant="h5"
                sx={{mt: 4}}
            >
                Details zur Aufgabe
            </Typography>

            <StatusTable
                sx={{mt: 2}}
                cardVariant="outlined"
                items={taskInfoItems}
            />

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

                    <Tooltip title="Diese Funktion ist noch nicht verfügbar.">
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

                <Tooltip title="Diese Funktion ist noch nicht verfügbar.">
                    <span>
                        <Button
                            disabled
                            startIcon={<AssignmentIndOutlinedIcon />}
                        >
                            Aufgabe neu zuweisen
                        </Button>
                    </span>
                </Tooltip>
            </Box>
        </Box>
    );
}
