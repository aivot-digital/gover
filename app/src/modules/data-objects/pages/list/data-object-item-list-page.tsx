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
                renderCell: () => <CellContentWrapper><CategoryOutlinedIcon /></CellContentWrapper>,
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
                        to={`/data-objects/${dataObjectSchema.key}/items/${params.id}`}
                        title="Datenobjekt bearbeiten"
                    >
                        {String(params.value)}
                    </CellLink>
                ),
            },
            ...(dataObjectSchema.schema.children ?? [])
                .filter(e => isAnyInputElement(e))
                .filter(e => ElementToMuiDataGridType[e.type] != null)
                .map(e => ({
                    field: e.id ?? '',
                    headerName: e.label ?? '',
                    flex: 1,
                    type: ElementToMuiDataGridType[e.type]!,
                    valueGetter: (_: any, row: any) => {
                        return row.data[e.id].inputValue;
                    },
                })),
        ];
    }, [dataObjectSchema]);

    if (dataObjectSchema == null) {
        return (
            <LoadingPlaceholder />
        );
    }

    return (
        <PageWrapper
            title={dataObjectSchema.name}
            fullWidth
            background
        >
            <GenericListPage<DataObjectItem>
                header={{
                    icon: <CategoryOutlinedIcon />,
                    title: dataObjectSchema.name,
                    actions: [
                        {
                            icon: <DataArrayOutlinedIcon />,
                            tooltip: 'Datenobjektschema bearbeiten',
                            to: `/data-objects/${dataObjectSchema.key}`,
                            variant: 'outlined',
                            label: 'Datenobjektschema bearbeiten',
                        },
                        {
                            label: 'Neues Datenobjekt',
                            icon: <AddOutlinedIcon />,
                            disabled: !userIsAdmin,
                            tooltip: userIsAdmin ? undefined : 'Sie müssen globale Administrator:in sein, um diese Aktion durchführen zu können.',
                            to: `/data-objects/${dataObjectSchema.key}/items/new`,
                            variant: 'contained',
                        },
                    ],
                    helpDialog: {
                        title: 'Hilfe zu Datenobjekten',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Ein Datenobjektschema ist eine Definition für ein Datenobjekt, das in Gover verwendet wird.
                                    Sie können damit Datenobjekte erstellen, die in Formularen und anderen Komponenten verwendet werden.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Ein Datenobjekt kann verschiedene Eigenschaften haben, die in Formularen verwendet werden können.
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
                noDataPlaceholder="Keine Datenobjektschemata angelegt"
                noSearchResultsPlaceholder="Keine Datenobjektschema gefunden"
                rowActionsCount={2}
                rowActions={(item: DataObjectItem) => [
                    {
                        icon: <EditOutlined />,
                        to: `/data-objects/${item.schemaKey}/items/${item.id}`,
                        tooltip: 'Datenobjekte bearbeiten',
                    },
                    {
                        icon: <DataArrayOutlinedIcon />,
                        to: `/data-objects/${item.schemaKey}`,
                        tooltip: 'Datenobjektschema bearbeiten',
                    },
                ]}
                defaultSortField="id"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}