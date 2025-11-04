import {ReactNode, useEffect, useMemo} from 'react';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {useAppSelector} from '../../hooks/use-app-selector';
import {addSnackbarMessage, ErrorMessage, selectErrorMessage, selectSetup, selectStatus, setErrorMessage, setSetup, setStatus, ShellStatus, SnackbarSeverity, SnackbarType} from '../../slices/shell-slice';
import {SystemApiService} from '../../modules/system/system-api-service';
import {SystemSetupDTO} from '../../modules/system/dtos/system-setup-dto';
import Box from '@mui/material/Box';
import {ShellProgress} from './components/shell-progress';
import {Outlet, useLocation, useRouteError} from 'react-router-dom';
import {ShellLoader} from './components/shell-loader';
import {isApiError} from '../../models/api-error';
import {ShellOffline} from './components/shell-offline';
import {setSystemConfigsFromMap} from '../../slices/system-config-slice';
import {API_EVENT_UNREACHABLE} from '../../services/base-api-service';
import {StaffShellError} from '../staff/staff-shell-error';

export function CustomerShell() {
    const routerError = useRouteError();
    const dispatch = useAppDispatch();
    const setup = useAppSelector(selectSetup);
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

    // Fetch the setup on mount to determine if the system is online, the theme, logo, etc.
    useEffect(() => {
        fetchSetup()
            .then((setup) => {
                dispatch(setSetup(setup));
                dispatch(setSystemConfigsFromMap(setup.publicConfigs));
                dispatch(setStatus(ShellStatus.Ready));
            })
            .catch((err) => {
                if (isApiError(err) && err.status >= 500) {
                    dispatch(setStatus(ShellStatus.Offline));
                } else {
                    console.error(err);
                }
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
            <ShellOffline />
        );
    }

    if (setup == null) {
        return null;
    }

    return (
        <>
            {
                status === ShellStatus.Loading &&
                <ShellLoader />
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
                            <ShellProgress />

                            {
                                error != null &&
                                <StaffShellError error={error} />
                            }
                            {
                                error == null &&
                                <Outlet />
                            }
                        </Box>
                    </Box>
                </>
            }
        </>
    );
}

async function fetchSetup(): Promise<SystemSetupDTO> {
    return new SystemApiService()
        .fetchSetup();
}
