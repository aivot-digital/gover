import {Box, ThemeProvider, Typography, useTheme} from '@mui/material';
import {Outlet, useNavigate, useParams} from 'react-router-dom';
import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {
    buildCustomerInstancePath,
    buildCustomerTaskPath,
    CustomerTaskViewApiService,
    getActiveCustomerTasks,
    ProcessInstanceStatusResponse,
} from './customer-task-view-api-service';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {LoadingPlaceholder} from '../../../components/loading-placeholder/loading-placeholder';
import {setErrorMessage} from '../../../slices/shell-slice';
import {isApiError} from '../../../models/api-error';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {CustomerInstanceViewHeader} from "./customer-instance-view-header";
import {SnackbarProvider} from "../../../providers/snackbar-provider";
import {useAppSelector} from "../../../hooks/use-app-selector";
import {createAppTheme} from "../../../theming/themes";
import {BaseTheme} from "../../../theming/base-theme";
import {CustomerInstanceViewFooter} from "./customer-instance-view-footer";
import {PrivacyDialog, PrivacyDialogId} from "../../../dialogs/privacy-dialog/privacy-dialog";
import {showDialog} from "../../../slices/app-slice";
import {ImprintDialog, ImprintDialogId} from "../../../dialogs/imprint-dialog/imprint-dialog";
import {AccessibilityDialog, AccessibilityDialogId} from "../../../dialogs/accessibility-dialog/accessibility-dialog";
import {HelpDialog, HelpDialogId} from "../../../dialogs/help-dialog/help.dialog";

const INSTANCE_POLL_INTERVAL_MS = 2000;

export interface CustomerInstanceViewOutletContext {
    refreshInstanceStatus: () => Promise<void>;
    invalidateInstanceTasks: () => void;
}

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
    const baseTheme = useTheme();

    const theme = null;

    const [instanceStatus, setInstanceStatus] = useState<ProcessInstanceStatusResponse | null | 'failed'>(null);
    const statusRequestGenerationRef = useRef(0);

    const metaDialog = useAppSelector((state) => state.app.showDialog);

    const resolvedTheme = useMemo(() => {
        if (theme == null) {
            return baseTheme;
        }

        return createAppTheme(theme, BaseTheme, baseTheme.palette.mode);
    }, [baseTheme, theme]);

    const refreshInstanceStatus = useCallback(async (): Promise<void> => {
        const requestGeneration = ++statusRequestGenerationRef.current;
        try {
            const status = await new CustomerTaskViewApiService().getInstanceStatus(instanceAccessKey);

            if (requestGeneration === statusRequestGenerationRef.current) {
                setInstanceStatus(status);
            }
        } catch (error) {
            if (requestGeneration === statusRequestGenerationRef.current) {
                throw error;
            }

            // A newer status request or an explicit invalidation already superseded this request.
        }
    }, [instanceAccessKey]);

    const fetchInstanceStatus = useCallback(() => {
        void refreshInstanceStatus()
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
    }, [dispatch, refreshInstanceStatus]);

    const invalidateInstanceTasks = useCallback(() => {
        statusRequestGenerationRef.current += 1;
        setInstanceStatus((currentStatus) => {
            if (currentStatus == null || currentStatus === 'failed') {
                return currentStatus;
            }

            return {
                ...currentStatus,
                tasks: null,
            };
        });
    }, []);

    const outletContext = useMemo<CustomerInstanceViewOutletContext>(() => ({
        refreshInstanceStatus,
        invalidateInstanceTasks,
    }), [invalidateInstanceTasks, refreshInstanceStatus]);

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
        if (instanceStatus == null || instanceStatus === 'failed' || instanceStatus.tasks == null) {
            return;
        }

        const activeTasks = getActiveCustomerTasks(instanceStatus.tasks);
        if (taskAccessKey != null && activeTasks.some((task) => task.accessKey === taskAccessKey)) {
            return;
        }

        const nextTask = activeTasks[0];
        if (nextTask != null) {
            navigate(buildCustomerTaskPath(instanceAccessKey, nextTask.accessKey), {replace: true});
        } else if (taskAccessKey != null) {
            navigate(buildCustomerInstancePath(instanceAccessKey), {replace: true});
        }
    }, [instanceAccessKey, instanceStatus, navigate, taskAccessKey]);

    if (instanceStatus == null) {
        return (
            <LoadingPlaceholder/>
        );
    }

    if (instanceStatus == 'failed') {
        return null;
    }

    const activeTasks = getActiveCustomerTasks(instanceStatus.tasks);
    const selectedTaskIsActive = taskAccessKey != null && activeTasks.some((task) => task.accessKey === taskAccessKey);

    return (
        <ThemeProvider theme={resolvedTheme}>
            <SnackbarProvider>
                <CustomerInstanceViewHeader
                    status={instanceStatus}
                />

                <PageWrapper
                    title={instanceStatus.title}
                >
                    {
                        instanceStatus.tasks == null &&
                        <LoadingPlaceholder/>
                    }

                    {
                        instanceStatus.tasks != null && activeTasks.length === 0 &&
                        <NoTaskToDoPlaceholder/>
                    }

                    {
                        instanceStatus.tasks != null && activeTasks.length > 0 && !selectedTaskIsActive &&
                        <LoadingPlaceholder/>
                    }

                    {
                        instanceStatus.tasks != null && selectedTaskIsActive &&
                        <Outlet context={outletContext}/>
                    }
                </PageWrapper>

                <CustomerInstanceViewFooter
                    status={instanceStatus}
                />

                <PrivacyDialog
                    onHide={() => dispatch(showDialog(undefined))}
                    open={metaDialog === PrivacyDialogId}
                    departmentId={instanceStatus.privacyDepartmentId}
                />

                <ImprintDialog
                    onHide={() => dispatch(showDialog(undefined))}
                    open={metaDialog === ImprintDialogId}
                    departmentId={instanceStatus.imprintDepartmentId}
                />

                <AccessibilityDialog
                    onHide={() => dispatch(showDialog(undefined))}
                    open={metaDialog === AccessibilityDialogId}
                    departmentId={instanceStatus.accessibilityDepartmentId}
                />

                <HelpDialog
                    onHide={() => dispatch(showDialog(undefined))}
                    open={metaDialog === HelpDialogId}
                    technicalSupportDepartmentId={instanceStatus.technicalSupportDepartmentId}
                    legalSupportDepartmentId={instanceStatus.legalSupportDepartmentId}
                />
            </SnackbarProvider>
        </ThemeProvider>
    );
}

function NoTaskToDoPlaceholder() {
    return (
        <Box>
            Freuen Sie sich. Es gibt für Sie nichts zu tun!
        </Box>
    );
}
