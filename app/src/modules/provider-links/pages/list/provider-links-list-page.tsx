import React from 'react';
import {useAdminGuard} from '../../../../hooks/use-admin-guard';
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

export function ProviderLinksListPage() {
    useAdminGuard();

    return (
        <PageWrapper
            title="Links"
            fullWidth
            background
        >
            <GenericListPage<ProviderLink>
                header={{
                    icon: <InsertLinkOutlinedIcon />,
                    title: 'Links',
                    actions: [
                        {
                            label: 'Neuer Link',
                            icon: <AddOutlinedIcon />,
                            to: '/provider-links/new',
                            variant: 'contained',
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
                }}
                searchLabel="Link suchen"
                searchPlaceholder="Linktext eingeben…"
                fetch={(options) => {
                    return new ProviderLinksApiService(options.api)
                        .list(
                            options.page,
                            options.size,
                            options.sort,
                            options.order,
                            {text: options.search},
                        );
                }}
                columnDefinitions={[
                    {
                        field: 'icon',
                        headerName: '',
                        renderCell: () => <InsertLinkOutlinedIcon />,
                        disableColumnMenu: true,
                        width: 24,
                        sortable: false,
                    },
                    {
                        field: 'text',
                        headerName: 'Linktext',
                        flex: 1,
                        renderCell: (params) => (
                            <CellLink
                                to={`/provider-links/${params.id}`}
                                title={`Link bearbeiten`}
                            >
                                {String(params.value)}
                            </CellLink>
                        )
                    },
                    {
                        field: 'link',
                        headerName: 'Link',
                        flex: 2,
                    },
                ]}
                getRowIdentifier={row => row.id.toString()}
                noDataPlaceholder="Keine Links angelegt"
                noSearchResultsPlaceholder="Keine Links gefunden"
                rowActionsCount={2}
                rowActions={(item: ProviderLink) => [
                    {
                        icon: <EditOutlined />,
                        to: `/provider-links/${item.id}`,
                        tooltip: 'Link bearbeiten',
                    },
                    {
                        icon: <OpenInNewOutlinedIcon />,
                        href: `${item.link}`,
                        tooltip: 'Link aufrufen (öffnet in neuem Tab)',
                    }
                ]}
                defaultSortField="text"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}
