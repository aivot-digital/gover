import {ReactNode, useEffect} from 'react';
import {AuthDataDto} from '../../models/dtos/auth-data-dto';
import {getUrlWithoutQuery} from '../../utils/location-utils';
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
import {setSystemConfigs} from '../../slices/system-config-slice';
import {Login} from '../../pages/staff-pages/login/login';
import Box from '@mui/material/Box';
import {ShellDrawer} from './components/shell-drawer';
import {ShellProgress} from './components/shell-progress';
import {ShellSearchDialog} from './components/shell-search-dialog';
import {ShellSnackbarContainer} from './components/shell-snackbar-container';
import {BaseApiService, getLocalStorageJwt, storeLocalStorageJwt} from '../../services/base-api-service';
import {Outlet} from 'react-router-dom';
import {ShellSessionEndWarnPopup} from './components/shell-session-end-warn-popup';
import {ShellLoader} from './components/shell-loader';

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

    useEffect(() => {
        fetchSetup()
            .then((setup) => {
                dispatch(setSetup(setup));
            });

        authenticateWithOidcCode()
            .then((res) => {
                if (res != null) {
                    dispatch(setUser(res.user));
                    dispatch(setMemberships(res.memberships));
                    dispatch(setSystemConfigs(res.configs));
                    dispatch(setStatus(ShellStatus.Ready));
                } else {
                    dispatch(setStatus(ShellStatus.Login));
                }
            });
    }, []);

    if (setup == null) {
        return <div />;
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
                    {/*<ShellSessionExpiredDialog />*/}
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

async function authenticateWithOidcCode(): Promise<{
    user: User;
    memberships: DepartmentMembership[];
    configs: SystemConfigResponseDto[];
} | undefined> {
    const apiService = new BaseApiService();

    let jwt = getLocalStorageJwt();

    if (jwt == null) {
        const searchParams = new URLSearchParams(window.location.search);
        const iss = searchParams.get('iss');
        const code = searchParams.get('code');

        if (iss == null || code == null) {
            return undefined;
        }

        const payload = new URLSearchParams({
            grant_type: 'authorization_code',
            client_id: AppConfig.oidc.client,
            code: code,
            redirect_uri: getUrlWithoutQuery(),
        });

        const authData = await apiService
            .postFormUrlEncodedUnauthenticated<AuthDataDto>(iss + '/protocol/openid-connect/token', payload);

        jwt = {
            access: {
                token: authData.access_token,
                expires: (Date.now() + (authData.expires_in * 1000)) - 60000,
            },
            refresh: {
                token: authData.refresh_token,
                expires: (Date.now() + (authData.refresh_expires_in * 1000)) - 60000,
            },
        };

        storeLocalStorageJwt(jwt);

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