import {Box} from '@mui/material';
import {useEffect, useState} from 'react';
import {CustomerTaskViewApiService, TaskViewResponse} from './customer-task-view-api-service';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {LoadingPlaceholder} from '../../../components/loading-placeholder/loading-placeholder';
import {clearLoadingMessage, setErrorMessage, setLoadingMessage} from '../../../slices/shell-slice';
import {isApiError} from '../../../models/api-error';
import {ElementDerivationContext} from '../../../modules/elements/components/element-derivation-context';
import {AuthoredElementValues} from '../../../models/element-data';
import {useParams} from 'react-router-dom';

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
            })
    }, [taskAccessKey]);

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
            />
        </Box>
    );
}