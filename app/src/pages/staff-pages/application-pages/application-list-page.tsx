import { Box, Container } from '@mui/material';
import React, { useEffect, useState } from 'react';
import { ApplicationService } from '../../../services/application-service';
import { useNavigate } from 'react-router-dom';
import {
    LoadingPlaceholderComponentView,
} from '../../../components/static-components/loading-placeholder/loading-placeholder.component.view';
import { faCloudUpload, faPlus } from '@fortawesome/pro-light-svg-icons';
import { AppFooter } from '../../../components/app-footer/app-footer';
import { Introductory } from '../../../components/introductory/introductory';
import {
    AddApplicationDialog,
} from '../../../dialogs/application-dialogs/add-application-dialog/add-application-dialog';
import {
    ImportApplicationDialog,
} from '../../../dialogs/application-dialogs/import-application-dialog/import-application-dialog';
import { MetaElement } from '../../../components/meta-element/meta-element';
import { type Application } from '../../../models/entities/application';
import { AppHeader } from '../../../components/app-header/app-header';
import { AppMode } from '../../../data/app-mode';
import { ListHeader } from '../../../components/list-header/list-header';
import { EmptyDataListPlaceholder } from '../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {
    EmptySearchDataListPlaceholder,
} from '../../../components/empty-search-data-list-placeholder/empty-search-data-list-placeholder';
import { useAppSelector } from '../../../hooks/use-app-selector';
import { selectSystemConfigValue } from '../../../slices/system-config-slice';
import { SystemConfigKeys } from '../../../data/system-config-keys';
import { type ListApplication } from '../../../models/entities/list-application';
import { type ListApplicationGroup } from '../../../models/lib/list-application-group';
import { compareVersions } from '../../../utils/version-utils';
import { ApplicationListItemGroup } from '../../../components/application-list-item-group/application-list-item-group';
import { selectMemberships } from '../../../slices/user-slice';
import { useAppDispatch } from '../../../hooks/use-app-dispatch';
import { showErrorSnackbar } from '../../../slices/snackbar-slice';
import {
    DeleteApplicationDialog,
} from '../../../dialogs/application-dialogs/delete-application-dialog/delete-application-dialog';
import { ProviderLinks } from './components/provider-links';

function groupApplications(applications: ListApplication[]): ListApplicationGroup[] {
    const appMap = new Map<string, ListApplication[]>();

    for (const app of applications) {
        if (appMap.has(app.slug)) {
            appMap.get(app.slug)!.push(app);
        } else {
            appMap.set(app.slug, [
                app,
            ]);
        }
    }

    return Array
        .from(appMap)
        .map(([_, value]) => ({
            slug: value[0].slug,
            applications: value.sort((a1, a2) => compareVersions(a1.version, a2.version)),
        }))
        .sort((g1, g2) => g1.slug.localeCompare(g2.slug));
}

export function ApplicationListPage(): JSX.Element {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const [applications, setApplications] = useState<ListApplication[]>();
    const [search, setSearch] = useState<string>('');

    const [showAddApplicationDialog, setShowAddApplicationDialog] = useState(false);
    const [showImportApplicationDialog, setShowImportApplicationDialog] = useState(false);

    const [applicationToClone, setApplicationToClone] = useState<Application>();
    const [applicationToImport, setApplicationToImport] = useState<Application>();
    const [applicationToUpgrade, setApplicationToUpgrade] = useState<Application>();
    const [applicationToDelete, setApplicationToDelete] = useState<ListApplication>();

    const memberships = useAppSelector(selectMemberships);
    const providerName = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));

    useEffect(() => {
        ApplicationService
            .list()
            .then(setApplications)
            .catch((err) => {
                console.error(err);
            });
    }, []);

    const handleAdd = (application: Application, navigateToEditAfterwards: boolean) => {
        if (applications != null) {
            ApplicationService
                .create(application)
                .then((createdApplication) => {
                    if (navigateToEditAfterwards) {
                        navigate(`/edit/${ createdApplication.id }`);
                    } else {
                        setApplications([
                            createdApplication,
                            ...applications,
                        ]);
                    }
                })
                .catch((err) => {
                    if (err.status === 409) {
                        dispatch(showErrorSnackbar('Formular konnte nicht angelegt werden. Es existiert bereits ein Formular mit dieser URL und dieser Version.'));
                    } else {
                        dispatch(showErrorSnackbar('Formular konnte nicht angelegt werden.'));
                        console.error(err);
                    }
                });
        }
    };

    const handleApplicationClone = (appToClone: ListApplication) => {
        ApplicationService
            .retrieve(appToClone.id)
            .then(setApplicationToClone);
    };

    const handleApplicationNewVersion = (appToClone: ListApplication) => {
        ApplicationService
            .retrieve(appToClone.id)
            .then(setApplicationToUpgrade);
    };

    if (applications == null) {
        return <LoadingPlaceholderComponentView
            message="Lade Formulare..."
        />;
    }

    const filteredApplications = applications
        .filter((app) => app
            .title
            .toLowerCase()
            .includes(search.toLowerCase()),
        );
    const groupedApplications = groupApplications(filteredApplications);

    return (
        <>
            <MetaElement
                title={ providerName != null && providerName.length > 0 ? providerName : 'powered by Aivot' }
            />

            <AppHeader
                mode={ AppMode.Staff }
            />

            <Introductory
                mode={ AppMode.Staff }
            />

            <Box sx={ {backgroundColor: '#F3F3F3'} }>
                <Container sx={ {mb: 5, py: 4} }>
                    <Box
                        sx={ {
                            mt: 3,
                            mb: 6,
                        } }
                    >
                        <ListHeader
                            title="Ihre Online-Formulare"
                            search={ search }
                            onSearchChange={ setSearch }
                            searchPlaceholder="Formular suchen..."
                            actions={ [
                                {
                                    label: 'Neues Formular',
                                    icon: faPlus,
                                    onClick: () => {
                                        setShowAddApplicationDialog(true);
                                    },
                                },
                                {
                                    tooltip: 'Formular importieren',
                                    icon: faCloudUpload,
                                    onClick: () => {
                                        setShowImportApplicationDialog(true);
                                    },
                                },
                            ] }
                        />
                    </Box>
                    <Box sx={ {mt: 3, mb: 5} }>
                        {
                            applications.length === 0 &&
                            <EmptyDataListPlaceholder
                                helperText="Sie haben aktuell keine Formulare. Starten Sie jetzt mit Ihrem ersten Formular!"
                                addText="Neues Formular"
                                onAdd={ () => {
                                    setShowAddApplicationDialog(true);
                                } }
                            />
                        }
                        {
                            applications.length > 0 &&
                            filteredApplications.length === 0 &&
                            <EmptySearchDataListPlaceholder
                                helperText="Es gibt keine Formulare, die Ihrer Suche entsprechen..."
                            />
                        }
                        {
                            groupedApplications.length > 0 &&
                            <Box>
                                {
                                    groupedApplications
                                        .map((group) => (
                                            <ApplicationListItemGroup
                                                key={ group.slug }
                                                group={ group }
                                                onClone={ handleApplicationClone }
                                                onDelete={ setApplicationToDelete }
                                                onNewVersion={ handleApplicationNewVersion }
                                                memberships={ memberships }
                                            />
                                        ))
                                }
                            </Box>
                        }
                    </Box>
                </Container>
            </Box>

            <Container sx={ {mt: 10, mb: 12} }>
                <ProviderLinks/>
            </Container>

            <AppFooter mode={ AppMode.Staff }/>

            <AddApplicationDialog
                mode={ showAddApplicationDialog ? 'new' : (applicationToUpgrade != null ? 'new-version' : (applicationToClone != null ? 'clone' : 'import')) }
                existingApplications={ applications }
                applicationToBaseOn={ applicationToUpgrade ?? applicationToClone ?? applicationToImport }
                open={ showAddApplicationDialog || applicationToUpgrade != null || applicationToClone != null || applicationToImport != null }
                onClose={ () => {
                    setShowAddApplicationDialog(false);
                    setApplicationToClone(undefined);
                    setApplicationToImport(undefined);
                    setApplicationToUpgrade(undefined);
                } }
                onSave={ handleAdd }
            />

            <ImportApplicationDialog
                open={ showImportApplicationDialog }
                onClose={ () => {
                    setShowImportApplicationDialog(false);
                } }
                onImport={ setApplicationToImport }
            />

            <DeleteApplicationDialog
                application={ applicationToDelete }
                onDelete={ () => {
                    setApplications(applications.filter((app) => app.id !== applicationToDelete?.id));
                    setApplicationToDelete(undefined);
                } }
                onCancel={ () => {
                    setApplicationToDelete(undefined);
                } }
            />
        </>
    );
}

