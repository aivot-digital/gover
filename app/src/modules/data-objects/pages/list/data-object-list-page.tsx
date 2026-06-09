import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
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
import React, {useCallback, useMemo} from 'react';
import {GenericListPropsFetchOptions} from '../../../../components/generic-list/generic-list-props';

export function DataObjectListPage() {
    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });

    const header = useMemo(() => ({
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
    }), []);

    const fetchSchemas = useCallback((options: GenericListPropsFetchOptions<DataObjectSchema>) => {
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
    }, []);

    const columnDefinitions = useMemo(() => [
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
            renderCell: (params: any) => (
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
    ], [hasAccess]);

    const getRowIdentifier = useCallback((row: DataObjectSchema) => row.key.toString(), []);

    const rowActions = useCallback((item: DataObjectSchema) => [
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
    ], [hasAccess]);

    return (
        <>
            <PageWrapper
                title="Datenobjekte"
                fullWidth
                background
            >
                <GenericListPage<DataObjectSchema>
                    header={header}
                    searchLabel="Datenmodell suchen"
                    searchPlaceholder="Name des Datenmodells eingeben…"
                    fetch={fetchSchemas}
                    columnDefinitions={columnDefinitions}
                    getRowIdentifier={getRowIdentifier}
                    noDataPlaceholder={
                        <EmptyDataListPlaceholder
                            title="Noch keine Datenmodelle angelegt"
                            description="Datenmodelle definieren die Struktur wiederverwendbarer Datensätze, die in Formularen und Prozessen genutzt werden können."
                        />
                    }
                    noSearchResultsPlaceholder="Keine Datenmodelle gefunden"
                    rowActionsCount={2}
                    rowActions={rowActions}
                    defaultSortField="name"
                    disableFullWidthToggle={true}
                />
            </PageWrapper>
        </>
    );
}
