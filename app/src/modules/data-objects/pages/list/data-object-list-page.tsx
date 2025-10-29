import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Typography} from '@mui/material';
import {EditOutlined} from '@mui/icons-material';
import {useSelector} from 'react-redux';
import {selectUser} from '../../../../slices/user-slice';
import {useMemo} from 'react';
import {isAdmin} from '../../../../utils/is-admin';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {DataObjectSchemasApiService} from '../../data-object-schemas-api-service';
import {useAdminGuard} from '../../../../hooks/use-admin-guard';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import {DataObjectSchema} from '../../models/data-object-schema';
import DataArrayOutlinedIcon from '@mui/icons-material/DataArrayOutlined';
import CategoryOutlinedIcon from '@mui/icons-material/CategoryOutlined';
import {uploadObjectFile} from '../../../../utils/download-utils';
import {useNavigate} from 'react-router-dom';
import {v4 as uuid4} from 'uuid';
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined';
import DataObject from '@aivot/mui-material-symbols-400-outlined/dist/data-object/DataObject';
import FolderData from '@aivot/mui-material-symbols-400-outlined/dist/folder-data/FolderData';

export function DataObjectListPage() {
    useAdminGuard();

    const navigate = useNavigate();
    const user = useSelector(selectUser);
    const userIsAdmin = useMemo(() => isAdmin(user), [user]);

    const handleImport = () => {
        uploadObjectFile<DataObjectSchema>('application/json')
            .then((importedSchema) => {
                if (importedSchema == null) {
                    return;
                }
                const importUUID = uuid4();
                sessionStorage.setItem(`import/${importUUID}`, JSON.stringify(importedSchema));
                navigate('/data-models/new', {
                    state: importedSchema,
                });
            });
    };

    return (
        <>
            <PageWrapper
                title="Datenobjekte"
                fullWidth
                background
            >
                <GenericListPage<DataObjectSchema>
                    header={{
                        icon: <DataObject />,
                        title: 'Datenobjekte',
                        helpDialog: {
                            title: 'Hilfe zu Datenobjekten',
                            tooltip: 'Hilfe anzeigen',
                            content: (
                                <>
                                    <Typography>
                                        Ein Datenobjekt ist eine konkrete Instanz eines Datenmodells. Es enthält die tatsächlichen Werte zu den im Datenmodell definierten Feldern und bildet damit die „laufenden“ Fachinformationen im System ab. Datenobjekte fließen durch Prozesse, Komponenten und Schnittstellen. Ihre Struktur, Datentypen und Prüfregeln ergeben sich immer aus dem verknüpften Datenmodell.
                                    </Typography>
                                    <Typography sx={{ mt: 2 }}>
                                        Typischerweise enthält ein Datenobjekt Werte für Text, Zahlen, Datums- oder Wahrheitsfelder sowie gegebenenfalls verschachtelte Strukturen. Neben den Nutzdaten können Metadaten wie Erstell- und Änderungszeitpunkte, Quelle oder Status sowie Referenzen auf andere Objekte vorhanden sein. Beim Anlegen werden Standardwerte aus dem Datenmodell übernommen; Validierungen stellen sicher, dass nur erlaubte, vollständige und konsistente Inhalte gespeichert werden. Änderungen an der Struktur erfolgen nicht am Datenobjekt selbst, sondern am zugrunde liegenden Datenmodell, das dann die Prüfung neuer oder geänderter Objekte steuert.
                                    </Typography>
                                    <Typography sx={{ mt: 2 }}>
                                        Ein einfaches Beispiel: Das Datenmodell „Bauvorhaben“ definiert Felder und Regeln, und das Datenobjekt „Erweiterungsbau Grundschule #2025-123“ füllt diese Felder mit konkreten Angaben.
                                    </Typography>
                                </>
                            ),
                        },
                    }}
                    searchLabel="Datenmodell suchen"
                    searchPlaceholder="Name des Datenmodells eingeben…"
                    fetch={(options) => {
                        return new DataObjectSchemasApiService(options.api)
                            .list(
                                options.page,
                                options.size,
                                options.sort,
                                options.order,
                                {
                                    name: options.search,
                                },
                            );
                    }}
                    columnDefinitions={[
                        {
                            field: 'icon',
                            headerName: '',
                            renderCell: () => <CellContentWrapper><FolderData /></CellContentWrapper>,
                            disableColumnMenu: true,
                            width: 24,
                            sortable: false,
                        },
                        {
                            field: 'name',
                            headerName: 'Name des Datenmodells',
                            flex: 1,
                            renderCell: (params) => (
                                <CellLink
                                    to={`/data-objects/${params.row.key}`}
                                    title="Datenobjekt bearbeiten"
                                >
                                    {String(params.value)}
                                </CellLink>
                            ),
                        },
                        {
                            field: 'description',
                            headerName: 'Beschreibung',
                            flex: 2,
                        },
                    ]}
                    getRowIdentifier={row => row.key.toString()}
                    noDataPlaceholder="Keine Datenmodelle angelegt"
                    noSearchResultsPlaceholder="Keine Datenmodelle gefunden"
                    rowActionsCount={2}
                    rowActions={(item: DataObjectSchema) => [
                        {
                            icon: <DataObject />,
                            to: `/data-objects/${item.key}`,
                            tooltip: 'Datenobjekte zu diesem Modell anzeigen',
                        },
                        {
                            icon: <EditOutlined />,
                            to: `/data-models/${item.key}`,
                            tooltip: 'Datenmodell bearbeiten',
                        },
                    ]}
                    defaultSortField="name"
                    disableFullWidthToggle={true}
                />
            </PageWrapper>
        </>
    );
}