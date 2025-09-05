import {useParams, useSearchParams} from 'react-router-dom';
import React, {useEffect, useMemo, useState} from 'react';
import {LoadingPlaceholder} from '../../components/loading-placeholder/loading-placeholder';
import {ThemeProvider, useTheme} from '@mui/material';
import {createAppTheme} from '../../theming/themes';
import {ViewDispatcherComponent} from '../../components/view-dispatcher.component';
import {NotFoundPage} from '../../components/not-found-page/not-found-page';
import {MetaElement} from '../../components/meta-element/meta-element';
import {selectLoadedForm, showDialog, updateLoadedForm} from '../../slices/app-slice';
import {useAppSelector} from '../../hooks/use-app-selector';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {flattenElements} from '../../utils/flatten-elements';
import {HelpDialog, HelpDialogId} from '../../dialogs/help-dialog/help.dialog';
import {PrivacyDialog, PrivacyDialogId} from '../../dialogs/privacy-dialog/privacy-dialog';
import {ImprintDialog, ImprintDialogId} from '../../dialogs/imprint-dialog/imprint-dialog';
import {AccessibilityDialog, AccessibilityDialogId} from '../../dialogs/accessibility-dialog/accessibility-dialog';
import {Theme} from '../../modules/themes/models/theme';
import {useApi} from '../../hooks/use-api';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {ThemesApiService} from '../../modules/themes/themes-api-service';
import {FormsApiService} from '../../modules/forms/forms-api-service';
import {SnackbarProvider} from '../../providers/snackbar-provider';
import {selectIdentityId} from '../../slices/identity-slice';
import {ElementData} from '../../models/element-data';
import {CustomerInputService} from '../../services/customer-input-service';

export const DialogSearchParam = 'dialog';

export function FormPage() {
    const baseTheme = useTheme();
    const api = useApi();

    const [searchParams, setSearchParams] = useSearchParams();
    const metaDialogName = useMemo(() => searchParams.get(DialogSearchParam), [searchParams]);

    const {
        slug,
        version,
    } = useParams();

    const dispatch = useAppDispatch();
    const form = useAppSelector(selectLoadedForm);
    const systemThemeId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.theme));
    const [failedToLoad, setFailedToLoad] = useState(false);
    const metaDialog = useAppSelector((state) => state.app.showDialog);
    const provider = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));
    const identityId = useAppSelector(selectIdentityId);

    const [elementData, setElementData] = useState<ElementData>({});

    const [theme, setTheme] = useState<Theme>();

    const handleSetElementData = (data: ElementData, storeData: boolean = true) => {
        setElementData(data);

        if (storeData && form != null) {
            CustomerInputService
                .storeCustomerInput(form, data);
        }
    };

    useEffect(() => {
        dispatch(showDialog(metaDialogName ?? undefined));
    }, [metaDialogName]);

    useEffect(() => {
        if (slug == null) {
            return;
        }

        setFailedToLoad(false);
        new FormsApiService(api)
            .retrieveBySlugAndVersion(slug, version, identityId)
            .then((application) => {
                dispatch(updateLoadedForm(application));
            })
            .catch(err => {
                console.error(err);
                setFailedToLoad(true);
            });
    }, [slug, api, identityId]);

    useEffect(() => {
        if (form != null && form.themeId != null) {
            new ThemesApiService(api)
                .retrievePublic(form.themeId)
                .then(setTheme)
                .catch((err) => {
                    console.error(err);
                });
        }

        if (systemThemeId != null) {
            new ThemesApiService(api)
                .retrieve(parseInt(systemThemeId))
                .then(setTheme)
                .catch((err) => {
                    console.error(err);
                });
        }
    }, [form, systemThemeId]);

    const _theme = useMemo(() => {
        return createAppTheme(theme, baseTheme);
    }, [theme, baseTheme]);

    if (failedToLoad) {
        return (
            <>
                <MetaElement
                    title="Seite nicht gefunden"
                    titlePrefix={provider}
                />
                <NotFoundPage />
            </>
        );
    } else if (form == null) {
        return (
            <LoadingPlaceholder />
        );
    } else {
        const allElements = flattenElements(form.rootElement);

        return (
            <ThemeProvider theme={_theme}>
                <SnackbarProvider>
                    <MetaElement
                        title={form.rootElement.tabTitle ?? form.rootElement.headline ?? ''}
                        titlePrefix={provider}
                    />

                    <ViewDispatcherComponent
                        rootElement={form.rootElement}
                        allElements={allElements}
                        element={form.rootElement}
                        isBusy={false}
                        isDeriving={false}
                        mode="viewer"
                        elementData={elementData}
                        onElementDataChange={(data) => handleSetElementData(data)}
                        derivationTriggerIdQueue={[]}
                        disableVisibility={false}
                    />

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
                </SnackbarProvider>
            </ThemeProvider>
        );
    }
}
