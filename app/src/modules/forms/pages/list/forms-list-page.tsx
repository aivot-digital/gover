import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Box, Stack} from '@mui/material';
import React, {useCallback, useMemo, useRef, useState} from 'react';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import {showErrorSnackbar} from '../../../../slices/snackbar-slice';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectMemberships, selectUser} from '../../../../slices/user-slice';
import {AddFormDialog} from '../../dialogs/add-form-dialog';
import {downloadFormExportFile} from '../../../../utils/download-utils';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {FormVersionsDialog} from '../../dialogs/form-versions-dialog';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import Typography from '@mui/material/Typography';
import {GridColDef} from '@mui/x-data-grid';
import {hideLoadingOverlay, showLoadingOverlay} from '../../../../slices/loading-overlay-slice';
import {useNavigate} from 'react-router-dom';
import {FormsListPageHelp} from '../../components/forms-list-page-help';
import {useConfirm} from '../../../../providers/confirm-provider';
import {FormsListRowMenu} from '../../components/forms-list-row-menu';
import {setLoadingMessage} from '../../../../slices/shell-slice';
import {MoveFormToDepartmentDialog} from '../../dialogs/move-form-to-department-dialog';
import {ListControlRef} from '../../../../components/generic-list/generic-list-props';
import Edit from '@aivot/mui-material-symbols-400-outlined/dist/edit/Edit';
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
import {GenericPageHeaderProps} from '../../../../components/generic-page-header/generic-page-header-props';
import {FormTriggerApiService, FormTriggerListItem} from '../../services/form-trigger-api-service';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {ProcessStatusChip} from '../../../process/components/process-status/process-status-chip';
import {Action} from '../../../../components/actions/actions-props';
import {ProcessStatus} from '../../../process/enums/process-status';
import ArrowForward from '@aivot/mui-material-symbols-400-outlined/dist/arrow-forward/ArrowForward';

const columns: GridColDef<FormTriggerListItem>[] = [
    {
        field: 'icon',
        headerName: '',
        renderCell: () => (
            <CellContentWrapper
                sx={{
                    alignItems: 'start',
                    py: 2,
                }}
            >
                <DescriptionOutlinedIcon/>
            </CellContentWrapper>),
        disableColumnMenu: true,
        width: 24,
        sortable: false,
    },
    {
        field: 'node.name',
        headerName: 'Formulareingang',
        flex: 1,
        valueGetter: (_, row) => {
            return row.node.name ?? 'Formulareingang';
        },
        renderCell: (params) => (
            <CellLink to={`/form-triggers/${params.row.node.id}/formLayout/0`}>
                {params.row.node.name ?? 'Formulareingang'}
            </CellLink>
        ),
    },
    {
        field: 'process.internalTitle',
        headerName: 'Prozess',
        flex: 1,
        valueGetter: (_, row) => {
            return row.process.internalTitle;
        },
        renderCell: (params) => (
            <Stack direction="row"
                   alignItems="center"
                   height="100%">
                <CellLink to={`/processes/${params.row.process.id}/versions/${params.row.version.processVersion}`}>
                    {params.row.process.internalTitle} (Version {params.row.version.processVersion})
                </CellLink>

                <ProcessStatusChip
                    sx={{
                        ml: 1,
                    }}
                    status={params.row.version.status}
                />
            </Stack>
        ),
    },
];

export function FormsListPage() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
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

    const header: GenericPageHeaderProps = useMemo(() => ({
        icon: <DescriptionOutlinedIcon/>,
        title: 'Formulare',
        helpDialog: {
            title: 'Hilfe zu Formularen',
            tooltip: 'Hilfe anzeigen',
            content: <FormsListPageHelp/>,
        },
    }), []);

    const fetch = useCallback(async (options: any) => {
        const deps = (await new DepartmentApiService().listAll()).content;

        const formsPage = await new FormTriggerApiService()
            .list(options.page, options.size, options.sort as any, options.order, {
                name: options.search,
            });

        return formsPage;
        /*
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

         */
    }, []);

    const noDataPlaceholder = useMemo(() => (
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
                (memberships == null || memberships.length === 0) &&
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
    ), [memberships]);

    const rowActions = useCallback((item: FormTriggerListItem): Action[] => [
        {
            icon: <Edit/>,
            to: `/form-triggers/${item.node.id}/formLayout/0`,
            tooltip: 'Formular bearbeiten',
            visible: item.version.status === ProcessStatus.Drafted,
        },
        {
            icon: <ArrowForward/>,
            to: `/form-triggers/${item.node.id}/formLayout/0`,
            tooltip: 'Formular ansehen',
            visible: item.version.status !== ProcessStatus.Drafted,
        },
        {
            icon: ModuleIcons.processes,
            to: `/processes/${item.process.id}/versions/${item.version.processVersion}`,
            tooltip: 'Prozess ansehen',
        },
    ], []);

    return (
        <>
            <PageWrapper
                title="Formulare"
                fullWidth
                background
            >
                <GenericListPage<FormTriggerListItem>
                    controlRef={listControlRef}
                    dynamicRowHeight={true}
                    defaultFilter="all"
                    header={header}
                    searchLabel="Formulareingang suchen"
                    searchPlaceholder="Titel des Formulareingangs eingeben…"
                    fetch={fetch}
                    columnDefinitions={columns}
                    getRowIdentifier={getRowIdentifier}
                    noDataPlaceholder={noDataPlaceholder}
                    noSearchResultsPlaceholder="Keine Formulareingänge gefunden"
                    rowActionsCount={2}
                    rowActions={rowActions}
                    defaultSortField={'id' as any}
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

function getRowIdentifier(row: FormTriggerListItem): string {
    return row.node.id.toString();
}