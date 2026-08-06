import {Box, Stack, Typography} from '@mui/material';
import {Outlet, useNavigate, useParams} from 'react-router-dom';
import {useCallback, useEffect, useState} from 'react';
import {CustomerTaskViewApiService, ProcessInstanceStatusResponse} from './customer-task-view-api-service';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {LoadingPlaceholder} from '../../../components/loading-placeholder/loading-placeholder';
import {setErrorMessage} from '../../../slices/shell-slice';
import {isApiError} from '../../../models/api-error';
import {ProcessInstanceStatusIcon} from '../../../modules/process/components/process-instance-status-icon';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';

export function CustomerInstanceView() {
    const {
        instanceAccessKey = '',
        taskAccessKey,
    } = useParams<{
        instanceAccessKey: string;
        taskAccessKey?: string;
    }>();

    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const [instanceStatus, setInstanceStatus] = useState<ProcessInstanceStatusResponse | null | 'failed'>(null);

    const fetchInstanceStatus = useCallback(() => {
        new CustomerTaskViewApiService()
            .getInstanceStatus(instanceAccessKey)
            .then(setInstanceStatus)
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
                setInstanceStatus('failed');
            });
    }, [instanceAccessKey]);

    useEffect(() => {
        fetchInstanceStatus();

        const intervalId = setInterval(() => {
            fetchInstanceStatus();
        }, 1000);

        return () => {
            clearInterval(intervalId);
        };
    }, [fetchInstanceStatus]);

    useEffect(() => {
        if (instanceStatus == null || instanceStatus == 'failed' || instanceStatus.currentTasks.length === 0) {
            return;
        }

        if (taskAccessKey == null) {
            navigate(`/process/${instanceAccessKey}/tasks/${instanceStatus.currentTasks[0]}`);
        }
    }, [instanceStatus]);

    if (instanceStatus == null) {
        return (
            <LoadingPlaceholder/>
        );
    }

    if (instanceStatus == 'failed') {
        return null;
    }

    return (
        <PageWrapper
            title={instanceStatus.title}
        >
            <Stack
                direction="row"
                spacing={2}
            >
                <Typography>
                    {instanceStatus.title}
                </Typography>

                <ProcessInstanceStatusIcon
                    status={instanceStatus.status}
                />
            </Stack>

            {
                instanceStatus.currentTasks.length == 0 &&
                <NoTaskToDoPlaceholder/>
            }

            {
                instanceStatus.currentTasks.length > 0 &&
                <Outlet/>
            }
        </PageWrapper>
    );
}

function NoTaskToDoPlaceholder() {
    return (
        <Box>
            Freuen Sie sich. Es gibt für Sie nichts zu tun!
        </Box>
    );
}
