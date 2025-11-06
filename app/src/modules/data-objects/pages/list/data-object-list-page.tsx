import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {EditOutlined} from '@mui/icons-material';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {DataObjectSchemasApiService} from '../../data-object-schemas-api-service';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import {DataObjectSchema} from '../../models/data-object-schema';
import DataObject from '@aivot/mui-material-symbols-400-outlined/dist/data-object/DataObject';
import FolderData from '@aivot/mui-material-symbols-400-outlined/dist/folder-data/FolderData';
import {useAccessGuard} from '../../../../hooks/use-admin-guard';
import Visibility from '@aivot/mui-material-symbols-400-outlined/dist/visibility/Visibility';

export function DataObjectListPage() {
    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });

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
                                        Ein Datenobjekt ist eine konkrete Instanz eines Datenmodells. Es enthält die tatsächlichen Werte zu den im Datenmodell definierten Feldern und bildet damit die „laufenden“ Fachinformationen im System
                                        ab. Datenobjekte fließen durch Prozesse, Komponenten und Schnittstellen. Ihre Struktur, Datentypen und Prüfregeln ergeben sich immer aus dem verknüpften Datenmodell.
                                    </Typography>
                                    <Typography sx={{mt: 2}}>
                                        Typischerweise enthält ein Datenobjekt Werte für Text, Zahlen, Datums- oder Wahrheitsfelder sowie gegebenenfalls verschachtelte Strukturen. Neben den Nutzdaten können Metadaten wie Erstell- und
                                        Änderungszeitpunkte, Quelle oder Status sowie Referenzen auf andere Objekte vorhanden sein. Beim Anlegen werden Standardwerte aus dem Datenmodell übernommen; Validierungen stellen sicher, dass nur
                                        erlaubte, vollständige und konsistente Inhalte gespeichert werden. Änderungen an der Struktur erfolgen nicht am Datenobjekt selbst, sondern am zugrunde liegenden Datenmodell, das dann die Prüfung
                                        neuer oder geänderter Objekte steuert.
                                    </Typography>
                                    <Typography sx={{mt: 2}}>
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
                                    title={hasAccess ? 'Datenmodell bearbeiten' : 'Datenmodell anzeigen'}
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
                            icon: hasAccess ? <EditOutlined /> : <Visibility />,
                            to: `/data-models/${item.key}`,
                            tooltip: hasAccess ? 'Datenmodell bearbeiten' : 'Datenmodell anzeigen',
                        },
                    ]}
                    defaultSortField="name"
                    disableFullWidthToggle={true}
                />
            </PageWrapper>
        </>
    );
}