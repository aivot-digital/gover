import React, {type ReactNode, useEffect, useMemo, useRef, useState} from 'react';
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
    type TaskViewEventAlignment,
    type TaskViewEventColor,
    type TaskViewEventVariant,
} from '../../services/process-instance-task-api-service';
import {
    AuthoredElementValues,
    DerivedRuntimeElementData,
    isDerivedRuntimeElementData,
} from '../../../../models/element-data';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {clearLoadingMessage, setErrorMessage, setLoadingMessage} from '../../../../slices/shell-slice';
import {showApiErrorSnackbar, showErrorSnackbar} from '../../../../slices/snackbar-slice';
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
import {isApiError} from '../../../../models/api-error';
import {useChangeBlocker} from '../../../../hooks/use-change-blocker-2';
import {
    ProcessTaskInputSaveState,
    ProcessTaskInputSaveStateChip,
} from './components/process-task-input-save-state-chip';

const TASK_INPUT_DATA_PUSH_DELAY_MS = 2000;
const TASK_INPUT_DATA_MIN_SAVE_DURATION_MS = 800;

export function ProcessTaskViewPageEdit(): ReactNode {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const {
        item,
    } = useGenericDetailsPageContext<ProcessTaskDetailsPageItem, undefined>();

    const pushUpdateTimeoutRef = useRef<number | null>(null);
    const saveCycleRef = useRef(0);

    const [taskView, setTaskView] = useState<TaskView>();
    const [taskInputData, setTaskInputData] = useState<AuthoredElementValues>({});
    const [taskInputDataSaveState, setTaskInputDataSaveState] = useState<ProcessTaskInputSaveState>(ProcessTaskInputSaveState.Saved);
    const [lastSavedAt, setLastSavedAt] = useState<Date | null>(null);
    const [derivedErrors, setDerivedErrors] = useState<DerivedRuntimeElementData | null>(null);

    const {
        dialog,
    } = useChangeBlocker({
        original: ProcessTaskInputSaveState.Saved,
        edited: taskInputDataSaveState,
    });

    // Clear
    useEffect(() => {
        return () => {
            if (pushUpdateTimeoutRef.current != null) {
                clearTimeout(pushUpdateTimeoutRef.current);
            }
        };
    }, []);

    useEffect(() => {
        let cancelled = false;

        if (item == null) {
            setTaskView(undefined);
            setTaskInputData({});
            setTaskInputDataSaveState(ProcessTaskInputSaveState.Saved);
            setLastSavedAt(null);
            return () => {
                cancelled = true;
            };
        }

        setTaskView(undefined);
        setTaskInputData({});
        setTaskInputDataSaveState(ProcessTaskInputSaveState.Saved);
        setLastSavedAt(null);
        saveCycleRef.current = 0;

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
                icon: <Task/>,
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

        if (pushUpdateTimeoutRef.current != null) {
            window.clearTimeout(pushUpdateTimeoutRef.current);
            pushUpdateTimeoutRef.current = null;
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
                    setLastSavedAt(new Date());
                    setTaskInputDataSaveState(ProcessTaskInputSaveState.Saved);
                    return;
                }

                setLastSavedAt(new Date());
                setTaskInputDataSaveState(ProcessTaskInputSaveState.Saved);
                setTimeout(() => {
                    navigate('/tasks');
                }, 1);
            })
            .catch((err) => {
                if (isApiError(err) && isDerivedRuntimeElementData(err.details)) {
                    dispatch(showErrorSnackbar(err.message));
                    setDerivedErrors(err.details);
                } else {
                    dispatch(showApiErrorSnackbar(err, 'Die Aufgabe konnte nicht verarbeitet werden.'));
                }
            })
            .finally(() => {
                dispatch(clearLoadingMessage());
            });
    };

    const handleAuthoredValuesChange = (authoredValues: AuthoredElementValues) => {
        if (item == null) {
            return;
        }

        setTaskInputData(authoredValues);
        const currentSaveCycle = ++saveCycleRef.current;

        // Do not save the first
        if (currentSaveCycle <= 1) {
            return;
        }

        if (pushUpdateTimeoutRef.current != null) {
            window.clearTimeout(pushUpdateTimeoutRef.current);
        }

        pushUpdateTimeoutRef.current = window.setTimeout(() => {
            pushUpdateTimeoutRef.current = null;

            setTaskInputDataSaveState(ProcessTaskInputSaveState.Saving);

            withDelay(
                new ProcessInstanceTaskApiService()
                    .putStaffTaskView(item.task.processInstanceId, item.task.id, authoredValues),
                TASK_INPUT_DATA_MIN_SAVE_DURATION_MS,
            )
                .then(() => {
                    setTaskInputDataSaveState(ProcessTaskInputSaveState.Saved);
                    setLastSavedAt(new Date());
                })
                .catch((err) => {
                    dispatch(showApiErrorSnackbar(err, 'Die Eingaben konnten nicht gespeichert werden.'));
                    setTaskInputDataSaveState(ProcessTaskInputSaveState.Failed);
                })
                .finally(() => {
                    if (saveCycleRef.current !== currentSaveCycle) {
                        return;
                    }
                });
        }, TASK_INPUT_DATA_PUSH_DELAY_MS);

        setTaskInputDataSaveState(ProcessTaskInputSaveState.Waiting);
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
                                onAuthoredElementValuesChange={handleAuthoredValuesChange}
                                computedErrors={derivedErrors?.elementStates}
                                doDerive={(aev) => {
                                    if (item == null || item.instance == null || item.task == null) {
                                        return Promise.resolve({
                                            effectiveValues: {},
                                            elementStates: {},
                                        });
                                    }

                                    return new ProcessInstanceTaskApiService()
                                        .deriveStaffTaskView(item.instance.id, item.task.id, aev);
                                }}
                            />
                        </Box>

                        <Stack
                            direction="row"
                            alignItems="flex-end"
                        >
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

                            <ProcessTaskInputSaveStateChip
                                state={taskInputDataSaveState}
                                lastSavedAt={lastSavedAt}
                            />

                        </Stack>
                    </>
            }

            {dialog}
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
