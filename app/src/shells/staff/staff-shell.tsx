import {ReactNode, useEffect} from 'react';
import {User} from '../../modules/users/models/user';
import {DepartmentMembership} from '../../modules/departments/models/department-membership';
import {Page} from '../../models/dtos/page';
import {setMemberships, setUser} from '../../slices/user-slice';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {SystemConfigResponseDto} from '../../modules/configs/dtos/system-config-response-dto';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectSetup, selectStatus, setSetup, setStatus, ShellStatus} from '../../slices/shell-slice';
import {SystemApiService} from '../../modules/system/system-api-service';
import {SystemSetupDTO} from '../../modules/system/dtos/system-setup-dto';
import {setSystemConfigs, setSystemConfigsFromMap} from '../../slices/system-config-slice';
import {Login} from '../../pages/staff-pages/login/login';
import Box from '@mui/material/Box';
import {ShellDrawer} from './components/shell-drawer';
import {ShellProgress} from './components/shell-progress';
import {ShellSearchDialog} from './components/shell-search-dialog';
import {ShellSnackbarContainer} from './components/shell-snackbar-container';
import {BaseApiService} from '../../services/base-api-service';
import {Outlet} from 'react-router-dom';
import {ShellSessionEndWarnPopup} from './components/shell-session-end-warn-popup';
import {ShellLoader} from './components/shell-loader';
import {AuthService} from '../../services/auth-service';
import {ShellSessionExpiredDialog} from './components/shell-session-expired-dialog';
import {isApiError} from '../../models/api-error';
import {ShellOffline} from './components/shell-offline';
import {isStringNotNullOrEmpty} from '../../utils/string-utils';

interface StaffShellProps {
    children?: ReactNode;
}

export function StaffShell(props: StaffShellProps) {
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
            })
            .catch((err) => {
                if (isApiError(err) && err.status >= 500) {
                    dispatch(setStatus(ShellStatus.Offline));
                } else {
                    console.error(err);
                }
            });
    }, []);

    // Fetch the auth state after the setup for a more consistent startup order.
    useEffect(() => {
        if (setup == null) {
            return;
        }

        const search = new URLSearchParams(window.location.search);
        if (isStringNotNullOrEmpty(search.get('logout'))) {
            new AuthService().logout();

            dispatch(setUser(undefined));
            dispatch(setMemberships([]));
            dispatch(setStatus(ShellStatus.Login));

            return;
        }

        authenticateWithOidcCode(search)
            .then((res) => {
                if (res != null) {
                    dispatch(setUser(res.user));
                    dispatch(setMemberships(res.memberships));
                    dispatch(setSystemConfigs(res.configs));
                    dispatch(setStatus(ShellStatus.Ready));
                } else {
                    dispatch(setStatus(ShellStatus.Login));
                }
            })
            .catch((err) => {
                console.error(err);
                dispatch(setStatus(ShellStatus.Login));
            });
    }, [setup]);

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
                status === ShellStatus.Login &&
                <Login />
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
                        <ShellDrawer />

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

                    <ShellSearchDialog />
                    <ShellSessionEndWarnPopup />
                    <ShellSessionExpiredDialog />
                    <ShellSnackbarContainer />
                </>
            }
        </>
    );
}

async function fetchSetup(): Promise<SystemSetupDTO> {
    return new SystemApiService()
        .fetchSetup();
}

async function authenticateWithOidcCode(searchParams: URLSearchParams): Promise<{
    user: User;
    memberships: DepartmentMembership[];
    configs: SystemConfigResponseDto[];
} | undefined> {
    const authService = new AuthService();
    const apiService = new BaseApiService();

    if (!authService.isAuthenticated()) {
        const iss = searchParams.get('iss');
        const code = searchParams.get('code');

        if (iss == null || code == null) {
            return undefined;
        }

        await authService
            .authenticate(code);

        window.location.search = '';
    }

    const user = await apiService
        .get<User>('/api/users/self/');

    const membershipsPage = await apiService
        .get<Page<DepartmentMembership>>('/api/department-memberships/', {
            query: new URLSearchParams({
                userId: user.id,
            }),
        });
    const memberships = membershipsPage.content;

    const configsPage = await apiService
        .get<Page<SystemConfigResponseDto>>('/api/system-configs/');

    const configs = configsPage.content;

    return {
        user,
        memberships,
        configs,
    };
}