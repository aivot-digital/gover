import React, {useEffect, useState} from 'react';
import {createBrowserRouter, RouterProvider} from 'react-router-dom';
import {logout, refreshMemberships, refreshUser, selectUser} from '../slices/user-slice';
import {fetchPublicSystemConfig, fetchSystemConfig, selectSystemConfigValue} from '../slices/system-config-slice';
import {Alert, Backdrop, CircularProgress, Snackbar, type Theme as MuiTheme, ThemeProvider, Typography} from '@mui/material';
import {createAppTheme, createDefaultAppTheme} from '../theming/themes';
import {useAppDispatch} from '../hooks/use-app-dispatch';
import {useAppSelector} from '../hooks/use-app-selector';
import {resetSnackbar} from '../slices/snackbar-slice';
import {InfoDialog} from '../dialogs/info-dialog/info-dialog';
import {isAnonymousUser} from '../models/entities/user';
import {staffAppRoutes} from './staff-app-routes';
import {Login} from '../pages/staff-pages/login/login';
import {SystemConfigKeys} from '../data/system-config-keys';
import {type Theme} from '../models/entities/theme';
import {isStringNotNullOrEmpty} from '../utils/string-utils';
import {ThemesService} from '../services/themes-service';

const router = createBrowserRouter(
    Object.keys(staffAppRoutes).map((key) => staffAppRoutes[key]),
);

function StaffApp(): JSX.Element {
    const dispatch = useAppDispatch();

    const snackbar = useAppSelector((state) => state.snackbar);
    const user = useAppSelector(selectUser);
    const themeId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.theme));

    const [theme, setTheme] = useState<Theme>();
    const [showTimeout, setShowTimeout] = useState(false);

    useEffect(() => {
        const originalFetch = window.fetch;
        window.fetch = async (
            info: RequestInfo | URL,
            init?: RequestInit,
        ): Promise<Response> => {
            let response: Response;
            try {
                response = await originalFetch(info, init);
            } catch (err: any) {
                if (err.name === 'AbortError') {
                    setShowTimeout(true);
                    throw err;
                } else {
                    throw err;
                }
            }

            if (response.status === 401) {
                console.log('Unauthorized response received. Logging out.');
                dispatch(logout());
            }

            return response;
        };
    }, []);

    useEffect(() => {
        if (themeId != null && isStringNotNullOrEmpty(themeId)) {
            if (theme == null || theme.id !== parseInt(themeId)) {
                ThemesService
                    .retrieve(parseInt(themeId))
                    .then(setTheme);
            }
        } else {
            setTheme(undefined);
        }
    }, [themeId]);

    useEffect(() => {
        if (user == null) {
            dispatch(refreshUser());
        }
    }, [user]);


    useEffect(() => {
        if (user != null && !isAnonymousUser(user)) {
            dispatch(fetchSystemConfig());
        } else {
            dispatch(fetchPublicSystemConfig());
        }
    }, [user]);

    useEffect(() => {
        if (user != null && !isAnonymousUser(user)) {
            dispatch(refreshMemberships(user));
        }
    }, [user]);

    if (showTimeout) {
        return (
            <ThemeProvider theme={createDefaultAppTheme}>
                <InfoDialog
                    open={true}
                    severity="error"
                    title="Serververbindung fehlgeschlagen"
                >
                    <Typography>
                        Die Verbindung mit dem Gover-Server ist fehlgeschlagen oder hat zu lange gedauert.
                        Bitte laden Sie die Seite neu.
                        Wenn das Problem weiterhin besteht, probieren Sie es später erneut oder wenden Sie sich an den
                        Betreiber der Gover-Installation.
                    </Typography>
                </InfoDialog>
            </ThemeProvider>
        );
    }

    if (user == null) {
        return (
            <Backdrop open={true}>
                <CircularProgress/>
            </Backdrop>
        );
    }

    if (isAnonymousUser(user)) {
        return (
            <ThemeProvider theme={createDefaultAppTheme}>
                <Login/>
            </ThemeProvider>
        );
    }

    return (
        <ThemeProvider theme={(baseTheme: MuiTheme) => createAppTheme(theme, baseTheme)}>
            <RouterProvider router={router}/>

            <Snackbar
                open={snackbar.message != null}
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
        </ThemeProvider>
    );
}

export default StaffApp;
