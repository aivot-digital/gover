import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Typography} from '@mui/material';
import {EditOutlined} from '@mui/icons-material';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {DataObjectSchemasApiService} from '../../data-object-schemas-api-service';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import {DataObjectSchema} from '../../models/data-object-schema';
import {uploadObjectFile} from '../../../../utils/download-utils';
import {useNavigate} from 'react-router-dom';
import {v4 as uuid4} from 'uuid';
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined';
import FolderData from '@aivot/mui-material-symbols-400-outlined/dist/folder-data/FolderData';
import DataObject from '@aivot/mui-material-symbols-400-outlined/dist/data-object/DataObject';
import {useUserIsAdmin} from '../../../../hooks/use-admin-guard';
import ArrowForward from '@aivot/mui-material-symbols-400-outlined/dist/arrow-forward/ArrowForward';

export function DataObjectSchemaListPage() {
    const navigate = useNavigate();
    const userIsAdmin = useUserIsAdmin();

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
                title="Datenmodelle"
                fullWidth
                background
            >
                <GenericListPage<DataObjectSchema>
                    header={{
                        icon: <FolderData />,
                        title: 'Datenmodelle',
                        actions: userIsAdmin ? [
                            {
                                icon: <CloudUploadOutlinedIcon />,
                                onClick: handleImport,
                                variant: 'outlined',
                                label: 'Modell importieren',
                            },
                            {
                                label: 'Neues Datenmodell',
                                icon: <AddOutlinedIcon />,
                                to: '/data-models/new',
                                variant: 'contained',
                            },
                        ] : undefined,
                        helpDialog: {
                            title: 'Hilfe zu Datenmodellen',
                            tooltip: 'Hilfe anzeigen',
                            content: (
                                <>
                                    <Typography>
                                        Ein Datenmodell beschreibt die Struktur eines Datenobjekts in Gover und legt fest, welche Datenfelder existieren, welche Datentypen sie haben, welche Standardwerte gelten und wie Werte geprüft werden.
                                        Es sorgt dafür, dass Daten aus Formularen, Workflows und Schnittstellen konsistent, valide und eindeutig interpretierbar sind.
                                    </Typography>
                                    <Typography sx={{mt: 2}}>
                                        Dazu können auch verschachtelte Objekte, Pflichtangaben, Wertebereiche oder Muster sowie Beschreibungen, Labels und optionale Sichtbarkeitsregeln gehören. Dasselbe Datenmodell kann in mehreren
                                        Prozessen und Komponenten wiederverwendet werden, sodass überall dieselbe Definition gilt. Bei der Ausgestaltung empfiehlt es sich, sprechende und langlebige Feldnamen zu verwenden,
                                        Weiterentwicklungen kompatibel vorzunehmen (zum Beispiel Felder hinzufügen statt umzubenennen oder zu entfernen) und Validierungen deutlich zu setzen.
                                    </Typography>
                                    <Typography sx={{mt: 2}}>
                                        Bei der Beziehung zwischen Datenmodell und Datenobjekt gilt: Das Datenmodell definiert die Form und das Datenobjekt füllt diese Form mit konkreten Werten. Änderungen am Datenmodell beeinflussen, wie
                                        neue oder geänderte Datenobjekte geprüft und gespeichert werden.
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
                            headerName: 'Name',
                            flex: 1,
                            renderCell: (params) => (
                                <CellLink
                                    to={`/data-models/${params.row.key}`}
                                    title="Datenmodell bearbeiten"
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
                            icon: userIsAdmin ? <EditOutlined /> : <ArrowForward />,
                            to: `/data-models/${item.key}`,
                            tooltip: userIsAdmin ? 'Datenmodell bearbeiten' : 'Datenmodell anzeigen',
                        },
                        {
                            icon: <DataObject />,
                            to: `/data-objects/${item.key}`,
                            tooltip: 'Datenobjekte zu diesem Modell anzeigen',
                        },
                    ]}
                    defaultSortField="name"
                    disableFullWidthToggle={true}
                />
            </PageWrapper>
        </>
    );
}