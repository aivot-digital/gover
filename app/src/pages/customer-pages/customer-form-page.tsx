import {useParams, useSearchParams} from 'react-router-dom';
import React, {useEffect, useMemo, useState} from 'react';
import {LoadingPlaceholder} from '../../components/loading-placeholder/loading-placeholder';
import {Box, ThemeProvider, useTheme} from '@mui/material';
import {createAppTheme} from '../../theming/themes';
import {ViewDispatcherComponent} from '../../components/view-dispatcher.component';
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
import {SnackbarProvider} from '../../providers/snackbar-provider';
import {selectIdentityId} from '../../slices/identity-slice';
import {ElementData} from '../../models/element-data';
import {CustomerInputService} from '../../services/customer-input-service';
import {setErrorMessage} from '../../slices/shell-slice';
import {isApiError} from '../../models/api-error';
import {FormApiService} from '../../modules/forms/services/form-api-service';
import {formCitizenDetailsResponseDTO} from '../../modules/forms/dtos/form-citizen-details-response-dto';

export const DialogSearchParam = 'dialog';

export function CustomerFormPage() {
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

    const metaDialog = useAppSelector((state) => state.app.showDialog);
    const provider = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));
    const identityId = useAppSelector(selectIdentityId);

    const [elementData, setElementData] = useState<ElementData>({});

    const [theme, setTheme] = useState<Theme>();

    const handleSetElementData = (data: ElementData, storeData: boolean = true) => {
        setElementData(data);

        if (storeData && form != null) {
            CustomerInputService
                .storeCustomerInput(form.form.slug, form.version.version, form.version.rootElement, data);
        }
    };

    useEffect(() => {
        dispatch(showDialog(metaDialogName ?? undefined));
    }, [metaDialogName]);

    useEffect(() => {
        if (slug == null) {
            return;
        }

        new FormApiService()
            .retrieveBySlugAndVersion(slug, version, identityId)
            .then((application) => {
                const form = formCitizenDetailsResponseDTO(application);
                dispatch(updateLoadedForm(form));
            })
            .catch(err => {
                if (err.status === 404) {
                    dispatch(setErrorMessage({
                        status: 404,
                        message: 'Das angeforderte Formular wurde nicht gefunden.',
                    }));
                } else if (isApiError(err) && err.displayableToUser) {
                    dispatch(setErrorMessage({
                        status: err.status,
                        message: err.message,
                    }));
                } else {
                    dispatch(setErrorMessage({
                        status: 500,
                        message: 'Beim Laden des Formulars ist ein unbekannter Fehler aufgetreten.',
                    }));
                    console.error(err);
                }
            });
    }, [slug, api, identityId]);

    useEffect(() => {
        if (slug == null) {
            return;
        }

        new FormApiService()
            .getFormTheme(slug, version != null ? parseInt(version) : undefined)
            .then(setTheme)
            .catch(() => {
                // Ignore theme loading errors
            });
    }, [slug, version]);

    const _theme = useMemo(() => {
        return createAppTheme(theme, baseTheme);
    }, [theme, baseTheme]);

    if (form == null) {
        return (
            <LoadingPlaceholder />
        );
    } else {
        const allElements = flattenElements(form.version.rootElement);

        return (
            <ThemeProvider theme={_theme}>
                <SnackbarProvider>
                    <MetaElement
                        faviconUrl={new FormApiService().getFormFaviconLink(form.form.slug, form.version.version)}
                        title={form.version.rootElement.tabTitle ?? form.version.rootElement.headline ?? ''}
                        titlePrefix={provider}
                    />

                    <Box
                        sx={{
                            backgroundColor: 'white',
                        }}
                    >
                        <ViewDispatcherComponent
                            rootElement={form.version.rootElement}
                            allElements={allElements}
                            element={form.version.rootElement}
                            isBusy={false}
                            isDeriving={false}
                            mode="viewer"
                            elementData={elementData}
                            onElementDataChange={(data) => handleSetElementData(data)}
                            derivationTriggerIdQueue={[]}
                            disableVisibility={false}
                        />
                    </Box>

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
