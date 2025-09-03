import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {ListItemIcon, ListItemText, Menu, MenuItem, Typography} from '@mui/material';
import {EditOutlined} from '@mui/icons-material';
import Chip from '@mui/material/Chip';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import {FormListResponseDTO} from '../../dtos/form-list-response-dto';
import {FormsApiService} from '../../forms-api-service';
import MoreVertOutlinedIcon from '@mui/icons-material/MoreVertOutlined';
import DriveFolderUploadOutlinedIcon from '@mui/icons-material/DriveFolderUploadOutlined';
import OpenInNewOutlinedIcon from '@mui/icons-material/OpenInNewOutlined';
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined';
import React, {useState} from 'react';
import {ImportApplicationDialog} from '../../../../dialogs/application-dialogs/import-application-dialog/import-application-dialog';
import {FormDetailsResponseDTO} from '../../dtos/form-details-response-dto';
import {format} from 'date-fns/format';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import FileCopyOutlinedIcon from '@mui/icons-material/FileCopyOutlined';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import ContentPasteOutlinedIcon from '@mui/icons-material/ContentPasteOutlined';
import QrCode2OutlinedIcon from '@mui/icons-material/QrCode2Outlined';
import ImportExportOutlinedIcon from '@mui/icons-material/ImportExportOutlined';
import {createCustomerPath} from '../../../../utils/url-path-utils';
import {useDispatch} from 'react-redux';
import {downloadQrCode} from '../../../../utils/download-qrcode';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectUser} from '../../../../slices/user-slice';
import {AddFormDialog} from '../../dialogs/add-form-dialog';

export function FormsListPage() {
    const dispatch = useDispatch();

    const user = useAppSelector(selectUser);

    const [newForm, setNewForm] = useState<FormDetailsResponseDTO | undefined>(undefined);
    const [showImportFormDialog, setShowImportFormDialog] = useState(false);
    const [showExportFormDialog, setShowExportFormDialog] = useState(false);

    const [rowMenu, setRowMenu] = useState<{
        target: HTMLElement;
        form: FormListResponseDTO;
    } | undefined>(undefined);

    const handleDownloadQrCode = async (link: string, filename: string) => {
        try {
            await downloadQrCode(link, filename);
            dispatch(showSuccessSnackbar('QR-Code wurde als PNG heruntergeladen!'));
        } catch {
            dispatch(showErrorSnackbar('Fehler beim Herunterladen des QR-Codes!'));
        }
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
                            content: (
                                <>
                                    <Typography
                                        variant="body1"
                                        paragraph
                                    >
                                        Konfigurieren Sie hier die Nutzerkontenanbieter, die in Ihrer Gover-Instanz global verfügbar sein sollen.
                                        Die angebundenen Nutzerkonten können in Formularen als Authentifizierungsoptionen verwendet werden.
                                        Unterstützt werden alle Anbieter, die eine OpenID Connect (OIDC) kompatible Schnittstelle bereitstellen.
                                    </Typography>
                                    <Typography
                                        variant="body1"
                                        paragraph
                                    >
                                        <strong>Mögliche Szenarien:</strong>
                                    </Typography>
                                    <ul>
                                        <li>
                                            <Typography
                                                variant="body1"
                                                paragraph
                                            >
                                                <strong>Direkt OpenID Connect kompatible IDPs</strong>
                                                (z.B. BundID, BayernID, Mein Unternehmenskonto, Servicekonto SH, Keycloak, Azure AD):
                                                <br />
                                                → Sie können den Anbieter direkt anbinden, indem Sie die Verbindungsdaten hier hinterlegen.
                                            </Typography>
                                        </li>
                                        <li>
                                            <Typography
                                                variant="body1"
                                                paragraph
                                            >
                                                <strong>Systeme ohne OpenID Connect Unterstützung</strong>
                                                (z.B. LDAP/AD, andere IDPs):
                                                <br />
                                                → Die Anbindung erfolgt über den integrierten Keycloak von Gover. Tragen Sie anschließend die OpenID Connect-Daten des Keycloak-Realms hier ein.
                                            </Typography>
                                        </li>
                                        <li>
                                            <Typography
                                                variant="body1"
                                                paragraph
                                            >
                                                <strong>LDAP/AD für Gover-Mitarbeitende:</strong>
                                                <br />
                                                → Nutzung der User Federation im Staff Realm des Gover-Keycloaks.
                                                <br />
                                                Diese Nutzerkonten werden nicht über die Funktion "Nutzerkontenanbieter" verwaltet.
                                            </Typography>
                                        </li>
                                    </ul>
                                    <Typography
                                        variant="body1"
                                        paragraph
                                    >
                                        Es wird empfohlen, für jeden Nutzerkontenanbieter sowohl eine produktive als auch eine vorproduktive Anbindung einzurichten, um Tests zu erleichtern.
                                    </Typography>
                                    <Typography
                                        variant="body1"
                                        paragraph
                                    >
                                        Die notwendigen Konfigurationsdaten erhalten Sie in der Dokumentation des Anbieters oder direkt vom Anbieter selbst.
                                    </Typography>
                                </>
                            ),
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
                    columnDefinitions={[
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
                                <>
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
                                        Entwickelt durch: {params.row.developingDepartmentId}
                                    </Typography>
                                </>
                            ),
                        },
                        {
                            field: 'updated',
                            headerName: 'Zuletzt bearbeitet',
                            valueFormatter: (value, _) => {
                                return format(value, 'dd.MM.yyyy — HH:mm') + ' Uhr';
                            },
                            flex: 1,
                        },
                        {
                            field: 'publishedVersion',
                            headerName: 'Status',
                            renderCell: (params) => (
                                <>
                                    {params.row.draftedVersion != null &&
                                        <Chip
                                            label="In Bearbeitung"
                                            color="success"
                                            variant="outlined"
                                            size={'small'}
                                        />
                                    }

                                    {params.row.publishedVersion != null &&
                                        <Chip
                                            label="Veröffentlicht"
                                            color="success"
                                            variant="outlined"
                                            size={'small'}
                                        />
                                    }
                                </>
                            ),
                        },
                    ]}
                    getRowIdentifier={row => row.id.toString()}
                    noDataPlaceholder="Keine Formulare angelegt"
                    noSearchResultsPlaceholder="Keine Formulare gefunden"
                    rowActionsCount={4}
                    rowActions={(item: FormListResponseDTO) => [
                        {
                            icon: <EditOutlined />,
                            to: `/forms/${item.id}/${item.draftedVersion}`,
                            tooltip: 'Formular bearbeiten',
                            disabled: item.draftedVersion == null,
                        },
                        {
                            icon: <DriveFolderUploadOutlinedIcon />,
                            onClick: () => {
                            },
                            tooltip: 'Neue version',
                            disabled: item.draftedVersion != null,
                        },
                        {
                            icon: <OpenInNewOutlinedIcon />,
                            href: `/${item.slug}`,
                            tooltip: 'Konfiguration testen',
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
                    onClick={() => {
                        // TODO: Handle Clonse
                    }}
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
                    onClick={() => {
                        navigator
                            .clipboard
                            .writeText(createCustomerPath(rowMenu?.form.slug ?? ''))
                            .then(() => {
                                dispatch(showSuccessSnackbar('Formularlink in Zwischenablage kopiert'));
                            })
                            .catch((err) => {
                                console.error(err);
                                dispatch(showSuccessSnackbar('Formularlink konnte nicht kopiert werden'));
                            });
                        setRowMenu(undefined);
                    }}
                >
                    <ListItemIcon>
                        <ContentPasteOutlinedIcon />
                    </ListItemIcon>
                    <ListItemText>
                        Formularlink in Zwischenablage kopieren
                    </ListItemText>
                </MenuItem>

                <MenuItem
                    onClick={async () => {
                        await handleDownloadQrCode(createCustomerPath(rowMenu?.form.slug ?? ''), `qr-code-${rowMenu?.form.slug}.png`);
                        setRowMenu(undefined);
                    }}
                >
                    <ListItemIcon>
                        <QrCode2OutlinedIcon />
                    </ListItemIcon>
                    <ListItemText>
                        QR-Code mit Formularlink herunterladen
                    </ListItemText>
                </MenuItem>

                <MenuItem onClick={() => setShowExportFormDialog(true)}>
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
                    onSave={(created, navigateToEditAfterwards) => {
                        // TODO
                    }}
                    open={true}
                    basis={newForm}
                />
            }

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
        </>
    );
}

