import React, {useEffect, useState} from 'react';
import {LoadingPlaceholderComponentView} from '../../components/loading-placeholder/loading-placeholder.component.view';
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
import {clearCustomerInput, clearErrors, clearLoadedForm} from '../../slices/app-slice';
import {AlertComponent} from '../../components/alert/alert-component';
import {useApi} from '../../hooks/use-api';
import {useFormsApi} from '../../hooks/use-forms-api';
import {FormListProjection, FormListProjectionPublic} from '../../models/entities/form';
import {Theme} from '../../models/entities/theme';
import {useThemesApi} from '../../hooks/use-themes-api';
import type {Theme as MuiTheme} from '@mui/material/styles/createTheme';
import {isStringNotNullOrEmpty} from '../../utils/string-utils';

export function ListPage(): JSX.Element {
    const api = useApi();
    const dispatch = useAppDispatch();
    const [failedToLoad, setFailedToLoad] = useState(false);
    const [applications, setApplications] = useState<FormListProjectionPublic[]>();
    const [search, setSearch] = useState('');

    const provider = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));
    const themeId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.theme));

    const [theme, setTheme] = useState<Theme>();

    useEffect(() => {
        useFormsApi(api)
            .listPublic()
            .then(setApplications)
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
            useThemesApi(api)
                .retrievePublicTheme(parseInt(themeId))
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
            title={provider}
        /><NotFoundPage/></>;
    } else if (applications == null) {
        return <LoadingPlaceholderComponentView/>;
    } else {
        const filteredApplications = applications.filter((app) => app.title == null || app.title.toLowerCase().includes(search));

        return (
            <ThemeProvider theme={(baseTheme: MuiTheme) => createAppTheme(theme, baseTheme)}>
                <MetaElement
                    title={provider}
                />

                <AppHeader
                    mode={AppMode.CustomerDisplay}
                />

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
                            py: 4,
                        }}
                    >
                        {
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
                            filteredApplications.length > 0 &&
                            <>
                                <ListHeader
                                    title="Unsere Formulare"
                                    search={undefined}
                                    onSearchChange={setSearch}
                                    searchPlaceholder="Formular suchen..."
                                    actions={[]}
                                />

                                <Box
                                    sx={{
                                        mt: 3,
                                        mb: 5,
                                    }}
                                >
                                    <List>
                                        {
                                            filteredApplications.map((app) => (
                                                <ApplicationListItemPublic
                                                    key={app.slug + app.version}
                                                    form={app}
                                                />
                                            ))
                                        }
                                    </List>
                                </Box>
                            </>
                        }
                    </Container>
                </Box>

                <AppFooter
                    mode={AppMode.CustomerDisplay}
                />

            </ThemeProvider>
        );
    }
}
