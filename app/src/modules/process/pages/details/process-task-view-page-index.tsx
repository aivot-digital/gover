import React, {type ReactNode, useMemo} from 'react';
import {Box, Button, Link, Skeleton, Tooltip, Typography} from '@mui/material';
import {Link as RouterLink} from 'react-router-dom';
import FingerprintOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Fingerprint';
import SellOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Sell';
import RouteOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Route';
import EventAvailableOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/EventAvailable';
import ScheduleOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Schedule';
import ScienceOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Science';
import EditOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import AssignmentIndOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/AssignmentInd';
import OpenInNewIcon from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import {formatDistanceToNowStrict} from 'date-fns';
import {de} from 'date-fns/locale';
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
    instantToEpochMillis,
} from '../../../../utils/temporal-utils';

function formatDateTimeWithRelative(value?: string | null, fallback = 'Nicht hinterlegt'): ReactNode {
    if (value == null || value.trim().length === 0) {
        return fallback;
    }

    const epochMillis = instantToEpochMillis(value);
    const formatted = formatInstantInApplicationTimeZone(value, 'dd.MM.yyyy – HH:mm');
    if (epochMillis == null || formatted == null) {
        return fallback;
    }
    const parsed = new Date(epochMillis);

    return (
        <Box component="span">
            {formatted} Uhr{' '}
            <Box
                component="span"
                sx={{
                    color: 'text.secondary',
                }}
            >
                ({formatDistanceToNowStrict(parsed, {addSuffix: true, locale: de})})
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
                label: 'Vorgangsschlüssel',
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

        return [
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
        ];
    }, [item]);

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
                Zugewiesene Aufgabe
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
