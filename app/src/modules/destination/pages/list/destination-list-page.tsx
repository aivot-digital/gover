import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {useNavigate} from 'react-router-dom';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Typography} from '@mui/material';
import {DescriptionOutlined, EditOutlined} from '@mui/icons-material';
import React, {useCallback, useMemo} from 'react';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {DestinationType, DestinationTypeIcons, DestinationTypeLabels} from '../../../../data/destination-type';
import {Destination} from '../../models/destination';
import {DestinationsApiService} from '../../destinations-api-service';
import DataObjectOutlinedIcon from '@mui/icons-material/DataObjectOutlined';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import {GenericListPropsFetchOptions} from '../../../../components/generic-list/generic-list-props';

export function DestinationListPage() {
    const navigate = useNavigate();
    const header = useMemo(() => ({
        icon: <DataObjectOutlinedIcon/>,
        title: 'Schnittstellen',
        actions: [
            {
                label: 'Neue Schnittstelle',
                icon: <AddOutlinedIcon/>,
                to: '/destinations/new',
                variant: 'contained' as const,
            },
        ],
        helpDialog: {
            title: 'Hilfe zu Schnittstellen',
            tooltip: 'Hilfe anzeigen',
            content: (
                <>
                    <Typography>
                        Schnittstellen dienen in der Gover-Anwendung zur Übertragung von durch Bürger:innen
                        gestellten Anträgen in Folgesysteme.
                    </Typography>
                    <ul>
                        <li>
                            <Typography variant="body1" paragraph>
                                Über eine E-Mail-Schnittstelle können Sie eingegangene Anträge an eine oder mehrere
                                E-Mail-Adressen senden lassen.
                            </Typography>
                        </li>
                        <li>
                            <Typography variant="body1" paragraph>
                                Über eine HTTP-Schnittstelle können Sie eingegangene Anträge an eine HTTP-Adresse
                                via POST-Anfrage übertragen lassen.
                            </Typography>
                        </li>
                        <li>
                            <Typography variant="body1" paragraph>
                                Über eine JavaScript-Schnittstelle können Sie eingegangene Anträge direkt mit eigenem JavaScript verarbeiten.
                            </Typography>
                        </li>
                    </ul>
                    <Typography sx={{mt: 2}}>
                        Falls einen von ihnen benötigte Schnittstelle nicht vorhanden ist, bietet Aivot
                        Ihnen die Möglichkeit, neue Schnittstellen zum System hinzufügen zu lassen.
                        So können Sie beispielsweise Ihre eigenen Fachverfahren oder Folgesysteme
                        anschließen. Bitte wenden Sie sich dazu an den Support.
                    </Typography>
                </>
            ),
        },
    }), []);

    const fetchDestinations = useCallback((options: GenericListPropsFetchOptions<Destination>) => {
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
    }, []);

    const columnDefinitions = useMemo(() => [
        {
            field: 'icon',
            headerName: '',
            renderCell: (params: any) => {
                const row = params.row as Destination;
                const Icon = DestinationTypeIcons[row.type];
                return (<CellContentWrapper><Icon/></CellContentWrapper>);
            },
            disableColumnMenu: true,
            width: 24,
            sortable: false,
        },
        {
            field: 'name',
            headerName: 'Name',
            flex: 1,
            renderCell: (params: any) => (
                <CellLink
                    to={`/destinations/${params.id}`}
                    title="Schnittstelle bearbeiten"
                >
                    {String(params.value)}
                </CellLink>
            ),
        },
        {
            field: 'type',
            headerName: 'Typ',
            flex: 1,
            renderCell: (params: any) => {
                const row = params.row as Destination;
                return DestinationTypeLabels[row.type];
            },
        },
        {
            field: 'target',
            headerName: 'Ziel',
            flex: 2,
            renderCell: (params: any) => params.row.type === DestinationType.Mail ? params.row.mailTo : params.row.apiAddress,
        },
    ], []);

    const getRowIdentifier = useCallback((row: Destination) => row.id.toString(), []);

    const rowActions = useCallback((item: Destination) => [
        {
            icon: <EditOutlined/>,
            to: `/destinations/${item.id}`,
            tooltip: 'Schnittstelle bearbeiten',
        },
        {
            icon: <DescriptionOutlined/>,
            to: `/destinations/${item.id}/forms`,
            tooltip: 'Formulare mit dieser Schnittstelle ansehen',
        },
    ], []);

    return (
        <PageWrapper
            title="Schnittstellen"
            fullWidth
            background
        >
            <GenericListPage<Destination>
                header={header}
                searchLabel="Schnittstelle suchen"
                searchPlaceholder="Name der Schnittstelle eingeben…"
                fetch={fetchDestinations}
                columnDefinitions={columnDefinitions}
                getRowIdentifier={getRowIdentifier}
                noDataPlaceholder={
                    <EmptyDataListPlaceholder
                        title="Noch keine Schnittstellen angelegt"
                        description="Schnittstellen übertragen Vorgangs- oder Formulardaten an angebundene Fachverfahren und externe Dienste."
                        addText="Neue Schnittstelle anlegen"
                        onAdd={() => navigate('/destinations/new')}
                    />
                }
                noSearchResultsPlaceholder="Keine Schnittstellen gefunden"
                rowActionsCount={3}
                rowActions={rowActions}
                defaultSortField="name"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}
