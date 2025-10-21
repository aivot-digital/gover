import {ReactNode, useEffect} from 'react';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectSetup, selectStatus, setSetup, setStatus, ShellStatus} from '../../slices/shell-slice';
import {SystemApiService} from '../../modules/system/system-api-service';
import {SystemSetupDTO} from '../../modules/system/dtos/system-setup-dto';
import Box from '@mui/material/Box';
import {ShellProgress} from './components/shell-progress';
import {Outlet} from 'react-router-dom';
import {ShellLoader} from './components/shell-loader';
import {isApiError} from '../../models/api-error';
import {ShellOffline} from './components/shell-offline';
import {setSystemConfigsFromMap} from '../../slices/system-config-slice';

interface StaffShellProps {
    children?: ReactNode;
}

export function CustomerShell(props: StaffShellProps) {
    const {
        children,
    } = props;

    const dispatch = useAppDispatch();
    const setup = useAppSelector(selectSetup);
    const status = useAppSelector(selectStatus);

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

                            {children}
                            {
                                children == null &&
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
