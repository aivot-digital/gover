import React, { type FunctionComponent, useEffect, useState } from 'react';
import { createHashRouter, RouterProvider } from 'react-router-dom';
import { ApplicationListPage } from '../pages/staff-pages/application-pages/application-list-page';
import { ApplicationEditorPage } from '../pages/staff-pages/application-pages/application-editor-page';
import { Login } from '../pages/staff-pages/login/login';
import { refreshMemberships, refreshUser, selectUser } from '../slices/user-slice';
import { Settings } from '../pages/staff-pages/settings/settings';
import { Profile } from '../pages/staff-pages/profile/profile';
import { fetchSystemConfig } from '../slices/system-config-slice';
import { Alert, Snackbar, ThemeProvider, Typography } from '@mui/material';
import { createDefaultAppTheme } from '../theming/themes';
import { PresetListPage } from '../pages/staff-pages/preset-pages/preset-list-page';
import { PresetEditPage } from '../pages/staff-pages/preset-pages/preset-edit-page';
import { useAppDispatch } from '../hooks/use-app-dispatch';
import { useAppSelector } from '../hooks/use-app-selector';
import { resetSnackbar } from '../slices/snackbar-slice';
import { logout, selectAuthenticationState } from '../slices/auth-slice';
import { AuthState } from '../data/auth-state';
import { InfoDialog } from '../dialogs/info-dialog/info-dialog';
import { UserListPage } from '../pages/staff-pages/user-pages/user-list-page';
import { UserEditPage } from '../pages/staff-pages/user-pages/user-edit-page';
import { DepartmentListPage } from '../pages/staff-pages/department-pages/department-list-page';
import { DepartmentEditPage } from '../pages/staff-pages/department-pages/department-edit-page';
import { SubmissionListPage } from '../pages/staff-pages/submission-pages/submission-list-page';
import { SubmissionEditPage } from '../pages/staff-pages/submission-pages/submission-edit-page';
import { DestinationListPage } from '../pages/staff-pages/destination-pages/destination-list-page';
import { DestinationEditPage } from '../pages/staff-pages/destination-pages/destination-edit-page';
import { ProviderLinkListPage } from '../pages/staff-pages/provider-link-pages/provider-link-list-page';
import { ProviderLinkEditPage } from '../pages/staff-pages/provider-link-pages/provider-link-edit-page';
import { AssetListPage } from '../pages/staff-pages/asset-pages/asset-list-page';
import { AssetEditPage } from '../pages/staff-pages/asset-pages/asset-edit-page';
import { ThemeListPage } from '../pages/staff-pages/theme-pages/theme-list-page';
import { ThemeEditPage } from '../pages/staff-pages/theme-pages/theme-edit-page';

const routes: Array<[string, FunctionComponent]> = [
    ['/', Login],
    ['/overview', ApplicationListPage],

    ['/presets', PresetListPage],
    ['/presets/edit/:id', PresetEditPage],

    ['/departments', DepartmentListPage],
    ['/departments/:id', DepartmentEditPage],

    ['/destinations', DestinationListPage],
    ['/destinations/:id', DestinationEditPage],

    ['/settings', Settings],
    ['/profile', Profile],
    ['/edit/:id', ApplicationEditorPage],

    ['/users', UserListPage],
    ['/users/:id', UserEditPage],

    ['/provider-links', ProviderLinkListPage],
    ['/provider-links/:id', ProviderLinkEditPage],

    ['/submissions/:id', SubmissionListPage],
    ['/submissions/:applicationId/:id', SubmissionEditPage],

    ['/assets', AssetListPage],
    ['/assets/:name', AssetEditPage],

    ['/themes', ThemeListPage],
    ['/themes/:id', ThemeEditPage],
];

const router = createHashRouter(
    routes.map(([path, View]) => ({
        path,
        element: <View/>,
    })),
);

function StaffApp(): JSX.Element {
    const dispatch = useAppDispatch();

    const snackbar = useAppSelector((state) => state.snackbar);
    const authState = useAppSelector(selectAuthenticationState);
    const user = useAppSelector(selectUser);
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

            if (response.status === 401 && authState === AuthState.Authenticated) {
                dispatch(logout());
            }

            return response;
        };
    }, [dispatch]);

    useEffect(() => {
        dispatch(fetchSystemConfig());
        dispatch(refreshUser());
    }, [authState, dispatch]);

    useEffect(() => {
        if (user != null) {
            dispatch(refreshMemberships(user));
        }
    }, [user, dispatch]);

    return (
        <ThemeProvider theme={ createDefaultAppTheme }>
            <RouterProvider router={ router }/>

            <InfoDialog
                open={ showTimeout }
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

            <Snackbar
                open={ snackbar.message != null }
                autoHideDuration={ 6000 }
                onClose={ () => dispatch(resetSnackbar()) }
            >
                <Alert
                    onClose={ () => dispatch(resetSnackbar()) }
                    severity={ snackbar.severity }
                    sx={ {width: '100%'} }
                >
                    { snackbar.message }
                </Alert>
            </Snackbar>
        </ThemeProvider>
    );
}

export default StaffApp;
