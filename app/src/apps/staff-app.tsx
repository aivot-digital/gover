import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {createBrowserRouter as createRouter, RouterProvider} from 'react-router-dom';
import {selectMemberships, selectUser, setMemberships, setUser} from '../slices/user-slice';
import {selectSystemConfigValue, setSystemConfigs} from '../slices/system-config-slice';
import {Box, CircularProgress, type Theme as MuiTheme, ThemeProvider, Typography} from '@mui/material';
import {createAppTheme, createDefaultAppTheme} from '../theming/themes';
import {useAppDispatch} from '../hooks/use-app-dispatch';
import {useAppSelector} from '../hooks/use-app-selector';
import {staffAppRoutes} from './staff-app-routes';
import {Login} from '../pages/staff-pages/login/login';
import {SystemConfigKeys} from '../data/system-config-keys';
import {type Theme} from '../modules/themes/models/theme';
import {isStringNotNullOrEmpty, stringOrUndefined} from '../utils/string-utils';
import {useApi} from '../hooks/use-api';
import {setAuthData, updateAuthDataFromLocalStorage} from '../slices/auth-slice';
import {ApiService} from '../services/api-service';
import {AuthDataDto} from '../models/dtos/auth-data-dto';
import {getUrlWithoutQuery} from '../utils/location-utils';
import {SessionExpiredDialog} from '../dialogs/session-expired-dialog/session-expired-dialog';
import {AppConfig} from '../app-config';
import {LoadingOverlay} from '../components/loading-overlay/loading-overlay';
import {Error} from '../pages/shared/error/error';
import {providerLinksRoutes} from '../modules/provider-links/provider-links-routes';
import {secretsRoutes} from '../modules/secrets/secrets-routes';
import {departmentsRoutes} from '../modules/departments/departments-routes';
import {DepartmentMembershipsApiService} from '../modules/departments/department-memberships-api-service';
import {ThemesApiService} from '../modules/themes/themes-api-service';
import {SystemConfigsApiService} from '../modules/configs/system-configs-api-service';
import {RouterLayout} from './router-layout';
import {loader} from '@monaco-editor/react';
import * as monaco from 'monaco-editor';
import {AppProvider} from '../providers/app-provider';
import {UsersApiService} from '../modules/users/users-api-service';
import {ExpirationTimer} from '../components/auth-token-debugger/auth-token-debugger';
import {useLocalStorageEffect} from '../hooks/use-local-storage-effect';
import {AuthDataAccessToken} from '../models/dtos/auth-data';
import {StorageKey} from '../data/storage-key';

loader.config({monaco});

const router = createRouter(
    [
        {
            element: <RouterLayout />,
            errorElement: <Error />,
            children: [
                ...Object
                    .keys(staffAppRoutes)
                    .map((key) => staffAppRoutes[key]),
                ...departmentsRoutes,
                ...providerLinksRoutes,
                ...secretsRoutes,
            ],
        },
    ],
    {
        basename: '/staff',
    },
);

function StaffApp() {
    const dispatch = useAppDispatch();
    const api = useApi();

    const loadingOverlay = useAppSelector((state) => state.loadingOverlay);

    const user = useAppSelector(selectUser);
    const memberships = useAppSelector(selectMemberships);
    const themeId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.theme));

    const [theme, setTheme] = useState<Theme>();

    // Reload the auth data from the local storage if any change happens.
    // This is needed for the case when the user refrshes the tokens in another tab.
    const handleAuthDataChange = useCallback(() => {
        dispatch(updateAuthDataFromLocalStorage());
    }, []);
    useLocalStorageEffect<AuthDataAccessToken>(handleAuthDataChange, StorageKey.AuthDataAccessToken);

    const authCode = useMemo(() => {
        const searchParams = new URLSearchParams(window.location.search);
        const iss = searchParams.get('iss');
        if (iss != null && iss.endsWith('realms/' + AppConfig.staff.realm)) {
            const code = searchParams.get('code');
            return stringOrUndefined(code);
        }
        return undefined;
    }, []);

    useEffect(() => {
        if (authCode != null && authCode.length > 0) {
            new ApiService()
                .postFormUrlEncoded<AuthDataDto>(`${AppConfig.staff.host}/realms/${AppConfig.staff.realm}/protocol/openid-connect/token`, {
                    grant_type: 'authorization_code',
                    client_id: AppConfig.staff.client,
                    code: authCode,
                    redirect_uri: getUrlWithoutQuery(),
                })
                .then((authData) => {
                    if (authData != null) {
                        dispatch(setAuthData(authData));
                        window.location.search = '';
                    }
                })
                .catch((err) => {
                    console.error(err);
                });
        }
    }, [authCode]);

    useEffect(() => {
        if (authCode == null && themeId != null && isStringNotNullOrEmpty(themeId)) {
            if (theme == null || theme.id !== parseInt(themeId)) {
                new ThemesApiService(api)
                    .retrieve(parseInt(themeId))
                    .then(setTheme);
            }
        } else {
            setTheme(undefined);
        }
    }, [themeId, authCode]);

    useEffect(() => {
        if (authCode == null && api.isAuthenticated()) {
            if (user == null) {
                new UsersApiService(api)
                    .retrieveSelf()
                    .then((user) => dispatch(setUser(user)));
            }
        } else {
            dispatch(setUser(undefined));
            dispatch(setMemberships([]));
        }
    }, [api, authCode]);

    useEffect(() => {
        if (api.isAuthenticated()) {
            new SystemConfigsApiService(api)
                .listAll()
                .then((systemConfigs) => {
                    if (systemConfigs != null && systemConfigs.content != null) {
                        dispatch(setSystemConfigs(systemConfigs.content));
                    }
                });
        } else {
            new SystemConfigsApiService(api)
                .listPublicAll()
                .then((systemConfigs) => {
                    if (systemConfigs != null && systemConfigs.content != null) {
                        dispatch(setSystemConfigs(systemConfigs.content));
                    }
                });
        }
    }, [api]);

    useEffect(() => {
        if (authCode == null && api.isAuthenticated() && user != null) {
            new DepartmentMembershipsApiService(api)
                .listAll({
                    userId: user.id,
                })
                .then((memberships) => dispatch(setMemberships(memberships.content)));
        }
    }, [authCode, api, user]);

    if (!api.isAuthenticated()) {
        return (
            <ThemeProvider theme={createDefaultAppTheme}>
                <Login />
            </ThemeProvider>
        );
    }

    if (api.isAuthenticated() && (user == null || memberships == null)) {
        return (
            <Box
                sx={{
                    width: '100%',
                    height: '100vh',
                    display: 'flex',
                    justifyContent: 'center',
                    alignItems: 'center',
                }}
            >
                <CircularProgress />
                <Typography sx={{ml: 2}}>Wird geladen…</Typography>
            </Box>
        );
    }

    return (
        <AppProvider theme={(baseTheme: MuiTheme) => createAppTheme(theme, baseTheme)}>
            <RouterProvider router={router} />

            <LoadingOverlay
                isLoading={loadingOverlay.open && typeof (loadingOverlay.open) != undefined}
                message={loadingOverlay.message}
            />

            <ExpirationTimer />

            <SessionExpiredDialog />
        </AppProvider>
    );
}

export default StaffApp;
