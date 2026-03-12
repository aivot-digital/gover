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
    type TaskViewEvent,
} from '../../services/process-instance-task-api-service';
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

    useEffect(() => {
        let cancelled = false;

        if (item == null) {
            setTaskView(undefined);
            return () => {
                cancelled = true;
            };
        }

        new ProcessInstanceTaskApiService()
            .getStaffTaskView(item.task.processInstanceId, item.task.id)
            .then((view) => {
                if (cancelled) {
                    return;
                }

                setTaskView(view);
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
                .putStaffTaskView(item.task.processInstanceId, item.task.id, taskView.data, evt.event),
            500,
        )
            .then(async (updatedTaskView) => {
                dispatchProcessAssignedTaskCountRefreshEvent();

                const updatedTask = await new ProcessInstanceTaskApiService().retrieve(item.task.id);

                if (updatedTask.status === ProcessTaskStatus.Running) {
                    setTaskView(updatedTaskView);
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
                                elementData={taskView.data}
                                onElementDataChange={(elementData) => {
                                    setTaskView((currentTaskView) => {
                                        if (currentTaskView == null) {
                                            return currentTaskView;
                                        }

                                        return {
                                            ...currentTaskView,
                                            data: elementData,
                                        };
                                    });
                                }}
                            />
                        </Box>

                        {
                            taskView.events.length > 0 &&
                            <Stack
                                direction={{
                                    xs: 'column',
                                    sm: 'row',
                                }}
                                spacing={2}
                                sx={{
                                    mt: 4,
                                }}
                            >
                                {
                                    taskView.events.map((evt) => (
                                        <Button
                                            key={evt.event}
                                            variant="contained"
                                            onClick={() => {
                                                handleEventClick(evt);
                                            }}
                                        >
                                            {evt.label}
                                        </Button>
                                    ))
                                }
                            </Stack>
                        }
                    </>
            }
        </Box>
    );
}
