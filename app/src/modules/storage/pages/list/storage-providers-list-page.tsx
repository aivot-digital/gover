import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {useNavigate} from 'react-router-dom';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Typography} from '@mui/material';
import {EditOutlined} from '@mui/icons-material';
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

    const header = useMemo(() => ({
        icon: ModuleIcons.storage,
        title: 'Speicheranbieter',
        actions: [
            {
                label: 'Neuer Speicheranbieter',
                icon: <AddOutlinedIcon/>,
                to: '/storage-providers/new',
                variant: 'contained' as const,
            },
        ],
        helpDialog: {
            title: 'Hilfe zu Speicheranbietern',
            tooltip: 'Hilfe anzeigen',
            content: (
                <>
                    <Typography
                        variant="body1"
                        paragraph
                    >
                        Konfigurieren Sie hier Speicheranbieter, die in Ihrer Gover-Instanz global
                        verfügbar sein sollen.
                        Die erforderlichen Konfigurationsdaten erhalten Sie vom Zahlungsdienstleister
                        oder finden Sie in dessen Dokumentation.
                    </Typography>
                    <Typography
                        variant="body1"
                        paragraph
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

    const columnDefinitions = useMemo(() => [
        {
            field: 'name',
            headerName: 'Name',
            flex: 1,
            renderCell: (params: any) => (
                <CellLink
                    to={`/storage-providers/${params.id}`}
                    title="Konfiguration bearbeiten"
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

    const rowActions = useCallback((item: StorageProviderEntity) => [
        {
            icon: <EditOutlined/>,
            to: `/storage-providers/${item.id}`,
            tooltip: 'Konfiguration bearbeiten',
        },
    ], []);

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
                    searchLabel="Speicheranbieter suchen"
                    searchPlaceholder="Name der Konfiguration eingeben…"
                    preSearchElements={preSearchElements}
                    fetch={fetchStorageProviders}
                    columnIcon={columnIcon}
                    columnDefinitions={columnDefinitions}
                    getRowIdentifier={getRowIdentifier}
                    noDataPlaceholder={
                        <EmptyDataListPlaceholder
                            title="Noch keine Speicheranbieter angelegt"
                            description="Speicheranbieter verbinden Gover mit Dateispeichern für Uploads, Anlagen und erzeugte Dokumente."
                            addText="Neuen Speicheranbieter anlegen"
                            onAdd={() => navigate('/storage-providers/new')}
                        />
                    }
                    noSearchResultsPlaceholder="Keine Speicheranbieter gefunden"
                    rowActionsCount={1}
                    rowActions={rowActions}
                    defaultSortField="name"
                />
            </PageWrapper>
        </>
    );
}
