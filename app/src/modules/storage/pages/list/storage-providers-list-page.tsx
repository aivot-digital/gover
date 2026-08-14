import {
    GenericListPage,
    type GenericListPagePermissionConfig,
    type GenericListPagePermissionState,
} from '../../../../components/generic-list-page/generic-list-page';
import {useNavigate} from 'react-router-dom';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import {Typography} from '@mui/material';
import EditOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import {StorageProvidersApiService, type StorageProviderFilter} from '../../storage-providers-api-service';
import React, {type ReactNode, useCallback, useEffect, useMemo, useState} from 'react';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {type StorageProviderDefinition} from '../../entities/storage-provider-definition';
import {type StorageProviderEntity} from '../../entities/storage-provider-entity';
import {type StorageProviderStatus} from '../../enums/storage-provider-status';
import {StorageStatusChip} from '../../components/storage-status-chip';
import {SelectFieldComponent} from '../../../../components/select-field/select-field-component';
import {GenericListPropsFetchOptions} from '../../../../components/generic-list/generic-list-props';
import {Permission} from '../../../../data/permissions/permission';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import FolderOpen from '@aivot/mui-material-symbols-400-n25-outlined/FolderOpen';
import ScienceOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Science';

const availableFilter = [
    {
        label: 'Alle',
        value: 'all',
    },
    {
        label: 'Systemanbieter',
        value: 'systemProvider',
    },
    {
        label: 'Read-only Speicheranbieter',
        value: 'readOnlyStorage',
    },
];

const storageProvidersListPermissionCheck: GenericListPagePermissionConfig<StorageProviderEntity> = {
    scope: {
        type: 'system',
    },
    read: Permission.STORAGE_PROVIDER_READ,
    create: Permission.STORAGE_PROVIDER_CREATE,
    update: Permission.STORAGE_PROVIDER_UPDATE,
};

export function StorageProvidersListPage(): ReactNode {
    const navigate = useNavigate();
    const [definitions, setDefinitions] = useState<StorageProviderDefinition[]>([]);
    const [selectedDefinitionKey, setSelectedDefinitionKey] = useState<string | undefined>(undefined);

    useEffect(() => {
        new StorageProvidersApiService()
            .listDefinitions()
            .then(setDefinitions)
            .catch(console.error);
    }, []);

    const header = useCallback((permissions: GenericListPagePermissionState<StorageProviderEntity>) => ({
        icon: ModuleIcons.storage,
        title: 'Speicheranbieter',
        actions: [
            {
                label: 'Neuer Speicheranbieter',
                icon: <AddOutlinedIcon/>,
                to: '/storage-providers/new',
                variant: 'contained' as const,
                disabled: !permissions.canCreate,
                disabledTooltip: permissions.createDisabledTooltip,
            },
        ],
        helpDialog: {
            title: 'Hilfe zu Speicheranbietern',
            tooltip: 'Hilfe anzeigen',
            content: (
                <>
                    <Typography
                        variant="body1"
                        sx={{
                            marginBottom: "16px"
                        }}
                    >
                        Konfigurieren Sie hier Speicheranbieter, die in Ihrer Prosuna-Instanz global
                        verfügbar sein sollen.
                        Die erforderlichen Konfigurationsdaten erhalten Sie vom Zahlungsdienstleister
                        oder finden Sie in dessen Dokumentation.
                    </Typography>
                    <Typography
                        variant="body1"
                        sx={{
                            marginBottom: "16px"
                        }}
                    >
                        Es wird empfohlen, für jeden Speicheranbieter sowohl eine produktive als
                        auch eine vorproduktive Anbindung einzurichten, um Tests zu erleichtern.
                    </Typography>
                </>
            ),
        },
    }), []);

    const definitionOptions = useMemo(() => definitions.map((def) => ({
        value: def.key,
        label: def.name,
        subLabel: def.description,
    })), [definitions]);

    const preSearchElements = useMemo(() => [
        <SelectFieldComponent
            key="definition"
            label="Speichertyp"
            value={selectedDefinitionKey}
            onChange={(value) => setSelectedDefinitionKey(value ?? undefined)}
            options={definitionOptions}
            placeholder="Alle Speichertypen"
            size="small"
        />,
    ], [definitionOptions, selectedDefinitionKey]);

    const fetchStorageProviders = useCallback((options: GenericListPropsFetchOptions<StorageProviderEntity>) => {
        const filter: Partial<StorageProviderFilter> = {};
        if (options.search) {
            filter.name = options.search;
        }
        if (options.filter === 'systemProvider') {
            filter.systemProvider = true;
        } else if (options.filter === 'readOnlyStorage') {
            filter.readOnlyStorage = true;
        }
        if (selectedDefinitionKey) {
            filter.storageProviderDefinitionKey = selectedDefinitionKey;
        }
        return new StorageProvidersApiService()
            .list(options.page, options.size, options.sort, options.order, filter);
    }, [selectedDefinitionKey]);

    const columnIcon = useCallback(() => ModuleIcons.storage, []);

    const columnDefinitions = useCallback((permissions: GenericListPagePermissionState<StorageProviderEntity>) => [
        {
            field: 'name',
            headerName: 'Name',
            flex: 1,
            renderCell: (params: any) => (
                <CellLink
                    to={`/storage-providers/${params.id}`}
                    title={permissions.canUpdate(params.row) ? 'Konfiguration bearbeiten' : 'Konfiguration anzeigen'}
                >
                    {String(params.value)}
                </CellLink>
            ),
        },
        {
            field: 'storageProviderDefinitionKey',
            headerName: 'Speichertyp',
            flex: 1,
            valueGetter: (_: any, row: StorageProviderEntity) => {
                const provider = definitions.find((def) => (
                    def.key === row.storageProviderDefinitionKey &&
                    def.version === row.storageProviderDefinitionVersion
                ));
                return provider?.name;
            },
        },
        {
            field: 'description',
            headerName: 'Beschreibung',
            flex: 2,
        },
        {
            field: 'status',
            headerName: 'Status',
            width: 150,
            renderCell: (params: any) => {
                return (
                    <CellContentWrapper>
                        <StorageStatusChip
                            status={params.value as StorageProviderStatus}
                        />
                    </CellContentWrapper>
                );
            },
        },
    ], [definitions]);

    const getRowIdentifier = useCallback((row: StorageProviderEntity) => row.id.toString(), []);

    const rowActions = useCallback((item: StorageProviderEntity, permissions: GenericListPagePermissionState<StorageProviderEntity>) => {
        const canUpdateStorageProvider = permissions.canUpdate(item);

        return [
            {
                icon: canUpdateStorageProvider ? <EditOutlined/> : <Visibility/>,
                to: `/storage-providers/${item.id}`,
                tooltip: canUpdateStorageProvider ? 'Konfiguration bearbeiten' : 'Konfiguration anzeigen',
            },
            {
                icon: <FolderOpen/>,
                to: `/storage-providers/${item.id}/explore`,
                tooltip: 'Dateiexplorer öffnen',
            },
            {
                icon: <ScienceOutlinedIcon/>,
                to: `/storage-providers/${item.id}/test`,
                tooltip: 'Konfiguration testen',
                disabled: !canUpdateStorageProvider,
                disabledTooltip: permissions.getMissingPermissionTooltip(Permission.STORAGE_PROVIDER_UPDATE),
            },
        ];
    }, []);

    const noDataPlaceholder = useCallback((permissions: GenericListPagePermissionState<StorageProviderEntity>) => (
        <EmptyDataListPlaceholder
            title="Keine Speicheranbieter vorhanden"
            description="Speicheranbieter verbinden Prosuna mit Dateispeichern für Uploads, Anlagen und erzeugte Dokumente."
            addText="Neuen Speicheranbieter anlegen"
            onAdd={() => navigate('/storage-providers/new')}
            addDisabled={!permissions.canCreate}
            addDisabledTooltip={permissions.createDisabledTooltip}
        />
    ), [navigate]);

    return (
        <>
            <PageWrapper
                title="Speicheranbieter"
                fullWidth
                background
            >
                <GenericListPage<StorageProviderEntity>
                    defaultFilter="all"
                    filters={availableFilter}
                    header={header}
                    permissionCheck={storageProvidersListPermissionCheck}
                    searchLabel="Speicheranbieter suchen"
                    searchPlaceholder="Name der Konfiguration eingeben…"
                    preSearchElements={preSearchElements}
                    fetch={fetchStorageProviders}
                    columnIcon={columnIcon}
                    columnDefinitions={columnDefinitions}
                    getRowIdentifier={getRowIdentifier}
                    noDataPlaceholder={noDataPlaceholder}
                    noSearchResultsPlaceholder="Keine Speicheranbieter gefunden"
                    rowActionsCount={3}
                    rowActions={rowActions}
                    defaultSortField="name"
                />
            </PageWrapper>
        </>
    );
}
