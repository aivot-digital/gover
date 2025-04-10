import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Typography} from '@mui/material';
import {DescriptionOutlined, EditOutlined} from '@mui/icons-material';
import React from 'react';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {DestinationType, DestinationTypeIcons} from '../../../../data/destination-type';
import {Destination} from '../../models/destination';
import {DestinationsApiService} from '../../destinations-api-service';
import DataObjectOutlinedIcon from '@mui/icons-material/DataObjectOutlined';
import {useAdminGuard} from '../../../../hooks/use-admin-guard';

export function DestinationListPage() {
    useAdminGuard();

    return (
        <PageWrapper
            title="Schnittstellen"
            fullWidth
            background
        >
            <GenericListPage<Destination>
                header={{
                    icon: <DataObjectOutlinedIcon />,
                    title: 'Schnittstellen',
                    actions: [
                        {
                            label: 'Neue Schnittstelle',
                            icon: <AddOutlinedIcon />,
                            to: '/destinations/new',
                            variant: 'contained',
                        },
                    ],
                    helpDialog: {
                        title: 'Hilfe zu Schnittstellen',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Schnittstellen dienen in der Gover-Anwendung zur Übertragung von durch Bürger:innen gestellten Anträgen in Folgesysteme.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Über eine E-Mail-Schnittstelle können Sie eingegangene Anträge an eine oder mehrere E-Mail-Adressen senden lassen.
                                    Über eine HTTP-Schnittstelle können Sie eingegangene Anträge an eine HTTP-Adresse via POST-Anfrage übertragen lassen.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Falls einen von ihnen benötigte Schnittstelle nicht vorhanden ist, bietet Aivot Ihnen die Möglichkeit, neue Schnittstellen zum System hinzufügen zu lassen.
                                    So können Sie beispielsweise Ihre eigenen Fachverfahren oder Folgesysteme anschließen. Bitte wenden Sie sich dazu an den Support.
                                </Typography>
                            </>
                        ),
                    },
                }}
                searchLabel="Schnittstelle suchen"
                searchPlaceholder="Name der Schnittstelle eingeben…"
                fetch={(options) => {
                    return new DestinationsApiService(options.api)
                        .list(
                            options.page,
                            options.size,
                            options.sort,
                            options.order,
                            {
                                name: options.search,
                            },
                        );
                }}
                columnDefinitions={[
                    {
                        field: 'icon',
                        headerName: '',
                        renderCell: (params) => {
                            const Icon = DestinationTypeIcons[params.row.type];
                            return (<Icon />);
                        },
                        disableColumnMenu: true,
                        width: 24,
                        sortable: false,
                    },
                    {
                        field: 'name',
                        headerName: 'Name',
                        flex: 1,
                        renderCell: (params) => (
                            <CellLink
                                to={`/destinations/${params.id}`}
                                title={`Schnittstelle bearbeiten`}
                            >
                                {String(params.value)}
                            </CellLink>
                        ),
                    },
                    {
                        field: 'type',
                        headerName: 'Typ',
                        flex: 0.5,
                    },
                    {
                        field: 'target',
                        headerName: 'Ziel',
                        renderCell: (params) => params.row.type === DestinationType.HTTP ? params.row.apiAddress : params.row.mailTo,
                        flex: 2,
                    },
                ]}
                getRowIdentifier={row => row.id.toString()}
                noDataPlaceholder="Keine Schnittstellen angelegt"
                noSearchResultsPlaceholder="Keine Schnittstellen gefunden"
                rowActionsCount={3}
                rowActions={(item: Destination) => [
                    {
                        icon: <EditOutlined />,
                        to: `/destinations/${item.id}`,
                        tooltip: 'Schnittstelle bearbeiten',
                    },
                    {
                        icon: <DescriptionOutlined />,
                        to: `/destinations/${item.id}/forms`,
                        tooltip: 'Formulare mit dieser Schnittstelle ansehen',
                    },
                ]}
                defaultSortField="name"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}