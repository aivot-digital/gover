import React, {useEffect, useState} from 'react';
import {
    LoadingPlaceholderComponentView
} from '../../components/static-components/loading-placeholder/loading-placeholder.component.view';
import {Box, Container, List, Theme, ThemeProvider} from '@mui/material';
import {createAppTheme} from '../../theming/themes';
import {NotFoundPage} from '../../components/static-components/not-found-page/not-found-page';
import {MetaElement} from '../../components/meta-element/meta-element';
import {useAppSelector} from '../../hooks/use-app-selector';
import {ListApplication} from "../../models/entities/list-application";
import {selectSystemConfigValue} from "../../slices/system-config-slice";
import {SystemConfigKeys} from "../../data/system-config-keys";
import {ApplicationService} from "../../services/application.service";
import {AppHeader} from "../../components/app-header/app-header";
import {AppMode} from "../../data/app-mode";
import {Link} from "react-router-dom";
import {ListHeader} from "../../components/list-header/list-header";
import {AppFooter} from "../../components/app-footer/app-footer";
import {Introductory} from "../../components/introductory/introductory";
import {ApplicationListItemDisplay} from "../../components/application-list-item/application-list-item-display";

export function ListPage() {
    const [failedToLoad, setFailedToLoad] = useState(false);
    const [applications, setApplications] = useState<ListApplication[]>();
    const [search, setSearch] = useState('');

    const provider = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));
    const systemTheme = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.theme));

    useEffect(() => {
        ApplicationService.listPublishedApplications()
            .then(setApplications)
            .catch(err => {
                console.error(err);
                setFailedToLoad(true);
            });
    }, []);

    if (failedToLoad) {
        return <NotFoundPage/>;
    } else if (applications == null) {
        return <LoadingPlaceholderComponentView/>;
    } else {
        const filteredApplications = applications.filter(app => app.headline.toLowerCase().includes(search));

        return (
            <ThemeProvider theme={(baseTheme: Theme) => createAppTheme(systemTheme, baseTheme)}>
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

                        <Box sx={{mt: 3, mb: 5}}>
                            <List>
                                {
                                    filteredApplications.map(app => (
                                        <ApplicationListItemDisplay
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
