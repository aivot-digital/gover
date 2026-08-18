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

const INSTANCE_POLL_INTERVAL_MS = 2000;

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
        }, INSTANCE_POLL_INTERVAL_MS);

        return () => {
            clearInterval(intervalId);
        };
    }, [fetchInstanceStatus]);

    useEffect(() => {
        if (instanceStatus == null || instanceStatus == 'failed' || instanceStatus.tasks.length === 0 || taskAccessKey != null) {
            return;
        }

        if (taskAccessKey == null) {
            navigate(`/process/${instanceAccessKey}/tasks/${instanceStatus.tasks[0].accessKey}`);
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
                instanceStatus.tasks.length == 0 &&
                <NoTaskToDoPlaceholder/>
            }

            {
                instanceStatus.tasks.length > 0 &&
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
