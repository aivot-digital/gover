import React, {useEffect, useMemo, useState} from 'react';
import {createBrowserRouter as createRouter, RouterProvider} from 'react-router-dom';
import {selectMemberships, selectUser, setMemberships, setUser} from '../slices/user-slice';
import {selectSystemConfigValue, setSystemConfigs} from '../slices/system-config-slice';
import {Alert, Box, CircularProgress, Snackbar, type Theme as MuiTheme, ThemeProvider, Typography} from '@mui/material';
import {createAppTheme, createDefaultAppTheme} from '../theming/themes';
import {useAppDispatch} from '../hooks/use-app-dispatch';
import {useAppSelector} from '../hooks/use-app-selector';
import {resetSnackbar} from '../slices/snackbar-slice';
import {staffAppRoutes} from './staff-app-routes';
import {Login} from '../pages/staff-pages/login/login';
import {SystemConfigKeys} from '../data/system-config-keys';
import {type Theme} from '../models/entities/theme';
import {isStringNotNullOrEmpty, stringOrUndefined} from '../utils/string-utils';
import {useUsersApi} from '../hooks/use-users-api';
import {useMembershipsApi} from '../hooks/use-memberships-api';
import {useSystemConfigsApi} from '../hooks/use-system-configs-api';
import {useApi} from '../hooks/use-api';
import {useThemesApi} from '../hooks/use-themes-api';
import {setAuthData} from '../slices/auth-slice';
import {ApiService} from '../services/api-service';
import {AuthDataDto} from '../models/dtos/auth-data-dto';
import {getUrlWithoutQuery} from '../utils/location-utils';
import {SessionExpiredDialog} from '../dialogs/session-expired-dialog/session-expired-dialog';
import {AppConfig} from '../app-config';
import {LoadingOverlay} from '../components/loading-overlay/loading-overlay';

const router = createRouter(
    Object.keys(staffAppRoutes).map((key) => staffAppRoutes[key]),
    {
        basename: '/staff',
    },
);

function StaffApp(): JSX.Element {
    const dispatch = useAppDispatch();
    const api = useApi();

    const snackbar = useAppSelector((state) => state.snackbar);
    const loadingOverlay = useAppSelector((state) => state.loadingOverlay);

    const user = useAppSelector(selectUser);
    const memberships = useAppSelector(selectMemberships);
    const themeId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.theme));

    const [theme, setTheme] = useState<Theme>();

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
                useThemesApi(api)
                    .retrieveTheme(parseInt(themeId))
                    .then(setTheme);
            }
        } else {
            setTheme(undefined);
        }
    }, [themeId, authCode]);

    useEffect(() => {
        if (authCode == null && api.isAuthenticated()) {
            if (user == null) {
                useUsersApi(api)
                    .self()
                    .then((user) => dispatch(setUser(user)));
            }
        } else {
            dispatch(setUser(undefined));
            dispatch(setMemberships([]));
        }
    }, [api, authCode]);

    useEffect(() => {
        const {
            list,
            listPublic,
        } = useSystemConfigsApi(api);

        if (authCode == null && api.isAuthenticated()) {
            list()
                .then((systemConfigs) => dispatch(setSystemConfigs(systemConfigs)));
        } else {
            listPublic()
                .then((systemConfigs) => dispatch(setSystemConfigs(systemConfigs)));
        }
    }, [api, authCode]);

    useEffect(() => {
        if (authCode == null && api.isAuthenticated() && user != null) {
            useMembershipsApi(api)
                .list({user: user.id})
                .then((memberships) => dispatch(setMemberships(memberships)));
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
        <ThemeProvider theme={(baseTheme: MuiTheme) => createAppTheme(theme, baseTheme)}>
            <RouterProvider router={router} />

            <Snackbar
                open={snackbar.open && typeof (snackbar.open) != undefined}
                autoHideDuration={6000}
                onClose={() => dispatch(resetSnackbar())}
            >
                <Alert
                    onClose={() => dispatch(resetSnackbar())}
                    severity={snackbar.severity}
                    sx={{
                        width: '100%',
                    }}
                >
                    {snackbar.message}
                </Alert>
            </Snackbar>

            <LoadingOverlay
                isLoading={loadingOverlay.open && typeof (loadingOverlay.open) != undefined}
                message={loadingOverlay.message}
            />

            <SessionExpiredDialog />
        </ThemeProvider>
    );
}

export default StaffApp;
