import {GenericListPage} from '../../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Box, ListItemIcon, ListItemText, Menu, MenuItem} from '@mui/material';
import {EditOutlined} from '@mui/icons-material';
import {FormListResponseDTO} from '../../../dtos/form-list-response-dto';
import {FormsApiService} from '../../../forms-api-service';
import MoreVertOutlinedIcon from '@mui/icons-material/MoreVertOutlined';
import DriveFolderUploadOutlinedIcon from '@mui/icons-material/DriveFolderUploadOutlined';
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
import {FormsListPageHelp} from './forms-list-page-help';
import {useAppDispatch} from '../../../../../hooks/use-app-dispatch';
import {FormVersionsDialog} from '../../../dialogs/form-versions-dialog';
import {DepartmentResponseDTO} from '../../../../departments/dtos/department-response-dto';
import {DepartmentsApiService} from '../../../../departments/departments-api-service';
import {CellContentWrapper} from '../../../../../components/cell-content-wrapper/cell-content-wrapper';
import Typography from '@mui/material/Typography';
import {format} from 'date-fns/format';
import Chip from '@mui/material/Chip';
import {GridColDef} from '@mui/x-data-grid';
import {FormStatus, FormStatusColors, FormStatusIcons} from '../../../enums/form-status';
import UploadIcon from '@mui/icons-material/Upload';
import {hideLoadingOverlay, showLoadingOverlay} from '../../../../../slices/loading-overlay-slice';
import {useNavigate} from 'react-router-dom';
import HistoryIcon from '@mui/icons-material/History';

export function FormsListPage() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const api = useApi();

    const user = useAppSelector(selectUser);

    const [newForm, setNewForm] = useState<FormDetailsResponseDTO | undefined>(undefined);
    const [showImportFormDialog, setShowImportFormDialog] = useState(false);
    const [showExportFormDialog, setShowExportFormDialog] = useState(false);
    const [showFormVersionsDialogFor, setShowFormVersionsDialogFor] = useState<FormListResponseDTO | undefined>();

    const [departments, setDepartments] = useState<DepartmentResponseDTO[]>([]);

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
                renderCell: (params) => (
                    <Box
                        sx={{
                            py: 2,
                        }}
                    >
                        <Typography
                            variant="h5"
                            sx={{mb: 0.5}}
                        >
                            {params.row.internalTitle}
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
                                params.row.publishedVersion != null ?
                                    <span>Veröffentlicht: Version {params.row.publishedVersion}</span> :
                                    <span>Noch nicht veröffentlicht</span>
                            }
                            {
                                params.row.draftedVersion != null &&
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
                            color={'text.secondary'}
                        >
                            Entwickelt durch: {departments.find(dep => dep.id === params.row.developingDepartmentId)?.name}
                        </Typography>
                    </Box>
                ),
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
                )
            },
            {
                field: 'publishedVersion',
                headerName: 'Status',
                renderCell: (params) => (
                    <Box
                        sx={{
                            display: 'flex',
                            flexDirection: 'column',
                            justifyContent: 'center',
                            gap: 2,
                            py: 2,
                        }}
                    >
                        {params.row.draftedVersion != null &&
                            <Chip
                                icon={FormStatusIcons[FormStatus.Drafted]}
                                label="In Bearbeitung"
                                variant="outlined"
                                color={FormStatusColors[FormStatus.Drafted]}
                                size={'small'}
                            />
                        }

                        {params.row.publishedVersion != null &&
                            <Chip
                                icon={FormStatusIcons[FormStatus.Published]}
                                label="Veröffentlicht"
                                variant="outlined"
                                color={FormStatusColors[FormStatus.Published]}
                                size={'small'}
                            />
                        }
                    </Box>
                ),
            },
        ];
    }, [departments]);

    useEffect(() => {
        new DepartmentsApiService(api)
            .listAll()
            .then((response) => {
                setDepartments(response.content);
            });
    }, [api]);

    const handleNewFormDraft = (formId: number, formVersion: number) => {
        dispatch(showLoadingOverlay('Neuer Entwurf wird erstellt…'));
        new FormsApiService(api)
            .newVersion({
                id: formId,
                version: formVersion,
            })
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
                    searchPlaceholder="Title des Formulars eingeben…"
                    fetch={(options) => {
                        return new FormsApiService(options.api)
                            .list(options.page, options.size, options.sort, options.order, {
                                internalTitle: options.search,
                                isDeveloper: true,
                                userId: user?.id,
                            });
                    }}
                    columnDefinitions={columns}
                    getRowIdentifier={row => row.id.toString()}
                    noDataPlaceholder="Keine Formulare angelegt"
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
                            icon: <UploadIcon />,
                            onClick: () => {
                                handleNewFormDraft(item.id, item.publishedVersion);
                            },
                            tooltip: 'Neue Version anlegen',
                            visible: item.draftedVersion == null,
                            disabled: item.publishedVersion == null,
                        },
                        {
                            icon: <HistoryIcon />,
                            onClick: () => {
                                setShowFormVersionsDialogFor(item);
                            },
                            tooltip: 'Versionen anzeigen',
                        },
                        {
                            icon: <OpenInNewOutlinedIcon />,
                            href: `/${item.slug}`,
                            tooltip: 'Formular als antragstellende Person öffnen',
                            target: '_blank',
                            disabled: item.publishedVersion == null,
                        },
                        {
                            icon: <MoreVertOutlinedIcon />,
                            onClick: (event) => {
                                setRowMenu({
                                    target: event.currentTarget as HTMLElement,
                                    form: item,
                                });
                            },
                            tooltip: 'Konfiguration testen',
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
                >
                    <ListItemIcon>
                        <FileCopyOutlinedIcon />
                    </ListItemIcon>
                    <ListItemText>
                        Formular duplizieren
                    </ListItemText>
                </MenuItem>

                <MenuItem
                    component="a"
                    href={createCustomerPath(rowMenu?.form.slug ?? '')}
                    target="_blank"
                >
                    <ListItemIcon>
                        <OpenInNewOutlinedIcon />
                    </ListItemIcon>
                    <ListItemText>
                        Formular als antragstellende Person öffnen (in neuem Tab)
                    </ListItemText>
                </MenuItem>

                <MenuItem
                    onClick={handleFormLinkCopy}
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
                >
                    <ListItemIcon>
                        <ImportExportOutlinedIcon />
                    </ListItemIcon>
                    <ListItemText>
                        Formular exportieren
                    </ListItemText>
                </MenuItem>

                {
                    /* TODO: Check isDeveloper */ false &&
                    <MenuItem
                        onClick={() => {
                            // TODO: Handle delete
                        }}
                    >
                        <ListItemIcon>
                            <DeleteForeverOutlinedIcon />
                        </ListItemIcon>
                        <ListItemText>
                            Formular löschen
                        </ListItemText>
                    </MenuItem>
                }
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
        </>
    );
}

