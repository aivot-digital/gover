import {useParams} from 'react-router-dom';
import React, {useEffect} from 'react';
import {
    LoadingPlaceholderComponentView
} from '../../components/static-components/loading-placeholder/loading-placeholder.component.view';
import {Alert, Snackbar, Theme, ThemeProvider} from '@mui/material';
import {createAppTheme} from '../../theming/themes';
import {LoadUserInputDialog} from '../../dialogs/load-user-input/load-user-input.dialog';
import {ViewDispatcherComponent} from '../../components/view-dispatcher.component';
import {NotFoundPage} from '../../components/static-components/not-found-page/not-found-page';
import {MetaElement} from '../../components/meta-element/meta-element';
import {InputWatcher} from '../../components/static-components/input-watcher/input-watcher';
import {fetchApplicationBySlug, selectApplicationLoadFailed, selectLoadedApplication} from '../../slices/app-slice';
import {useAppSelector} from '../../hooks/use-app-selector';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {resetSnackbar} from "../../slices/snackbar-slice";

export function ApplicationPage() {
    const params = useParams();

    const dispatch = useAppDispatch();
    const application = useAppSelector(selectLoadedApplication);
    const failedToLoad = useAppSelector(selectApplicationLoadFailed);
    const snackbar = useAppSelector(state => state.snackbar);

    useEffect(() => {
        if (params.slug != null && params.version != null) {
            dispatch(fetchApplicationBySlug({slug: params.slug, version: params.version}));
        }
    }, [params, dispatch]);

    if (failedToLoad) {
        return <NotFoundPage/>;
    } else if (application == null) {
        return <LoadingPlaceholderComponentView/>;
    } else {
        return (
            <ThemeProvider theme={(baseTheme: Theme) => createAppTheme(application.root.theme, baseTheme)}>
                <MetaElement title={application.root.tabTitle}/>
                <ViewDispatcherComponent element={application.root}/>
                <LoadUserInputDialog application={application}/>
                <InputWatcher application={application}/>

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
}
