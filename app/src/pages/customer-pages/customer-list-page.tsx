import React, {useEffect, useState} from 'react';
import {LoadingPlaceholder} from '../../components/loading-placeholder/loading-placeholder';
import {Box, Container, List} from '@mui/material';
import {NotFoundPage} from '../../components/not-found-page/not-found-page';
import {MetaElement} from '../../components/meta-element/meta-element';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {ListHeader} from '../../components/list-header/list-header';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {resetStepper} from '../../slices/stepper-slice';
import {showDialog} from '../../slices/app-slice';
import {AlertComponent} from '../../components/alert/alert-component';
import {EmptySearchDataListPlaceholder} from '../../components/empty-search-data-list-placeholder/empty-search-data-list-placeholder';
import {PrivacyDialog, PrivacyDialogId} from '../../dialogs/privacy-dialog/privacy-dialog';
import {ImprintDialog, ImprintDialogId} from '../../dialogs/imprint-dialog/imprint-dialog';
import {AccessibilityDialog, AccessibilityDialogId} from '../../dialogs/accessibility-dialog/accessibility-dialog';
import {FormCitizenListResponseDTO} from '../../modules/forms/dtos/form-citizen-list-response-dto';
import {setIdentityId} from '../../slices/identity-slice';
import {PublicFormListItem} from '../../components/public-form-list-item/public-form-list-item';
import {CustomerListPageHeader} from './customer-list-page-header';
import {CustomerListPageFooter} from './customer-list-page-footer';
import {isApiError} from '../../models/api-error';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import {FormTriggerApiService, FormTriggerListItem} from '../../modules/forms/services/form-trigger-api-service';
import {resolveFormNodeName} from '../../models/elements/form-layout-element';

function mapPublicFormListItem(form: FormTriggerListItem): FormCitizenListResponseDTO | null {
    const formLayout = form.node.configuration.formLayout;
    if (formLayout?.showOnFormIndexPage === false) {
        return null;
    }

    const formSlug = form.node.configuration.formSlug;
    if (formSlug == null || formSlug.length === 0) {
        return null;
    }

    return {
        slug: `form/${form.process.slug}/${formSlug}`,
        version: form.version.processVersion,
        title: resolveFormNodeName(formLayout, form.version),
        updated: form.version.updated,
    };
}

export function CustomerListPage() {
    const dispatch = useAppDispatch();

    const [failedToLoad, setFailedToLoad] = useState(false);
    const [forms, setForms] = useState<FormCitizenListResponseDTO[]>();
    const [search, setSearch] = useState('');

    const provider = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));
    const disableProsunaListingPage = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.listingPage.disableProsunaListingPage));
    const metaDialog = useAppSelector((state) => state.app.showDialog);

    useEffect(() => {
        new FormTriggerApiService()
            .listPublicAll()
            .then((page) => page.content
                .map(mapPublicFormListItem)
                .filter((form): form is FormCitizenListResponseDTO => form != null)
                .sort((a, b) => a.title.localeCompare(b.title, 'de')))
            .then(setForms)
            .catch((err) => {
                if (isApiError(err) && err.displayableToUser) {
                    dispatch(showErrorSnackbar(err.message));
                } else {
                    dispatch(showErrorSnackbar('Beim Laden der Formulare ist ein unbekannter Fehler aufgetreten.'));
                }

                setFailedToLoad(true);
                console.error(err);
            });

        dispatch(resetStepper());
        dispatch(setIdentityId(undefined));
    }, []);

    if (failedToLoad) {
        return (
            <>
                <MetaElement
                    title={'Seite nicht gefunden'}
                    titlePrefix={provider}
                />
                <NotFoundPage />
            </>
        );
    } else if (disableProsunaListingPage == 'true') {
        return (
            <>
                <MetaElement
                    title={'Formularverzeichnis'}
                    titlePrefix={provider}
                />
                <NotFoundPage
                    title="Formularverzeichnis nicht verfügbar"
                    msg={'Auf diesem System ist kein öffentliches Formularverzeichnis verfügbar. Bitte nutzen Sie die direkten Links zu den einzelnen Formularen.'}
                />
            </>
        );
    } else if (forms == null) {
        return <LoadingPlaceholder />;
    } else {
        const filteredApplications = forms.filter((app) => app
            .title
            .toLowerCase()
            .includes(search.toLowerCase()),
        );

        return (
            <Box
                sx={{
                    backgroundColor: 'background.default',
                }}
            >
                <MetaElement
                    title={'Formularverzeichnis'}
                    titlePrefix={provider}
                />

                <CustomerListPageHeader />

                <main role="main">
                    <Box
                        sx={{
                            minHeight: '75vh',
                        }}
                    >
                        <Container
                            sx={{
                                mb: 5,
                                py: 4,
                            }}
                        >
                            <Box
                                sx={{
                                    mt: 3,
                                    mb: 6,
                                }}
                            >
                                <ListHeader
                                    title="Unsere Formulare"
                                    search={search}
                                    onSearchChange={setSearch}
                                    searchLabel="Formular suchen"
                                    searchPlaceholder="Titel des Formulars eingeben…"
                                    actions={[]}
                                />

                                <Box
                                    sx={{
                                        mt: 3,
                                        mb: 6,
                                    }}
                                >
                                    <List component={'div'}>
                                        {
                                            filteredApplications.map((app) => (
                                                <PublicFormListItem
                                                    key={app.slug}
                                                    form={app}
                                                />
                                            ))
                                        }
                                        {
                                            forms.length === 0 &&
                                            filteredApplications.length === 0 &&
                                            <AlertComponent
                                                color="info"
                                                title="Noch keine Formulare veröffentlicht"
                                            >
                                                Es wurden noch keine Formulare veröffentlicht.
                                                Schauen Sie einfach später wieder vorbei.
                                            </AlertComponent>
                                        }
                                        {
                                            forms.length > 0 &&
                                            filteredApplications.length === 0 &&
                                            <EmptySearchDataListPlaceholder
                                                helperText="Es gibt keine Formulare, die Ihrer Suche entsprechen…"
                                            />
                                        }
                                    </List>
                                </Box>
                            </Box>
                        </Container>
                    </Box>

                    <CustomerListPageFooter />
                </main>

                <PrivacyDialog
                    form={{} as any}
                    onHide={() => dispatch(showDialog(undefined))}
                    open={metaDialog === PrivacyDialogId}
                    isListingPage
                />

                <ImprintDialog
                    onHide={() => dispatch(showDialog(undefined))}
                    open={metaDialog === ImprintDialogId}
                    isListingPage
                    form={{} as any}
                />

                <AccessibilityDialog
                    onHide={() => dispatch(showDialog(undefined))}
                    open={metaDialog === AccessibilityDialogId}
                    isListingPage
                    form={{} as any}
                />
            </Box>
        );
    }
}
