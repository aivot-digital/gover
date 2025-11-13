import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Box} from '@mui/material';
import {FormListResponseDTO} from '../../dtos/form-list-response-dto';
import {FormsApiService} from '../../forms-api-service';
import {FormsApiService as FormsApiServiceV2} from '../../forms-api-service-v2';
import MoreVertOutlinedIcon from '@mui/icons-material/MoreVertOutlined';
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined';
import React, {useRef, useState} from 'react';
import {ImportApplicationDialog} from '../../../../dialogs/application-dialogs/import-application-dialog/import-application-dialog';
import {FormDetailsResponseDTO} from '../../dtos/form-details-response-dto';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import {showErrorSnackbar} from '../../../../slices/snackbar-slice';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectMemberships, selectUser} from '../../../../slices/user-slice';
import {AddFormDialog} from '../../dialogs/add-form-dialog';
import {ExportApplicationDialog} from '../../../../dialogs/application-dialogs/export-application-dialog/export-application-dialog';
import {downloadConfigFile} from '../../../../utils/download-utils';
import {useApi} from '../../../../hooks/use-api';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {FormVersionsDialog} from '../../dialogs/form-versions-dialog';
import {DepartmentResponseDTO} from '../../../departments/dtos/department-response-dto';
import {DepartmentsApiService} from '../../../departments/departments-api-service';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import Typography from '@mui/material/Typography';
import {format} from 'date-fns/format';
import {GridColDef} from '@mui/x-data-grid';
import {hideLoadingOverlay, showLoadingOverlay} from '../../../../slices/loading-overlay-slice';
import {Link, useNavigate} from 'react-router-dom';
import {DeleteApplicationDialog} from '../../../../dialogs/application-dialogs/delete-application-dialog/delete-application-dialog';
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

interface FormListEntry extends FormListResponseDTO {
    developingDepartmentName?: string;
    lastEditorName?: string;
}

const columns: GridColDef<FormListEntry>[] = [
    {
        field: 'icon',
        headerName: '',
        renderCell: () => <CellContentWrapper sx={{alignItems: 'start', py: 2}}><DescriptionOutlinedIcon /></CellContentWrapper>,
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
            <FormStatusChipGroup form={params.row} />
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

    const [newForm, setNewForm] = useState<FormDetailsResponseDTO | undefined>(undefined);
    const [showImportFormDialog, setShowImportFormDialog] = useState(false);
    const [showExportFormDialog, setShowExportFormDialog] = useState(false);
    const [showFormVersionsDialogFor, setShowFormVersionsDialogFor] = useState<FormListResponseDTO | undefined>();

    const [formToMove, setFormToMove] = useState<FormListResponseDTO>();
    const [formToDelete, setFormToDelete] = useState<FormListResponseDTO>();

    const [rowMenu, setRowMenu] = useState<{
        target: HTMLElement;
        form: FormListResponseDTO;
    } | undefined>(undefined);

    const handleNewFormDraft = (formId: number, formVersion: number | undefined | null) => {
        dispatch(showLoadingOverlay('Neuer Entwurf wird erstellt…'));

        let prom: Promise<FormDetailsResponseDTO>;
        if (formVersion == null) {
            prom = new FormsApiService(api)
                .latestAsNewVersion(formId);
        } else {
            prom = new FormsApiService(api)
                .versionAsNewVersion({
                    id: formId,
                    version: formVersion,
                });
        }

        prom
            .then((createdDraft) => {
                navigate(`/forms/${formId}/${createdDraft.draftedVersion}`);
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Erstellen eines neuen Entwurfs'));
            })
            .finally(() => {
                dispatch(hideLoadingOverlay());
            });
    };

    const handleFormClone = async (form: FormDetailsResponseDTO) => {
        const formToClone: FormDetailsResponseDTO = JSON.parse(JSON.stringify(form));
        formToClone.slug = '';
        formToClone.version = 0;
        setNewForm(formToClone);
        setRowMenu(undefined);
    };

    const handleDeleteForm = async (form: FormListResponseDTO) => {
        dispatch(setLoadingMessage({
            message: 'Formular wird gelöscht',
            blocking: true,
            estimatedTime: 500,
        }));

        new FormsApiServiceV2()
            .destroyAll(form.id)
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

    const handleExportForm = (form: FormListResponseDTO) => {
        new FormsApiService(api)
            .retrieve({
                id: form.id,
                version: form.draftedVersion ?? form.publishedVersion!,
            })
            .then((fullForm) => {
                downloadConfigFile(fullForm);
            });
    };

    const handleNewDraft = (item: FormListResponseDTO) => {
        showConfirm({
            title: 'Neuen Entwurf anlegen?',
            confirmButtonText: 'Ja, Entwurf anlegen',
            children: (
                <Box>
                    Für dieses Formular existiert derzeit kein aktiver Entwurf.
                    Möchten Sie einen neuen Entwurf (Arbeitsversion) für dieses Formular anlegen um diesen zu bearbeiten?
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
                        icon: <DescriptionOutlinedIcon />,
                        title: 'Formulare',
                        actions: [
                            {
                                icon: <CloudUploadOutlinedIcon />,
                                onClick: () => {
                                    setShowImportFormDialog(true);
                                },
                                variant: 'text',
                                label: 'Importieren',
                            },
                            {
                                label: 'Neues Formular',
                                icon: <AddOutlinedIcon />,
                                onClick: () => {
                                    setNewForm(FormsApiService.initialize());
                                },
                                variant: 'contained',
                            },
                        ],
                        helpDialog: {
                            title: 'Hilfe zu Formularen',
                            tooltip: 'Hilfe anzeigen',
                            content: <FormsListPageHelp />,
                        },
                    }}
                    searchLabel="Formular suchen"
                    searchPlaceholder="Titel des Formulars eingeben…"
                    fetch={async (options) => {
                        const deps = (await new DepartmentsApiService().listAll()).content;

                        const formsPage = await new FormsApiServiceV2()
                            .list(options.page, options.size, options.sort as any, options.order, {
                                internalTitle: options.search,
                                isDeveloper: true,
                                userId: user?.id,
                                isPublished: options.filter === 'published',
                                isDrafted: options.filter === 'drafted',
                                isRevoked: options.filter === 'revoked',
                            });

                        const formIds = formsPage.content.map(form => form.id);

                        const editorsList = await new FormsApiServiceV2()
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
                                        Noch keinem Fachbereich zugeordnet
                                    </Typography>
                                    <Typography>
                                        Eine Administrator:in muss Sie noch einem Fachbereich zuordnen und Ihnen eine Rolle
                                        zuweisen.
                                        Erst dann können Sie mit der Entwicklung von Formularen loslegen.
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
                            icon: <Edit />,
                            to: `/forms/${item.id}/${item.draftedVersion}`,
                            tooltip: 'Formular bearbeiten',
                            visible: item.draftedVersion != null,
                        },
                        {
                            icon: <Visibility />,
                            to: `/forms/${item.id}`,
                            tooltip: 'Formular ansehen',
                            visible: item.draftedVersion === null,
                        },
                        {
                            icon: <NewWindow />,
                            onClick: () => handleNewDraft(item),
                            tooltip: 'Neuen Entwurf anlegen',
                            visible: item.draftedVersion == null,
                            disabled: item.publishedVersion == null && item.draftedVersion != null,
                        },
                        {
                            icon: <HomeStorage />,
                            onClick: () => setShowFormVersionsDialogFor(item),
                            tooltip: 'Versionen anzeigen',
                        },
                        {
                            icon: <MoreVertOutlinedIcon />,
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
                    onSave={(created) => {
                        navigate(`/forms/${created.id}/${created.draftedVersion}`);
                    }}
                    open={true}
                    basis={newForm}
                />
            }

            <ExportApplicationDialog
                open={showExportFormDialog}
                onCancel={() => {
                    setShowExportFormDialog(false);
                    setRowMenu(undefined);
                }}
                onExport={() => {
                    if (rowMenu?.form != null) {
                        handleExportForm(rowMenu.form);
                    }
                    setShowExportFormDialog(false);
                    setRowMenu(undefined);
                }}
            />

            <ImportApplicationDialog
                open={showImportFormDialog}
                onClose={() => {
                    setShowImportFormDialog(false);
                }}
                onImport={(formToImport) => {
                    setNewForm(formToImport);
                    setShowImportFormDialog(false);
                }}
            />

            {
                showFormVersionsDialogFor != null &&
                <FormVersionsDialog
                    formId={showFormVersionsDialogFor.id}
                    onClose={() => {
                        setShowFormVersionsDialogFor(undefined);
                    }}
                    onNewDraft={(basis) => {
                        handleNewFormDraft(basis.id, basis.version);
                    }}
                    onNewForm={handleFormClone}
                    onChange={() => {
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

            <DeleteApplicationDialog
                form={formToDelete}
                onDelete={(form) => {
                    handleDeleteForm(form);
                    setFormToDelete(undefined);
                }}
                onCancel={() => {
                    setFormToDelete(undefined);
                }}
            />
        </>
    );
}

