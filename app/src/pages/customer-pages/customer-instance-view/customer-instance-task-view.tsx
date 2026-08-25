import {Box} from '@mui/material';
import {useCallback, useEffect, useState} from 'react';
import {CustomerTaskViewApiService, TaskViewResponse} from './customer-task-view-api-service';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {LoadingPlaceholder} from '../../../components/loading-placeholder/loading-placeholder';
import {clearLoadingMessage, setErrorMessage, setLoadingMessage} from '../../../slices/shell-slice';
import {isApiError} from '../../../models/api-error';
import {ElementDerivationContext} from '../../../modules/elements/components/element-derivation-context';
import {
    AuthoredElementValues,
} from '../../../models/element-data';
import {useParams} from 'react-router-dom';
import {ProcessInstanceTaskApiService} from '../../../modules/process/services/process-instance-task-api-service';

export function CustomerInstanceTaskView() {
    const {
        instanceAccessKey = '',
        taskAccessKey = '',
    } = useParams<{
        instanceAccessKey: string;
        taskAccessKey: string;
    }>();

    const dispatch = useAppDispatch();

    const [taskView, setTaskView] = useState<TaskViewResponse | null | 'failed'>(null);
    const [editedAuthoredValues, setEditedAuthoredValues] = useState<AuthoredElementValues | null>(null);

    useEffect(() => {
        dispatch(setLoadingMessage({
            message: 'Lade Aufgabenansicht',
            blocking: false,
            estimatedTime: 1000,
        }));

        setTaskView(null);
        setEditedAuthoredValues(null);
        new CustomerTaskViewApiService()
            .getTaskView(instanceAccessKey, taskAccessKey)
            .then(setTaskView)
            .catch((error) => {
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
                dispatch(clearLoadingMessage());
            });
    }, [dispatch, instanceAccessKey, taskAccessKey]);

    const handleDerive = useCallback((values: AuthoredElementValues, skipErrorsForElements: string[]) => {
        return new CustomerTaskViewApiService()
            .deriveTaskView(instanceAccessKey, taskAccessKey, values, skipErrorsForElements);
    }, [instanceAccessKey, taskAccessKey]);

    const handleTaskEvent = useCallback((values: AuthoredElementValues, event: string) => {
        dispatch(setLoadingMessage({
            message: 'Verarbeite Aktion',
            blocking: true,
            estimatedTime: 500,
        }));

        return new ProcessInstanceTaskApiService()
            .putCustomerTaskView(instanceAccessKey, taskAccessKey, values, event)
            .then((updatedTaskView) => {
                setTaskView(updatedTaskView);
                setEditedAuthoredValues(updatedTaskView.data);
            })
            .catch((error) => {
                if (isApiError(error) && error.displayableToUser) {
                    dispatch(setErrorMessage({
                        message: error.message,
                        status: error.status,
                    }));
                } else {
                    dispatch(setErrorMessage({
                        message: 'Die Aufgabe konnte nicht verarbeitet werden.',
                        status: isApiError(error) ? error.status : 500,
                    }));
                }
            })
            .finally(() => {
                dispatch(clearLoadingMessage());
            });
    }, [dispatch, instanceAccessKey, taskAccessKey]);

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
                onAuthoredElementValuesChange={setEditedAuthoredValues}
                onDeriveOverride={handleDerive}
                onEvent={handleTaskEvent}
                taskViewMode="customer"
            />
        </Box>
    );
}
