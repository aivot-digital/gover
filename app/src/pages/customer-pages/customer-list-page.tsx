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
import {clearLoadedForm, showDialog} from '../../slices/app-slice';
import {AlertComponent} from '../../components/alert/alert-component';
import {EmptySearchDataListPlaceholder} from '../../components/empty-search-data-list-placeholder/empty-search-data-list-placeholder';
import {PrivacyDialog, PrivacyDialogId} from '../../dialogs/privacy-dialog/privacy-dialog';
import {ImprintDialog, ImprintDialogId} from '../../dialogs/imprint-dialog/imprint-dialog';
import {AccessibilityDialog, AccessibilityDialogId} from '../../dialogs/accessibility-dialog/accessibility-dialog';
import {FormCitizenListResponseDTO} from '../../modules/forms/dtos/form-citizen-list-response-dto';
import {setIdentityId} from '../../slices/identity-slice';
import {PublicFormListItem} from '../../components/public-form-list-item/public-form-list-item';
import {FormsApiService} from '../../modules/forms/forms-api-service-v2';
import {CustomerListPageHeader} from './customer-list-page-header';
import {CustomerListPageFooter} from './customer-list-page-footer';

export function CustomerListPage() {
    const dispatch = useAppDispatch();

    const [failedToLoad, setFailedToLoad] = useState(false);
    const [applications, setApplications] = useState<FormCitizenListResponseDTO[]>();
    const [search, setSearch] = useState('');

    const provider = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));
    const disableGoverListingPage = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.listingPage.disableGoverListingPage));
    const metaDialog = useAppSelector((state) => state.app.showDialog);

    useEffect(() => {
        new FormsApiService()
            .listPublicAll()
            .then(setApplications)
            .catch((err) => {
                console.error(err);
                setFailedToLoad(true);
            });

        dispatch(resetStepper());
        dispatch(clearLoadedForm());
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
    } else if (disableGoverListingPage == 'true') {
        return (
            <>
                <MetaElement
                    title={'Online-Antrags-Management'}
                    titlePrefix={provider}
                />
                <NotFoundPage
                    title={'Online-Antrags-Management von ' + provider}
                    msg={'Dieses System besitzt keine öffentliche Index-Seite. Bitte nutzen Sie die direkten Links zu den Formularen dieses Anbieters.'}
                />
            </>
        );
    } else if (applications == null) {
        return <LoadingPlaceholder />;
    } else {
        const filteredApplications = applications.filter((app) => app
            .title
            .toLowerCase()
            .includes(search.toLowerCase()),
        );

        return (
            <Box
                sx={{
                    backgroundColor: 'white',
                }}
            >
                <MetaElement
                    title={'Online-Antrags-Management'}
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
                                    searchPlaceholder="Formular suchen…"
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
                                            applications.length === 0 &&
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
                                            applications.length > 0 &&
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
                    onHide={() => dispatch(showDialog(undefined))}
                    open={metaDialog === PrivacyDialogId}
                    isListingPage
                />

                <ImprintDialog
                    onHide={() => dispatch(showDialog(undefined))}
                    open={metaDialog === ImprintDialogId}
                    isListingPage
                />

                <AccessibilityDialog
                    onHide={() => dispatch(showDialog(undefined))}
                    open={metaDialog === AccessibilityDialogId}
                    isListingPage
                />
            </Box>
        );
    }
}
