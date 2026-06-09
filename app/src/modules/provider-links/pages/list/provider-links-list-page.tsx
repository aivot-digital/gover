import React, {useCallback, useMemo} from 'react';
import {useNavigate} from 'react-router-dom';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {Typography} from '@mui/material';
import {EditOutlined} from '@mui/icons-material';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import InsertLinkOutlinedIcon from '@mui/icons-material/InsertLinkOutlined';
import {ProviderLinksApiService} from '../../provider-links-api-service';
import {ProviderLink} from '../../models/provider-link';
import OpenInNewOutlinedIcon from '@mui/icons-material/OpenInNewOutlined';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import {useAccessGuard} from '../../../../hooks/use-admin-guard';
import Visibility from '@aivot/mui-material-symbols-400-outlined/dist/visibility/Visibility';
import {GenericListPropsFetchOptions} from '../../../../components/generic-list/generic-list-props';

export function ProviderLinksListPage() {
    const navigate = useNavigate();
    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });

    const header = useMemo(() => ({
        icon: <InsertLinkOutlinedIcon />,
        title: 'Links',
        actions: [
            {
                label: 'Neuer Link',
                icon: <AddOutlinedIcon />,
                to: '/provider-links/new',
                variant: 'contained' as const,
                disabled: !hasAccess,
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
    }), [hasAccess]);

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
                    title={hasAccess ? 'Link bearbeiten' : 'Link anzeigen'}
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
    ], [hasAccess]);

    const getRowIdentifier = useCallback((row: ProviderLink) => row.id.toString(), []);

    const rowActions = useCallback((item: ProviderLink) => [
        {
            icon: hasAccess ? <EditOutlined /> : <Visibility/>,
            to: `/provider-links/${item.id}`,
            tooltip: hasAccess ? 'Link bearbeiten' : 'Link anzeigen',
        },
        {
            icon: <OpenInNewOutlinedIcon />,
            href: `${item.link}`,
            tooltip: 'Link aufrufen (öffnet in neuem Tab)',
        },
    ], [hasAccess]);

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
                        addText={hasAccess ? "Neuen Link anlegen" : undefined}
                        onAdd={hasAccess ? () => navigate('/provider-links/new') : undefined}
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
