import React, {useEffect, useState} from 'react';
import {LoadingPlaceholderComponentView} from '../../components/static-components/loading-placeholder/loading-placeholder.component.view';
import {Box, Container, List, ThemeProvider} from '@mui/material';
import {createDefaultAppTheme} from '../../theming/themes';
import {NotFoundPage} from '../../components/static-components/not-found-page/not-found-page';
import {MetaElement} from '../../components/meta-element/meta-element';
import {useAppSelector} from '../../hooks/use-app-selector';
import {type PublicListApplication} from '../../models/entities/public-list-application';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {ApplicationService} from '../../services/application-service';
import {AppHeader} from '../../components/app-header/app-header';
import {AppMode} from '../../data/app-mode';
import {ListHeader} from '../../components/list-header/list-header';
import {AppFooter} from '../../components/app-footer/app-footer';
import {Introductory} from '../../components/introductory/introductory';
import {ApplicationListItemPublic} from '../../components/application-list-item/application-list-item-public';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {resetUserInput} from '../../slices/customer-input-slice';
import {resetErrors} from '../../slices/customer-input-errors-slice';
import {resetStepper} from '../../slices/stepper-slice';
import {clearAppModel} from '../../slices/app-slice';

export function ListPage() {
    const dispatch = useAppDispatch();
    const [failedToLoad, setFailedToLoad] = useState(false);
    const [applications, setApplications] = useState<PublicListApplication[]>();
    const [search, setSearch] = useState('');

    const provider = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));
    const systemTheme = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.theme));

    useEffect(() => {
        ApplicationService.listPublic()
            .then((apps) => {
                setApplications(apps.sort());
            })
            .catch((err) => {
                console.error(err);
                setFailedToLoad(true);
            });

        dispatch(resetUserInput());
        dispatch(resetErrors());
        dispatch(resetStepper());
        dispatch(clearAppModel());
    }, []);

    if (failedToLoad) {
        return <NotFoundPage/>;
    } else if (applications == null) {
        return <LoadingPlaceholderComponentView/>;
    } else {
        const filteredApplications = applications.filter((app) => app.headline == null || app.headline.toLowerCase().includes(search));

        return (
            <ThemeProvider theme={createDefaultAppTheme}>
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
                        <ListHeader
                            title="Unsere Formulare"
                            search={search}
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
                                            application={app}
                                        />
                                    ))
                                    // TODO: Empty state and cleaning
                                }
                            </List>
                        </Box>
                    </Container>
                </Box>

                <AppFooter
                    mode={AppMode.CustomerDisplay}
                />

            </ThemeProvider>
        );
    }
}
