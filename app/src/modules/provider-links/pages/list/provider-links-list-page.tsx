import React, {useCallback, useMemo} from 'react';
import {useNavigate} from 'react-router-dom';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import AddOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
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
import {useCheckSystemPermission, useHasSystemPermission} from '../../../permissions/hooks/use-permissions';
import {Permission} from '../../../../data/permissions/permission';
import {formatMissingPermissionTooltip} from '../../../permissions/utils/permission-utils';

export function ProviderLinksListPage() {
    const navigate = useNavigate();
    useHasSystemPermission(Permission.SYSTEM_CONFIG_READ);
    const canCreateProviderLink = useCheckSystemPermission(Permission.SYSTEM_CONFIG_CREATE);
    const canUpdateProviderLink = useCheckSystemPermission(Permission.SYSTEM_CONFIG_UPDATE);

    const header = useMemo(() => ({
        icon: <InsertLinkOutlinedIcon />,
        title: 'Links',
        actions: [
            {
                label: 'Neuer Link',
                icon: <AddOutlinedIcon />,
                to: '/provider-links/new',
                variant: 'contained' as const,
                disabled: !canCreateProviderLink,
                disabledTooltip: formatMissingPermissionTooltip(Permission.SYSTEM_CONFIG_CREATE),
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
    }), [canCreateProviderLink]);

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

    const columnDefinitions = useMemo(() => [
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
                    title={canUpdateProviderLink ? 'Link bearbeiten' : 'Link anzeigen'}
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
    ], [canUpdateProviderLink]);

    const getRowIdentifier = useCallback((row: ProviderLink) => row.id.toString(), []);

    const rowActions = useCallback((item: ProviderLink) => [
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
    ], [canUpdateProviderLink]);

    return (
        <PageWrapper
            title="Links"
            fullWidth
            background
        >
            <GenericListPage<ProviderLink>
                header={header}
                searchLabel="Link suchen"
                searchPlaceholder="Linktext eingeben…"
                fetch={fetchProviderLinks}
                columnDefinitions={columnDefinitions}
                getRowIdentifier={getRowIdentifier}
                noDataPlaceholder={
                    <EmptyDataListPlaceholder
                        title="Noch keine Links angelegt"
                        description="Links verweisen auf externe Seiten oder interne Inhalte, die angemeldeten Nutzer:innen auf der Übersicht angeboten werden."
                        addText="Neuen Link anlegen"
                        onAdd={() => navigate('/provider-links/new')}
                        addDisabled={!canCreateProviderLink}
                        addDisabledTooltip={formatMissingPermissionTooltip(Permission.SYSTEM_CONFIG_CREATE)}
                    />
                }
                noSearchResultsPlaceholder="Keine Links gefunden"
                rowActionsCount={2}
                rowActions={rowActions}
                defaultSortField="text"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}
