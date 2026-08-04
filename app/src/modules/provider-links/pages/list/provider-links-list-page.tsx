import React, {useCallback} from 'react';
import {useNavigate} from 'react-router-dom';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import AddOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import {
    GenericListPage,
    type GenericListPagePermissionConfig,
    type GenericListPagePermissionState,
} from '../../../../components/generic-list-page/generic-list-page';
import {Typography} from '@mui/material';
import EditOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import InsertLinkOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Link';
import {ProviderLinksApiService} from '../../provider-links-api-service';
import {ProviderLink} from '../../models/provider-link';
import OpenInNewOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import {GenericListPropsFetchOptions} from '../../../../components/generic-list/generic-list-props';
import {Permission} from '../../../../data/permissions/permission';

const providerLinksListPermissionCheck: GenericListPagePermissionConfig<ProviderLink> = {
    scope: {
        type: 'system',
    },
    read: Permission.SYSTEM_CONFIG_READ,
    create: Permission.SYSTEM_CONFIG_CREATE,
    update: Permission.SYSTEM_CONFIG_UPDATE,
};

export function ProviderLinksListPage() {
    const navigate = useNavigate();

    const header = useCallback((permissions: GenericListPagePermissionState<ProviderLink>) => ({
        icon: <InsertLinkOutlinedIcon />,
        title: 'Links',
        actions: [
            {
                label: 'Neuer Link',
                icon: <AddOutlinedIcon />,
                to: '/provider-links/new',
                variant: 'contained' as const,
                disabled: !permissions.canCreate,
                disabledTooltip: permissions.createDisabledTooltip,
            },
        ],
        helpDialog: {
            title: 'Hilfe zu Links',
            tooltip: 'Hilfe anzeigen',
            content: (
                <>
                    <Typography
                        variant="body1"
                        paragraph
                    >
                        Hier können Sie Verlinkungen anlegen, welche anschließend auf der Startseite der Gover-Instanz für angemeldete Nutzer:innen angezeigt werden.
                        Diese Funktion kann z. B. dafür genutzt werden, um auf externe Seiten oder interne Inhalte zu verweisen, die wichtig für Ihr Team sein könnten.
                    </Typography>
                </>
            ),
        },
    }), []);

    const fetchProviderLinks = useCallback((options: GenericListPropsFetchOptions<ProviderLink>) => {
        return new ProviderLinksApiService(options.api)
            .list(
                options.page,
                options.size,
                options.sort,
                options.order,
                {text: options.search},
            );
    }, []);

    const columnDefinitions = useCallback((permissions: GenericListPagePermissionState<ProviderLink>) => [
        {
            field: 'icon',
            headerName: '',
            renderCell: () => <CellContentWrapper><InsertLinkOutlinedIcon /></CellContentWrapper>,
            disableColumnMenu: true,
            width: 24,
            sortable: false,
        },
        {
            field: 'text',
            headerName: 'Linktext',
            flex: 1,
            renderCell: (params: any) => (
                <CellLink
                    to={`/provider-links/${params.id}`}
                    title={permissions.canUpdate(params.row) ? 'Link bearbeiten' : 'Link anzeigen'}
                >
                    {String(params.value)}
                </CellLink>
            ),
        },
        {
            field: 'link',
            headerName: 'Link',
            flex: 2,
        },
    ], []);

    const getRowIdentifier = useCallback((row: ProviderLink) => row.id.toString(), []);

    const rowActions = useCallback((item: ProviderLink, permissions: GenericListPagePermissionState<ProviderLink>) => {
        const canUpdateProviderLink = permissions.canUpdate(item);

        return [
            {
                icon: canUpdateProviderLink ? <EditOutlined /> : <Visibility/>,
                to: `/provider-links/${item.id}`,
                tooltip: canUpdateProviderLink ? 'Link bearbeiten' : 'Link anzeigen',
            },
            {
                icon: <OpenInNewOutlinedIcon />,
                href: `${item.link}`,
                tooltip: 'Link aufrufen (öffnet in neuem Tab)',
            },
        ];
    }, []);

    const noDataPlaceholder = useCallback((permissions: GenericListPagePermissionState<ProviderLink>) => (
        <EmptyDataListPlaceholder
            title="Keine Links vorhanden"
            description="Links verweisen auf externe Seiten oder interne Inhalte, die angemeldeten Nutzer:innen auf der Übersicht angeboten werden."
            addText="Neuen Link anlegen"
            onAdd={() => navigate('/provider-links/new')}
            addDisabled={!permissions.canCreate}
            addDisabledTooltip={permissions.createDisabledTooltip}
        />
    ), [navigate]);

    return (
        <PageWrapper
            title="Links"
            fullWidth
            background
        >
            <GenericListPage<ProviderLink>
                header={header}
                permissionCheck={providerLinksListPermissionCheck}
                searchLabel="Link suchen"
                searchPlaceholder="Linktext eingeben…"
                fetch={fetchProviderLinks}
                columnDefinitions={columnDefinitions}
                getRowIdentifier={getRowIdentifier}
                noDataPlaceholder={noDataPlaceholder}
                noSearchResultsPlaceholder="Keine Links gefunden"
                rowActionsCount={2}
                rowActions={rowActions}
                defaultSortField="text"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}
