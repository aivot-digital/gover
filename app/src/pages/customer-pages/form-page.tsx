import {useParams, useSearchParams} from 'react-router-dom';
import React, {useEffect, useMemo, useState} from 'react';
import {LoadingPlaceholder} from '../../components/loading-placeholder/loading-placeholder';
import {Alert, Snackbar, type Theme as MuiTheme, ThemeProvider} from '@mui/material';
import {createAppTheme} from '../../theming/themes';
import {LoadCustomerInputDialog} from '../../dialogs/load-customer-input/load-customer-input.dialog';
import {ViewDispatcherComponent} from '../../components/view-dispatcher.component';
import {NotFoundPage} from '../../components/not-found-page/not-found-page';
import {MetaElement} from '../../components/meta-element/meta-element';
import {selectLoadedForm, showDialog, updateLoadedForm} from '../../slices/app-slice';
import {useAppSelector} from '../../hooks/use-app-selector';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {resetSnackbar} from '../../slices/snackbar-slice';
import {flattenElements} from '../../utils/flatten-elements';
import {HelpDialog, HelpDialogId} from '../../dialogs/help-dialog/help.dialog';
import {PrivacyDialog, PrivacyDialogId} from '../../dialogs/privacy-dialog/privacy-dialog';
import {ImprintDialog, ImprintDialogId} from '../../dialogs/imprint-dialog/imprint-dialog';
import {AccessibilityDialog, AccessibilityDialogId} from '../../dialogs/accessibility-dialog/accessibility-dialog';
import {Theme} from '../../models/entities/theme';
import {useThemesApi} from '../../hooks/use-themes-api';
import {useApi} from '../../hooks/use-api';
import {useFormsApi} from '../../hooks/use-forms-api';
import {selectSystemConfigValue} from "../../slices/system-config-slice";
import {SystemConfigKeys} from "../../data/system-config-keys";

export const DialogSearchParam = 'dialog';

export function FormPage() {
    const api = useApi();

    const [searchParams, setSearchParams] = useSearchParams();
    const metaDialogName = useMemo(() => searchParams.get(DialogSearchParam), [searchParams]);

    const params = useParams();

    const dispatch = useAppDispatch();
    const form = useAppSelector(selectLoadedForm);
    const [failedToLoad, setFailedToLoad] = useState(false);
    const snackbar = useAppSelector((state) => state.snackbar);
    const metaDialog = useAppSelector((state) => state.app.showDialog);
    const provider = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));

    const [theme, setTheme] = useState<Theme>();

    useEffect(() => {
        dispatch(showDialog(metaDialogName ?? undefined));
    }, [metaDialogName]);

    useEffect(() => {
        if (params.slug != null) {
            setFailedToLoad(false);
            useFormsApi(api)
                .retrieveBySlugAndVersion(params.slug, params.version)
                .then((application) => {
                    dispatch(updateLoadedForm(application));
                })
                .catch(err => {
                    console.error(err);
                    setFailedToLoad(true);
                });
        }
    }, [params, dispatch, api]);

    useEffect(() => {
        if (form?.themeId != null) {
            useThemesApi(api)
                .retrievePublicTheme(form.themeId)
                .then(setTheme)
                .catch((err) => {
                    console.error(err);
                });
        }
    }, [form]);

    if (failedToLoad) {
        return <><MetaElement
            title={"Seite nicht gefunden"}
            titlePrefix={provider}
        /><NotFoundPage /></>;
    } else if (form == null) {
        return <LoadingPlaceholder />;
    } else {
        const allElements = flattenElements(form.root);

        return (
            <ThemeProvider theme={(baseTheme: MuiTheme) => createAppTheme(theme, baseTheme)}>
                <MetaElement
                    title={form.root.tabTitle ?? form.root.headline}
                    titlePrefix={provider}
                />

                <ViewDispatcherComponent
                    allElements={allElements}
                    element={form.root}
                />

                <LoadCustomerInputDialog
                    form={form}
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
                    onHide={() => dispatch(showDialog(undefined))}
                    open={metaDialog === HelpDialogId}
                />

                <PrivacyDialog
                    onHide={() => dispatch(showDialog(undefined))}
                    open={metaDialog === PrivacyDialogId}
                />

                <ImprintDialog
                    onHide={() => dispatch(showDialog(undefined))}
                    open={metaDialog === ImprintDialogId}
                />

                <AccessibilityDialog
                    onHide={() => dispatch(showDialog(undefined))}
                    open={metaDialog === AccessibilityDialogId}
                />
            </ThemeProvider>
        );
    }
}
