import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Box} from '@mui/material';
import MoreVertOutlinedIcon from '@mui/icons-material/MoreVertOutlined';
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined';
import React, {useRef, useState} from 'react';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import {showErrorSnackbar} from '../../../../slices/snackbar-slice';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectMemberships, selectUser} from '../../../../slices/user-slice';
import {AddFormDialog} from '../../dialogs/add-form-dialog';
import {downloadFormExportFile} from '../../../../utils/download-utils';
import {useApi} from '../../../../hooks/use-api';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {FormVersionsDialog} from '../../dialogs/form-versions-dialog';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import Typography from '@mui/material/Typography';
import {format} from 'date-fns/format';
import {GridColDef} from '@mui/x-data-grid';
import {hideLoadingOverlay, showLoadingOverlay} from '../../../../slices/loading-overlay-slice';
import {Link, useNavigate} from 'react-router-dom';
import {FormsListPageHelp} from '../../components/forms-list-page-help';
import {FormStatusChipGroup, getFormStatus} from '../../components/form-status-chip-group';
import HomeStorage from '@aivot/mui-material-symbols-400-outlined/dist/home-storage/HomeStorage';
import {useConfirm} from '../../../../providers/confirm-provider';
import NewWindow from '@aivot/mui-material-symbols-400-outlined/dist/new-window/NewWindow';
import {FormsListRowMenu} from '../../components/forms-list-row-menu';
import {setLoadingMessage} from '../../../../slices/shell-slice';
import {MoveFormToDepartmentDialog} from '../../dialogs/move-form-to-department-dialog';
import {ListControlRef} from '../../../../components/generic-list/generic-list-props';
import {Page} from '../../../../models/dtos/page';
import Edit from '@aivot/mui-material-symbols-400-outlined/dist/edit/Edit';
import Visibility from '@aivot/mui-material-symbols-400-outlined/dist/visibility/Visibility';
import {
    FormResourceAccessControlDialog,
} from '../../../resource-access-controls/dialogs/form-resource-access-control-dialog';
import {DepartmentApiService} from '../../../departments/services/department-api-service';
import {FormEntity} from '../../entities/form-entity';
import {FormApiService} from '../../services/form-api-service';
import {FormVersionEntity} from '../../entities/form-version-entity';
import {FormVersionApiService} from '../../services/form-version-api-service';
import {ExportFormDialog} from '../../dialogs/export-form-dialog';
import {ImportFormDialog} from '../../dialogs/import-form-dialog';
import {DeleteFormDialog} from '../../dialogs/delete-form-dialog';

const availableFilter = [
    {
        label: 'Alle Formulare',
        value: 'all',
    },
    {
        label: 'Entwürfe',
        value: 'drafted',
    },
    {
        label: 'Veröffentlicht',
        value: 'published',
    },
    {
        label: 'Zurückgezogen',
        value: 'revoked',
    },
];

interface FormListEntry extends FormEntity {
    developingDepartmentName?: string;
    lastEditorName?: string;
}

const columns: GridColDef<FormListEntry>[] = [
    {
        field: 'icon',
        headerName: '',
        renderCell: () => <CellContentWrapper sx={{
            alignItems: 'start',
            py: 2,
        }}><DescriptionOutlinedIcon/></CellContentWrapper>,
        disableColumnMenu: true,
        width: 24,
        sortable: false,
    },
    {
        field: 'internalTitle',
        headerName: 'Formular',
        flex: 2,
        renderCell: (params) => {
            const {
                isDrafted,
                isPublished,
                isRevoked,
            } = getFormStatus(params.row);

            return (
                <Box
                    sx={{
                        py: 2,
                    }}
                >
                    <Typography
                        variant="h5"
                        sx={{
                            mb: 0.5,
                            fontSize: '1rem',
                        }}
                    >
                        <Link
                            style={{
                                color: 'inherit',
                                textDecoration: 'none',
                            }}
                            to={`/forms/${params.row.id}/${params.row.draftedVersion ?? params.row.publishedVersion ?? ''}`}
                            title="Formular bearbeiten"
                        >
                            {params.row.internalTitle}
                        </Link>
                    </Typography>

                    <Typography
                        variant="body2"
                        sx={{
                            mt: -0.75,
                            fontSize: '0.875rem',
                            lineHeight: '1.5rem',
                        }}
                        color="textSecondary"
                    >
                        {
                            isPublished ?
                                <span>Veröffentlicht: Version {params.row.publishedVersion}</span> :
                                <span>{isRevoked ? 'Zurückgezogen' : 'Noch nicht veröffentlicht'}</span>
                        }
                        {
                            isDrafted &&
                            <span> &bull; In Bearbeitung: Version {params.row.draftedVersion}</span>
                        }
                    </Typography>

                    <Typography
                        variant="body2"
                        sx={{
                            mt: -0.75,
                            fontSize: '0.875rem',
                            lineHeight: '1.5rem',
                        }}
                        color="textSecondary"
                    >
                        Entwickelt von: {params.row.developingDepartmentName ?? 'Unbekannt'}
                    </Typography>
                </Box>
            );
        },
    },
    {
        field: 'updated',
        headerName: 'Zuletzt bearbeitet',
        flex: 1,
        renderCell: (params) => (
            <Box
                sx={{
                    py: 2,
                    display: 'flex',
                    flexDirection: 'column',
                }}
            >
                <Typography sx={{fontSize: '0.875rem'}}>
                    {format(params.row.updated, 'dd.MM.yyyy — HH:mm')} Uhr
                </Typography>
                <Typography
                    color="textSecondary"
                    sx={{fontSize: '0.875rem'}}
                >
                    {params.row.lastEditorName ?? 'Unbekannte Nutzer:in'}
                </Typography>
            </Box>
        ),
    },
    {
        field: 'publishedVersion',
        headerName: 'Status',
        flex: 0.75,
        sortable: false,
        renderCell: (params) => (
            <FormStatusChipGroup form={params.row}/>
        ),
    },
];

export function FormsListPage() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const api = useApi();
    const showConfirm = useConfirm();

    const user = useAppSelector(selectUser);
    const memberships = useAppSelector(selectMemberships);

    const listControlRef = useRef<ListControlRef>(null);

    const [newForm, setNewForm] = useState<{
        form: FormEntity;
        version: FormVersionEntity;
    } | undefined>(undefined);

    const [showImportFormDialog, setShowImportFormDialog] = useState(false);
    const [showExportFormDialog, setShowExportFormDialog] = useState(false);

    const [showFormVersionsDialogFor, setShowFormVersionsDialogFor] = useState<FormEntity | undefined>();

    const [formToManageAccess, setFormToManageAccess] = useState<FormEntity>();
    const [formToMove, setFormToMove] = useState<FormEntity>();
    const [formToDelete, setFormToDelete] = useState<FormEntity>();

    const [rowMenu, setRowMenu] = useState<{
        target: HTMLElement;
        form: FormEntity;
    } | undefined>(undefined);

    const handleNewFormDraft = (formId: number, formVersion: number | undefined | null) => {
        dispatch(showLoadingOverlay('Neuer Entwurf wird erstellt…'));

        let prom: Promise<FormVersionEntity>;
        if (formVersion == null) {
            prom = new FormVersionApiService()
                .latestAsNewVersion(formId);
        } else {
            prom = new FormVersionApiService()
                .versionAsNewVersion({
                    formId: formId,
                    version: formVersion,
                });
        }

        prom
            .then((createdDraft) => {
                navigate(`/forms/${createdDraft.formId}/${createdDraft.version}`);
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Erstellen eines neuen Entwurfs'));
            })
            .finally(() => {
                dispatch(hideLoadingOverlay());
            });
    };

    const handleFormClone = async (form: FormEntity, _version?: FormVersionEntity) => {
        const version = _version ?? await new FormVersionApiService()
            .retrieve({
                formId: form.id,
                version: 'latest',
            });

        setNewForm({
            form: {
                ...form,
                slug: '',
            },
            version: version,
        });

        setRowMenu(undefined);
    };

    const handleDeleteForm = async (form: FormEntity) => {
        dispatch(setLoadingMessage({
            message: 'Formular wird gelöscht',
            blocking: true,
            estimatedTime: 500,
        }));

        new FormApiService()
            .destroy(form.id)
            .then(() => {
                if (listControlRef.current != null) {
                    listControlRef.current.refresh();
                }
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Löschen des Formulars'));
            })
            .finally(() => {
                dispatch(hideLoadingOverlay());
            });
    };

    const handleExportForm = (formId: number, version?: number) => {
        new FormApiService()
            .export(formId, version)
            .then(downloadFormExportFile);
    };

    const handleNewDraft = (item: FormEntity) => {
        showConfirm({
            title: 'Neuen Entwurf anlegen?',
            confirmButtonText: 'Ja, Entwurf anlegen',
            children: (
                <Box>
                    Für dieses Formular existiert derzeit kein aktiver Entwurf.
                    Möchten Sie einen neuen Entwurf (Arbeitsversion) für dieses Formular anlegen um diesen zu
                    bearbeiten?
                </Box>
            ),
        }).then((confirmed) => {
            if (confirmed) {
                handleNewFormDraft(item.id, item.publishedVersion);
            }
        });
    };

    return (
        <>
            <PageWrapper
                title="Formulare"
                fullWidth
                background
            >
                <GenericListPage<FormListEntry>
                    controlRef={listControlRef}
                    dynamicRowHeight={true}
                    filters={availableFilter}
                    defaultFilter="all"
                    header={{
                        icon: <DescriptionOutlinedIcon/>,
                        title: 'Formulare',
                        actions: [
                            {
                                icon: <CloudUploadOutlinedIcon/>,
                                onClick: () => {
                                    setShowImportFormDialog(true);
                                },
                                variant: 'text',
                                label: 'Importieren',
                            },
                            {
                                label: 'Neues Formular',
                                icon: <AddOutlinedIcon/>,
                                onClick: () => {
                                    setNewForm({
                                        form: FormApiService.initialize(),
                                        version: FormVersionApiService.initialize(),
                                    });
                                },
                                variant: 'contained',
                            },
                        ],
                        helpDialog: {
                            title: 'Hilfe zu Formularen',
                            tooltip: 'Hilfe anzeigen',
                            content: <FormsListPageHelp/>,
                        },
                    }}
                    searchLabel="Formular suchen"
                    searchPlaceholder="Titel des Formulars eingeben…"
                    fetch={async (options) => {
                        const deps = (await new DepartmentApiService().listAll()).content;

                        const formsPage = await new FormApiService()
                            .list(options.page, options.size, options.sort as any, options.order, {
                                internalTitle: options.search,
                                isPublished: options.filter === 'published',
                                isDrafted: options.filter === 'drafted',
                                isRevoked: options.filter === 'revoked',
                            });

                        const formIds = formsPage.content.map(form => form.id);

                        const editorsList = await new FormApiService()
                            .listEditorsForForms(formIds);

                        const extendedFormsPage: Page<FormListEntry> = {
                            ...formsPage,
                            content: formsPage.content.map(form => ({
                                ...form,
                                developingDepartmentName: deps.find(dep => dep.id === form.developingDepartmentId)?.name,
                                lastEditorName: editorsList.find(editor => editor.formId === form.id)?.fullName,
                            })),
                        };

                        return extendedFormsPage;
                    }}
                    columnDefinitions={columns}
                    getRowIdentifier={row => row.id.toString()}
                    noDataPlaceholder={
                        <Box
                            sx={{
                                display: 'flex',
                                flexDirection: 'column',
                                alignItems: 'center',
                                textAlign: 'center',
                                p: 4,
                            }}
                        >
                            {
                                (memberships == null ||
                                    memberships.length === 0) &&
                                <>
                                    <Typography
                                        variant="h5"
                                        component="h2"
                                    >
                                        Noch keiner Organisationseinheit zugeordnet
                                    </Typography>
                                    <Typography>
                                        Eine Administrator:in muss Sie einer Organisationseinheit zuordnen und Ihnen
                                        eine Domänenrolle zuweisen.
                                        Erst dann können Sie mit der Entwicklung von Formularen beginnen. Nach der
                                        Zuweisung müssen Sie diese Seite ggf. einmal neu laden.
                                    </Typography>
                                </>
                            }
                            {
                                memberships != null &&
                                memberships.length > 0 &&
                                <Typography>
                                    Sie haben aktuell keine Formulare. Starten Sie jetzt mit Ihrem ersten Formular!
                                </Typography>
                            }
                        </Box>
                    }
                    noSearchResultsPlaceholder="Keine Formulare gefunden"
                    rowActionsCount={4}
                    rowActions={(item: FormListEntry) => [
                        {
                            icon: <Edit/>,
                            to: `/forms/${item.id}/${item.draftedVersion}`,
                            tooltip: 'Formular bearbeiten',
                            visible: item.draftedVersion != null,
                        },
                        {
                            icon: <Visibility/>,
                            to: `/forms/${item.id}`,
                            tooltip: 'Formular ansehen',
                            visible: item.draftedVersion === null,
                        },
                        {
                            icon: <NewWindow/>,
                            onClick: () => handleNewDraft(item),
                            tooltip: 'Neuen Entwurf anlegen',
                            visible: item.draftedVersion == null,
                            disabled: item.publishedVersion == null && item.draftedVersion != null,
                        },
                        {
                            icon: <HomeStorage/>,
                            onClick: () => setShowFormVersionsDialogFor(item),
                            tooltip: 'Versionen anzeigen',
                        },
                        {
                            icon: <MoreVertOutlinedIcon/>,
                            onClick: (event) => setRowMenu({
                                target: event.currentTarget as HTMLElement,
                                form: item,
                            }),
                            tooltip: 'Optionen',
                        },
                    ]}
                    defaultSortField="internalTitle"
                    disableFullWidthToggle={true}
                />
            </PageWrapper>

            {
                rowMenu != null &&
                rowMenu.target != null &&
                rowMenu.form != null &&
                <FormsListRowMenu
                    anchorEl={rowMenu.target}
                    onClose={() => {
                        setRowMenu(undefined);
                    }}
                    form={rowMenu.form}
                    onManageAccess={setFormToManageAccess}
                    onMoveFormToDepartment={setFormToMove}
                    onDeleteForm={setFormToDelete}
                />
            }

            {
                newForm != null &&
                <AddFormDialog
                    onClose={() => {
                        setNewForm(undefined);
                    }}
                    onSave={(createdForm, createdVersion) => {
                        navigate(`/forms/${createdForm.id}/${createdVersion.version}`);
                    }}
                    open={true}
                    basis={newForm}
                />
            }

            <ExportFormDialog
                open={showExportFormDialog}
                onCancel={() => {
                    setShowExportFormDialog(false);
                    setRowMenu(undefined);
                }}
                onExport={() => {
                    if (rowMenu?.form != null) {
                        handleExportForm(rowMenu.form.id, undefined);
                    }
                    setShowExportFormDialog(false);
                    setRowMenu(undefined);
                }}
            />

            <ImportFormDialog
                open={showImportFormDialog}
                onClose={() => {
                    setShowImportFormDialog(false);
                }}
                onImport={(form, version) => {
                    setNewForm({
                        form,
                        version,
                    });
                    setShowImportFormDialog(false);
                }}
            />

            {
                showFormVersionsDialogFor != null &&
                <FormVersionsDialog
                    form={showFormVersionsDialogFor}
                    onClose={() => {
                        setShowFormVersionsDialogFor(undefined);
                    }}
                    onNewDraft={({
                                     form,
                                     version,
                                 }) => {
                        // TODO
                    }}
                    onNewForm={({
                                    form,
                                    version,
                                }) => {
                        setNewForm({
                            form,
                            version,
                        });
                    }}
                    onShouldReload={() => {
                        if (listControlRef.current?.refresh != null) {
                            listControlRef.current.refresh();
                        }
                    }}
                />
            }

            {
                formToMove != null &&
                <MoveFormToDepartmentDialog
                    formId={formToMove.id}
                    onClose={() => {
                        setFormToMove(undefined);
                    }}
                    onMoved={() => {
                        setFormToMove(undefined);
                        if (listControlRef.current != null) {
                            listControlRef.current.refresh();
                        }
                    }}
                />
            }

            <DeleteFormDialog
                form={formToDelete}
                onDelete={(form) => {
                    handleDeleteForm(form);
                    setFormToDelete(undefined);
                }}
                onCancel={() => {
                    setFormToDelete(undefined);
                }}
            />

            <FormResourceAccessControlDialog
                open={formToManageAccess != null}
                formId={formToManageAccess?.id}
                onClose={() => {
                    setFormToManageAccess(undefined);
                }}
            />
        </>
    );
}
