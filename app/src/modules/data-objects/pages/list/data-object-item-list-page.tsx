import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Typography} from '@mui/material';
import {EditOutlined} from '@mui/icons-material';
import {useSelector} from 'react-redux';
import {selectUser} from '../../../../slices/user-slice';
import {useEffect, useMemo, useState} from 'react';
import {isAdmin} from '../../../../utils/is-admin';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {DataObjectSchemasApiService} from '../../data-object-schemas-api-service';
import {useAdminGuard} from '../../../../hooks/use-admin-guard';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import {DataObjectSchema} from '../../models/data-object-schema';
import {useApi} from '../../../../hooks/use-api';
import {useParams} from 'react-router-dom';
import {LoadingPlaceholder} from '../../../../components/loading-placeholder/loading-placeholder';
import {DataObjectItemsApiService} from '../../data-object-items-api-service';
import {GridColDef} from '@mui/x-data-grid';
import {isAnyInputElement} from '../../../../models/elements/form/input/any-input-element';
import {ElementToMuiDataGridType} from '../../../../data/element-type/element-to-mui-data-grid-type';
import {DataObjectItem} from '../../models/data-object-item';
import DataArrayOutlinedIcon from '@mui/icons-material/DataArrayOutlined';
import CategoryOutlinedIcon from '@mui/icons-material/CategoryOutlined';
import {flattenElements} from '../../../../utils/flatten-elements';
import {generateComponentTitle} from '../../../../utils/generate-component-title';
import {ElementType} from '../../../../data/element-type/element-type';
import {format} from 'date-fns/format';
import {parseISO} from 'date-fns/parseISO';
import FolderData from '@aivot/mui-material-symbols-400-outlined/dist/folder-data/FolderData';
import DataObject from '@aivot/mui-material-symbols-400-outlined/dist/data-object/DataObject';

export function DataObjectItemListPage() {
    useAdminGuard();

    const schemaKey = useParams().schemaKey;

    const api = useApi();

    const user = useSelector(selectUser);
    const userIsAdmin = useMemo(() => isAdmin(user), [user]);

    const [dataObjectSchema, setDataObjectSchema] = useState<DataObjectSchema>();

    useEffect(() => {
        if (schemaKey == null) {
            return;
        }

        new DataObjectSchemasApiService(api)
            .retrieve(schemaKey)
            .then((dataObject) => {
                setDataObjectSchema(dataObject);
            })
            .catch((error) => {
                console.error('Error fetching data object:', error);
            });
    }, [schemaKey]);

    const columns: GridColDef[] = useMemo(() => {
        if (dataObjectSchema == null) {
            return [];
        }

        return [
            {
                field: 'icon',
                headerName: '',
                renderCell: () => <CellContentWrapper><DataObject /></CellContentWrapper>,
                disableColumnMenu: true,
                width: 24,
                sortable: false,
            },
            {
                field: 'id',
                headerName: 'ID',
                flex: 1,
                renderCell: (params) => (
                    <CellLink
                        to={`/data-objects/${dataObjectSchema.key}/${params.id}`}
                        title="Datenobjekt bearbeiten"
                    >
                        {String(params.value)}
                    </CellLink>
                ),
            },
            ...dataObjectSchemaExtractDisplayFields(dataObjectSchema),
        ];
    }, [dataObjectSchema]);

    if (dataObjectSchema == null) {
        return (
            <LoadingPlaceholder />
        );
    }

    return (
        <PageWrapper
            title={`Datenobjekte: ${dataObjectSchema.name}`}
            fullWidth
            background
        >
            <GenericListPage<DataObjectItem>
                header={{
                    icon: <DataObject />,
                    title: `Datenobjekte: ${dataObjectSchema.name}`,
                    actions: [
                        {
                            icon: <FolderData />,
                            to: `/data-models/${dataObjectSchema.key}`,
                            variant: 'outlined',
                            label: 'Datenmodell bearbeiten',
                        },
                        {
                            label: 'Neues Datenobjekt',
                            icon: <AddOutlinedIcon />,
                            disabled: !userIsAdmin,
                            tooltip: userIsAdmin ? undefined : 'Sie müssen globale Administrator:in sein, um diese Aktion durchführen zu können.',
                            to: `/data-objects/${dataObjectSchema.key}/new`,
                            variant: 'contained',
                        },
                    ],
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
                searchLabel="Datenobjekt suchen"
                searchPlaceholder="ID des Datenobjekts eingeben…"
                fetch={(options) => {
                    return new DataObjectItemsApiService(options.api, dataObjectSchema?.key)
                        .list(
                            options.page,
                            options.size,
                            options.sort,
                            options.order,
                            {
                                id: options.search,
                            },
                        );
                }}
                columnDefinitions={columns}
                getRowIdentifier={row => row.id.toString()}
                noDataPlaceholder="Keine Datenobjekte angelegt"
                noSearchResultsPlaceholder="Keine Datenobjekte gefunden"
                rowActionsCount={2}
                rowActions={(item: DataObjectItem) => [
                    {
                        icon: <EditOutlined />,
                        to: `/data-objects/${item.schemaKey}/${item.id}`,
                        tooltip: 'Datenobjekte bearbeiten',
                    },
                    {
                        icon: <DataArrayOutlinedIcon />,
                        to: `/data-models/${item.schemaKey}`,
                        tooltip: 'Datenmodell bearbeiten',
                    },
                ]}
                defaultSortField="id"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}

function dataObjectSchemaExtractDisplayFields(dataObjectSchema: DataObjectSchema): GridColDef[] {
    const cols: GridColDef[] = [];
    const allElements = flattenElements(dataObjectSchema.schema, true);

    for (const elementId of dataObjectSchema.displayFields ?? []) {
        const element = allElements
            .find((el) => el.id === elementId);

        if (element == null) {
            continue;
        }

        if (isAnyInputElement(element)) {
            cols.push({
                field: element.id ?? '',
                headerName: generateComponentTitle(element),
                flex: 1,
                type: ElementToMuiDataGridType[element.type] ?? 'string',
                valueGetter: (_: any, row: any) => {
                    const value = row.data[element.id]?.inputValue;

                    if (value == null) {
                        return null;
                    }

                    switch (element.type) {
                        case ElementType.MultiCheckbox:
                            return value
                                .map((val: string) => element.options?.find((opt) => opt.value === val)?.label)
                                .join(', ');
                        case ElementType.Date:
                            return format(parseISO(value), 'dd.MM.yyyy');
                        case ElementType.Time:
                            return format(parseISO(value), 'HH:mm');
                        case ElementType.Radio:
                        case ElementType.Select:
                            return element.options?.find((opt) => opt.value === value)?.label;
                    }

                    return row.data[element.id].inputValue;
                },
                sortable: false,
            });
        }
    }

    return cols;
}