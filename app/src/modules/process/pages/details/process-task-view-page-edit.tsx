import React, {type ReactNode, useEffect, useMemo, useState} from 'react';
import {Box, Button, Skeleton, Stack, Typography} from '@mui/material';
import {useNavigate} from 'react-router-dom';
import {StatusTable} from '../../../../components/status-table/status-table';
import {type StatusTablePropsItem} from '../../../../components/status-table/status-table-props';
import {useGenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {ElementDerivationContext} from '../../../elements/components/element-derivation-context';
import {
    ProcessInstanceTaskApiService,
    type TaskView,
    type TaskViewEventAlignment,
    type TaskViewEventColor,
    type TaskViewEvent,
    type TaskViewEventVariant,
} from '../../services/process-instance-task-api-service';
import {AuthoredElementValues} from '../../../../models/element-data';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {clearLoadingMessage, setErrorMessage, setLoadingMessage} from '../../../../slices/shell-slice';
import {showApiErrorSnackbar} from '../../../../slices/snackbar-slice';
import {withDelay} from '../../../../utils/with-delay';
import {ProcessTaskStatus} from '../../enums/process-task-status';
import {
    getProcessTaskDescription,
    getProcessTaskName,
    getProcessTaskNodeIcon,
    type ProcessTaskDetailsPageItem,
} from './process-task-view-page';
import Task from '@aivot/mui-material-symbols-400-outlined/dist/task/Task';
import {dispatchProcessAssignedTaskCountRefreshEvent} from '../../utils/process-assigned-task-count-events';

export function ProcessTaskViewPageEdit(): ReactNode {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const {
        item,
    } = useGenericDetailsPageContext<ProcessTaskDetailsPageItem, undefined>();

    const [taskView, setTaskView] = useState<TaskView>();
    const [taskInputData, setTaskInputData] = useState<AuthoredElementValues>({});

    useEffect(() => {
        let cancelled = false;

        if (item == null) {
            setTaskView(undefined);
            setTaskInputData({});
            return () => {
                cancelled = true;
            };
        }

        setTaskView(undefined);
        setTaskInputData({});

        new ProcessInstanceTaskApiService()
            .getStaffTaskView(item.task.processInstanceId, item.task.id)
            .then((view) => {
                if (cancelled) {
                    return;
                }

                setTaskView(view);
                setTaskInputData(view.data);
            })
            .catch((err) => {
                if (cancelled) {
                    return;
                }

                if (err.status === 403) {
                    dispatch(setErrorMessage({
                        message: 'Sie haben keine Berechtigung, diese Aufgabenansicht zu sehen.',
                        status: 403,
                    }));
                } else {
                    dispatch(showApiErrorSnackbar(err, 'Die Aufgabenansicht konnte nicht geladen werden.'));
                }
            });

        return () => {
            cancelled = true;
        };
    }, [dispatch, item?.task.id, item?.task.processInstanceId]);

    const introItems = useMemo<StatusTablePropsItem[]>(() => {
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

    const leftAlignedTaskViewEvents = useMemo(() => {
        return (taskView?.events ?? []).filter((evt) => getTaskViewEventAlignment(evt) === 'left');
    }, [taskView?.events]);

    const rightAlignedTaskViewEvents = useMemo(() => {
        return (taskView?.events ?? []).filter((evt) => getTaskViewEventAlignment(evt) === 'right');
    }, [taskView?.events]);

    const handleEventClick = (evt: TaskViewEvent) => {
        if (item == null || taskView == null) {
            return;
        }

        dispatch(setLoadingMessage({
            message: `Verarbeite Aktion: ${evt.label}`,
            blocking: true,
            estimatedTime: 500,
        }));

        withDelay(
            new ProcessInstanceTaskApiService()
                .putStaffTaskView(item.task.processInstanceId, item.task.id, taskInputData, evt.event),
            500,
        )
            .then(async (updatedTaskView) => {
                dispatchProcessAssignedTaskCountRefreshEvent();

                const updatedTask = await new ProcessInstanceTaskApiService().retrieve(item.task.id);

                if (updatedTask.status === ProcessTaskStatus.Running) {
                    setTaskView(updatedTaskView);
                    setTaskInputData(updatedTaskView.data);
                    return;
                }

                navigate('/tasks');
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Die Aufgabe konnte nicht verarbeitet werden.'));
            })
            .finally(() => {
                dispatch(clearLoadingMessage());
            });
    };

    if (item == null) {
        return (
            <Box
                sx={{
                    pt: 1.5,
                }}
            >
                <Typography variant="h5">
                    Aufgabe bearbeiten
                </Typography>
                <Skeleton
                    sx={{mt: 3}}
                    height={320}
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
                Aufgabe bearbeiten
            </Typography>

            <StatusTable
                sx={{mt: 2}}
                cardVariant="outlined"
                items={introItems}
            />

            {
                taskView == null ?
                    <Skeleton
                        sx={{mt: 4}}
                        height={360}
                    /> :
                    <>
                        <Box
                            sx={{
                                mt: 4,
                            }}
                        >
                            <ElementDerivationContext
                                element={taskView.layout}
                                authoredElementValues={taskInputData}
                                onAuthoredElementValuesChange={setTaskInputData}
                            />
                        </Box>

                        {
                            taskView.events.length > 0 &&
                            <Box
                                sx={{
                                    mt: 4,
                                    display: 'flex',
                                    flexDirection: {
                                        xs: 'column',
                                        sm: 'row',
                                    },
                                    gap: 2,
                                    justifyContent: 'space-between',
                                    alignItems: {
                                        sm: 'center',
                                    },
                                }}
                            >
                                {
                                    leftAlignedTaskViewEvents.length > 0 &&
                                    <Stack
                                        direction={{
                                            xs: 'column',
                                            sm: 'row',
                                        }}
                                        spacing={2}
                                        sx={{
                                            width: {
                                                xs: '100%',
                                                sm: 'auto',
                                            },
                                        }}
                                    >
                                        {
                                            leftAlignedTaskViewEvents.map((evt) => (
                                                <Button
                                                    key={evt.event}
                                                    variant={getTaskViewEventVariant(evt)}
                                                    color={getTaskViewEventColor(evt)}
                                                    onClick={() => {
                                                        handleEventClick(evt);
                                                    }}
                                                    sx={{
                                                        width: {
                                                            xs: '100%',
                                                            sm: 'auto',
                                                        },
                                                    }}
                                                >
                                                    {evt.label}
                                                </Button>
                                            ))
                                        }
                                    </Stack>
                                }

                                {
                                    rightAlignedTaskViewEvents.length > 0 &&
                                    <Stack
                                        direction={{
                                            xs: 'column',
                                            sm: 'row',
                                        }}
                                        spacing={2}
                                        sx={{
                                            width: {
                                                xs: '100%',
                                                sm: 'auto',
                                            },
                                            marginLeft: {
                                                sm: 'auto',
                                            },
                                        }}
                                    >
                                        {
                                            rightAlignedTaskViewEvents.map((evt) => (
                                                <Button
                                                    key={evt.event}
                                                    variant={getTaskViewEventVariant(evt)}
                                                    color={getTaskViewEventColor(evt)}
                                                    onClick={() => {
                                                        handleEventClick(evt);
                                                    }}
                                                    sx={{
                                                        width: {
                                                            xs: '100%',
                                                            sm: 'auto',
                                                        },
                                                    }}
                                                >
                                                    {evt.label}
                                                </Button>
                                            ))
                                        }
                                    </Stack>
                                }
                            </Box>
                        }
                    </>
            }
        </Box>
    );
}

function getTaskViewEventVariant(evt: TaskViewEvent): TaskViewEventVariant {
    if (evt.variant === 'outlined' || evt.variant === 'text') {
        return evt.variant;
    }

    return 'contained';
}

function getTaskViewEventColor(evt: TaskViewEvent): TaskViewEventColor {
    if (evt.color === 'secondary' || evt.color === 'error' || evt.color === 'success') {
        return evt.color;
    }

    return 'primary';
}

function getTaskViewEventAlignment(evt: TaskViewEvent): TaskViewEventAlignment {
    if (evt.alignment === 'right') {
        return evt.alignment;
    }

    return 'left';
}
