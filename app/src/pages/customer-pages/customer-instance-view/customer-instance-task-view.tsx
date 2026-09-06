import {Box} from '@mui/material';
import {useCallback, useEffect, useRef, useState} from 'react';
import {
    buildCustomerInstancePath,
    CustomerTaskViewApiService,
    TaskViewResponse,
} from './customer-task-view-api-service';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {LoadingPlaceholder} from '../../../components/loading-placeholder/loading-placeholder';
import {clearLoadingMessage, setErrorMessage, setLoadingMessage} from '../../../slices/shell-slice';
import {isApiError} from '../../../models/api-error';
import {ElementDerivationContext} from '../../../modules/elements/components/element-derivation-context';
import {
    AuthoredElementValues,
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

    const [taskView, setTaskView] = useState<TaskViewResponse | null | 'failed'>(null);
    const [editedAuthoredValues, setEditedAuthoredValues] = useState<AuthoredElementValues | null>(null);
    const [derivedErrors, setDerivedErrors] = useState<DerivedRuntimeElementData | null>(null);
    const latestAuthoredValuesRef = useRef<AuthoredElementValues>({});
    const taskViewLoadGenerationRef = useRef(0);

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

                setTaskView(view);
                latestAuthoredValuesRef.current = view.data;
            })
            .catch((error) => {
                if (loadGeneration !== taskViewLoadGenerationRef.current) {
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
    }, [dispatch, instanceAccessKey, taskAccessKey]);

    const handleDerive = useCallback((values: AuthoredElementValues, skipErrorsForElements: string[]) => {
        return new CustomerTaskViewApiService()
            .deriveTaskView(instanceAccessKey, taskAccessKey, values, skipErrorsForElements);
    }, [instanceAccessKey, taskAccessKey]);

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
        const taskViewEvent = taskView !== 'failed' ? taskView?.events.find((candidate) => candidate.event === event) : undefined;

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
