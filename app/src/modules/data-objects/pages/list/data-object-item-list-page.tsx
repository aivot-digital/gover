import {
    GenericListPage,
    type GenericListPagePermissionConfig,
    type GenericListPagePermissionState,
} from '../../../../components/generic-list-page/generic-list-page';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import {Typography} from '@mui/material';
import EditOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import {useCallback, useEffect, useMemo, useState} from 'react';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {DataObjectSchemasApiService} from '../../data-object-schemas-api-service';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import {DataObjectSchema} from '../../models/data-object-schema';
import {useApi} from '../../../../hooks/use-api';
import {useParams, useNavigate} from 'react-router-dom';
import {LoadingPlaceholder} from '../../../../components/loading-placeholder/loading-placeholder';
import {DataObjectItemsApiService} from '../../data-object-items-api-service';
import {GridColDef} from '@mui/x-data-grid';
import {isAnyInputElement} from '../../../../models/elements/form/input/any-input-element';
import {ElementToMuiDataGridType} from '../../../../data/element-type/element-to-mui-data-grid-type';
import {DataObjectItem} from '../../models/data-object-item';
import {flattenElements} from '../../../../utils/flatten-elements';
import {generateComponentTitle} from '../../../../utils/generate-component-title';
import {ElementType} from '../../../../data/element-type/element-type';
import {format} from 'date-fns/format';
import {parseISO} from 'date-fns/parseISO';
import FolderData from '@aivot/mui-material-symbols-400-n25-outlined/FolderData';
import DataObject from '@aivot/mui-material-symbols-400-n25-outlined/DataObject';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import {TimeFieldComponentModelMode} from '../../../../models/elements/form/input/time-field-element';
import {
    formatDomainAndUserSelectValue,
    normalizeDomainAndUserSelectItem,
} from '../../../../components/domain-user-select-field/domain-user-select-options';
import {DomainAndUserSelectItem} from '../../../../models/elements/form/input/domain-user-select-field-element';
import {AssignmentContextValue} from '../../../../models/elements/form/input/assignment-context-field-element';
import {GenericListPropsFetchOptions} from '../../../../components/generic-list/generic-list-props';
import {useRequireSystemPermission} from '../../../permissions/hooks/use-permissions';
import {Permission} from '../../../../data/permissions/permission';

const dataObjectItemListPermissionCheck: GenericListPagePermissionConfig<DataObjectItem> = {
    scope: {
        type: 'system',
    },
    listAccess: [
        Permission.OBJECT_ITEM_READ,
        Permission.OBJECT_SCHEMA_READ,
    ],
    read: Permission.OBJECT_ITEM_READ,
    create: Permission.OBJECT_ITEM_CREATE,
    update: Permission.OBJECT_ITEM_UPDATE,
};

export function DataObjectItemListPage() {
    const navigate = useNavigate();
    const api = useApi();
    const schemaKey = useParams().schemaKey;
    // The schema is fetched before GenericListPage renders, so these read guards must stay here.
    useRequireSystemPermission(Permission.OBJECT_ITEM_READ);
    useRequireSystemPermission(Permission.OBJECT_SCHEMA_READ);

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

    const columns = useCallback((permissions: GenericListPagePermissionState<DataObjectItem>): GridColDef[] => {
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
                        title={permissions.canUpdate(params.row) ? 'Datenobjekt bearbeiten' : 'Datenobjekt anzeigen'}
                    >
                        {String(params.value)}
                    </CellLink>
                ),
            },
            ...dataObjectSchemaExtractDisplayFields(dataObjectSchema),
        ];
    }, [dataObjectSchema]);

    const dataObjectSchemaKey = dataObjectSchema?.key ?? '';
    const dataObjectSchemaName = dataObjectSchema?.name ?? '';

    const header = useCallback((permissions: GenericListPagePermissionState<DataObjectItem>) => {
        const canUpdateDataObjectSchema = permissions.hasPermission(Permission.OBJECT_SCHEMA_UPDATE);

        return ({
            icon: <DataObject />,
            title: `Datenobjekte: ${dataObjectSchemaName}`,
            actions: [
                {
                    icon: <FolderData />,
                    to: `/data-models/${dataObjectSchemaKey}`,
                    variant: 'text' as const,
                    label: canUpdateDataObjectSchema ? 'Datenmodell bearbeiten' : 'Datenmodell anzeigen',
                },
                {
                    label: 'Neues Datenobjekt',
                    icon: <AddOutlinedIcon />,
                    to: `/data-objects/${dataObjectSchemaKey}/new`,
                    variant: 'contained' as const,
                    disabled: !permissions.canCreate,
                    disabledTooltip: permissions.createDisabledTooltip,
                },
            ],
            helpDialog: {
                title: 'Hilfe zu Datenobjekten',
                tooltip: 'Hilfe anzeigen',
                content: (
                    <>
                        <Typography>
                            Ein Datenobjekt ist eine konkrete Instanz eines Datenmodells. Es enthält die tatsächlichen Werte zu den im Datenmodell definierten Feldern und bildet damit die „laufenden“ Fachinformationen im System ab.
                            Datenobjekte fließen durch Prozesse, Komponenten und Schnittstellen. Ihre Struktur, Datentypen und Prüfregeln ergeben sich immer aus dem verknüpften Datenmodell.
                        </Typography>
                        <Typography sx={{mt: 2}}>
                            Typischerweise enthält ein Datenobjekt Werte für Text, Zahlen, Datums- oder Wahrheitsfelder sowie gegebenenfalls verschachtelte Strukturen. Neben den Nutzdaten können Metadaten wie Erstell- und
                            Änderungszeitpunkte, Quelle oder Status sowie Referenzen auf andere Objekte vorhanden sein. Beim Anlegen werden Standardwerte aus dem Datenmodell übernommen; Validierungen stellen sicher, dass nur
                            erlaubte, vollständige und konsistente Inhalte gespeichert werden. Änderungen an der Struktur erfolgen nicht am Datenobjekt selbst, sondern am zugrunde liegenden Datenmodell, das dann die Prüfung neuer
                            oder geänderter Objekte steuert.
                        </Typography>
                        <Typography sx={{mt: 2}}>
                            Ein einfaches Beispiel: Das Datenmodell „Bauvorhaben“ definiert Felder und Regeln, und das Datenobjekt „Erweiterungsbau Grundschule #2025-123“ füllt diese Felder mit konkreten Angaben.
                        </Typography>
                    </>
                ),
            },
        });
    }, [dataObjectSchemaKey, dataObjectSchemaName]);

    const fetchDataObjectItems = useCallback((options: GenericListPropsFetchOptions<DataObjectItem>) => {
        return new DataObjectItemsApiService(options.api, dataObjectSchemaKey)
            .list(
                options.page,
                options.size,
                options.sort,
                options.order,
                {
                    id: options.search,
                },
            );
    }, [dataObjectSchemaKey]);

    const getRowIdentifier = useCallback((row: DataObjectItem) => row.id.toString(), []);

    const rowActions = useCallback((item: DataObjectItem, permissions: GenericListPagePermissionState<DataObjectItem>) => {
        const canUpdateDataObjectItem = permissions.canUpdate(item);
        const canUpdateDataObjectSchema = permissions.hasPermission(Permission.OBJECT_SCHEMA_UPDATE);

        return [
            {
                icon: canUpdateDataObjectItem ? <EditOutlined /> : <Visibility />,
                to: `/data-objects/${item.schemaKey}/${item.id}`,
                tooltip: canUpdateDataObjectItem ? 'Datenobjekt bearbeiten' : 'Datenobjekt anzeigen',
            },
            {
                icon: ModuleIcons.dataModels,
                to: `/data-models/${item.schemaKey}`,
                tooltip: canUpdateDataObjectSchema ? 'Datenmodell bearbeiten' : 'Datenmodell anzeigen',
            },
        ];
    }, []);

    const noDataPlaceholder = useCallback((permissions: GenericListPagePermissionState<DataObjectItem>) => (
        <EmptyDataListPlaceholder
            title="Keine Datenobjekte vorhanden"
            description="Datenobjekte sind einzelne Datensätze eines Datenmodells, die zentral gepflegt und wiederverwendet werden können."
            addText="Neues Datenobjekt anlegen"
            onAdd={() => navigate(`/data-objects/${dataObjectSchemaKey}/new`)}
            addDisabled={!permissions.canCreate}
            addDisabledTooltip={permissions.createDisabledTooltip}
        />
    ), [dataObjectSchemaKey, navigate]);

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
                header={header}
                permissionCheck={dataObjectItemListPermissionCheck}
                searchLabel="Datenobjekt suchen"
                searchPlaceholder="ID des Datenobjekts eingeben…"
                fetch={fetchDataObjectItems}
                columnDefinitions={columns}
                getRowIdentifier={getRowIdentifier}
                noDataPlaceholder={noDataPlaceholder}
                noSearchResultsPlaceholder="Keine Datenobjekte gefunden"
                rowActionsCount={2}
                rowActions={rowActions}
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
                    const value = row.data[element.id];

                    if (value == null) {
                        return null;
                    }

                    switch (element.type) {
                        case ElementType.MultiCheckbox:
                            return value
                                .map((val: string) => element.options?.find((opt) => opt.value === val)?.label)
                                .join(', ');
                        case ElementType.ChipInput:
                            return value
                                .map((val: string) => val)
                                .join(', ');
                        case ElementType.Date:
                            return format(parseISO(value), 'dd.MM.yyyy');
                        case ElementType.DateTime:
                            return format(
                                parseISO(value),
                                (element.mode ?? TimeFieldComponentModelMode.Minute) === TimeFieldComponentModelMode.Second
                                    ? 'dd.MM.yyyy HH:mm:ss'
                                    : 'dd.MM.yyyy HH:mm',
                            );
                        case ElementType.DateRange:
                            return `${formatRangeValue(value?.start, 'dd.MM.yyyy')} bis ${formatRangeValue(value?.end, 'dd.MM.yyyy')}`;
                        case ElementType.TimeRange:
                            return `${formatRangeValue(
                                value?.start,
                                (element.mode ?? TimeFieldComponentModelMode.Minute) === TimeFieldComponentModelMode.Second ? 'HH:mm:ss' : 'HH:mm',
                            )} bis ${formatRangeValue(
                                value?.end,
                                (element.mode ?? TimeFieldComponentModelMode.Minute) === TimeFieldComponentModelMode.Second ? 'HH:mm:ss' : 'HH:mm',
                            )}`;
                        case ElementType.DateTimeRange:
                            return `${formatRangeValue(
                                value?.start,
                                (element.mode ?? TimeFieldComponentModelMode.Minute) === TimeFieldComponentModelMode.Second
                                    ? 'dd.MM.yyyy HH:mm:ss'
                                    : 'dd.MM.yyyy HH:mm',
                            )} bis ${formatRangeValue(
                                value?.end,
                                (element.mode ?? TimeFieldComponentModelMode.Minute) === TimeFieldComponentModelMode.Second
                                    ? 'dd.MM.yyyy HH:mm:ss'
                                    : 'dd.MM.yyyy HH:mm',
                            )}`;
                        case ElementType.MapPoint:
                            return value?.address ?? (
                                value?.latitude != null && value?.longitude != null
                                    ? `${value.latitude.toFixed(6)}, ${value.longitude.toFixed(6)}`
                                    : null
                            );
                        case ElementType.DomainAndUserSelect:
                            if (!Array.isArray(value)) {
                                return null;
                            }

                            return value
                                .map((val: unknown) => normalizeDomainAndUserSelectItem(val))
                                .filter((val): val is DomainAndUserSelectItem => val != null)
                                .map((val) => formatDomainAndUserSelectValue(val))
                                .join(', ');
                        case ElementType.AssignmentContext:
                            if (value == null || typeof value !== 'object') {
                                return null;
                            }

                            const assignmentContextValue = value as AssignmentContextValue;
                            const selectedLabels = (assignmentContextValue.domainAndUserSelection ?? [])
                                .map((val: unknown) => normalizeDomainAndUserSelectItem(val))
                                .filter((val): val is DomainAndUserSelectItem => val != null)
                                .map((val) => formatDomainAndUserSelectValue(val));

                            const preferenceLabels = [
                                assignmentContextValue.preferPreviousTaskAssignee === true ? 'Vorherige Bearbeiter:in bevorzugen' : null,
                                assignmentContextValue.preferUninvolvedUser === true ? 'Unbeteiligte Mitarbeiter:in bevorzugen' : null,
                                assignmentContextValue.preferProcessInstanceAssignee === true ? 'Vorgangszuweisung bevorzugen' : null,
                            ]
                                .filter((entry): entry is string => entry != null);

                            return [...selectedLabels, ...preferenceLabels].join(', ');
                        case ElementType.Time:
                            return format(
                                parseISO(value),
                                (element.mode ?? TimeFieldComponentModelMode.Minute) === TimeFieldComponentModelMode.Second ? 'HH:mm:ss' : 'HH:mm',
                            );
                        case ElementType.Radio:
                        case ElementType.Select: {
                            const matchedOption = element.options
                                ?.find((opt) => typeof opt === 'string' ? opt === value : opt.value === value);

                            return typeof matchedOption === 'string' ? matchedOption : matchedOption?.label;
                        }
                        case ElementType.DataModelSelect:
                        case ElementType.DataObjectSelect:
                            return value;
                    }

                    return row.data[element.id];
                },
                sortable: false,
            });
        }
    }

    return cols;
}

function formatRangeValue(value: string | undefined, formatStr: string): string {
    if (value == null || value.length === 0) {
        return 'Keine Angabe';
    }

    return format(parseISO(value), formatStr);
}
