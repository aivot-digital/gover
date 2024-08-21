import React, {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {type Destination} from '../../../models/entities/destination';
import {type GridColDef} from '@mui/x-data-grid';
import {DestinationType, DestinationTypeIcons} from '../../../data/destination-type';
import {TablePageWrapper} from '../../../components/table-page-wrapper/table-page-wrapper';
import {filterItems} from '../../../utils/filter-items';
import {useAdminGuard} from '../../../hooks/use-admin-guard';
import {format, parseISO} from 'date-fns';
import {useApi} from '../../../hooks/use-api';
import {useDestinationsApi} from '../../../hooks/use-destinations-api';

const columns: Array<GridColDef<Destination>> = [
    {
        field: 'type',
        headerName: 'Typ',
        renderCell: (params) => {
            const Icon = DestinationTypeIcons[params.row.type];
            return (
                <>
                    <Icon
                        sx={{marginRight: '1em'}}
                    />
                    {params.row.type}
                </>
            );
        },
        flex: 1,
    },
    {
        field: 'name',
        headerName: 'Name',
        flex: 1,
    },
    {
        field: 'target',
        headerName: 'Ziel',
        valueGetter: (params) => params.row.type === DestinationType.HTTP ? params.row.apiAddress : params.row.mailTo,
        flex: 1,
    },
    {
        field: 'updated',
        headerName: 'Letzte Aktualisierung',
        flex: 1,
        type: 'date',
        valueFormatter: (params) => format(parseISO(params.value), 'dd.MM.yyyy HH:mm'),
    },
];


export function DestinationListPage(): JSX.Element {
    useAdminGuard();

    const api = useApi();
    const navigate = useNavigate();

    const [search, setSearch] = useState('');
    const [destinations, setDestinations] = useState<Destination[]>();
    const [isLoading, setIsLoading] = useState(false);
    const [loadError, setLoadError] = useState<string>();

    useEffect(() => {
        setIsLoading(true);
        setLoadError(undefined);

        useDestinationsApi(api)
            .list()
            .then(setDestinations)
            .catch((err) => {
                console.error(err);
                setLoadError('Die Liste der Schnittstellen konnte nicht geladen werden.');
            })
            .finally(() => {
                setIsLoading(false);
            });
    }, []);

    const filtered = filterItems(destinations, 'name', search);

    return (
        <TablePageWrapper
            title="Schnittstellen"
            isLoading={isLoading}
            error={loadError}

            hint={{
                text: 'Hier können Sie Schnittstellen anlegen, an die abgesendete Anträge gesendet werden können.',
                moreLink: 'https://wiki.teamaivot.de/de/dokumentation/gover/benutzerhandbuch/konzepte/schnittstellenkonzept' /* TODO: Link anpassen */,
            }}

            columns={columns}
            rows={filtered ?? []}
            onRowClick={(dest) => {
                navigate(`/destinations/${dest.id}`);
            }}

            search={search}
            searchPlaceholder="Schnittstelle suchen..."
            onSearchChange={setSearch}

            actions={[{
                label: 'Neue Schnittstelle',
                icon: <AddOutlinedIcon/>,
                tooltip: 'Neue Schnittstelle anlegen',
                link: '/destinations/new',
            }]}
        />
    );
}
