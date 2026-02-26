import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Typography} from '@mui/material';
import {EditOutlined} from '@mui/icons-material';
import {StorageProvidersApiService, type StorageProviderFilter} from '../../storage-providers-api-service';
import React, {type ReactNode, useEffect, useState} from 'react';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {type StorageProviderDefinition} from '../../entities/storage-provider-definition';
import {type StorageProviderEntity} from '../../entities/storage-provider-entity';
import {type StorageProviderStatus} from '../../enums/storage-provider-status';
import {StorageStatusChip} from '../../components/storage-status-chip';

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
        label: 'Read-only Speicher',
        value: 'readOnlyStorage',
    },
];

export function StorageProvidersListPage(): ReactNode {
    const [definitions, setDefinitions] = useState<StorageProviderDefinition[]>([]);

    useEffect(() => {
        new StorageProvidersApiService()
            .listDefinitions()
            .then(setDefinitions)
            .catch(console.error);
    }, []);

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
                    header={{
                        icon: ModuleIcons.storage,
                        title: 'Speicheranbieter',
                        actions: [
                            {
                                label: 'Neuer Speicheranbieter',
                                icon: <AddOutlinedIcon/>,
                                to: '/storage-providers/new',
                                variant: 'contained',
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
                    }}
                    searchLabel="Speicheranbieter suchen"
                    searchPlaceholder="Name der Konfiguration eingeben…"
                    fetch={(options) => {
                        const filter: Partial<StorageProviderFilter> = {};
                        if (options.search) {
                            filter.name = options.search;
                        }
                        if (options.filter === 'systemProvider') {
                            filter.systemProvider = true;
                        } else if (options.filter === 'readOnlyStorage') {
                            filter.readOnlyStorage = true;
                        }
                        return new StorageProvidersApiService()
                            .list(options.page, options.size, options.sort, options.order, filter);
                    }}
                    columnIcon={ModuleIcons.storage}
                    columnDefinitions={[
                        {
                            field: 'name',
                            headerName: 'Name',
                            flex: 1,
                            renderCell: (params) => (
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
                            valueGetter: (_, row) => {
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
                            renderCell: (params) => {
                                return (
                                    <CellContentWrapper>
                                        <StorageStatusChip
                                            status={params.value as StorageProviderStatus}
                                        />
                                    </CellContentWrapper>
                                );
                            },
                        },
                    ]}
                    getRowIdentifier={(row) => row.id.toString()}
                    noDataPlaceholder="Keine Speicheranbieter angelegt"
                    noSearchResultsPlaceholder="Keine Speicheranbieter gefunden"
                    rowActionsCount={1}
                    rowActions={(item: StorageProviderEntity) => [
                        {
                            icon: <EditOutlined/>,
                            to: `/storage-providers/${item.id}`,
                            tooltip: 'Konfiguration bearbeiten',
                        },
                    ]}
                    defaultSortField="name"
                    disableFullWidthToggle={true}
                />
            </PageWrapper>
        </>
    );
}