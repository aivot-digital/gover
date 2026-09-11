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
    const [editedAuthoredValues, setEditedAuthoredValues] = useState<AuthoredElementValues | null>(null);
    const [derivedErrors, setDerivedErrors] = useState<DerivedRuntimeElementData | null>(null);
    const latestAuthoredValuesRef = useRef<AuthoredElementValues>({});
    const taskViewLoadGenerationRef = useRef(0);

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

    const identitySelectionApi = useMemo(() => (
        taskApi.createIdentitySelectionApi(instanceAccessKey, taskAccessKey)
    ), [instanceAccessKey, taskAccessKey, taskApi]);

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
                if (handleRequiredIdentityAuthentication(error)) {
                    return createDerivedRuntimeElementData();
                }
                throw error;
            });
    }, [handleRequiredIdentityAuthentication, instanceAccessKey, taskAccessKey, taskApi]);

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
            if (handleRequiredIdentityAuthentication(error)) {
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
                onDeriveOverride={handleDerive}
                onEvent={taskIsActive ? handleInlineEvent : undefined}
                taskViewMode="customer"
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
        try {
            const controls = newIdentitySlot != null && controlsRef.current != null
                ? [[newIdentitySlot.id, controlsRef.current] as const]
                : [];
            return await persistPendingIdentitySelections(controls, targetIdentityId);
        } finally {
            identityProviderStartPendingRef.current = false;
            setIsStartingIdentityProvider(false);
        }
    }, [identityControlBusy, newIdentitySlot]);

    const handleContinue = async () => {
        if (continueDisabled) {
            return;
        }

        setIsContinuing(true);
        try {
            if (newIdentitySlot != null && newIdentitySelected) {
                const controls = controlsRef.current;
                if (controls == null || !await controls.commitPendingSelection()) {
                    return;
                }
            }

            await onContinue();
        } finally {
            setIsContinuing(false);
        }
    };

    let introduction: string;
    if (existingIdentitySlot != null && newIdentitySlot != null) {
        introduction = 'Bestätigen Sie die Empfängeridentität dieser Aufgabe und vervollständigen Sie die weitere Identitätsangabe.';
    } else if (existingIdentitySlot != null) {
        introduction = 'Für diese Aufgabe ist eine erneute Anmeldung erforderlich. Melden Sie sich mit dem Nutzerkonto an, an das diese Aufgabe gesendet wurde.';
    } else if (newIdentitySlot?.isRequired) {
        introduction = 'Für diese Aufgabe ist eine Identität erforderlich. Wählen Sie ein Nutzerkonto oder, sofern angeboten, eine E-Mail-Adresse aus.';
    } else {
        introduction = 'Sie können ein Nutzerkonto oder eine angebotene E-Mail-Adresse verwenden. Alternativ können Sie die Aufgabe ohne Identitätsangabe bearbeiten.';
    }

    return (
        <Box
            sx={{
                maxWidth: 1200,
                mx: 'auto',
                py: {
                    xs: 3,
                    md: 5,
                },
            }}
        >
            <Grid container spacing={3}>
                <Grid size={{xs: 12, md: 10, lg: 8}} sx={{mb: 2}}>
                    <Typography variant="h2" component="div">
                        Identität auswählen
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
                                <Typography variant="h4" component="h2">
                                    Empfängeridentität
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
                                        Falsches Nutzerkonto
                                    </Typography>
                                    Das verwendete Nutzerkonto gehört nicht zur Empfängeridentität dieser Aufgabe.
                                    Melden Sie sich mit dem richtigen Nutzerkonto an.
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
                            api={identitySelectionApi}
                            saveMode="deferred"
                            beforeIdentityProviderStart={handleIdentityProviderStart}
                            identityProviderAuthenticationDisabled={identityProviderAuthenticationDisabled}
                            onChange={onIdentitySlotChange}
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
                                : 'Mit Aufgabe fortfahren'
                        }
                    </Button>
                </Grid>
            </Grid>
        </Box>
    );
}
