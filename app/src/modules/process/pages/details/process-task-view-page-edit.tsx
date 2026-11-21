import React, {type ReactNode, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {Alert, Box, Stack, Typography} from '@mui/material';
import {Blocker, useBeforeUnload, useBlocker, useNavigate} from 'react-router-dom';
import {StatusTable} from '../../../../components/status-table/status-table';
import {type StatusTablePropsItem} from '../../../../components/status-table/status-table-props';
import {useGenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {ElementDerivationContext} from '../../../elements/components/element-derivation-context';
import {
    ProcessInstanceTaskApiService,
    type TaskView,
    type TaskViewEvent,
} from '../../services/process-instance-task-api-service';
import {
    AuthoredElementValues,
    DerivedRuntimeElementData,
    isDerivedRuntimeElementData,
} from '../../../../models/element-data';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {clearLoadingMessage, setErrorMessage, setLoadingMessage} from '../../../../slices/shell-slice';
import {showApiErrorSnackbar, showErrorSnackbar, showWarningSnackbar} from '../../../../slices/snackbar-slice';
import {withDelay} from '../../../../utils/with-delay';
import {isProcessTaskEditableByStaff, ProcessTaskStatus} from '../../enums/process-task-status';
import {
    getProcessTaskDescription,
    getProcessTaskName,
    getProcessTaskNodeIcon,
    PROCESS_TASK_NOT_EDITABLE_MESSAGE,
    type ProcessTaskDetailsPageItem,
} from './process-task-view-page';
import Task from '@aivot/mui-material-symbols-400-n25-outlined/Task';
import {dispatchProcessAssignedTaskCountRefreshEvent} from '../../utils/process-assigned-task-count-events';
import {getPreviewableAttachmentMediaType} from '../../utils/process-attachment-preview';
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
import {TaskViewEventButtons} from '../../components/task-view-event-buttons';
import {isTaskDeadlineExceededError} from '../../utils/task-deadline-error';
import {
    collectStoredTaskViewFileUploadItems,
    collectTaskViewFileUploads,
    haveTaskViewFileUploadItemsChanged,
    replaceStoredTaskViewFileUploadItems,
} from '../../utils/task-view-file-uploads';
import type {FileUploadElementItem} from '../../../../models/elements/form/input/file-upload-element';
import {RemovableFileUploadAttachmentsProvider} from '../../../../components/file-upload-field/removable-file-upload-attachments-context';
import {
    clearRecipient,
    getRecipientLabel,
    getRecipientOptions,
    getRejectedRecipient,
    getSelectedRecipient,
    hasRecipientOption,
    MANUAL_ASSIGNMENT_PROVIDER,
    RECIPIENT_FIELD,
    removeRecipientOption,
    unavailableRecipientMessage,
} from '../../utils/manual-assignment-recipient';

const TASK_INPUT_DATA_PUSH_DELAY_MS = 2000;
const TASK_INPUT_DATA_MIN_SAVE_DURATION_MS = 800;
const NAVIGATION_SAVE_MESSAGE = 'Eingaben werden zwischengespeichert';

export function ProcessTaskViewPageEdit(): ReactNode {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const {item, refresh} = useGenericDetailsPageContext<ProcessTaskDetailsPageItem, undefined>();

    const pushUpdateTimeoutRef = useRef<number | null>(null);
    const taskSessionRef = useRef(0);
    const recipientRefreshRef = useRef(0);
    const rejectedRecipientRef = useRef<string | null>(null);
    const inFlightSavePromiseRef = useRef<Promise<boolean> | null>(null);
    const latestTaskInputDataRef = useRef<AuthoredElementValues>({});
    const lastPersistedTaskInputDataRef = useRef<AuthoredElementValues>({});
    const pendingOfflineRetryRef = useRef(false);
    const isResolvingBlockedNavigationRef = useRef(false);
    const [submittingEvent, setSubmittingEvent] = useState(false);
    const skipChangeBlockerRef = useRef(false);
    const deadlineExceededRef = useRef(false);
    const [deadlineExceeded, setDeadlineExceeded] = useState(false);

    const [taskView, setTaskView] = useState<TaskView>();
    const [taskInputData, setTaskInputData] = useState<AuthoredElementValues>({});
    const [lastPersistedTaskInputData, setLastPersistedTaskInputData] = useState<AuthoredElementValues>({});
    const [taskInputDataSaveState, setTaskInputDataSaveState] = useState<ProcessTaskInputSaveState>(
        ProcessTaskInputSaveState.Saved,
    );
    const [lastSavedAt, setLastSavedAt] = useState<Date | null>(null);
    const [derivedErrors, setDerivedErrors] = useState<DerivedRuntimeElementData | null>(null);
    const [unavailableRecipient, setUnavailableRecipient] = useState<{name?: string} | null>(null);
    const manualAssignment = item?.provider?.key === MANUAL_ASSIGNMENT_PROVIDER;
    const computedErrors = useMemo(() => {
        if (unavailableRecipient == null || taskView == null) return derivedErrors?.elementStates;
        return {
            ...derivedErrors?.elementStates,
            [RECIPIENT_FIELD]: {error: unavailableRecipientMessage(unavailableRecipient.name, getRecipientOptions(taskView).length > 0)},
        };
    }, [derivedErrors, taskView, unavailableRecipient]);
    const [pendingBlockedNavigation, setPendingBlockedNavigation] = useState<Blocker | null>(null);
    const [taskAttachments, setTaskAttachments] = useState<ProcessInstanceAttachmentEntity[]>([]);
    const [taskAttachmentSets, setTaskAttachmentSets] = useState<ProcessInstanceAttachmentSetEntity[]>([]);
    const [isLoadingTaskAttachments, setIsLoadingTaskAttachments] = useState(false);
    const [taskAttachmentsReloadCount, setTaskAttachmentsReloadCount] = useState(0);
    const loadedTaskAttachmentsInstanceIdRef = useRef<number | null>(null);
    // Blob URIs are unique, so the stored item of every file that a save of this page sent replaces its transient item.
    const storedFileItemsRef = useRef(new Map<string, FileUploadElementItem>());
    const [removableAttachmentKeys, setRemovableAttachmentKeys] = useState<ReadonlySet<string>>(new Set());

    const hasUnsavedChanges = useMemo(() => {
        return !deepEquals(taskInputData, lastPersistedTaskInputData);
    }, [lastPersistedTaskInputData, taskInputData]);

    const hasUnsavedUserInput = useCallback(() => {
        const latest = latestTaskInputDataRef.current;
        const persisted = lastPersistedTaskInputDataRef.current;
        if (deepEquals(latest, persisted)) return false;
        // An unavailable saved choice will be cleaned again on reopen. Other draft edits still need protection.
        return rejectedRecipientRef.current == null || getSelectedRecipient(persisted) !== rejectedRecipientRef.current ||
            !deepEquals(latest, clearRecipient(persisted));
    }, []);

    const hasPendingUnloadChanges = useCallback(() => {
        if (skipChangeBlockerRef.current || !AuthService.isAuthenticated()) {
            return false;
        }

        return hasUnsavedUserInput();
    }, [hasUnsavedUserInput]);

    useBeforeUnload(
        useCallback(
            (event: BeforeUnloadEvent) => {
                if (!hasPendingUnloadChanges()) {
                    return;
                }

                event.preventDefault();
                event.returnValue = '';
            },
            [hasPendingUnloadChanges],
        ),
    );

    const handleDeadlineExceeded = useCallback((error: unknown): boolean => {
        if (!isTaskDeadlineExceededError(error)) return false;
        deadlineExceededRef.current = true;
        setDeadlineExceeded(true);
        pendingOfflineRetryRef.current = false;
        skipChangeBlockerRef.current = true;
        setPendingBlockedNavigation(null);
        if (pushUpdateTimeoutRef.current != null) {
            window.clearTimeout(pushUpdateTimeoutRef.current);
            pushUpdateTimeoutRef.current = null;
        }
        dispatchProcessAssignedTaskCountRefreshEvent();
        refresh();
        return true;
    }, [refresh]);

    const collectFileUploads = useCallback((values: AuthoredElementValues): Promise<FileUploadElementItem[]> => {
        if (item == null || taskView == null) {
            return Promise.resolve([]);
        }

        return collectTaskViewFileUploads(taskView.layout, values, (derivableValues) => (
            new ProcessInstanceTaskApiService().deriveStaffTaskView(item.task.processInstanceId, item.task.id, derivableValues)
        ));
    }, [item, taskView]);

    const clearUnavailableRecipient = useCallback((recipient: string, previousView: TaskView) => {
        if (getSelectedRecipient(latestTaskInputDataRef.current) !== recipient) return false;
        const cleaned = clearRecipient(latestTaskInputDataRef.current);
        rejectedRecipientRef.current = recipient;
        latestTaskInputDataRef.current = cleaned;
        setTaskInputData(cleaned);
        setUnavailableRecipient({name: getRecipientLabel(previousView, recipient)});
        setTaskInputDataSaveState(deepEquals(cleaned, lastPersistedTaskInputDataRef.current)
            ? ProcessTaskInputSaveState.Saved : ProcessTaskInputSaveState.Waiting);
        return true;
    }, []);

    const handleUnavailableRecipient = (
        details: unknown, submitted: AuthoredElementValues, session: number,
    ): boolean => {
        const rejected = getRejectedRecipient(details);
        if (!manualAssignment || taskView == null || item == null || rejected == null ||
            rejected !== getSelectedRecipient(submitted)) return false;
        if (session !== taskSessionRef.current || !clearUnavailableRecipient(rejected, taskView)) return true;

        // Use the previously displayed label for the error and the latest authorized list for the next choice.
        setTaskView(current => current && removeRecipientOption(current, rejected));
        const request = ++recipientRefreshRef.current;
        void new ProcessInstanceTaskApiService().getStaffTaskView(item.task.processInstanceId, item.task.id)
            .then(freshView => {
                if (session === taskSessionRef.current && request === recipientRefreshRef.current) setTaskView(freshView);
            })
            .catch(error => {
                if (session === taskSessionRef.current && request === recipientRefreshRef.current) {
                    dispatch(showApiErrorSnackbar(error, 'Die Aufgabenansicht konnte nicht geladen werden.'));
                }
            });
        return true;
    };

    // Saves and events queued during a save still hold the transient items of the files that it stored.
    const adoptStoredFileItems = useCallback((values: AuthoredElementValues): AuthoredElementValues => {
        if (taskView == null) {
            return values;
        }

        return replaceStoredTaskViewFileUploadItems(taskView.layout, values, storedFileItemsRef.current);
    }, [taskView]);

    // Each update stores the files it sends, so later updates must reference the stored attachments instead of
    // sending the files again. Inputs changed during the update keep their other values.
    const adoptSavedTaskView = useCallback((payload: AuthoredElementValues, updatedTaskView: TaskView, recipientRevision?: number): AuthoredElementValues => {
        let persistedPayload = payload;
        if (taskView != null) {
            const storedFileItems = collectStoredTaskViewFileUploadItems(taskView.layout, payload, updatedTaskView.data);
            storedFileItems.forEach((storedItem, uri) => storedFileItemsRef.current.set(uri, storedItem));
            if (storedFileItems.size > 0) {
                persistedPayload = replaceStoredTaskViewFileUploadItems(taskView.layout, payload, storedFileItems);
                latestTaskInputDataRef.current = replaceStoredTaskViewFileUploadItems(
                    taskView.layout,
                    latestTaskInputDataRef.current,
                    storedFileItems,
                );
                setTaskInputData(latestTaskInputDataRef.current);
            }

            // Stored and removed files change the attachments of the instance, e.g. for attachment displays.
            if (haveTaskViewFileUploadItemsChanged(taskView.layout, lastPersistedTaskInputDataRef.current, persistedPayload)) {
                setTaskAttachmentsReloadCount((count) => count + 1);
            }

            if (manualAssignment && recipientRevision != null) {
                // This node sanitizes a revoked recipient on autosave. The returned draft is authoritative;
                // a newer local selection may already have replaced the one that the server removed.
                const submitted = getSelectedRecipient(payload);
                persistedPayload = updatedTaskView.data;
                if (recipientRevision === recipientRefreshRef.current) {
                    if (submitted != null && (getSelectedRecipient(persistedPayload) == null || !hasRecipientOption(updatedTaskView, submitted))) {
                        clearUnavailableRecipient(submitted, taskView);
                    }
                    recipientRefreshRef.current++;
                    // An unchanged layout would otherwise re-derive and briefly disable the form after every save.
                    setTaskView(current => current && deepEquals(current.layout, updatedTaskView.layout)
                        ? {...updatedTaskView, layout: current.layout} : updatedTaskView);
                }
            }
        }

        setRemovableAttachmentKeys(new Set(updatedTaskView.removableAttachmentKeys));
        lastPersistedTaskInputDataRef.current = persistedPayload;
        setLastPersistedTaskInputData(persistedPayload);

        return persistedPayload;
    }, [clearUnavailableRecipient, manualAssignment, taskView]);

    const saveTaskInputData = useCallback(
        async (requestedPayload: AuthoredElementValues): Promise<boolean> => {
            if (item == null || deadlineExceededRef.current) {
                return false;
            }
            const currentTaskSession = taskSessionRef.current;
            let payload = adoptStoredFileItems(requestedPayload);

            if (deepEquals(payload, lastPersistedTaskInputDataRef.current)) {
                const hasNewerUnsavedChanges = !deepEquals(
                    latestTaskInputDataRef.current,
                    lastPersistedTaskInputDataRef.current,
                );
                setTaskInputDataSaveState(
                    hasNewerUnsavedChanges ? ProcessTaskInputSaveState.Waiting : ProcessTaskInputSaveState.Saved,
                );
                return true;
            }

            if (inFlightSavePromiseRef.current != null) {
                await inFlightSavePromiseRef.current;

                if (deadlineExceededRef.current || currentTaskSession !== taskSessionRef.current) return false;

                // The preceding save may have sanitized the selected recipient while this save waited.
                payload = adoptStoredFileItems(manualAssignment ? latestTaskInputDataRef.current : payload);
                if (deepEquals(payload, lastPersistedTaskInputDataRef.current)) {
                    return true;
                }
            }

            if (typeof navigator !== 'undefined' && navigator.onLine === false) {
                pendingOfflineRetryRef.current = true;
                setTaskInputDataSaveState(ProcessTaskInputSaveState.RetryQueued);
                return false;
            }

            const recipientRevision = recipientRefreshRef.current;
            setTaskInputDataSaveState(ProcessTaskInputSaveState.Saving);

            const savePromise = withDelay(
                collectFileUploads(payload).then((files) => new ProcessInstanceTaskApiService().putStaffTaskView(
                    item.task.processInstanceId,
                    item.task.id,
                    payload,
                    files,
                )),
                TASK_INPUT_DATA_MIN_SAVE_DURATION_MS,
            )
                .then((updatedTaskView) => {
                    if (currentTaskSession !== taskSessionRef.current) {
                        return false;
                    }

                    const persistedPayload = adoptSavedTaskView(payload, updatedTaskView, recipientRevision);
                    pendingOfflineRetryRef.current = false;
                    setLastSavedAt(new Date());

                    const hasNewerUnsavedChanges = !deepEquals(latestTaskInputDataRef.current, persistedPayload);
                    setTaskInputDataSaveState(
                        hasNewerUnsavedChanges ? ProcessTaskInputSaveState.Waiting : ProcessTaskInputSaveState.Saved,
                    );
                    refresh();

                    return true;
                })
                .catch((err) => {
                    if (currentTaskSession !== taskSessionRef.current) {
                        return false;
                    }

                    if (handleDeadlineExceeded(err)) return false;

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
        },
        [adoptSavedTaskView, adoptStoredFileItems, collectFileUploads, dispatch, handleDeadlineExceeded, item, manualAssignment, refresh],
    );

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

    useEffect(() => {
        if (!hasUnsavedChanges || taskInputDataSaveState !== ProcessTaskInputSaveState.Waiting || submittingEvent) return;
        if (!navigator.onLine) {
            pendingOfflineRetryRef.current = true;
            setTaskInputDataSaveState(ProcessTaskInputSaveState.RetryQueued);
            return;
        }
        const timer = window.setTimeout(() => {
            pushUpdateTimeoutRef.current = null;
            void saveTaskInputData(latestTaskInputDataRef.current);
        }, TASK_INPUT_DATA_PUSH_DELAY_MS);
        pushUpdateTimeoutRef.current = timer;
        return () => {
            window.clearTimeout(timer);
            if (pushUpdateTimeoutRef.current === timer) pushUpdateTimeoutRef.current = null;
        };
    }, [hasUnsavedChanges, saveTaskInputData, submittingEvent, taskInputData, taskInputDataSaveState]);

    // Clear
    useEffect(() => {
        return () => {
            taskSessionRef.current += 1;
            skipChangeBlockerRef.current = false;
            if (pushUpdateTimeoutRef.current != null) {
                clearTimeout(pushUpdateTimeoutRef.current);
            }
        };
    }, []);

    useEffect(() => {
        let cancelled = false;
        const currentTaskSession = ++taskSessionRef.current;

        deadlineExceededRef.current = false;
        setDeadlineExceeded(false);

        pendingOfflineRetryRef.current = false;
        inFlightSavePromiseRef.current = null;
        storedFileItemsRef.current = new Map();
        isResolvingBlockedNavigationRef.current = false;
        setSubmittingEvent(false);
        skipChangeBlockerRef.current = false;
        setPendingBlockedNavigation(null);
        setDerivedErrors(null);
        rejectedRecipientRef.current = null;
        setUnavailableRecipient(null);
        if (pushUpdateTimeoutRef.current != null) {
            window.clearTimeout(pushUpdateTimeoutRef.current);
            pushUpdateTimeoutRef.current = null;
        }

        if (item?.task.status === ProcessTaskStatus.DeadlineExceeded) {
            deadlineExceededRef.current = true;
            skipChangeBlockerRef.current = true;
            return () => { cancelled = true; };
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
        latestTaskInputDataRef.current = {};
        lastPersistedTaskInputDataRef.current = {};

        // The route remains reachable by URL, but the backend rejects staff task views outside staff statuses.
        if (!isProcessTaskEditableByStaff(item.task.status)) {
            skipChangeBlockerRef.current = true;
            return () => {
                cancelled = true;
            };
        }

        new ProcessInstanceTaskApiService()
            .getStaffTaskView(item.task.processInstanceId, item.task.id)
            .then((view) => {
                if (cancelled || currentTaskSession !== taskSessionRef.current) {
                    return;
                }

                setTaskView(view);
                setRemovableAttachmentKeys(new Set(view.removableAttachmentKeys));
                setTaskInputData(view.data);
                setLastPersistedTaskInputData(view.data);
                latestTaskInputDataRef.current = view.data;
                lastPersistedTaskInputDataRef.current = view.data;
                const savedRecipient = getSelectedRecipient(view.data);
                if (manualAssignment && savedRecipient != null && !hasRecipientOption(view, savedRecipient)) {
                    clearUnavailableRecipient(savedRecipient, view);
                }
            })
            .catch((err) => {
                if (cancelled || currentTaskSession !== taskSessionRef.current) {
                    return;
                }

                if (err.status === 403) {
                    dispatch(
                        setErrorMessage({
                            message: 'Sie haben keine Berechtigung, diese Aufgabenansicht zu sehen.',
                            status: 403,
                        }),
                    );
                } else {
                    dispatch(showApiErrorSnackbar(err, 'Die Aufgabenansicht konnte nicht geladen werden.'));
                }
            });

        return () => {
            cancelled = true;
        };
    }, [clearUnavailableRecipient, dispatch, manualAssignment, item?.task.id, item?.task.processInstanceId, item?.task.status === ProcessTaskStatus.DeadlineExceeded,
        isProcessTaskEditableByStaff(item?.task.status)]);

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
            loadedTaskAttachmentsInstanceIdRef.current = null;
            setTaskAttachments([]);
            setTaskAttachmentSets([]);
            setIsLoadingTaskAttachments(false);
            return () => {
                cancelled = true;
            };
        }

        const instanceId = item.instance.id;
        // A reload after an update keeps showing the loaded attachments until the new ones arrive.
        const isReload = loadedTaskAttachmentsInstanceIdRef.current === instanceId;
        if (!isReload) {
            setTaskAttachments([]);
            setTaskAttachmentSets([]);
            setIsLoadingTaskAttachments(true);
        }

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

                loadedTaskAttachmentsInstanceIdRef.current = instanceId;
                setTaskAttachments(attachmentPage.content);
                setTaskAttachmentSets(attachmentSetPage.content);
            })
            .catch((err) => {
                if (cancelled) {
                    return;
                }

                dispatch(showApiErrorSnackbar(err, 'Die Vorgangsanlagen konnten nicht geladen werden.'));
                if (!isReload) {
                    setTaskAttachments([]);
                    setTaskAttachmentSets([]);
                }
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
    }, [dispatch, item?.instance?.id, taskAttachmentsReloadCount]);

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

    const handleDownloadAttachment = useCallback(
        async (attachment: ProcessInstanceAttachmentEntity): Promise<void> => {
            try {
                const blob = await new BaseApiService().getBlob(
                    `/api/process-instance-attachments/${encodeURIComponent(attachment.key)}/file/?download=true`,
                );
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
                dispatch(showApiErrorSnackbar(error, 'Die Anlage konnte nicht heruntergeladen werden.'));
            }
        },
        [dispatch],
    );

    const handleViewAttachment = useCallback(
        async (attachment: ProcessInstanceAttachmentEntity): Promise<void> => {
            const previewWindow = window.open('', '_blank');
            if (previewWindow == null) {
                dispatch(
                    showErrorSnackbar(
                        'Die Anlage konnte nicht geöffnet werden. Bitte erlauben Sie Pop-ups für diese Seite.',
                    ),
                );
                return;
            }

            previewWindow.opener = null;
            previewWindow.document.title = attachment.fileName;
            previewWindow.document.body.textContent = 'Anlage wird geladen …';

            try {
                const blob = await new BaseApiService().getBlob(
                    `/api/process-instance-attachments/${encodeURIComponent(attachment.key)}/file/?download=false`,
                );

                // A blob URL belongs to the application origin. Opening active content such as HTML or SVG
                // there would execute its scripts with the session of the staff user, so only inert media
                // types are previewed. All other attachments are downloaded instead.
                const previewMediaType = getPreviewableAttachmentMediaType(blob.type);
                if (previewMediaType == null) {
                    previewWindow.close();
                    dispatch(showWarningSnackbar('Dieser Dateityp kann nicht in der Vorschau angezeigt werden. Die Anlage wird stattdessen heruntergeladen.'));
                    await handleDownloadAttachment(attachment);
                    return;
                }

                const objectUrl = URL.createObjectURL(new Blob([blob], {type: previewMediaType}));
                previewWindow.location.replace(objectUrl);

                window.setTimeout(() => {
                    URL.revokeObjectURL(objectUrl);
                }, 60_000);
            } catch (error) {
                previewWindow.close();
                dispatch(showApiErrorSnackbar(error, 'Die Anlage konnte nicht angezeigt werden.'));
            }
        },
        [dispatch, handleDownloadAttachment],
    );

    const taskViewAttachmentContextValue = useMemo(
        () => ({
            attachments: taskAttachments,
            attachmentSets: taskAttachmentSets,
            isLoadingAttachments: isLoadingTaskAttachments,
            viewAttachment: handleViewAttachment,
            downloadAttachment: handleDownloadAttachment,
        }),
        [handleDownloadAttachment, handleViewAttachment, isLoadingTaskAttachments, taskAttachmentSets, taskAttachments],
    );

    const handleTaskViewEvent = async (evt: TaskViewEvent, requestedEventPayload: AuthoredElementValues) => {
        if (item == null || taskView == null || deadlineExceededRef.current) {
            return;
        }

        let shouldNavigateAway = false;
        const session = taskSessionRef.current;
        setSubmittingEvent(true);

        if (pushUpdateTimeoutRef.current != null) {
            window.clearTimeout(pushUpdateTimeoutRef.current);
            pushUpdateTimeoutRef.current = null;
        }

        if (inFlightSavePromiseRef.current != null) {
            await inFlightSavePromiseRef.current;
        }

        if (deadlineExceededRef.current || session !== taskSessionRef.current) return;

        const eventPayload = adoptStoredFileItems(requestedEventPayload);
        if (manualAssignment && getSelectedRecipient(requestedEventPayload) !== getSelectedRecipient(latestTaskInputDataRef.current)) {
            // A save that the event waited for may already have cleared this revoked selection.
            setSubmittingEvent(false);
            return;
        }

        dispatch(
            setLoadingMessage({
                message: `Aktion wird verarbeitet: ${evt.label}`,
                blocking: true,
                estimatedTime: 500,
            }),
        );

        latestTaskInputDataRef.current = eventPayload;

        return withDelay(
            collectFileUploads(eventPayload).then((files) => new ProcessInstanceTaskApiService().putStaffTaskView(
                item.task.processInstanceId,
                item.task.id,
                eventPayload,
                files,
                evt.event,
            )),
            500,
        )
            .then(async (updatedTaskView) => {
                dispatchProcessAssignedTaskCountRefreshEvent();
                // The update succeeded, so a repeated event must not upload its files again if the status lookup fails.
                adoptSavedTaskView(eventPayload, updatedTaskView);

                const updatedTask = await new ProcessInstanceTaskApiService().retrieve(item.task.id);

                if (updatedTask.status === ProcessTaskStatus.Running || updatedTask.status === ProcessTaskStatus.InProgress ||
                    updatedTask.status === ProcessTaskStatus.AwaitingStaff) {
                    refresh();
                    setTaskView(updatedTaskView);
                    setRemovableAttachmentKeys(new Set(updatedTaskView.removableAttachmentKeys));
                    setTaskAttachmentsReloadCount((count) => count + 1);
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
                if (session !== taskSessionRef.current || handleDeadlineExceeded(err)) {
                    return;
                }
                if (isApiError(err) && handleUnavailableRecipient(err.details, eventPayload, session)) return;
                if (isApiError(err) && isDerivedRuntimeElementData(err.details)) {
                    dispatch(showErrorSnackbar(err.message));
                    setDerivedErrors(err.details);
                } else if (!isApiUnreachableError(err)) {
                    dispatch(showApiErrorSnackbar(err, 'Die Aufgabe konnte nicht verarbeitet werden.'));
                }
            })
            .finally(() => {
                if (session === taskSessionRef.current) setSubmittingEvent(false);
                if (!shouldNavigateAway && !deadlineExceededRef.current) {
                    skipChangeBlockerRef.current = false;
                }
                dispatch(clearLoadingMessage());
            });
    };

    const handleEventClick = async (evt: TaskViewEvent) => {
        await handleTaskViewEvent(evt, latestTaskInputDataRef.current);
    };

    const handleInlineEvent = async (values: AuthoredElementValues, event: string): Promise<void> => {
        const evt = taskView?.events.find((taskViewEvent) => taskViewEvent.event === event) ?? {
            label: event,
            event,
        };

        await handleTaskViewEvent(evt, values);
    };

    const handleAuthoredValuesChange = (authoredValues: AuthoredElementValues) => {
        if (item == null) {
            return;
        }

        if (submittingEvent || deadlineExceededRef.current) {
            return;
        }

        if (deepEquals(authoredValues, latestTaskInputDataRef.current)) {
            return;
        }

        latestTaskInputDataRef.current = authoredValues;
        setTaskInputData(authoredValues);
        if (unavailableRecipient != null && getSelectedRecipient(authoredValues) != null) {
            rejectedRecipientRef.current = null;
            setUnavailableRecipient(null);
        }
        setTaskInputDataSaveState(deepEquals(authoredValues, lastPersistedTaskInputDataRef.current)
            ? ProcessTaskInputSaveState.Saved : ProcessTaskInputSaveState.Waiting);
    };

    const blocker = useBlocker(({currentLocation, nextLocation}) => {
        if (skipChangeBlockerRef.current) {
            return false;
        }

        if (currentLocation.pathname === nextLocation.pathname && currentLocation.search === nextLocation.search) {
            return false;
        }

        if (!navigator.onLine || taskInputDataSaveState === ProcessTaskInputSaveState.Failed) return hasUnsavedUserInput();
        return !deepEquals(latestTaskInputDataRef.current, lastPersistedTaskInputDataRef.current);
    });

    const shouldAutoResolveBlockedNavigation = useMemo(() => {
        const canRetryNow =
            taskInputDataSaveState === ProcessTaskInputSaveState.RetryQueued &&
            (typeof navigator === 'undefined' || navigator.onLine);

        return (
            taskInputDataSaveState === ProcessTaskInputSaveState.Waiting ||
            taskInputDataSaveState === ProcessTaskInputSaveState.Saving ||
            canRetryNow ||
            pushUpdateTimeoutRef.current != null ||
            inFlightSavePromiseRef.current != null
        );
    }, [taskInputDataSaveState]);

    const handleBlockedNavigation = useCallback(
        async (blockedNavigation: Blocker) => {
            if (item == null) {
                return;
            }

            isResolvingBlockedNavigationRef.current = true;
            setPendingBlockedNavigation(null);

            dispatch(
                setLoadingMessage({
                    message: NAVIGATION_SAVE_MESSAGE,
                    blocking: true,
                    estimatedTime: TASK_INPUT_DATA_MIN_SAVE_DURATION_MS,
                }),
            );

            await flushCurrentTaskInputData();

            dispatch(clearLoadingMessage());

            // After a deadline expiry only the expiry notice is rendered, so the confirmation dialog could never resolve it.
            if (deadlineExceededRef.current || !hasUnsavedUserInput()) {
                blockedNavigation.proceed?.();
                return;
            }

            isResolvingBlockedNavigationRef.current = false;
            setPendingBlockedNavigation(blockedNavigation);
        },
        [dispatch, flushCurrentTaskInputData, hasUnsavedUserInput, item],
    );

    useEffect(() => {
        if (blocker.state !== 'blocked') {
            isResolvingBlockedNavigationRef.current = false;
            setPendingBlockedNavigation(null);
            return;
        }

        // A successful flush can render before the router finishes proceeding. Resolve each blocked navigation once.
        if (isResolvingBlockedNavigationRef.current) {
            return;
        }

        if (deepEquals(latestTaskInputDataRef.current, lastPersistedTaskInputDataRef.current) ||
            (!shouldAutoResolveBlockedNavigation && !hasUnsavedUserInput())) {
            isResolvingBlockedNavigationRef.current = true;
            blocker.proceed();
            return;
        }

        if (shouldAutoResolveBlockedNavigation) {
            void handleBlockedNavigation(blocker);
            return;
        }

        setPendingBlockedNavigation(blocker);
    }, [blocker, handleBlockedNavigation, hasUnsavedUserInput, shouldAutoResolveBlockedNavigation]);

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
        return <GenericDetailsSkeleton />;
    }

    if (deadlineExceeded || item.task.status === ProcessTaskStatus.DeadlineExceeded) {
        return <Alert severity="error">Die Bearbeitungsfrist dieser Aufgabe ist abgelaufen. Eine Bearbeitung ist nicht mehr möglich.</Alert>;
    }

    if (!isProcessTaskEditableByStaff(item.task.status)) {
        return <Alert severity="info">{PROCESS_TASK_NOT_EDITABLE_MESSAGE}</Alert>;
    }

    return (
        <Box
            sx={{
                pt: 1,
            }}
        >
            <Typography variant="h5">Aufgabe bearbeiten</Typography>

            <StatusTable
                sx={{mt: 2}}
                cardVariant="outlined"
                items={introItems}
            />

            {taskView == null ? (
                <Box sx={{mt: 4}}>
                    <GenericDetailsSkeleton />
                </Box>
            ) : (
                <ProcessTaskViewAttachmentProvider value={taskViewAttachmentContextValue}>
                    <Box
                        sx={{
                            mt: 4,
                        }}
                    >
                        <RemovableFileUploadAttachmentsProvider value={removableAttachmentKeys}>
                            <ElementDerivationContext
                                element={taskView.layout}
                                authoredElementValues={taskInputData}
                                onAuthoredElementValuesChange={handleAuthoredValuesChange}
                                computedErrors={computedErrors}
                                onEvent={handleInlineEvent}
                                taskViewMode="staff"
                                showErrorSummary
                                onDerive={(aev, derivationOptions) => {
                                    if (item == null || item.instance == null || item.task == null) {
                                        return Promise.resolve({
                                            effectiveValues: {},
                                            elementStates: {},
                                        });
                                    }

                                    const session = taskSessionRef.current;
                                    return new ProcessInstanceTaskApiService().deriveStaffTaskView(
                                        item.instance.id,
                                        item.task.id,
                                        aev,
                                        derivationOptions.skipErrorsForElementIds,
                                    ).then(result => {
                                        if (!handleUnavailableRecipient(result, aev, session)) return result;
                                        // The page retains the person-specific message until a replacement is selected.
                                        return {
                                            ...result,
                                            effectiveValues: {...result.effectiveValues, [RECIPIENT_FIELD]: null},
                                            elementStates: {...result.elementStates, [RECIPIENT_FIELD]: {
                                                ...result.elementStates[RECIPIENT_FIELD], error: null, errorDetails: null,
                                            }},
                                        };
                                    });
                                }}
                            />
                        </RemovableFileUploadAttachmentsProvider>
                    </Box>

                    <Stack
                        direction="row"
                        sx={{
                            alignItems: 'flex-end',
                        }}
                    >
                        <TaskViewEventButtons
                            events={taskView.events}
                            onEvent={handleEventClick}
                        />

                        <ProcessTaskInputSaveStateChip
                            state={taskInputDataSaveState}
                            lastSavedAt={lastSavedAt}
                        />
                    </Stack>
                </ProcessTaskViewAttachmentProvider>
            )}

            {blocker.state === 'blocked' && pendingBlockedNavigation != null && hasUnsavedUserInput() && (
                <ConfirmDialog
                    confirmButtonColor="error"
                    title="Ungespeicherte Eingaben"
                    onConfirm={handleConfirmBlockedNavigation}
                    onCancel={handleCancelBlockedNavigation}
                    confirmButtonText="Seite verlassen"
                >
                    {blockedNavigationMessage}
                </ConfirmDialog>
            )}
        </Box>
    );
}
