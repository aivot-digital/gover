import React, {useEffect, useState} from 'react';
import {LoadingPlaceholder} from '../../components/loading-placeholder/loading-placeholder';
import {Box, Container, List, ThemeProvider} from '@mui/material';
import {createAppTheme} from '../../theming/themes';
import {NotFoundPage} from '../../components/not-found-page/not-found-page';
import {MetaElement} from '../../components/meta-element/meta-element';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {AppHeader} from '../../components/app-header/app-header';
import {AppMode} from '../../data/app-mode';
import {ListHeader} from '../../components/list-header/list-header';
import {AppFooter} from '../../components/app-footer/app-footer';
import {Introductory} from '../../components/introductory/introductory';
import {ApplicationListItemPublic} from '../../components/application-list-item/application-list-item-public';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {resetStepper} from '../../slices/stepper-slice';
import {clearCustomerInput, clearErrors, clearLoadedForm, showDialog} from '../../slices/app-slice';
import {AlertComponent} from '../../components/alert/alert-component';
import {useApi} from '../../hooks/use-api';
import {FormListProjectionPublic} from '../../models/entities/form';
import {Theme} from '../../modules/themes/models/theme';
import type {Theme as MuiTheme} from '@mui/material/styles/createTheme';
import {isStringNotNullOrEmpty} from '../../utils/string-utils';
import {EmptySearchDataListPlaceholder} from '../../components/empty-search-data-list-placeholder/empty-search-data-list-placeholder';
import {PrivacyDialog, PrivacyDialogId} from '../../dialogs/privacy-dialog/privacy-dialog';
import {ImprintDialog, ImprintDialogId} from '../../dialogs/imprint-dialog/imprint-dialog';
import {AccessibilityDialog, AccessibilityDialogId} from '../../dialogs/accessibility-dialog/accessibility-dialog';
import {ThemesApiService} from '../../modules/themes/themes-api-service';
import {FormsApiService} from '../../modules/forms/forms-api-service';
import {FormCitizenListResponseDTO} from '../../modules/forms/dtos/form-citizen-list-response-dto';

export function ListPage(): JSX.Element {
    const api = useApi();
    const dispatch = useAppDispatch();
    const [failedToLoad, setFailedToLoad] = useState(false);
    const [applications, setApplications] = useState<FormCitizenListResponseDTO[]>();
    const [search, setSearch] = useState('');

    const provider = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));
    const disableGoverListingPage = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.listingPage.disableGoverListingPage));
    const themeId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.theme));
    const metaDialog = useAppSelector((state) => state.app.showDialog);

    const [theme, setTheme] = useState<Theme>();

    useEffect(() => {
        new FormsApiService(api)
            .listPublicAll()
            .then(forms => setApplications(forms.content))
            .catch((err) => {
                console.error(err);
                setFailedToLoad(true);
            });

        dispatch(clearCustomerInput());
        dispatch(clearErrors());
        dispatch(resetStepper());
        dispatch(clearLoadedForm());
    }, []);

    useEffect(() => {
        if (themeId != null && isStringNotNullOrEmpty(themeId)) {
            new ThemesApiService(api)
                .retrievePublic(parseInt(themeId))
                .then((theme) => {
                    setTheme(theme);
                })
                .catch((err) => {
                    console.error(err);
                    setFailedToLoad(true);
                });
        }
    }, [themeId]);

    if (failedToLoad) {
        return <><MetaElement
            title={'Seite nicht gefunden'}
            titlePrefix={provider}
        /><NotFoundPage /></>;
    } else if (disableGoverListingPage == 'true') {
        return <><MetaElement
            title={'Online-Antrags-Management'}
            titlePrefix={provider}
        /><NotFoundPage
            title={'Online-Antrags-Management von ' + provider}
            msg={'Dieses System besitzt keine öffentliche Index-Seite. Bitte nutzen Sie die direkten Links zu den Formularen dieses Anbieters.'}
        /></>;
    } else if (applications == null) {
        return <LoadingPlaceholder />;
    } else {
        const filteredApplications = applications.filter((app) => app
            .title
            .toLowerCase()
            .includes(search.toLowerCase()),
        );

        return (
            <ThemeProvider theme={(baseTheme: MuiTheme) => createAppTheme(theme, baseTheme)}>
                <MetaElement
                    title={'Online-Antrags-Management'}
                    titlePrefix={provider}
                />

                <AppHeader
                    mode={AppMode.CustomerDisplay}
                />

                <main role="main">

                    <Introductory
                        mode={AppMode.CustomerDisplay}
                    />

                    <Box
                        style={{
                            backgroundColor: '#F3F3F3',
                            minHeight: '50vh',
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
                                                <ApplicationListItemPublic
                                                    key={app.slug + app.version}
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

                </main>

                <AppFooter
                    mode={AppMode.CustomerDisplay}
                />

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

            </ThemeProvider>
        );
    }
}
