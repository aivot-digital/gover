import {useParams, useSearchParams} from 'react-router-dom';
import React, {useEffect, useState} from 'react';
import {LoadingPlaceholderComponentView} from '../../components/static-components/loading-placeholder/loading-placeholder.component.view';
import {Alert, Snackbar, type Theme as MuiTheme, ThemeProvider} from '@mui/material';
import {createAppTheme} from '../../theming/themes';
import {LoadUserInputDialog} from '../../dialogs/load-user-input/load-user-input.dialog';
import {ViewDispatcherComponent} from '../../components/view-dispatcher.component';
import {NotFoundPage} from '../../components/static-components/not-found-page/not-found-page';
import {MetaElement} from '../../components/meta-element/meta-element';
import {InputWatcher} from '../../components/static-components/input-watcher/input-watcher';
import {fetchApplicationBySlug, MetaDialog, selectApplicationLoadFailed, selectLoadedApplication, showMetaDialog} from '../../slices/app-slice';
import {useAppSelector} from '../../hooks/use-app-selector';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {resetSnackbar} from '../../slices/snackbar-slice';
import {flattenElements} from '../../utils/flatten-elements';
import {HelpDialog} from '../../dialogs/help-dialog/help.dialog';
import {PrivacyDialog} from '../../dialogs/privacy-dialog/privacy-dialog';
import {ImprintDialog} from '../../dialogs/imprint-dialog/imprint-dialog';
import {AccessibilityDialog} from '../../dialogs/accessibility-dialog/accessibility-dialog';
import {ThemesService} from "../../services/themes-service";
import {Theme} from "../../models/entities/theme";

export function ApplicationPage() {
    const [searchParams, setSearchParams] = useSearchParams();
    const metaDialogName = searchParams.get('dialog');

    const params = useParams();

    const dispatch = useAppDispatch();
    const application = useAppSelector(selectLoadedApplication);
    const failedToLoad = useAppSelector(selectApplicationLoadFailed);
    const snackbar = useAppSelector((state) => state.snackbar);
    const metaDialog = useAppSelector((state) => state.app.showMetaDialog);

    const [theme, setTheme] = useState<Theme>();

    useEffect(() => {
        if (params.slug != null && params.version != null) {
            dispatch(fetchApplicationBySlug({
                slug: params.slug,
                version: params.version,
            }));
        }
    }, [params, dispatch]);

    useEffect(() => {
        if (metaDialogName != null) {
            dispatch(showMetaDialog(metaDialogName as MetaDialog));
            setSearchParams({});
        }
    }, [metaDialogName]);

    useEffect(() => {
        if (application?.theme != null) {
            ThemesService
                .retrievePublic(application.theme)
                .then(setTheme)
                .catch((err) => {
                    console.error(err);
                });
        }
    }, [application]);

    if (failedToLoad) {
        return <NotFoundPage/>;
    } else if (application == null) {
        return <LoadingPlaceholderComponentView/>;
    } else {
        const allElements = flattenElements(application.root);

        return (
            <ThemeProvider theme={(baseTheme: MuiTheme) => createAppTheme(theme, baseTheme)}>
                <MetaElement
                    title={application.root.tabTitle}
                />

                <ViewDispatcherComponent
                    allElements={allElements}
                    element={application.root}
                />

                <LoadUserInputDialog
                    application={application}
                />

                <InputWatcher
                    application={application}
                />

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

                <HelpDialog
                    onHide={() => dispatch(showMetaDialog(undefined))}
                    open={metaDialog === MetaDialog.Help}
                />

                <PrivacyDialog
                    onHide={() => dispatch(showMetaDialog(undefined))}
                    open={metaDialog === MetaDialog.Privacy}
                />

                <ImprintDialog
                    onHide={() => dispatch(showMetaDialog(undefined))}
                    open={metaDialog === MetaDialog.Imprint}
                />

                <AccessibilityDialog
                    onHide={() => dispatch(showMetaDialog(undefined))}
                    open={metaDialog === MetaDialog.Accessibility}
                />
            </ThemeProvider>
        );
    }
}
