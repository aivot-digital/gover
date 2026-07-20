import React, {type ReactNode, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {Box, Button, Skeleton, Stack, Typography} from '@mui/material';
import {Blocker, useBeforeUnload, useBlocker, useNavigate} from 'react-router-dom';
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
import Task from '@aivot/mui-material-symbols-400-n25-outlined/Task';
import {dispatchProcessAssignedTaskCountRefreshEvent} from '../../utils/process-assigned-task-count-events';
import {isApiError, isApiUnreachableError, isOfflineApiError} from '../../../../models/api-error';
import {
    ProcessTaskInputSaveState,
    ProcessTaskInputSaveStateChip,
} from './components/process-task-input-save-state-chip';
import {deepEquals} from '../../../../utils/equality-utils';
import {ConfirmDialog} from '../../../../dialogs/confirm-dialog/confirm-dialog';
import {AuthService} from '../../../../services/auth-service';
import type {ProcessInstanceAttachmentEntity} from '../../entities/process-instance-attachment-entity';
import type {ProcessInstanceAttachmentSetEntity} from '../../entities/process-instance-attachment-set-entity';
import {ProcessInstanceAttachmentApiService} from '../../services/process-instance-attachment-api-service';
import {ProcessInstanceAttachmentSetApiService} from '../../services/process-instance-attachment-set-api-service';
import {ProcessTaskViewAttachmentProvider} from './process-task-view-attachment-context';
import {BaseApiService} from '../../../../services/base-api-service';

const TASK_INPUT_DATA_PUSH_DELAY_MS = 2000;
const TASK_INPUT_DATA_MIN_SAVE_DURATION_MS = 800;
const NAVIGATION_SAVE_MESSAGE = 'Eingaben werden zwischengespeichert';

export function ProcessTaskViewPageEdit(): ReactNode {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const {
        item,
    } = useGenericDetailsPageContext<ProcessTaskDetailsPageItem, undefined>();

    const pushUpdateTimeoutRef = useRef<number | null>(null);
    const saveCycleRef = useRef(0);
    const taskSessionRef = useRef(0);
    const inFlightSavePromiseRef = useRef<Promise<boolean> | null>(null);
    const latestTaskInputDataRef = useRef<AuthoredElementValues>({});
    const lastPersistedTaskInputDataRef = useRef<AuthoredElementValues>({});
    const pendingOfflineRetryRef = useRef(false);
    const isResolvingBlockedNavigationRef = useRef(false);
    const ignoreAuthoredValuesChangeRef = useRef(false);
    const skipChangeBlockerRef = useRef(false);

    const [taskView, setTaskView] = useState<TaskView>();
    const [taskInputData, setTaskInputData] = useState<AuthoredElementValues>({});
    const [lastPersistedTaskInputData, setLastPersistedTaskInputData] = useState<AuthoredElementValues>({});
    const [taskInputDataSaveState, setTaskInputDataSaveState] = useState<ProcessTaskInputSaveState>(ProcessTaskInputSaveState.Saved);
    const [lastSavedAt, setLastSavedAt] = useState<Date | null>(null);
    const [derivedErrors, setDerivedErrors] = useState<DerivedRuntimeElementData | null>(null);
    const [pendingBlockedNavigation, setPendingBlockedNavigation] = useState<Blocker | null>(null);
    const [taskAttachments, setTaskAttachments] = useState<ProcessInstanceAttachmentEntity[]>([]);
    const [taskAttachmentSets, setTaskAttachmentSets] = useState<ProcessInstanceAttachmentSetEntity[]>([]);
    const [isLoadingTaskAttachments, setIsLoadingTaskAttachments] = useState(false);

    const hasUnsavedChanges = useMemo(() => {
        return !deepEquals(taskInputData, lastPersistedTaskInputData);
    }, [lastPersistedTaskInputData, taskInputData]);

    const hasPendingUnloadChanges = useCallback(() => {
        if (skipChangeBlockerRef.current || !AuthService.isAuthenticated()) {
            return false;
        }

        return !deepEquals(latestTaskInputDataRef.current, lastPersistedTaskInputDataRef.current);
    }, []);

    useBeforeUnload(useCallback((event: BeforeUnloadEvent) => {
        if (!hasPendingUnloadChanges()) {
            return;
        }

        event.preventDefault();
        event.returnValue = '';
    }, [hasPendingUnloadChanges]));

    const saveTaskInputData = useCallback(async (payload: AuthoredElementValues): Promise<boolean> => {
        if (item == null) {
            return false;
        }

        if (deepEquals(payload, lastPersistedTaskInputDataRef.current)) {
            const hasNewerUnsavedChanges = !deepEquals(latestTaskInputDataRef.current, lastPersistedTaskInputDataRef.current);
            setTaskInputDataSaveState(hasNewerUnsavedChanges ? ProcessTaskInputSaveState.Waiting : ProcessTaskInputSaveState.Saved);
            return true;
        }

        if (inFlightSavePromiseRef.current != null) {
            await inFlightSavePromiseRef.current;

            if (deepEquals(payload, lastPersistedTaskInputDataRef.current)) {
                return true;
            }
        }

        if (typeof navigator !== 'undefined' && navigator.onLine === false) {
            pendingOfflineRetryRef.current = true;
            setTaskInputDataSaveState(ProcessTaskInputSaveState.RetryQueued);
            return false;
        }

        const currentTaskSession = taskSessionRef.current;
        setTaskInputDataSaveState(ProcessTaskInputSaveState.Saving);

        const savePromise = withDelay(
            new ProcessInstanceTaskApiService()
                .putStaffTaskView(item.task.processInstanceId, item.task.id, payload),
            TASK_INPUT_DATA_MIN_SAVE_DURATION_MS,
        )
            .then(() => {
                if (currentTaskSession !== taskSessionRef.current) {
                    return false;
                }

                pendingOfflineRetryRef.current = false;
                lastPersistedTaskInputDataRef.current = payload;
                setLastPersistedTaskInputData(payload);
                setLastSavedAt(new Date());

                const hasNewerUnsavedChanges = !deepEquals(latestTaskInputDataRef.current, payload);
                setTaskInputDataSaveState(hasNewerUnsavedChanges ? ProcessTaskInputSaveState.Waiting : ProcessTaskInputSaveState.Saved);

                return true;
            })
            .catch((err) => {
                if (currentTaskSession !== taskSessionRef.current) {
                    return false;
                }

                if (isOfflineApiError(err)) {
                    pendingOfflineRetryRef.current = true;
                    setTaskInputDataSaveState(ProcessTaskInputSaveState.RetryQueued);
                    return false;
                }

                pendingOfflineRetryRef.current = false;
                if (!isApiUnreachableError(err)) {
                    dispatch(showApiErrorSnackbar(err, 'Die Eingaben konnten nicht gespeichert werden.'));
                }
                setTaskInputDataSaveState(ProcessTaskInputSaveState.Failed);

                return false;
            })
            .finally(() => {
                if (currentTaskSession === taskSessionRef.current) {
                    inFlightSavePromiseRef.current = null;
                }
            });

        inFlightSavePromiseRef.current = savePromise;

        return await savePromise;
    }, [dispatch, item]);

    const flushCurrentTaskInputData = useCallback(async (): Promise<boolean> => {
        if (pushUpdateTimeoutRef.current != null) {
            window.clearTimeout(pushUpdateTimeoutRef.current);
            pushUpdateTimeoutRef.current = null;
        }

        while (!deepEquals(latestTaskInputDataRef.current, lastPersistedTaskInputDataRef.current)) {
            if (typeof navigator !== 'undefined' && navigator.onLine === false) {
                pendingOfflineRetryRef.current = true;
                setTaskInputDataSaveState(ProcessTaskInputSaveState.RetryQueued);
                return false;
            }

            const success = await saveTaskInputData(latestTaskInputDataRef.current);
            if (!success) {
                return false;
            }
        }

        setTaskInputDataSaveState(ProcessTaskInputSaveState.Saved);
        return true;
    }, [saveTaskInputData]);

    // Clear
    useEffect(() => {
        return () => {
            taskSessionRef.current += 1;
            ignoreAuthoredValuesChangeRef.current = false;
            skipChangeBlockerRef.current = false;
            if (pushUpdateTimeoutRef.current != null) {
                clearTimeout(pushUpdateTimeoutRef.current);
            }
        };
    }, []);

    useEffect(() => {
        let cancelled = false;
        const currentTaskSession = ++taskSessionRef.current;

        pendingOfflineRetryRef.current = false;
        inFlightSavePromiseRef.current = null;
        isResolvingBlockedNavigationRef.current = false;
        ignoreAuthoredValuesChangeRef.current = false;
        skipChangeBlockerRef.current = false;
        setPendingBlockedNavigation(null);
        setDerivedErrors(null);
        if (pushUpdateTimeoutRef.current != null) {
            window.clearTimeout(pushUpdateTimeoutRef.current);
            pushUpdateTimeoutRef.current = null;
        }

        if (item == null) {
            setTaskView(undefined);
            setTaskInputData({});
            setLastPersistedTaskInputData({});
            setTaskInputDataSaveState(ProcessTaskInputSaveState.Saved);
            setLastSavedAt(null);
            latestTaskInputDataRef.current = {};
            lastPersistedTaskInputDataRef.current = {};
            return () => {
                cancelled = true;
            };
        }

        setTaskView(undefined);
        setTaskInputData({});
        setLastPersistedTaskInputData({});
        setTaskInputDataSaveState(ProcessTaskInputSaveState.Saved);
        setLastSavedAt(null);
        saveCycleRef.current = 0;
        latestTaskInputDataRef.current = {};
        lastPersistedTaskInputDataRef.current = {};

        new ProcessInstanceTaskApiService()
            .getStaffTaskView(item.task.processInstanceId, item.task.id)
            .then((view) => {
                if (cancelled || currentTaskSession !== taskSessionRef.current) {
                    return;
                }

                setTaskView(view);
                setTaskInputData(view.data);
                setLastPersistedTaskInputData(view.data);
                latestTaskInputDataRef.current = view.data;
                lastPersistedTaskInputDataRef.current = view.data;
            })
            .catch((err) => {
                if (cancelled || currentTaskSession !== taskSessionRef.current) {
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

    useEffect(() => {
        const handleOnline = () => {
            if (!pendingOfflineRetryRef.current) {
                return;
            }

            void flushCurrentTaskInputData();
        };

        window.addEventListener('online', handleOnline);

        return () => {
            window.removeEventListener('online', handleOnline);
        };
    }, [flushCurrentTaskInputData]);

    useEffect(() => {
        let cancelled = false;

        if (item?.instance == null) {
            setTaskAttachments([]);
            setTaskAttachmentSets([]);
            setIsLoadingTaskAttachments(false);
            return () => {
                cancelled = true;
            };
        }

        setTaskAttachments([]);
        setTaskAttachmentSets([]);
        setIsLoadingTaskAttachments(true);

        Promise.all([
            new ProcessInstanceAttachmentApiService().listAll({
                processInstanceId: item.instance.id,
            }),
            new ProcessInstanceAttachmentSetApiService().listAll({
                processInstanceId: item.instance.id,
            }),
        ])
            .then(([attachmentPage, attachmentSetPage]) => {
                if (cancelled) {
                    return;
                }

                setTaskAttachments(attachmentPage.content);
                setTaskAttachmentSets(attachmentSetPage.content);
            })
            .catch((err) => {
                if (cancelled) {
                    return;
                }

                dispatch(showApiErrorSnackbar(err, 'Die Vorgangsanhänge konnten nicht geladen werden.'));
                setTaskAttachments([]);
                setTaskAttachmentSets([]);
            })
            .finally(() => {
                if (cancelled) {
                    return;
                }

                setIsLoadingTaskAttachments(false);
            });

        return () => {
            cancelled = true;
        };
    }, [dispatch, item?.instance?.id]);

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

    const handleDownloadAttachment = useCallback(async (attachment: ProcessInstanceAttachmentEntity): Promise<void> => {
        try {
            const blob = await new BaseApiService().getBlob(`/api/process-instance-attachments/${encodeURIComponent(attachment.key)}/file/?download=true`);
            const objectUrl = URL.createObjectURL(blob);

            const link = document.createElement('a');
            link.href = objectUrl;
            link.download = attachment.fileName;
            link.style.display = 'none';

            document.body.appendChild(link);
            link.click();
            link.remove();

            URL.revokeObjectURL(objectUrl);
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Der Anhang konnte nicht heruntergeladen werden.'));
        }
    }, [dispatch]);

    const handleViewAttachment = useCallback(async (attachment: ProcessInstanceAttachmentEntity): Promise<void> => {
        const previewWindow = window.open('', '_blank');
        if (previewWindow == null) {
            dispatch(showErrorSnackbar('Der Anhang konnte nicht geöffnet werden. Bitte erlauben Sie Pop-ups für diese Seite.'));
            return;
        }

        previewWindow.opener = null;
        previewWindow.document.title = attachment.fileName;
        previewWindow.document.body.textContent = 'Anhang wird geladen...';

        try {
            const blob = await new BaseApiService().getBlob(`/api/process-instance-attachments/${encodeURIComponent(attachment.key)}/file/?download=false`);
            const objectUrl = URL.createObjectURL(blob);
            previewWindow.location.replace(objectUrl);

            window.setTimeout(() => {
                URL.revokeObjectURL(objectUrl);
            }, 60_000);
        } catch (error) {
            previewWindow.close();
            dispatch(showApiErrorSnackbar(error, 'Der Anhang konnte nicht angezeigt werden.'));
        }
    }, [dispatch]);

    const taskViewAttachmentContextValue = useMemo(() => ({
        attachments: taskAttachments,
        attachmentSets: taskAttachmentSets,
        isLoadingAttachments: isLoadingTaskAttachments,
        viewAttachment: handleViewAttachment,
        downloadAttachment: handleDownloadAttachment,
    }), [handleDownloadAttachment, handleViewAttachment, isLoadingTaskAttachments, taskAttachmentSets, taskAttachments]);

    const handleEventClick = async (evt: TaskViewEvent) => {
        if (item == null || taskView == null) {
            return;
        }

        let shouldNavigateAway = false;
        ignoreAuthoredValuesChangeRef.current = true;

        if (pushUpdateTimeoutRef.current != null) {
            window.clearTimeout(pushUpdateTimeoutRef.current);
            pushUpdateTimeoutRef.current = null;
        }

        if (inFlightSavePromiseRef.current != null) {
            await inFlightSavePromiseRef.current;
        }

        dispatch(setLoadingMessage({
            message: `Verarbeite Aktion: ${evt.label}`,
            blocking: true,
            estimatedTime: 500,
        }));

        const eventPayload = latestTaskInputDataRef.current;

        withDelay(
            new ProcessInstanceTaskApiService()
                .putStaffTaskView(item.task.processInstanceId, item.task.id, eventPayload, evt.event),
            500,
        )
            .then(async (updatedTaskView) => {
                dispatchProcessAssignedTaskCountRefreshEvent();

                const updatedTask = await new ProcessInstanceTaskApiService().retrieve(item.task.id);

                if (updatedTask.status === ProcessTaskStatus.Running) {
                    setTaskView(updatedTaskView);
                    setTaskInputData(updatedTaskView.data);
                    setLastPersistedTaskInputData(updatedTaskView.data);
                    latestTaskInputDataRef.current = updatedTaskView.data;
                    lastPersistedTaskInputDataRef.current = updatedTaskView.data;
                    setLastSavedAt(new Date());
                    setTaskInputDataSaveState(ProcessTaskInputSaveState.Saved);
                    return;
                }

                setLastSavedAt(new Date());
                setTaskInputDataSaveState(ProcessTaskInputSaveState.Saved);
                shouldNavigateAway = true;
                skipChangeBlockerRef.current = true;
                setPendingBlockedNavigation(null);
                setTimeout(() => {
                    navigate('/tasks');
                }, 1);
            })
            .catch((err) => {
                if (isApiError(err) && isDerivedRuntimeElementData(err.details)) {
                    dispatch(showErrorSnackbar(err.message));
                    setDerivedErrors(err.details);
                } else if (!isApiUnreachableError(err)) {
                    dispatch(showApiErrorSnackbar(err, 'Die Aufgabe konnte nicht verarbeitet werden.'));
                }
            })
            .finally(() => {
                ignoreAuthoredValuesChangeRef.current = false;
                if (!shouldNavigateAway) {
                    skipChangeBlockerRef.current = false;
                }
                dispatch(clearLoadingMessage());
            });
    };

    const handleAuthoredValuesChange = (authoredValues: AuthoredElementValues) => {
        if (item == null) {
            return;
        }

        if (ignoreAuthoredValuesChangeRef.current) {
            return;
        }

        latestTaskInputDataRef.current = authoredValues;
        setTaskInputData(authoredValues);
        const currentSaveCycle = ++saveCycleRef.current;

        // Do not save the first
        if (currentSaveCycle <= 1) {
            return;
        }

        if (pushUpdateTimeoutRef.current != null) {
            window.clearTimeout(pushUpdateTimeoutRef.current);
            pushUpdateTimeoutRef.current = null;
        }

        if (typeof navigator !== 'undefined' && navigator.onLine === false) {
            pendingOfflineRetryRef.current = true;
            setTaskInputDataSaveState(ProcessTaskInputSaveState.RetryQueued);
            return;
        }

        pushUpdateTimeoutRef.current = window.setTimeout(() => {
            pushUpdateTimeoutRef.current = null;

            void saveTaskInputData(authoredValues).finally(() => {
                if (saveCycleRef.current !== currentSaveCycle) {
                    return;
                }
            });
        }, TASK_INPUT_DATA_PUSH_DELAY_MS);

        setTaskInputDataSaveState(ProcessTaskInputSaveState.Waiting);
    };

    const blocker = useBlocker(({currentLocation, nextLocation}) => {
        if (skipChangeBlockerRef.current) {
            return false;
        }

        if (currentLocation.pathname === nextLocation.pathname &&
            currentLocation.search === nextLocation.search) {
            return false;
        }

        return hasUnsavedChanges;
    });

    const shouldAutoResolveBlockedNavigation = useMemo(() => {
        const canRetryNow = taskInputDataSaveState === ProcessTaskInputSaveState.RetryQueued &&
            (typeof navigator === 'undefined' || navigator.onLine);

        return (
            taskInputDataSaveState === ProcessTaskInputSaveState.Waiting ||
            taskInputDataSaveState === ProcessTaskInputSaveState.Saving ||
            canRetryNow ||
            pushUpdateTimeoutRef.current != null ||
            inFlightSavePromiseRef.current != null
        );
    }, [taskInputDataSaveState]);

    const handleBlockedNavigation = useCallback(async (blockedNavigation: Blocker) => {
        if (item == null) {
            return;
        }

        isResolvingBlockedNavigationRef.current = true;
        setPendingBlockedNavigation(null);

        dispatch(setLoadingMessage({
            message: NAVIGATION_SAVE_MESSAGE,
            blocking: true,
            estimatedTime: TASK_INPUT_DATA_MIN_SAVE_DURATION_MS,
        }));

        const shouldProceed = await flushCurrentTaskInputData();

        dispatch(clearLoadingMessage());
        isResolvingBlockedNavigationRef.current = false;

        if (shouldProceed && deepEquals(latestTaskInputDataRef.current, lastPersistedTaskInputDataRef.current)) {
            blockedNavigation.proceed?.();
            return;
        }

        setPendingBlockedNavigation(blockedNavigation);
    }, [dispatch, flushCurrentTaskInputData, item]);

    useEffect(() => {
        if (blocker.state !== 'blocked' || isResolvingBlockedNavigationRef.current) {
            return;
        }

        if (shouldAutoResolveBlockedNavigation) {
            void handleBlockedNavigation(blocker);
            return;
        }

        setPendingBlockedNavigation(blocker);
    }, [blocker, handleBlockedNavigation, shouldAutoResolveBlockedNavigation]);

    const handleConfirmBlockedNavigation = useCallback(() => {
        pendingBlockedNavigation?.proceed?.();
        setPendingBlockedNavigation(null);
    }, [pendingBlockedNavigation]);

    const handleCancelBlockedNavigation = useCallback(() => {
        pendingBlockedNavigation?.reset?.();
        setPendingBlockedNavigation(null);
    }, [pendingBlockedNavigation]);

    const blockedNavigationMessage = useMemo(() => {
        if (taskInputDataSaveState === ProcessTaskInputSaveState.RetryQueued) {
            return 'Ihre Eingaben konnten noch nicht zwischengespeichert werden, weil die Verbindung unterbrochen ist. Wenn Sie die Seite jetzt verlassen, gehen diese Änderungen verloren.';
        }

        if (taskInputDataSaveState === ProcessTaskInputSaveState.Failed) {
            return 'Ihre Eingaben konnten nicht zwischengespeichert werden. Wenn Sie die Seite jetzt verlassen, gehen diese Änderungen verloren.';
        }

        return 'Sie haben ungespeicherte Eingaben. Wenn Sie die Seite jetzt verlassen, gehen diese Änderungen verloren.';
    }, [taskInputDataSaveState]);

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
                    <ProcessTaskViewAttachmentProvider
                        value={taskViewAttachmentContextValue}
                    >
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
                                onDeriveOverride={(aev, skipErrorsForElements) => {
                                    if (item == null || item.instance == null || item.task == null) {
                                        return Promise.resolve({
                                            effectiveValues: {},
                                            elementStates: {},
                                        });
                                    }

                                    return new ProcessInstanceTaskApiService()
                                        .deriveStaffTaskView(item.instance.id, item.task.id, aev, skipErrorsForElements);
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
                    </ProcessTaskViewAttachmentProvider>
            }

            {
                pendingBlockedNavigation != null &&
                <ConfirmDialog
                    title="Ungespeicherte Eingaben"
                    onConfirm={handleConfirmBlockedNavigation}
                    onCancel={handleCancelBlockedNavigation}
                    confirmButtonText="Seite verlassen"
                >
                    {blockedNavigationMessage}
                </ConfirmDialog>
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
