import {Box, Container, Paper, Typography} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {LoadingPlaceholder} from '../../../components/loading-placeholder/loading-placeholder';
import {AppFooter} from '../../../components/app-footer/app-footer';
import {Introductory} from '../../../components/introductory/introductory';
import {AddFormDialog} from '../../../dialogs/application-dialogs/add-form-dialog/add-form-dialog';
import {ImportApplicationDialog} from '../../../dialogs/application-dialogs/import-application-dialog/import-application-dialog';
import {MetaElement} from '../../../components/meta-element/meta-element';
import {Form} from '../../../models/entities/form';
import {AppHeader} from '../../../components/app-header/app-header';
import {AppMode} from '../../../data/app-mode';
import {ListHeader} from '../../../components/list-header/list-header';
import {EmptyDataListPlaceholder} from '../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {EmptySearchDataListPlaceholder} from '../../../components/empty-search-data-list-placeholder/empty-search-data-list-placeholder';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../data/system-config-keys';
import {selectMemberships, selectUser} from '../../../slices/user-slice';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import {DeleteApplicationDialog} from '../../../dialogs/application-dialogs/delete-application-dialog/delete-application-dialog';
import {ProviderLinksGrid} from '../../../modules/provider-links/components/provider-links-grid';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined';
import {useApi} from '../../../hooks/use-api';
import {hideLoadingOverlayWithTimeout, showLoadingOverlay} from '../../../slices/loading-overlay-slice';
import {FormsApiService} from '../../../modules/forms/forms-api-service';
import {ApplicationListItem} from '../../../components/application-list-item/application-list-item';
import {FormListResponseDTO} from '../../../modules/forms/dtos/form-list-response-dto';

export function FormListPage() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const api = useApi();

    const [forms, setForms] = useState<FormListResponseDTO[]>();
    const [search, setSearch] = useState<string>('');

    const [showAddApplicationDialog, setShowAddApplicationDialog] = useState(false);
    const [showImportApplicationDialog, setShowImportApplicationDialog] = useState(false);

    const [applicationToClone, setApplicationToClone] = useState<Form>();
    const [applicationToImport, setApplicationToImport] = useState<Form>();
    const [applicationToUpgrade, setApplicationToUpgrade] = useState<Form>();
    const [applicationToDelete, setApplicationToDelete] = useState<FormListResponseDTO>();

    const user = useAppSelector(selectUser);
    const memberships = useAppSelector(selectMemberships);
    const providerName = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));

    useEffect(() => {
        new FormsApiService(api)
            .list(0, 999, 'internalTitle', 'ASC', {
                userId: user?.id,
                isDeveloper: true,
                isCurrentlyPublishedVersion: true,
            })
            .then(forms => setForms(forms.content))
            .catch((err) => {
                console.error(err);
            });
    }, []);

    const handleAdd = (form: Form, navigateToEditAfterwards: boolean): void => {
        if (forms != null) {
            dispatch(showLoadingOverlay('Formular wird gespeichert'));
            new FormsApiService(api)
                .create(form)
                .then((createdApplication) => {
                    if (navigateToEditAfterwards) {
                        navigate(`/forms/${createdApplication.id}`);
                    } else {
                        setForms([
                            createdApplication,
                            ...forms,
                        ]);
                    }
                })
                .catch((err) => {
                    if (err.status === 409) {
                        dispatch(showErrorSnackbar('Formular konnte nicht angelegt werden. Es existiert bereits ein Formular mit dieser URL in einem Fachbereich, dem Sie nicht zugehörig sind.'));
                    } else {
                        dispatch(showErrorSnackbar('Formular konnte nicht angelegt werden.'));
                        console.error(err);
                    }
                })
                .finally(() => {
                    dispatch(hideLoadingOverlayWithTimeout(1000));
                });
        }
    };

    const handleApplicationClone = (formToClone: FormListResponseDTO): void => {
        // TODO: Implement this
    };

    const handleFormNewVersion = (formToBaseNewVersionOn: FormListResponseDTO): void => {
        new FormsApiService(api)
            .newVersion({
                id: formToBaseNewVersionOn.id,
                version: formToBaseNewVersionOn.draftedVersion ?? formToBaseNewVersionOn.publishedVersion,
            })
            .then((newFormVersion) => {
                navigate(`/forms/${newFormVersion.id}/${newFormVersion.version}`);
            });
    };

    if (forms == null || user == null || memberships == null) {
        return <LoadingPlaceholder
            message="Formulare werden geladen"
        />;
    }

    const filteredApplications = forms
        .filter((app) => app
            .internalTitle
            .toLowerCase()
            .includes(search.toLowerCase()),
        );

    return (
        <>
            <MetaElement
                title={providerName != null && providerName.length > 0 ? providerName : 'powered by Aivot'}
            />

            <AppHeader
                mode={AppMode.Staff}
                onDeleteFormData={() => {
                }}
            />

            <Introductory
                mode={AppMode.Staff}
            />

            <Box
                sx={{
                    backgroundColor: '#F3F3F3',
                    minHeight: '60vh',
                }}
            >
                <Container
                    sx={{
                        mb: 5,
                        py: 4,
                    }}
                >
                    {
                        memberships.length === 0 &&
                        <Paper
                            sx={{
                                p: 4,
                                mt: 4,
                            }}
                        >
                            <Typography
                                variant="h5"
                                component="h2"
                            >
                                Noch keinem Fachbereich zugeordnet
                            </Typography>
                            <Typography>
                                Eine Administrator:in muss Sie noch einem Fachbereich zuordnen und Ihnen eine Rolle
                                zuweisen.
                                Erst dann können Sie mit der Entwicklung von Formularen loslegen.
                            </Typography>
                        </Paper>
                    }

                    {
                        memberships.length > 0 &&
                        <>
                            <Box
                                sx={{
                                    mt: 3,
                                    mb: 6,
                                }}
                            >
                                <ListHeader
                                    title="Ihre Online-Formulare"
                                    search={search}
                                    onSearchChange={setSearch}
                                    searchPlaceholder="Formular suchen…"
                                    actions={[
                                        {
                                            label: 'Neues Formular',
                                            icon: <AddOutlinedIcon />,
                                            onClick: () => {
                                                setShowAddApplicationDialog(true);
                                            },
                                        },
                                        {
                                            tooltip: 'Formular importieren',
                                            icon: <CloudUploadOutlinedIcon sx={{transform: 'scale(1.2)'}} />,
                                            onClick: () => {
                                                setShowImportApplicationDialog(true);
                                            },
                                        },
                                    ]}
                                    hint={{
                                        text: 'Hier finden Sie alle Formulare, für die Sie eine Berechtigung besitzen. Initial werden diese alphabetisch sortiert (A-Z) nach Titel angezeigt.',
                                        moreLink: 'https://wiki.teamaivot.de/de/dokumentation/gover/benutzerhandbuch/home' /* TODO: Link anpassen */,
                                    }}
                                />
                            </Box>
                            <Box
                                sx={{
                                    mt: 3,
                                    mb: 5,
                                }}
                            >
                                {
                                    forms.length === 0 &&
                                    <EmptyDataListPlaceholder
                                        helperText="Sie haben aktuell keine Formulare. Starten Sie jetzt mit Ihrem ersten Formular!"
                                        addText="Neues Formular"
                                        onAdd={() => {
                                            setShowAddApplicationDialog(true);
                                        }}
                                    />
                                }
                                {
                                    forms.length > 0 &&
                                    filteredApplications.length === 0 &&
                                    <EmptySearchDataListPlaceholder
                                        helperText="Es gibt keine Formulare, die Ihrer Suche entsprechen…"
                                    />
                                }
                                {
                                    filteredApplications.length > 0 &&
                                    <Box>
                                        {
                                            filteredApplications
                                                .map((form) => (
                                                    <ApplicationListItem
                                                        key={form.id}
                                                        form={form}
                                                        onClone={handleApplicationClone}
                                                        onDelete={setApplicationToDelete}
                                                        onNewVersion={handleFormNewVersion}
                                                        memberships={memberships}
                                                        user={user}
                                                    />
                                                ))
                                        }
                                    </Box>
                                }
                            </Box>
                        </>
                    }
                </Container>
            </Box>

            <ProviderLinksGrid />

            <AppFooter mode={AppMode.Staff} />

            <AddFormDialog
                mode={showAddApplicationDialog ? 'new' : (applicationToUpgrade != null ? 'new-version' : (applicationToClone != null ? 'clone' : 'import'))}
                existingApplications={forms}
                applicationToBaseOn={applicationToUpgrade ?? applicationToClone ?? applicationToImport}
                open={showAddApplicationDialog || applicationToUpgrade != null || applicationToClone != null || applicationToImport != null}
                onClose={() => {
                    setShowAddApplicationDialog(false);
                    setApplicationToClone(undefined);
                    setApplicationToImport(undefined);
                    setApplicationToUpgrade(undefined);
                }}
                onSave={handleAdd}
            />

            <ImportApplicationDialog
                open={showImportApplicationDialog}
                onClose={() => {
                    setShowImportApplicationDialog(false);
                }}
                onImport={(formToImport) => {
                    setApplicationToImport(formToImport);
                    setShowImportApplicationDialog(false);
                }}
            />

            <DeleteApplicationDialog
                application={applicationToDelete}
                onDelete={() => {
                    setForms(forms.filter((app) => app.id !== applicationToDelete?.id));
                    setApplicationToDelete(undefined);
                }}
                onCancel={() => {
                    setApplicationToDelete(undefined);
                }}
            />
        </>
    );
}

