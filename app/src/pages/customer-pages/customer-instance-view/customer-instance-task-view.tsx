import ArrowForward from '@aivot/mui-material-symbols-400-n25-outlined/ArrowForward';
import {Alert, Box, Button, Grid, Paper, Typography} from '@mui/material';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useNavigate, useOutletContext, useParams} from 'react-router-dom';
import {Chip} from '../../../components/chip/chip';
import {LoadingPlaceholder} from '../../../components/loading-placeholder/loading-placeholder';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {isApiError} from '../../../models/api-error';
import {
    type AuthoredElementValues,
    createDerivedRuntimeElementData,
    type DerivedRuntimeElementData,
    isDerivedRuntimeElementData,
} from '../../../models/element-data';
import {ElementDerivationContext} from '../../../modules/elements/components/element-derivation-context';
import {
    type FormIdentitySelectionControlsHandle,
    type FormIdentitySelectionControlsStatus,
    persistPendingIdentitySelections,
} from '../../../modules/identity/components/form-identity-selection-controls/form-identity-selection-controls';
import {IdentityButton} from '../../../modules/identity/components/identity-button/identity-button';
import {IdentitySlotCard} from '../../../modules/identity/components/identity-slot-card/identity-slot-card';
import type {IdentitySelectionApi} from '../../../modules/identity/models/identity-selection-api';
import type {IdentitySlot} from '../../../modules/identity/models/identity-slot';
import {TaskViewEventButtons} from '../../../modules/process/components/task-view-event-buttons';
import {isTaskDeadlineExceededError} from '../../../modules/process/utils/task-deadline-error';
import {
    hasCustomerTaskIdentityRequirements,
    hasCustomerTaskViewContent,
    type CustomerTaskExistingIdentitySlot,
} from '../../../modules/process/models/customer-task-view';
import {
    ProcessInstanceTaskApiService,
    type TaskViewEvent,
} from '../../../modules/process/services/process-instance-task-api-service';
import {clearLoadingMessage, setErrorMessage, setLoadingMessage} from '../../../slices/shell-slice';
import {showApiErrorSnackbar, showErrorSnackbar} from '../../../slices/snackbar-slice';
import {withDelay} from '../../../utils/with-delay';
import type {CustomerInstanceViewOutletContext} from './customer-instance-view';
import {
    buildCustomerInstancePath,
    CustomerTaskViewApiService,
    isRequiredIdentityAuthenticationError,
    removeIdentityCallbackParameters,
    type TaskViewResponse,
} from './customer-task-view-api-service';

export function CustomerInstanceTaskView() {
    const {
        instanceAccessKey = '',
        taskAccessKey = '',
    } = useParams<{
        instanceAccessKey: string;
        taskAccessKey: string;
    }>();

    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const {
        refreshInstanceStatus,
        invalidateInstanceTasks,
        taskIsActive,
    } = useOutletContext<CustomerInstanceViewOutletContext>();
    const taskApi = useMemo(() => new CustomerTaskViewApiService(), []);

    const [taskView, setTaskView] = useState<TaskViewResponse | null | 'failed'>(null);
    const [identityStepDismissed, setIdentityStepDismissed] = useState(false);
    const [deadlineExceeded, setDeadlineExceeded] = useState(false);
    const [editedAuthoredValues, setEditedAuthoredValues] = useState<AuthoredElementValues | null>(null);
    const [derivedErrors, setDerivedErrors] = useState<DerivedRuntimeElementData | null>(null);
    const latestAuthoredValuesRef = useRef<AuthoredElementValues>({});
    const taskViewLoadGenerationRef = useRef(0);
    const previousTaskStateRef = useRef({instanceAccessKey, taskAccessKey, taskIsActive});

    useEffect(() => {
        const currentUrl = window.location.href;
        const cleanedUrl = removeIdentityCallbackParameters(currentUrl);
        if (cleanedUrl !== currentUrl) {
            window.history.replaceState(window.history.state, '', cleanedUrl);
        }
    }, [instanceAccessKey, taskAccessKey]);

    const retrieveTaskView = useCallback(async (): Promise<TaskViewResponse | null> => {
        const loadGeneration = ++taskViewLoadGenerationRef.current;
        try {
            const view = await taskApi.getTaskView(instanceAccessKey, taskAccessKey);
            if (loadGeneration !== taskViewLoadGenerationRef.current) {
                return null;
            }

            setTaskView(view);
            setEditedAuthoredValues(null);
            setDerivedErrors(null);
            latestAuthoredValuesRef.current = view.data ?? {};
            return view;
        } catch (error) {
            if (loadGeneration !== taskViewLoadGenerationRef.current) {
                return null;
            }

            if (isApiError(error) && error.displayableToUser) {
                dispatch(setErrorMessage({
                    message: error.message,
                    status: error.status,
                }));
            } else {
                dispatch(setErrorMessage({
                    message: 'Fehler beim Abrufen des Status des Vorgangs.',
                    status: isApiError(error) ? error.status : 500,
                }));
            }
            setTaskView('failed');
            return null;
        }
    }, [dispatch, instanceAccessKey, taskAccessKey, taskApi]);

    useEffect(() => {
        let active = true;
        let loadPending = true;

        dispatch(setLoadingMessage({
            message: 'Lade Aufgabenansicht',
            blocking: false,
            estimatedTime: 1000,
        }));

        setTaskView(null);
        setDeadlineExceeded(false);
        setIdentityStepDismissed(false);
        setEditedAuthoredValues(null);
        setDerivedErrors(null);
        latestAuthoredValuesRef.current = {};

        void retrieveTaskView()
            .finally(() => {
                if (!active) {
                    return;
                }
                loadPending = false;
                dispatch(clearLoadingMessage());
            });

        return () => {
            active = false;
            taskViewLoadGenerationRef.current += 1;
            if (loadPending) {
                dispatch(clearLoadingMessage());
            }
        };
    }, [dispatch, retrieveTaskView]);

    useEffect(() => {
        const previousTaskState = previousTaskStateRef.current;
        previousTaskStateRef.current = {instanceAccessKey, taskAccessKey, taskIsActive};

        if (
            previousTaskState.instanceAccessKey === instanceAccessKey &&
            previousTaskState.taskAccessKey === taskAccessKey &&
            previousTaskState.taskIsActive &&
            !taskIsActive
        ) {
            void retrieveTaskView();
        }
    }, [instanceAccessKey, retrieveTaskView, taskAccessKey, taskIsActive]);

    const handleRequiredIdentityAuthentication = useCallback((error: unknown): boolean => {
        if (!isRequiredIdentityAuthenticationError(error)) {
            return false;
        }

        setTaskView(null);
        setIdentityStepDismissed(false);
        setEditedAuthoredValues(null);
        setDerivedErrors(null);
        latestAuthoredValuesRef.current = {};
        void retrieveTaskView();
        return true;
    }, [retrieveTaskView]);

    const handleDeadlineExceeded = useCallback((error: unknown): boolean => {
        if (!isTaskDeadlineExceededError(error)) return false;
        setDeadlineExceeded(true);
        void retrieveTaskView();
        void refreshInstanceStatus().catch(() => invalidateInstanceTasks());
        return true;
    }, [invalidateInstanceTasks, refreshInstanceStatus, retrieveTaskView]);

    const identitySelectionApi = useMemo((): IdentitySelectionApi => {
        const api = taskApi.createIdentitySelectionApi(instanceAccessKey, taskAccessKey);
        const onError = (error: unknown): never => {
            handleDeadlineExceeded(error);
            throw error;
        };
        return {
            ...api,
            setEmailIdentity: (...args) => api.setEmailIdentity(...args).catch(onError),
            clearIdentity: (...args) => api.clearIdentity(...args).catch(onError),
            selectCommunication: (...args) => api.selectCommunication(...args).catch(onError),
            deriveCommunication: (...args) => api.deriveCommunication(...args).catch(onError),
        };
    }, [handleDeadlineExceeded, instanceAccessKey, taskAccessKey, taskApi]);

    const handleIdentitySlotChange = useCallback((nextSlot: IdentitySlot): void => {
        setTaskView((currentView) => {
            if (currentView == null || currentView === 'failed') {
                return currentView;
            }
            if (hasCustomerTaskViewContent(currentView)) {
                return {...currentView, newIdentitySlot: nextSlot};
            }
            return {...currentView, newIdentitySlot: nextSlot};
        });
    }, []);

    const handleIdentityContinue = useCallback(async (): Promise<boolean> => {
        const refreshedView = await retrieveTaskView();
        if (refreshedView == null || !hasCustomerTaskViewContent(refreshedView)) {
            setIdentityStepDismissed(false);
            return false;
        }

        setIdentityStepDismissed(true);
        return true;
    }, [retrieveTaskView]);

    const handleDerive = useCallback((values: AuthoredElementValues, skipErrorsForElements: string[]) => {
        return taskApi
            .deriveTaskView(instanceAccessKey, taskAccessKey, values, skipErrorsForElements)
            .catch((error) => {
                if (handleDeadlineExceeded(error) || handleRequiredIdentityAuthentication(error)) {
                    return createDerivedRuntimeElementData();
                }
                throw error;
            });
    }, [handleDeadlineExceeded, handleRequiredIdentityAuthentication, instanceAccessKey, taskAccessKey, taskApi]);

    const handleTaskViewEvent = useCallback(async (event: TaskViewEvent, values: AuthoredElementValues): Promise<void> => {
        dispatch(setLoadingMessage({
            message: `Verarbeite Aktion: ${event.label}`,
            blocking: true,
            estimatedTime: 500,
        }));

        latestAuthoredValuesRef.current = values;

        try {
            const updatedTaskView = await withDelay(
                new ProcessInstanceTaskApiService()
                    .putCustomerTaskView(instanceAccessKey, taskAccessKey, values, event.event),
                500,
            );

            setTaskView(updatedTaskView);
            setDerivedErrors(null);
            if (hasCustomerTaskViewContent(updatedTaskView)) {
                setEditedAuthoredValues(updatedTaskView.data);
                latestAuthoredValuesRef.current = updatedTaskView.data;
            } else {
                setIdentityStepDismissed(false);
                setEditedAuthoredValues(null);
                latestAuthoredValuesRef.current = {};
            }

            try {
                await refreshInstanceStatus();
            } catch (statusError) {
                invalidateInstanceTasks();
                dispatch(showApiErrorSnackbar(
                    statusError,
                    'Der Aufgabenstatus konnte nach der Aktion nicht aktualisiert werden.',
                ));
                navigate(buildCustomerInstancePath(instanceAccessKey), {replace: true});
            }
        } catch (error) {
            if (handleDeadlineExceeded(error) || handleRequiredIdentityAuthentication(error)) {
                return;
            }
            if (isApiError(error) && isDerivedRuntimeElementData(error.details)) {
                dispatch(showErrorSnackbar(error.message));
                setDerivedErrors(error.details);
            } else {
                dispatch(showApiErrorSnackbar(error, 'Die Aufgabe konnte nicht verarbeitet werden.'));
            }
        } finally {
            dispatch(clearLoadingMessage());
        }
    }, [
        dispatch,
        handleDeadlineExceeded,
        handleRequiredIdentityAuthentication,
        instanceAccessKey,
        invalidateInstanceTasks,
        navigate,
        refreshInstanceStatus,
        taskAccessKey,
    ]);

    const handleEventClick = useCallback(async (event: TaskViewEvent): Promise<void> => {
        await handleTaskViewEvent(event, latestAuthoredValuesRef.current);
    }, [handleTaskViewEvent]);

    const handleInlineEvent = useCallback(async (values: AuthoredElementValues, event: string): Promise<void> => {
        const taskViewEvent = taskView != null && taskView !== 'failed'
            ? taskView.events?.find((candidate) => candidate.event === event)
            : undefined;

        await handleTaskViewEvent(taskViewEvent ?? {
            label: event,
            event,
        }, values);
    }, [handleTaskViewEvent, taskView]);

    const handleAuthoredValuesChange = useCallback((values: AuthoredElementValues): void => {
        latestAuthoredValuesRef.current = values;
        setEditedAuthoredValues(values);
    }, []);

    if (deadlineExceeded) {
        return <Alert severity="error">Die Bearbeitungsfrist ist abgelaufen. Eine Bearbeitung ist nicht mehr möglich.</Alert>;
    }

    if (taskView == null) {
        return <LoadingPlaceholder/>;
    }

    if (taskView === 'failed') {
        return null;
    }

    const hasIdentityRequirements = hasCustomerTaskIdentityRequirements(taskView);
    if (hasIdentityRequirements && (!identityStepDismissed || !hasCustomerTaskViewContent(taskView))) {
        return (
            <TaskIdentityPlaceholder
                existingIdentitySlot={taskView.existingIdentitySlot}
                existingIdentityStartUri={taskApi.createRequiredIdentityAuthenticationStartLink(
                    instanceAccessKey,
                    taskAccessKey,
                )}
                newIdentitySlot={taskView.newIdentitySlot}
                identitySelectionApi={identitySelectionApi}
                onIdentitySlotChange={handleIdentitySlotChange}
                onContinue={handleIdentityContinue}
            />
        );
    }

    if (!hasCustomerTaskViewContent(taskView)) {
        return (
            <Alert severity="error">
                Die Aufgabenansicht konnte nicht vollständig geladen werden.
            </Alert>
        );
    }

    const authoredValues = editedAuthoredValues ?? taskView.data;

    return (
        <Box>
            <ElementDerivationContext
                element={taskView.layout}
                authoredElementValues={authoredValues}
                onAuthoredElementValuesChange={handleAuthoredValuesChange}
                computedErrors={derivedErrors?.elementStates}
                onDerive={(values, derivationOptions) => handleDerive(values, derivationOptions.skipErrorsForElementIds)}
                onEvent={taskIsActive ? handleInlineEvent : undefined}
                taskViewMode="customer"
                showErrorSummary
                readOnly={!taskIsActive}
            />

            {
                taskIsActive &&
                <TaskViewEventButtons
                    events={taskView.events}
                    onEvent={handleEventClick}
                />
            }
        </Box>
    );
}

interface TaskIdentityPlaceholderProps {
    existingIdentitySlot: CustomerTaskExistingIdentitySlot | null;
    existingIdentityStartUri: string;
    newIdentitySlot: IdentitySlot | null;
    identitySelectionApi: IdentitySelectionApi;
    onIdentitySlotChange: (slot: IdentitySlot) => void;
    onContinue: () => Promise<boolean>;
}

function TaskIdentityPlaceholder(props: TaskIdentityPlaceholderProps) {
    const {
        existingIdentitySlot,
        existingIdentityStartUri,
        identitySelectionApi,
        newIdentitySlot,
        onContinue,
        onIdentitySlotChange,
    } = props;
    const controlsRef = useRef<FormIdentitySelectionControlsHandle | null>(null);
    const [controlStatus, setControlStatus] = useState<FormIdentitySelectionControlsStatus | null>(null);
    const [isContinuing, setIsContinuing] = useState(false);
    const [isStartingIdentityProvider, setIsStartingIdentityProvider] = useState(false);
    const identityProviderStartPendingRef = useRef(false);
    const heldIdentitySlotChangesRef = useRef<IdentitySlot[] | null>(null);
    const handleControlStatusChange = useCallback((
        _slotId: string,
        status: FormIdentitySelectionControlsStatus | null,
    ) => {
        setControlStatus((currentStatus) => {
            if (
                currentStatus?.hasSelection === status?.hasSelection &&
                currentStatus?.canCommit === status?.canCommit &&
                currentStatus?.isBusy === status?.isBusy
            ) {
                return currentStatus;
            }
            return status;
        });
    }, []);

    const newIdentitySelected = newIdentitySlot != null && (
        controlStatus?.hasSelection ?? newIdentitySlot.identityType != null
    );
    const newIdentityCanCommit = newIdentitySlot == null || (
        !newIdentitySelected
            ? newIdentitySlot.isOptional
            : controlStatus?.canCommit ?? newIdentitySlot.isReady
    );
    const existingIdentityReady = existingIdentitySlot?.isReady ?? true;
    const identityControlBusy = controlStatus?.isBusy ?? false;
    const identityProviderAuthenticationDisabled = isStartingIdentityProvider || identityControlBusy;
    const continueDisabled = isContinuing || identityProviderAuthenticationDisabled || !existingIdentityReady || !newIdentityCanCommit;
    const canContinueWithoutAuthentication = existingIdentitySlot == null &&
        newIdentitySlot?.isOptional === true &&
        !newIdentitySelected;
    const wrongExistingAccount = existingIdentitySlot != null &&
        !existingIdentitySlot.isReady &&
        existingIdentitySlot.identityProvider.isAuthenticatedWithThis;

    const handleIdentityProviderStart = useCallback(async (targetIdentityId: string): Promise<boolean> => {
        if (identityProviderStartPendingRef.current || identityControlBusy) {
            return false;
        }

        identityProviderStartPendingRef.current = true;
        setIsStartingIdentityProvider(true);
        // On success the browser leaves for the identity provider and the slots are loaded again on return. Applying
        // the persisted slots would briefly show their saved-state confirmation and removal action before the redirect.
        const heldChanges: IdentitySlot[] = [];
        heldIdentitySlotChangesRef.current = heldChanges;
        let persisted = false;
        try {
            const controls = newIdentitySlot != null && controlsRef.current != null
                ? [[newIdentitySlot.id, controlsRef.current] as const]
                : [];
            persisted = await persistPendingIdentitySelections(controls, targetIdentityId);
            return persisted;
        } finally {
            heldIdentitySlotChangesRef.current = null;
            if (!persisted) {
                heldChanges.forEach(onIdentitySlotChange);
            }
            identityProviderStartPendingRef.current = false;
            setIsStartingIdentityProvider(false);
        }
    }, [identityControlBusy, newIdentitySlot, onIdentitySlotChange]);

    const handleIdentitySlotChange = (slot: IdentitySlot) => {
        const heldChanges = heldIdentitySlotChangesRef.current;
        if (heldChanges == null) {
            onIdentitySlotChange(slot);
        } else {
            heldChanges.push(slot);
        }
    };

    const commitNewIdentity = async (): Promise<boolean> => {
        const controls = controlsRef.current;
        if (controls == null) {
            return false;
        }

        // A successful commit reloads the task view, which supersedes the saved slot. Applying it right away would
        // briefly show the saved-state confirmation and removal action while the task is still loading.
        const heldChanges: IdentitySlot[] = [];
        heldIdentitySlotChangesRef.current = heldChanges;
        let committed = false;
        try {
            committed = await controls.commitPendingSelection();
            return committed;
        } finally {
            heldIdentitySlotChangesRef.current = null;
            if (!committed) {
                heldChanges.forEach(onIdentitySlotChange);
            }
        }
    };

    const handleContinue = async () => {
        if (continueDisabled) {
            return;
        }

        setIsContinuing(true);
        try {
            if (newIdentitySlot != null && newIdentitySelected && !await commitNewIdentity()) {
                return;
            }

            await onContinue();
        } finally {
            setIsContinuing(false);
        }
    };

    const existingIdentityTitle = existingIdentitySlot?.title?.trim();
    const existingIdentityLabel = existingIdentityTitle || 'Bestehende Identität';
    const accountInstruction = existingIdentityTitle
        ? `Melden Sie sich mit dem Nutzerkonto an, das Sie für „${existingIdentityTitle}“ in diesem Vorgang verwendet haben.`
        : 'Melden Sie sich mit dem Nutzerkonto an, das Sie für diese Identität in diesem Vorgang verwendet haben.';
    let introduction: string;
    if (existingIdentitySlot != null && newIdentitySlot != null) {
        introduction = existingIdentityReady
            ? 'Ihre bestehende Identität ist bestätigt. Vervollständigen Sie die weitere Identitätsangabe, um fortzufahren.'
            : 'Bestätigen Sie Ihre bestehende Identität und vervollständigen Sie die weitere Identitätsangabe.';
    } else if (existingIdentitySlot != null) {
        introduction = existingIdentityReady
            ? 'Ihre Identität ist bestätigt. Wählen Sie „Fortfahren“, um zum nächsten Schritt zu gelangen.'
            : 'Um fortzufahren, bestätigen Sie bitte Ihre Identität.';
    } else if (newIdentitySlot?.isRequired) {
        introduction = 'Um fortzufahren, wählen Sie ein Nutzerkonto oder, sofern angeboten, eine E-Mail-Adresse aus.';
    } else {
        introduction = 'Sie können ein Nutzerkonto oder eine angebotene E-Mail-Adresse verwenden. Alternativ können Sie ohne Identitätsangabe fortfahren.';
    }
    if (existingIdentitySlot != null && !existingIdentityReady && !wrongExistingAccount) {
        introduction += ` ${accountInstruction}`;
    }

    return (
        <Box>
            <Grid container spacing={3}>
                <Grid size={{xs: 12, md: 10, lg: 8}} sx={{mb: 2}}>
                    <Typography variant="h2" component="h2">
                        {existingIdentitySlot != null ? 'Identität bestätigen' : 'Identität auswählen'}
                    </Typography>
                    <Typography sx={{mt: 1, maxWidth: 680}}>
                        {introduction}
                    </Typography>
                </Grid>

                {
                    existingIdentitySlot != null &&
                    <Grid size={{xs: 12, md: 6}}>
                        <Paper
                            variant="outlined"
                            sx={{
                                height: '100%',
                                p: {
                                    xs: 2,
                                    md: 2.5,
                                },
                                borderColor: 'divider',
                                backgroundColor: 'background.paper',
                            }}
                        >
                            <Typography variant="caption">
                                Identität
                            </Typography>
                            <Box
                                sx={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    flexWrap: 'wrap',
                                    columnGap: 1.25,
                                    rowGap: 0.5,
                                    mt: 0.25,
                                }}
                            >
                                <Typography variant="h4" component="h3">
                                    {existingIdentityLabel}
                                </Typography>
                                <Chip
                                    mode="soft"
                                    label="Verpflichtend"
                                    color="warning"
                                    size="small"
                                />
                            </Box>

                            {
                                wrongExistingAccount &&
                                <Alert severity="error" sx={{mt: 2}}>
                                    <Typography variant="subtitle2" component="div">
                                        Anderes Nutzerkonto erforderlich
                                    </Typography>
                                    {existingIdentityTitle
                                        ? `Das verwendete Nutzerkonto gehört nicht zu „${existingIdentityTitle}“. `
                                        : 'Das verwendete Nutzerkonto gehört nicht zur erforderlichen Identität. '}
                                    {accountInstruction}
                                </Alert>
                            }

                            <IdentityButton
                                startUri={existingIdentityStartUri}
                                identityProviderName={existingIdentitySlot.identityProvider.identityProviderName}
                                identityProviderType={existingIdentitySlot.identityProvider.identityProviderType}
                                identityProviderAssetKey={existingIdentitySlot.identityProvider.identityProviderAssetKey}
                                isAuthenticated={existingIdentitySlot.isReady}
                                beforeStart={() => handleIdentityProviderStart(existingIdentitySlot.id)}
                                disabled={identityProviderAuthenticationDisabled}
                            />
                        </Paper>
                    </Grid>
                }

                {
                    newIdentitySlot != null &&
                    <Grid size={{xs: 12, md: 6}}>
                        <IdentitySlotCard
                            ref={controlsRef}
                            slot={newIdentitySlot}
                            headingComponent="h3"
                            api={identitySelectionApi}
                            saveMode="deferred"
                            beforeIdentityProviderStart={handleIdentityProviderStart}
                            identityProviderAuthenticationDisabled={identityProviderAuthenticationDisabled}
                            onChange={handleIdentitySlotChange}
                            onStatusChange={handleControlStatusChange}
                        />
                    </Grid>
                }

                <Grid size={{xs: 12}}>
                    <Button
                        variant="contained"
                        endIcon={<ArrowForward/>}
                        onClick={() => void handleContinue()}
                        disabled={continueDisabled}
                        sx={{
                            mt: 1,
                            width: {
                                xs: '100%',
                                sm: 'auto',
                            },
                        }}
                    >
                        {
                            canContinueWithoutAuthentication
                                ? 'Ohne Anmeldung fortfahren'
                                : 'Fortfahren'
                        }
                    </Button>
                </Grid>
            </Grid>
        </Box>
    );
}
