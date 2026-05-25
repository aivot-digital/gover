import {useEffect, useMemo} from 'react';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {useAppSelector} from '../../hooks/use-app-selector';
import {
    addSnackbarMessage,
    ErrorMessage,
    selectErrorMessage,
    selectStatus,
    setErrorMessage,
    setStatus,
    ShellStatus,
    SnackbarSeverity,
    SnackbarType,
} from '../../slices/shell-slice';
import Box from '@mui/material/Box';
import {ShellProgress} from './components/shell-progress';
import {Outlet, useLocation, useRouteError} from 'react-router-dom';
import {ShellLoader} from './components/shell-loader';
import {ShellOffline} from './components/shell-offline';
import {API_EVENT_UNREACHABLE} from '../../services/base-api-service';
import {StaffShellError} from '../staff/staff-shell-error';
import {DuplicatePageWarning} from '../../components/duplicate-page-warning/duplicate-page-warning';

export function CustomerShell() {
    const routerError = useRouteError();
    const dispatch = useAppDispatch();
    const status = useAppSelector(selectStatus);
    const appError = useAppSelector(selectErrorMessage);
    const location = useLocation();

    // Display a message if the API becomes unreachable.
    useEffect(() => {
        window.addEventListener(API_EVENT_UNREACHABLE, function () {
            dispatch(addSnackbarMessage({
                key: 'api-unreachable',
                message: 'Die Verbindung zum Server wurde unterbrochen. Bitte überprüfen Sie Ihre Internetverbindung und versuchen Sie es erneut.',
                severity: SnackbarSeverity.Error,
                type: SnackbarType.Dismissable,
            }));
        });
    }, []);

    const error: ErrorMessage | undefined = useMemo(() => {
        if (routerError == null && appError == null) {
            return undefined;
        }

        if (routerError != null && typeof routerError === 'object' && 'status' in routerError) {
            return {
                status: routerError.status as number,
                message: undefined,
            };
        }

        if (appError != null) {
            return {
                status: appError.status,
                message: appError.message,
            };
        }

        return {
            status: 500,
            message: undefined,
        };
    }, [routerError, appError]);

    useEffect(() => {
        dispatch(setErrorMessage(undefined));
    }, [location]);

    if (status === ShellStatus.Offline) {
        return (
            <ShellOffline/>
        );
    }

    useEffect(() => {
        dispatch(setStatus(ShellStatus.Ready));
    }, []);

    return (
        <>
            {
                status === ShellStatus.Loading &&
                <ShellLoader/>
            }
            {
                status === ShellStatus.Ready &&
                <>
                    <Box
                        sx={{
                            display: 'flex',
                            flexDirection: 'row',
                            height: '100vh',
                        }}
                    >
                        <Box
                            sx={{
                                flex: 1,
                                position: 'relative',
                                overflowY: 'auto',
                            }}
                        >
                            <ShellProgress/>

                            {
                                error != null &&
                                <StaffShellError error={error}/>
                            }
                            {
                                error == null &&
                                <Outlet/>
                            }
                        </Box>
                    </Box>
                    <DuplicatePageWarning/>
                </>
            }
        </>
    );
}
