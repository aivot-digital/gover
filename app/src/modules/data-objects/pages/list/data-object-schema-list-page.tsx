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
import Upload from '@aivot/mui-material-symbols-400-outlined/dist/upload/Upload';
import {uploadObjectFile} from '../../../../utils/download-utils';
import {useNavigate} from 'react-router-dom';
import {v4 as uuid4} from 'uuid';

export function DataObjectSchemaListPage() {
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
                navigate('/data-objects/new', {
                    state: importedSchema,
                });
            });
    };

    return (
        <>
            <PageWrapper
                title="Datenobjektschemata"
                fullWidth
                background
            >
                <GenericListPage<DataObjectSchema>
                    header={{
                        icon: <DataArrayOutlinedIcon />,
                        title: 'Datenobjektschema',
                        actions: [
                            {
                                label: 'Neues Datenobjektschema',
                                icon: <AddOutlinedIcon />,
                                disabled: !userIsAdmin,
                                tooltip: userIsAdmin ? undefined : 'Sie müssen globale Administrator:in sein, um diese Aktion durchführen zu können.',
                                to: '/data-objects/new',
                                variant: 'contained',
                            },
                            {
                                icon: <Upload />,
                                tooltip: 'Datenobjektschema importieren',
                                disabled: !userIsAdmin,
                                onClick: handleImport,
                            },
                        ],
                        helpDialog: {
                            title: 'Hilfe zu Datenobjektschemata',
                            tooltip: 'Hilfe anzeigen',
                            content: (
                                <>
                                    <Typography>
                                        Ein Datenobjektschema beschreibt die Struktur eines Datenobjekts in Gover und legt fest, welche Datenfelder existieren, welche Datentypen diese haben und wie Werte geprüft werden. Es sorgt dafür, dass Daten aus Formularen, Workflows und Schnittstellen konsistent, valide und eindeutig interpretierbar sind.
                                    </Typography>
                                    <Typography sx={{ mt: 2 }}>
                                        Typischerweise umfasst ein Schema Felder mit Schlüsseln, Datentypen wie Text, Zahl, Datum oder Wahrheitswerte, Validierungen für Pflichtangaben, Wertebereiche oder Muster, sowie die Möglichkeit verschachtelte Objekte abzubilden. Ergänzend können Metadaten wie Beschreibungen, Labels oder Sichtbarkeitsregeln hinterlegt und sinnvolle Standardwerte definiert werden.
                                    </Typography>
                                    <Typography sx={{ mt: 2 }}>
                                        Das Schema unterstützt verschiedene Komponenten bei der einheitlichen Nutzung desselben Datenmodells. Bei der Ausgestaltung empfehlen sich sprechende, langlebige Feldnamen, kompatible Weiterentwicklungen (hinzufügen statt umbenennen/entfernen) und deutliche Validierungen.
                                    </Typography>
                                </>
                            ),
                        },
                    }}
                    searchLabel="Datenobjektschema suchen"
                    searchPlaceholder="Name des Datenobjektschemas eingeben…"
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
                            renderCell: () => <CellContentWrapper><DataArrayOutlinedIcon /></CellContentWrapper>,
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
                                    to={`/data-objects/${params.row.key}`}
                                    title="Datenobjektschema bearbeiten"
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
                    noDataPlaceholder="Keine Datenobjektschemata angelegt"
                    noSearchResultsPlaceholder="Keine Datenobjektschema gefunden"
                    rowActionsCount={2}
                    rowActions={(item: DataObjectSchema) => [
                        {
                            icon: <EditOutlined />,
                            to: `/data-objects/${item.key}`,
                            tooltip: 'Datenobjektschema bearbeiten',
                        },
                        {
                            icon: <CategoryOutlinedIcon />,
                            to: `/data-objects/${item.key}/items`,
                            tooltip: 'Datenobjekte anzeigen',
                        },
                    ]}
                    defaultSortField="name"
                    disableFullWidthToggle={true}
                />
            </PageWrapper>
        </>
    );
}