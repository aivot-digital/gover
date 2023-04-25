import {Box, Container, Grid, List, Typography} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {ApplicationService} from '../../../services/application.service';
import {useNavigate} from 'react-router-dom';
import {
    LoadingPlaceholderComponentView
} from '../../../components/static-components/loading-placeholder/loading-placeholder.component.view';
import {faCloudUpload, faPlus} from '@fortawesome/pro-light-svg-icons';
import {AppFooter} from '../../../components/app-footer/app-footer';
import {Introductory} from '../../../components/introductory/introductory';
import {ApplicationListItem} from './components/application-list-item/application-list-item';
import {BoxLinkComponentView} from '../../../components/box-link/box-link.component.view';
import {AddApplicationDialog} from '../../../dialogs/add-application-dialog/add-application-dialog';
import {ImportApplicationDialog} from '../../../dialogs/import-application-dialog/import-application-dialog';
import {MetaElement} from '../../../components/meta-element/meta-element';
import {Application} from '../../../models/entities/application';
import {ProviderLink} from '../../../models/entities/provider-link';
import {ProviderLinksService} from '../../../services/provider-links.service';
import {AppHeader} from '../../../components/app-header/app-header';
import {AppMode} from '../../../data/app-mode';
import {ListHeader} from '../../../components/list-header/list-header';
import {EmptyDataListPlaceholder} from '../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {
    EmptySearchDataListPlaceholder
} from '../../../components/empty-search-data-list-placeholder/empty-search-data-list-placeholder';
import {Localization} from '../../../locale/localization';
import strings from './applications-overview-page-strings.json';
import {CloneApplicationDialog} from '../../../dialogs/clone-application-dialog/clone-application-dialog';
import {useAuthGuard} from '../../../hooks/use-auth-guard';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../data/system-config-keys';

const __ = Localization(strings);

export function ApplicationsOverviewPage() {
    useAuthGuard();

    const navigate = useNavigate();

    const [applications, setApplications] = useState<Application[]>();
    const [providerLinks, setProviderLinks] = useState<ProviderLink[]>();
    const [search, setSearch] = useState<string>('');

    const [applicationToClone, setApplicationToClone] = useState<Application>();
    const [showAddApplicationDialog, setShowAddApplicationDialog] = useState(false);
    const [showImportApplicationDialog, setShowImportApplicationDialog] = useState(false);

    const providerName = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));

    useEffect(() => {
        ApplicationService
            .list()
            .then(applicationsResponse => {
                setApplications(applicationsResponse._embedded.applications);
            });
        ProviderLinksService
            .list()
            .then(providerLinksResponse => {
                setProviderLinks(providerLinksResponse._embedded.providerLinks);
            });
    }, []);

    const handleClone = (application: Application, navigateToEditAfterwards: boolean) => {
        ApplicationService
            .clone(application.id, application.slug, application.version, application.root.title ?? '')
            .then((result) => {
                if (navigateToEditAfterwards) {
                    navigate('/edit/' + result.id);
                } else {
                    ApplicationService.list()
                        .then(applicationsResponse => {
                            setApplications(applicationsResponse._embedded.applications);
                        });
                }
            });
        setShowAddApplicationDialog(false);
    };

    const handleAdd = (application: Application, navigateToEditAfterwards: boolean) => {
        ApplicationService
            .createNew(application.slug, application.version, application.root.title ?? '', application.root)
            .then((result) => {
                if (navigateToEditAfterwards) {
                    navigate('/edit/' + result.id);
                } else {
                    ApplicationService.list()
                        .then(applicationsResponse => {
                            setApplications(applicationsResponse._embedded.applications);
                        });
                }
            });
        setShowAddApplicationDialog(false);
    };

    const handleImport = (application: Application, navigateToEditAfterwards: boolean) => {
        ApplicationService.createNew(application.slug, application.version, application.root.title ?? '', application.root)
            .then((result) => {
                if (navigateToEditAfterwards) {
                    navigate('/edit/' + result.id);
                } else {
                    ApplicationService.list()
                        .then(applicationsResponse => {
                            setApplications(applicationsResponse._embedded.applications);
                        });
                }
            });
        setShowImportApplicationDialog(false);
    };

    const handleApplicationDelete = (application: Application) => {
        if (application.slug != null) {
            ApplicationService.destroy(application.id)
                .then(() => {
                    ApplicationService.list()
                        .then(applicationsResponse => {
                            setApplications(applicationsResponse._embedded.applications);
                        });
                });
        }
    };

    if (applications == null) {
        return <LoadingPlaceholderComponentView
            message={__.loadingPlaceholder}
        />
    }

    const filteredApplications = applications
        .filter(app => (app.root.title ?? '')
            .toLowerCase()
            .includes(search.toLowerCase())
        );

    return (
        <>
            <MetaElement
                title={providerName != null && providerName.length > 0 ? providerName : 'powered by Aivot'}
            />

            <AppHeader
                mode={AppMode.Staff}
            />

            <Introductory/>

            <div style={{backgroundColor: '#F3F3F3'}}>
                <Container sx={{mb: 5, py: 4}}>
                    <Box
                        sx={{
                            mt: 3,
                            mb: 6
                        }}
                    >
                        <ListHeader
                            title={__.title}
                            search={search}
                            onSearchChange={setSearch}
                            searchPlaceholder={__.searchPlaceholder}
                            actions={[
                                {
                                    label: __.addApplicationLabel,
                                    icon: faPlus,
                                    onClick: () => setShowAddApplicationDialog(true),
                                },
                                {
                                    tooltip: __.importApplication,
                                    icon: faCloudUpload,
                                    onClick: () => setShowImportApplicationDialog(true),
                                },
                            ]}
                        />
                    </Box>
                    <Box sx={{mt: 3, mb: 5}}>
                        {
                            applications.length === 0 &&
                            <EmptyDataListPlaceholder
                                helperText={__.noApplicationsPlaceholder}
                                addText={__.addApplicationLabel}
                                onAdd={() => setShowAddApplicationDialog(true)}
                            />
                        }
                        {
                            applications.length > 0 &&
                            filteredApplications.length === 0 &&
                            <EmptySearchDataListPlaceholder
                                helperText={__.noMatchingApplicationsPlaceholder}
                            />
                        }
                        {
                            filteredApplications.length > 0 &&
                            <List>
                                {
                                    applications
                                        .filter(app => (app.root.title ?? '').toLowerCase().includes(search.toLowerCase()))
                                        .sort((app1, app2) => {
                                            const time1 = app1.updated ? new Date(app1.updated) : new Date();
                                            const time2 = app2.updated ? new Date(app2.updated) : new Date();
                                            return time2.getTime() - time1.getTime();
                                        })
                                        .map(app => (
                                            <ApplicationListItem
                                                key={app.slug + app.version}
                                                application={app}
                                                onClone={() => setApplicationToClone(app)}
                                                onDelete={handleApplicationDelete}
                                            />
                                        ))
                                }
                            </List>
                        }
                    </Box>
                </Container>
            </div>

            <Container sx={{mt: 10, mb: 12}}>
                <Typography
                    variant={'h5'}
                    sx={{fontSize: '1.75rem'}}
                >
                    {__.serviceHeadline}
                </Typography>
                <Grid
                    container
                    spacing={4}
                    sx={{mt: -2}}
                >
                    <Grid
                        item
                        xs={6}
                    >
                        <BoxLinkComponentView link="https://aivot.de/gover">
                            <span>{__.generalSupportLinkLabel1}</span>
                            <br/>
                            {__.generalSupportLinkLabel2}
                        </BoxLinkComponentView>
                    </Grid>
                    {
                        providerLinks != null &&
                        providerLinks.map(({link, text}) => (
                            <Grid
                                key={text}
                                item
                                xs={6}
                            >
                                <BoxLinkComponentView link={link}>
                                    {
                                        text
                                            .split('\n')
                                            .map((line, index) =>
                                                index === 0 ?
                                                    <React.Fragment key={index}>
                                                        <span>{line}</span>
                                                        <br/></React.Fragment> :
                                                    <React.Fragment key={index}>{line}<br/></React.Fragment>
                                            )
                                    }
                                </BoxLinkComponentView>
                            </Grid>
                        ))
                    }
                </Grid>
            </Container>


            <AppFooter mode={AppMode.Staff}/>

            <AddApplicationDialog
                applications={applications}
                open={showAddApplicationDialog}
                onHide={() => setShowAddApplicationDialog(false)}
                onSave={handleAdd}
            />

            {
                applicationToClone &&
                <CloneApplicationDialog
                    applications={applications}
                    onHide={() => setApplicationToClone(undefined)}
                    onSave={handleClone}
                    source={applicationToClone}
                    open={true}
                />
            }

            <ImportApplicationDialog
                applications={applications}
                open={showImportApplicationDialog}
                onHide={() => setShowImportApplicationDialog(false)}
                onImport={handleImport}
            />
        </>
    );
}
