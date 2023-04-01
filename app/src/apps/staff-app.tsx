import React, {FunctionComponent, useEffect} from 'react';
import {HashRouter, Route, Routes} from 'react-router-dom';
import {ApplicationOverview} from '../pages/staff-pages/application-overview/application-overview';
import {ApplicationEditor} from '../pages/staff-pages/application-editor/application-editor';
import {Login} from '../pages/staff-pages/login/login';
import {DepartmentsOverview} from '../pages/staff-pages/departments-overview/departments-overview';
import {DestinationsOverview} from '../pages/staff-pages/destinations-overview/destinations-overview';
import {refreshUser,} from '../slices/user-slice';
import axios from 'axios';
import {Settings} from '../pages/staff-pages/settings/settings';
import {Profile} from '../pages/staff-pages/profile/profile';
import {fetchSystemConfig, selectSystemConfigValue} from '../slices/system-config-slice';
import {ProviderLinksOverview} from '../pages/staff-pages/provider-links-overview/provider-links-overview';
import {Alert, Snackbar, Theme, ThemeProvider} from '@mui/material';
import {createAppTheme} from '../theming/themes';
import {SystemConfigKeys} from '../data/system-config-keys';
import {PresetsOverview} from '../pages/staff-pages/presets-overview/presets-overview';
import {PresetEditor} from '../pages/staff-pages/preset-editor/preset-editor';
import {useAppDispatch} from '../hooks/use-app-dispatch';
import {useAppSelector} from '../hooks/use-app-selector';
import {resetSnackbar} from '../slices/snackbar-slice';
import {logout, selectAuthenticationState} from '../slices/auth-slice';
import {AuthState} from "../data/auth-state";

const routes: [string, FunctionComponent][] = [
    ['/', Login],
    ['/overview', ApplicationOverview],
    ['/presets', PresetsOverview],
    ['/presets/edit/:id', PresetEditor],
    ['/vendors', DepartmentsOverview],
    ['/destinations', DestinationsOverview],
    ['/settings', Settings],
    ['/provider-links', ProviderLinksOverview],
    ['/profile', Profile],
    ['/edit/:id', ApplicationEditor],
];

function StaffApp() {
    const dispatch = useAppDispatch();

    const theme = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.theme));
    const snackbar = useAppSelector(state => state.snackbar);
    const authState = useAppSelector(selectAuthenticationState);

    useEffect(() => {
        axios.interceptors.response.use(response => {
            return response;
        }, error => {
            console.error(error);
            if (error.response.status === 401 && authState === AuthState.Authenticated) {
                dispatch(logout());
            }
            return Promise.reject(error);
        });

        dispatch(fetchSystemConfig());
        dispatch(refreshUser());
    }, [dispatch]);

    useEffect(() => {
        dispatch(fetchSystemConfig());
        dispatch(refreshUser());
    }, [authState, dispatch]);

    return (
        <ThemeProvider theme={(baseTheme: Theme) => createAppTheme(theme, baseTheme)}>
            <HashRouter>
                <Routes>
                    {
                        routes.map(([path, component]) => (
                            <Route
                                key={path}
                                path={path}
                                element={React.createElement(component)}
                            />
                        ))
                    }
                </Routes>
            </HashRouter>

            <Snackbar
                open={snackbar.message != null}
                autoHideDuration={6000}
                onClose={() => dispatch(resetSnackbar())}
            >
                <Alert
                    onClose={() => dispatch(resetSnackbar())}
                    severity={snackbar.severity}
                    sx={{width: '100%'}}
                >
                    {snackbar.message}
                </Alert>
            </Snackbar>
        </ThemeProvider>
    );
}

export default StaffApp;
