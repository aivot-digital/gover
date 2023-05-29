import React, {FunctionComponent, useEffect, useState} from 'react';
import {HashRouter, Route, Routes} from 'react-router-dom';
import {ApplicationsOverviewPage} from '../pages/staff-pages/applications-overview-page/applications-overview-page';
import {ApplicationEditorPage} from '../pages/staff-pages/application-editor-page/application-editor-page';
import {Login} from '../pages/staff-pages/login/login';
import {DepartmentsOverview} from '../pages/staff-pages/departments-overview/departments-overview';
import {DestinationsOverview} from '../pages/staff-pages/destinations-overview/destinations-overview';
import {refreshUser,} from '../slices/user-slice';
import axios from 'axios';
import {Settings} from '../pages/staff-pages/settings/settings';
import {Profile} from '../pages/staff-pages/profile/profile';
import {fetchSystemConfig, selectSystemConfigValue} from '../slices/system-config-slice';
import {ProviderLinksOverview} from '../pages/staff-pages/provider-links-overview/provider-links-overview';
import {Alert, Snackbar, Theme, ThemeProvider, Typography} from '@mui/material';
import {createAppTheme} from '../theming/themes';
import {SystemConfigKeys} from '../data/system-config-keys';
import {PresetsOverview} from '../pages/staff-pages/presets-overview/presets-overview';
import {PresetEditorPage} from '../pages/staff-pages/preset-editor-page/preset-editor-page';
import {useAppDispatch} from '../hooks/use-app-dispatch';
import {useAppSelector} from '../hooks/use-app-selector';
import {resetSnackbar} from '../slices/snackbar-slice';
import {logout, selectAuthenticationState} from '../slices/auth-slice';
import {AuthState} from "../data/auth-state";
import {InfoDialog} from "../dialogs/info-dialog/info-dialog";

const routes: [string, FunctionComponent][] = [
    ['/', Login],
    ['/overview', ApplicationsOverviewPage],
    ['/presets', PresetsOverview],
    ['/presets/edit/:id', PresetEditorPage],
    ['/vendors', DepartmentsOverview],
    ['/destinations', DestinationsOverview],
    ['/settings', Settings],
    ['/provider-links', ProviderLinksOverview],
    ['/profile', Profile],
    ['/edit/:id', ApplicationEditorPage],
];

function StaffApp() {
    const dispatch = useAppDispatch();

    const theme = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.theme));
    const snackbar = useAppSelector(state => state.snackbar);
    const authState = useAppSelector(selectAuthenticationState);
    const [showTimeout, setShowTimeout] = useState(false);

    useEffect(() => {
        axios.interceptors.response.use(response => {
            return response;
        }, error => {
            console.error('axios.interceptor', error);
            if (error.response.status === 401 && authState === AuthState.Authenticated) {
                dispatch(logout());
            }

            if (error.code === 'ECONNABORTED') {
                setShowTimeout(true);
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

            <InfoDialog
                open={showTimeout}
                severity="error"
                title="Serververbindung fehlgeschlagen"
            >
                <Typography>
                    Die Verbindung mit dem Gover-Server ist fehlgeschlagen oder hat zu lange gedauert.
                    Bitte laden Sie die Seite neu.
                    Wenn das Problem weiterhin besteht, probieren Sie es später erneut oder wenden Sie sich an den Betreiber der Gover-Installation.
                </Typography>
            </InfoDialog>

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
