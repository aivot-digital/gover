import {Alert, Box, Button, Stack, Typography} from '@mui/material';
import {useCallback, useEffect, useRef, useState} from 'react';
import {
    buildCustomerInstancePath,
    CustomerTaskViewApiService,
    isRequiredIdentityAuthenticationError,
    removeIdentityCallbackParameters,
    TaskViewResponse,
} from './customer-task-view-api-service';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {LoadingPlaceholder} from '../../../components/loading-placeholder/loading-placeholder';
import {clearLoadingMessage, setErrorMessage, setLoadingMessage} from '../../../slices/shell-slice';
import {isApiError} from '../../../models/api-error';
import {ElementDerivationContext} from '../../../modules/elements/components/element-derivation-context';
import {
    AuthoredElementValues,
    createDerivedRuntimeElementData,
    DerivedRuntimeElementData,
    isDerivedRuntimeElementData,
} from '../../../models/element-data';
import {useNavigate, useOutletContext, useParams} from 'react-router-dom';
import {
    ProcessInstanceTaskApiService,
    TaskViewEvent,
} from '../../../modules/process/services/process-instance-task-api-service';
import {TaskViewEventButtons} from '../../../modules/process/components/task-view-event-buttons';
import {showApiErrorSnackbar, showErrorSnackbar} from '../../../slices/snackbar-slice';
import {withDelay} from '../../../utils/with-delay';
import type {CustomerInstanceViewOutletContext} from './customer-instance-view';
import {IdentityStateQueryParam} from '../../../modules/identity/constants/identity-state-query-param';
import {IdentityResultState} from '../../../modules/identity/enums/identity-result-state';

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
    } = useOutletContext<CustomerInstanceViewOutletContext>();

    const [identityAuthenticationSucceeded, setIdentityAuthenticationSucceeded] = useState(() => {
        const currentUrl = new URL(window.location.href);
        const succeeded = currentUrl.searchParams.get(IdentityStateQueryParam) === String(IdentityResultState.Success);
        const cleanedUrl = removeIdentityCallbackParameters(currentUrl.toString());
        if (cleanedUrl !== currentUrl.toString()) {
            window.history.replaceState(window.history.state, '', cleanedUrl);
        }
        return succeeded;
    });
    const [taskView, setTaskView] = useState<TaskViewResponse | null | 'failed' | 'identity-required'>(null);
    const [editedAuthoredValues, setEditedAuthoredValues] = useState<AuthoredElementValues | null>(null);
    const [derivedErrors, setDerivedErrors] = useState<DerivedRuntimeElementData | null>(null);
    const latestAuthoredValuesRef = useRef<AuthoredElementValues>({});
    const taskViewLoadGenerationRef = useRef(0);

    const handleRequiredIdentityAuthentication = useCallback((error: unknown): boolean => {
        if (!isRequiredIdentityAuthenticationError(error)) {
            return false;
        }

        setTaskView('identity-required');
        setEditedAuthoredValues(null);
        setDerivedErrors(null);
        latestAuthoredValuesRef.current = {};
        return true;
    }, []);

    useEffect(() => {
        const loadGeneration = ++taskViewLoadGenerationRef.current;
        let loadPending = true;

        dispatch(setLoadingMessage({
            message: 'Lade Aufgabenansicht',
            blocking: false,
            estimatedTime: 1000,
        }));

        setTaskView(null);
        setEditedAuthoredValues(null);
        setDerivedErrors(null);
        latestAuthoredValuesRef.current = {};
        new CustomerTaskViewApiService()
            .getTaskView(instanceAccessKey, taskAccessKey)
            .then((view) => {
                if (loadGeneration !== taskViewLoadGenerationRef.current) {
                    return;
                }

                setIdentityAuthenticationSucceeded(false);
                setTaskView(view);
                latestAuthoredValuesRef.current = view.data;
            })
            .catch((error) => {
                if (loadGeneration !== taskViewLoadGenerationRef.current) {
                    return;
                }

                if (handleRequiredIdentityAuthentication(error)) {
                    return;
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
            })
            .finally(() => {
                loadPending = false;
                if (loadGeneration !== taskViewLoadGenerationRef.current) {
                    return;
                }

                dispatch(clearLoadingMessage());
            });

        return () => {
            if (loadGeneration !== taskViewLoadGenerationRef.current) {
                return;
            }

            taskViewLoadGenerationRef.current += 1;
            if (loadPending) {
                dispatch(clearLoadingMessage());
            }
        };
    }, [dispatch, handleRequiredIdentityAuthentication, instanceAccessKey, taskAccessKey]);

    const handleDerive = useCallback((values: AuthoredElementValues, skipErrorsForElements: string[]) => {
        return new CustomerTaskViewApiService()
            .deriveTaskView(instanceAccessKey, taskAccessKey, values, skipErrorsForElements)
            .catch((error) => {
                if (handleRequiredIdentityAuthentication(error)) {
                    return createDerivedRuntimeElementData();
                }
                throw error;
            });
    }, [handleRequiredIdentityAuthentication, instanceAccessKey, taskAccessKey]);

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
            setEditedAuthoredValues(updatedTaskView.data);
            setDerivedErrors(null);
            latestAuthoredValuesRef.current = updatedTaskView.data;

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
        const taskViewEvent = typeof taskView !== 'string'
            ? taskView?.events.find((candidate) => candidate.event === event)
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
        return (
            <LoadingPlaceholder/>
        );
    }

    if (taskView == 'failed') {
        return null;
    }

    if (taskView === 'identity-required') {
        const wrongAccountAuthenticated = identityAuthenticationSucceeded;
        const authenticationStartLink = new CustomerTaskViewApiService()
            .createRequiredIdentityAuthenticationStartLink(instanceAccessKey, taskAccessKey);

        return (
            <Box sx={{maxWidth: 720, mx: 'auto'}}>
                <Alert severity={wrongAccountAuthenticated ? 'error' : 'info'}>
                    <Stack spacing={2}>
                        <Box>
                            <Typography variant="h6" component="h2" gutterBottom>
                                {wrongAccountAuthenticated ? 'Falsches Nutzerkonto' : 'Anmeldung erforderlich'}
                            </Typography>
                            <Typography>
                                {
                                    wrongAccountAuthenticated
                                        ? 'Das verwendete Nutzerkonto gehört nicht zur Empfängeridentität dieser Aufgabe. Melden Sie sich mit dem richtigen Nutzerkonto an.'
                                        : 'Für diese Aufgabe ist eine erneute Anmeldung erforderlich. Melden Sie sich mit dem Nutzerkonto an, an das diese Aufgabe gesendet wurde.'
                                }
                            </Typography>
                        </Box>
                        <Box>
                            <Button
                                component="a"
                                href={authenticationStartLink}
                                variant="contained"
                            >
                                Mit Nutzerkonto anmelden
                            </Button>
                        </Box>
                    </Stack>
                </Alert>
            </Box>
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
                onEvent={handleInlineEvent}
                taskViewMode="customer"
            />

            <TaskViewEventButtons
                events={taskView.events}
                onEvent={handleEventClick}
            />
        </Box>
    );
}
