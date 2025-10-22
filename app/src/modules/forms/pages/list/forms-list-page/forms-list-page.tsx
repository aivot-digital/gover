import {GenericListPage} from '../../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Box, ListItemIcon, ListItemText, Menu, MenuItem} from '@mui/material';
import {EditOutlined} from '@mui/icons-material';
import {FormListResponseDTO} from '../../../dtos/form-list-response-dto';
import {FormsApiService} from '../../../forms-api-service';
import MoreVertOutlinedIcon from '@mui/icons-material/MoreVertOutlined';
import OpenInNewOutlinedIcon from '@mui/icons-material/OpenInNewOutlined';
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined';
import React, {useEffect, useMemo, useState} from 'react';
import {ImportApplicationDialog} from '../../../../../dialogs/application-dialogs/import-application-dialog/import-application-dialog';
import {FormDetailsResponseDTO} from '../../../dtos/form-details-response-dto';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import FileCopyOutlinedIcon from '@mui/icons-material/FileCopyOutlined';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../../slices/snackbar-slice';
import ContentPasteOutlinedIcon from '@mui/icons-material/ContentPasteOutlined';
import QrCode2OutlinedIcon from '@mui/icons-material/QrCode2Outlined';
import ImportExportOutlinedIcon from '@mui/icons-material/ImportExportOutlined';
import {createCustomerPath} from '../../../../../utils/url-path-utils';
import {downloadQrCode} from '../../../../../utils/download-qrcode';
import {useAppSelector} from '../../../../../hooks/use-app-selector';
import {selectUser} from '../../../../../slices/user-slice';
import {AddFormDialog} from '../../../dialogs/add-form-dialog';
import {ExportApplicationDialog} from '../../../../../dialogs/application-dialogs/export-application-dialog/export-application-dialog';
import {downloadConfigFile} from '../../../../../utils/download-utils';
import {useApi} from '../../../../../hooks/use-api';
import {useAppDispatch} from '../../../../../hooks/use-app-dispatch';
import {FormVersionsDialog} from '../../../dialogs/form-versions-dialog';
import {DepartmentResponseDTO} from '../../../../departments/dtos/department-response-dto';
import {DepartmentsApiService} from '../../../../departments/departments-api-service';
import {CellContentWrapper} from '../../../../../components/cell-content-wrapper/cell-content-wrapper';
import Typography from '@mui/material/Typography';
import {format} from 'date-fns/format';
import {GridColDef} from '@mui/x-data-grid';
import {FormStatus} from '../../../enums/form-status';
import UploadIcon from '@mui/icons-material/Upload';
import {hideLoadingOverlay, showLoadingOverlay} from '../../../../../slices/loading-overlay-slice';
import {Link, useNavigate} from 'react-router-dom';
import HistoryIcon from '@mui/icons-material/History';
import {FormStatusChip} from '../../../components/form-status-chip';
import {DeleteApplicationDialog} from '../../../../../dialogs/application-dialogs/delete-application-dialog/delete-application-dialog';
import {FormsListPageHelp} from './components/forms-list-page-help';
import {FormStatusChipGroup, getFormStatus} from '../../../components/form-status-chip-group';
import HomeStorage from '@aivot/mui-material-symbols-400-outlined/dist/home-storage/HomeStorage';
import {useConfirm} from '../../../../../providers/confirm-provider';
import NewWindow from '@aivot/mui-material-symbols-400-outlined/dist/new-window/NewWindow';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';

export function FormsListPage() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const api = useApi();
    const showConfirm = useConfirm();

    const user = useAppSelector(selectUser);

    const [newForm, setNewForm] = useState<FormDetailsResponseDTO | undefined>(undefined);
    const [showImportFormDialog, setShowImportFormDialog] = useState(false);
    const [showExportFormDialog, setShowExportFormDialog] = useState(false);
    const [showFormVersionsDialogFor, setShowFormVersionsDialogFor] = useState<FormListResponseDTO | undefined>();

    const [departments, setDepartments] = useState<DepartmentResponseDTO[]>([]);

    const [formToDelete, setFormToDelete] = useState<FormListResponseDTO>();

    const [rowMenu, setRowMenu] = useState<{
        target: HTMLElement;
        form: FormListResponseDTO;
    } | undefined>(undefined);

    const columns: GridColDef[] = useMemo(() => {
        return [
            {
                field: 'icon',
                headerName: '',
                renderCell: () => <CellContentWrapper><DescriptionOutlinedIcon /></CellContentWrapper>,
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
                                sx={{mb: 0.5}}
                            >
                                <Link style={{color: 'inherit', textDecoration: 'none'}} to={`/forms/${params.row.id}/${params.row.draftedVersion ?? params.row.publishedVersion ?? ''}`} title={"Formular bearbeiten"}>
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
                                color={'text.secondary'}
                            >
                                {
                                    isPublished ?
                                        <span>Veröffentlicht: Version {params.row.publishedVersion}</span> :
                                        <span>Noch nicht veröffentlicht</span>
                                }
                                {
                                    isDrafted &&
                                    <span> &bull; In Bearbeitung: Version {params.row.draftedVersion}</span>
                                }
                                {
                                    isRevoked &&
                                    <span> &bull; Zurückgezogen</span>
                                }
                            </Typography>

                            <Typography
                                variant="body2"
                                sx={{
                                    mt: -0.75,
                                    fontSize: '0.875rem',
                                    lineHeight: '1.5rem',
                                }}
                                color={'text.secondary'}
                            >
                                Entwickelt von: {departments.find(dep => dep.id === params.row.developingDepartmentId)?.name}
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
                        }}
                    >
                        {format(params.row.updated, 'dd.MM.yyyy — HH:mm')} Uhr
                    </Box>
                ),
            },
            {
                field: 'publishedVersion',
                headerName: 'Status',
                flex: 0.75,
                renderCell: (params) => (
                    <FormStatusChipGroup form={params.row} />
                ),
            },
        ];
    }, [departments]);

    useEffect(() => {
        new DepartmentsApiService()
            .listAll()
            .then((response) => {
                setDepartments(response.content);
            });
    }, [api]);

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

    const handleFormClone = async () => {
        if (rowMenu == null) {
            return;
        }

        let originalForm: FormDetailsResponseDTO;
        try {
            originalForm = await new FormsApiService(api)
                .retrieve({
                    id: rowMenu.form.id,
                    version: rowMenu.form.draftedVersion ?? rowMenu.form.publishedVersion!,
                });
        } catch (err) {
            console.error(err);
            dispatch(showErrorSnackbar('Formular konnte nicht dupliziert werden'));
            return;
        }

        const formToClone: FormDetailsResponseDTO = JSON.parse(JSON.stringify(originalForm));
        formToClone.slug = '';
        formToClone.version = 0;
        setNewForm(formToClone);

        setRowMenu(undefined);
    };

    const handleDeleteForm = async (formId: number) => {
        dispatch(showLoadingOverlay('Lösche Formular'));

        new FormsApiService(api)
            .destroyAll(formId)
            .then(() => {
                window.location.reload();
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Löschen des Formulars'));
            })
            .finally(() => {
                dispatch(hideLoadingOverlay());
            });
    };

    const handleFormLinkCopy = async () => {
        if (rowMenu == null) {
            return;
        }

        try {
            await navigator
                .clipboard
                .writeText(createCustomerPath(rowMenu.form.slug));
            dispatch(showSuccessSnackbar('Formularlink in Zwischenablage kopiert'));
        } catch (err) {
            console.error(err);
            dispatch(showSuccessSnackbar('Formularlink konnte nicht kopiert werden'));
        }

        setRowMenu(undefined);
    };

    const handleDownloadQrCode = async () => {
        if (rowMenu == null) {
            return;
        }

        try {
            await downloadQrCode(
                createCustomerPath(rowMenu.form.slug),
                `qr-code-${rowMenu?.form.slug}.png`,
            );
            dispatch(showSuccessSnackbar('QR-Code wurde als PNG heruntergeladen!'));
        } catch {
            dispatch(showErrorSnackbar('Fehler beim Herunterladen des QR-Codes!'));
        }
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

    return (
        <>
            <PageWrapper
                title="Formulare"
                fullWidth
                background
            >
                <GenericListPage<FormListResponseDTO>
                    dynamicRowHeight={true}
                    filters={[
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
                    ]}
                    defaultFilter="all"
                    header={{
                        icon: <DescriptionOutlinedIcon />,
                        title: 'Formulare',
                        actions: [
                            {
                                icon: <CloudUploadOutlinedIcon />,
                                tooltip: 'Formular importieren',
                                onClick: () => {
                                    setShowImportFormDialog(true);
                                },
                                variant: 'outlined',
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
                    fetch={(options) => {
                        return new FormsApiService(options.api)
                            .list(options.page, options.size, options.sort, options.order, {
                                internalTitle: options.search,
                                isDeveloper: true,
                                userId: user?.id,
                                isPublished: options.filter === 'published',
                                isDrafted: options.filter === 'drafted',
                                isRevoked: options.filter === 'revoked',
                            });
                    }}
                    columnDefinitions={columns}
                    getRowIdentifier={row => row.id.toString()}
                    noDataPlaceholder="Keine Formulare vorhanden"
                    noSearchResultsPlaceholder="Keine Formulare gefunden"
                    rowActionsCount={4}
                    rowActions={(item: FormListResponseDTO) => [
                        {
                            icon: <EditOutlined />,
                            to: `/forms/${item.id}/${item.draftedVersion}`,
                            tooltip: 'Formular bearbeiten',
                            visible: item.draftedVersion != null,
                        },
                        {
                            icon: <NewWindow />,
                            onClick: async (): Promise<void> => {
                                const confirmed = await showConfirm({
                                    title: 'Neue Arbeitsversion anlegen?',
                                    confirmButtonText: 'Ja, Arbeitsversion anlegen',
                                    children: (
                                        <>
                                            <Box>
                                                Für dieses Formular existiert derzeit keine aktive Arbeitsversion. Möchten Sie eine neue Arbeitsversion für dieses Formular anlegen um diese zu bearbeiten?
                                            </Box>
                                        </>
                                    ),
                                });
                                if(confirmed){
                                    handleNewFormDraft(item.id, item.publishedVersion);
                                }
                            },
                            tooltip: 'Neue Arbeitsversion anlegen',
                            visible: item.draftedVersion == null,
                            disabled: item.publishedVersion == null && item.draftedVersion != null,
                        },
                        {
                            icon: <HomeStorage />,
                            onClick: () => {
                                setShowFormVersionsDialogFor(item);
                            },
                            tooltip: 'Versionen anzeigen',
                        },
                        {
                            icon: <OpenInNewOutlinedIcon />,
                            href: `/${item.slug}`,
                            tooltip: 'Veröffentlichtes Formular öffnen (neuer Tab)',
                            target: '_blank',
                            visible: item.publishedVersion != null,
                        },
                        {
                            icon: <MoreVertOutlinedIcon />,
                            onClick: (event) => {
                                setRowMenu({
                                    target: event.currentTarget as HTMLElement,
                                    form: item,
                                });
                            },
                            tooltip: 'Optionen',
                        },
                    ]}
                    defaultSortField="internalTitle"
                    disableFullWidthToggle={true}
                />
            </PageWrapper>

            <Menu
                anchorEl={rowMenu?.target}
                open={rowMenu != null}
                onClose={() => {
                    setRowMenu(undefined);
                }}
            >
                <MenuItem
                    onClick={handleFormClone}
                    disabled={(
                        rowMenu?.form.draftedVersion == null &&
                        rowMenu?.form.publishedVersion == null
                    )}
                >
                    <ListItemIcon>
                        <FileCopyOutlinedIcon />
                    </ListItemIcon>
                    <ListItemText>
                        Formular duplizieren
                    </ListItemText>
                </MenuItem>

                <MenuItem
                    onClick={handleFormLinkCopy}
                    disabled={(
                        rowMenu?.form.draftedVersion == null &&
                        rowMenu?.form.publishedVersion == null
                    )}
                >
                    <ListItemIcon>
                        <ContentPasteOutlinedIcon />
                    </ListItemIcon>
                    <ListItemText>
                        Formularlink in Zwischenablage kopieren
                    </ListItemText>
                </MenuItem>

                <MenuItem
                    onClick={handleDownloadQrCode}
                    disabled={(
                        rowMenu?.form.draftedVersion == null &&
                        rowMenu?.form.publishedVersion == null
                    )}
                >
                    <ListItemIcon>
                        <QrCode2OutlinedIcon />
                    </ListItemIcon>
                    <ListItemText>
                        QR-Code mit Formularlink herunterladen
                    </ListItemText>
                </MenuItem>

                <MenuItem
                    onClick={() => {
                        setShowExportFormDialog(true);
                    }}
                    disabled={(
                        rowMenu?.form.draftedVersion == null &&
                        rowMenu?.form.publishedVersion == null
                    )}
                >
                    <ListItemIcon>
                        <ImportExportOutlinedIcon />
                    </ListItemIcon>
                    <ListItemText>
                        Formular exportieren
                    </ListItemText>
                </MenuItem>

                <MenuItem
                    onClick={() => {
                        if (rowMenu?.form != null) {
                            setFormToDelete(rowMenu?.form);
                        }
                        setRowMenu(undefined);
                    }}
                    disabled={rowMenu?.form.publishedVersion != null}
                >
                    <ListItemIcon>
                        <DeleteForeverOutlinedIcon />
                    </ListItemIcon>
                    <ListItemText>
                        Formular löschen
                    </ListItemText>
                </MenuItem>
            </Menu>

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
                />
            }

            <DeleteApplicationDialog
                form={formToDelete}
                onDelete={() => {
                    if (formToDelete == null) {
                        return;
                    }

                    handleDeleteForm(formToDelete.id);
                }}
                onCancel={() => {
                    setFormToDelete(undefined);
                }}
            />
        </>
    );
}

