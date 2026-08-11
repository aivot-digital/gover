import React, {type ReactNode, useCallback, useEffect, useLayoutEffect, useMemo, useRef} from 'react';
import {type User} from '../../modules/users/models/user';
import {selectUser, setMemberships, setPermissions, setUser} from '../../slices/user-slice';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {useAppSelector} from '../../hooks/use-app-selector';
import {
    addSnackbarMessage,
    type ErrorMessage,
    selectErrorMessage,
    selectStatus,
    setErrorMessage,
    setStatus,
    ShellStatus,
    SnackbarSeverity,
    SnackbarType,
} from '../../slices/shell-slice';
import {setSystemConfigsFromMap} from '../../slices/system-config-slice';
import {Login} from '../../pages/staff-pages/login/login';
import Box from '@mui/material/Box';
import {ShellDrawer} from './components/shell-drawer';
import {ShellProgress} from './components/shell-progress';
import {ShellSearchDialog} from './components/shell-search-dialog';
import {API_EVENT_UNREACHABLE} from '../../services/base-api-service';
import {Outlet, useLocation, useRouteError} from 'react-router-dom';
import {ShellSessionEndWarnPopup} from './components/shell-session-end-warn-popup';
import {ShellLoader} from './components/shell-loader';
import {AuthService} from '../../services/auth-service';
import {ShellSessionExpiredDialog} from './components/shell-session-expired-dialog';
import {ShellOffline} from './components/shell-offline';
import {isStringNotNullOrEmpty} from '../../utils/string-utils';
import {ShellResolutionOverlay} from './components/shell-resolution-overlay';
import {StaffShellError} from './staff-shell-error';
import {
    VDepartmentMembershipWithDetailsService,
} from '../../modules/departments/services/v-department-membership-with-details-service';
import {
    type VDepartmentMembershipWithDetailsEntity,
} from '../../modules/departments/entities/v-department-membership-with-details-entity';
import {UsersApiService} from '../../modules/users/users-api-service';
import {PermissionApiService} from '../../modules/permissions/permission-api-service';
import {PermissionSet} from '../../modules/permissions/models/permission-set';
import {PreReleaseVersionNoticeDialog} from '../../dialogs/pre-release-version-notice-dialog/pre-release-version-notice-dialog';
import {DuplicatePageWarning} from '../../components/duplicate-page-warning/duplicate-page-warning';
import {isApiError} from '../../models/api-error';
import {useCrossTabInvalidation} from '../../hooks/use-cross-tab-invalidation';
import {
    PERMISSION_SET_INVALIDATION_KEY,
    useRefreshPermissionSet,
} from '../../modules/permissions/hooks/use-permissions';

export function StaffShell(): ReactNode {
    const routerError = useRouteError();
    const dispatch = useAppDispatch();
    const status = useAppSelector(selectStatus);
    const appError = useAppSelector(selectErrorMessage);
    const user = useAppSelector(selectUser);
    const refreshPermissionSet = useRefreshPermissionSet();
    const contentContainerRef = useRef<HTMLElement>(null);

    const location = useLocation();

    useLayoutEffect(() => {
        // The staff shell owns page scrolling instead of the browser window. Reset it
        // before paint so every internal route opens at the top without a visible jump.
        if (contentContainerRef.current != null) {
            contentContainerRef.current.scrollTop = 0;
            contentContainerRef.current.scrollLeft = 0;
        }
    }, [location.pathname]);

    const handlePermissionSetInvalidation = useCallback(async () => {
        await refreshPermissionSet({broadcast: false});
    }, [refreshPermissionSet]);

    const handlePermissionSetInvalidationError = useCallback((err: unknown) => {
        console.error(err);
        dispatch(addSnackbarMessage({
            key: 'permission-set-cross-tab-refresh-failed',
            message: 'Die Berechtigungen konnten nach einer Änderung in einem anderen Tab nicht aktualisiert werden.',
            severity: SnackbarSeverity.Warning,
            type: SnackbarType.Dismissable,
        }));
    }, [dispatch]);

    useCrossTabInvalidation({
        key: PERMISSION_SET_INVALIDATION_KEY,
        scope: user?.id,
        enabled: status === ShellStatus.Ready && user?.id != null,
        deferWhileHidden: true,
        onInvalidate: handlePermissionSetInvalidation,
        onError: handlePermissionSetInvalidationError,
    });

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

    // Fetch the auth state after the setup for a more consistent startup order.
    useEffect(() => {
        const search = new URLSearchParams(window.location.search);
        if (isStringNotNullOrEmpty(search.get('logout'))) {
            AuthService.logout();

            dispatch(setUser(undefined));
            dispatch(setMemberships([]));
            dispatch(setPermissions(undefined));
            dispatch(setStatus(ShellStatus.Login));

            return;
        }

        authenticateWithOidcCode()
            .then((res) => {
                if (res != null) {
                    dispatch(setUser(res.user));
                    dispatch(setMemberships(res.memberships));
                    dispatch(setPermissions(res.permissionSet));
                    dispatch(setSystemConfigsFromMap(res.configs));
                    dispatch(setStatus(ShellStatus.Ready));
                } else {
                    dispatch(setStatus(ShellStatus.Login));
                }
            })
            .catch((err) => {
                if (isApiError(err) && err.status >= 501) {
                    dispatch(setStatus(ShellStatus.Offline));
                } else {
                    dispatch(setStatus(ShellStatus.Login));
                }
                console.error(err);
            });
    }, []);

    const error: ErrorMessage | undefined = useMemo(() => {
        if (routerError == null && appError == null) {
            return undefined;
        }

        if (routerError != null && typeof routerError === 'object' && 'status' in routerError) {
            const message = 'message' in routerError && typeof routerError.message === 'string'
                ? routerError.message
                : undefined;

            return {
                status: routerError.status as number,
                message,
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

    return (
        <>
            {
                status === ShellStatus.Loading &&
                <ShellLoader/>
            }
            {
                status === ShellStatus.Login &&
                <Login/>
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
                        <ShellDrawer/>

                        <Box
                            ref={contentContainerRef}
                            data-confetti-container="staff-shell-content"
                            sx={{
                                flex: 1,
                                position: 'relative',
                                overflowY: 'auto',
                                backgroundColor: 'background.default',
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

                    <ShellSearchDialog/>
                    <ShellSessionEndWarnPopup/>
                    <ShellSessionExpiredDialog/>
                    <ShellResolutionOverlay/>
                    <PreReleaseVersionNoticeDialog/>
                    <DuplicatePageWarning/>
                </>
            }
        </>
    );
}

async function authenticateWithOidcCode(): Promise<{
    user: User;
    memberships: VDepartmentMembershipWithDetailsEntity[];
    configs: Record<string, any>;
    permissionSet: PermissionSet;
} | undefined> {
    await AuthService.refresh();

    const user = await new UsersApiService()
        .retrieveSelf();

    const membershipsPage = await new VDepartmentMembershipWithDetailsService()
        .listAll({
            userId: user.id,
        });
    const memberships = membershipsPage.content;

    const permissionSet = await new PermissionApiService()
        .getOwnPermissionSet();

    const configs = AppConfig.systemConfigs;

    return {
        user,
        memberships,
        configs,
        permissionSet,
    };
}
