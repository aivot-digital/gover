import {Chip, Typography} from '@mui/material';
import AddOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import EditOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import {useCallback, useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {
    GenericListPage,
    type GenericListPagePermissionConfig,
    type GenericListPagePermissionState,
} from '../../../components/generic-list-page/generic-list-page';
import {EmptyDataListPlaceholder} from '../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {CellLink} from '../../../components/cell-link/cell-link';
import {type GenericListPropsFetchOptions} from '../../../components/generic-list/generic-list-props';
import {Permission} from '../../../data/permissions/permission';
import {ModuleIcons} from '../../../shells/staff/data/module-icons';
import {CommunicationProvidersApiService} from '../communication-providers-api-service';
import {type CommunicationProvider, type CommunicationProviderDefinition} from '../models';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';

const communicationProvidersListPermissionCheck: GenericListPagePermissionConfig<CommunicationProvider> = {
    scope: {
        type: 'system',
    },
    read: Permission.COMMUNICATION_PROVIDER_READ,
    create: Permission.COMMUNICATION_PROVIDER_CREATE,
    update: Permission.COMMUNICATION_PROVIDER_UPDATE,
};

export function CommunicationProvidersListPage() {
    const navigate = useNavigate();
    const dispatch = useAppDispatch();
    const [definitions, setDefinitions] = useState<CommunicationProviderDefinition[]>([]);

    useEffect(() => {
        new CommunicationProvidersApiService()
            .listDefinitions()
            .then(setDefinitions)
            .catch(error => dispatch(showApiErrorSnackbar(error, 'Kommunikationsanbieter konnten nicht geladen werden.')));
    }, [dispatch]);

    const header = useCallback((permissions: GenericListPagePermissionState<CommunicationProvider>) => ({
        icon: ModuleIcons.communication,
        title: 'Kommunikationsanbieter',
        actions: [
            {
                label: 'Neuer Kommunikationsanbieter',
                icon: <AddOutlinedIcon/>,
                to: '/communication-providers/new',
                variant: 'contained' as const,
                disabled: !permissions.canCreate,
                disabledTooltip: permissions.createDisabledTooltip,
            },
        ],
        helpDialog: {
            title: 'Hilfe zu Kommunikationsanbietern',
            tooltip: 'Hilfe anzeigen',
            content: (
                <Typography variant="body1">
                    Kommunikationsanbieter sind globale Versanddienste, die Nutzerkontenanbietern beliebig oft
                    zugeordnet werden können.
                </Typography>
            ),
        },
    }), []);

    const fetchCommunicationProviders = useCallback((options: GenericListPropsFetchOptions<CommunicationProvider>) => (
        new CommunicationProvidersApiService()
            .listProvidersPage(options.page, options.size, options.sort, options.order, options.search)
            .catch(error => {
                dispatch(showApiErrorSnackbar(error, 'Kommunikationsanbieter konnten nicht geladen werden.'));
                throw error;
            })
    ), [dispatch]);

    const columnDefinitions = useCallback((permissions: GenericListPagePermissionState<CommunicationProvider>) => [
        {
            field: 'name',
            headerName: 'Name der Konfiguration',
            flex: 1,
            renderCell: (params: any) => (
                <CellLink
                    to={`/communication-providers/${params.row.id}`}
                    title={permissions.canUpdate(params.row) ? 'Konfiguration bearbeiten' : 'Konfiguration anzeigen'}
                >
                    {String(params.value)}
                </CellLink>
            ),
        },
        {
            field: 'communicationProviderDefinitionKey',
            headerName: 'Anbieter',
            flex: 1,
            renderCell: (params: any) => {
                const definitionName = definitions.find(definition => (
                    definition.key === params.row.communicationProviderDefinitionKey &&
                    definition.version === params.row.communicationProviderDefinitionVersion
                ))?.name ?? params.row.communicationProviderDefinitionKey;

                return (
                    <>
                        {definitionName} (Version {params.row.communicationProviderDefinitionVersion})
                        {params.row.isTestProvider && (
                            <Chip
                                label="Test"
                                color="warning"
                                variant="outlined"
                                size="small"
                                sx={{ml: 1}}
                            />
                        )}
                    </>
                );
            },
        },
        {
            field: 'description',
            headerName: 'Beschreibung',
            flex: 2,
        },
        {
            field: 'isEnabled',
            headerName: 'Status',
            renderCell: (params: any) => (
                <Chip
                    label={params.row.isEnabled ? 'Aktiv' : 'Inaktiv'}
                    color={params.row.isEnabled ? 'success' : 'default'}
                    variant="outlined"
                    size="small"
                />
            ),
        },
    ], [definitions]);

    const getRowIdentifier = useCallback((row: CommunicationProvider) => row.id.toString(), []);

    const rowActions = useCallback((item: CommunicationProvider, permissions: GenericListPagePermissionState<CommunicationProvider>) => {
        const canUpdateProvider = permissions.canUpdate(item);

        return [
            {
                icon: canUpdateProvider ? <EditOutlined/> : <Visibility/>,
                to: `/communication-providers/${item.id}`,
                tooltip: canUpdateProvider ? 'Konfiguration bearbeiten' : 'Konfiguration anzeigen',
            },
        ];
    }, []);

    const noDataPlaceholder = useCallback((permissions: GenericListPagePermissionState<CommunicationProvider>) => (
        <EmptyDataListPlaceholder
            title="Keine Kommunikationsanbieter vorhanden"
            description="Kommunikationsanbieter verbinden Prosuna mit Versanddiensten für Nachrichten an Kund:innen."
            addText="Neuen Kommunikationsanbieter anlegen"
            onAdd={() => navigate('/communication-providers/new')}
            addDisabled={!permissions.canCreate}
            addDisabledTooltip={permissions.createDisabledTooltip}
        />
    ), [navigate]);

    return (
        <PageWrapper title="Kommunikationsanbieter" fullWidth background>
            <GenericListPage<CommunicationProvider>
                header={header}
                permissionCheck={communicationProvidersListPermissionCheck}
                searchLabel="Kommunikationsanbieter suchen"
                searchPlaceholder="Name der Konfiguration eingeben…"
                fetch={fetchCommunicationProviders}
                columnIcon={ModuleIcons.communication}
                columnDefinitions={columnDefinitions}
                getRowIdentifier={getRowIdentifier}
                noDataPlaceholder={noDataPlaceholder}
                noSearchResultsPlaceholder="Keine Kommunikationsanbieter gefunden"
                rowActionsCount={1}
                rowActions={rowActions}
                defaultSortField="name"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}
