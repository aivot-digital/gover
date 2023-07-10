import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { faPlus } from '@fortawesome/pro-light-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { type Destination } from '../../../models/entities/destination';
import { DestinationsService } from '../../../services/destinations-service';
import { type GridColDef } from '@mui/x-data-grid';
import { DestinationType, DestinationTypeIcons } from '../../../data/destination-type/destination-type';
import { TablePageWrapper } from '../../../components/table-page-wrapper/table-page-wrapper';
import { filterItems } from '../../../utils/filter-items';
import { useAdminGuard } from '../../../hooks/use-admin-guard';

const columns: Array<GridColDef<Destination>> = [
    {
        field: 'type',
        headerName: 'Typ',
        renderCell: (params) => (
            <>
                <FontAwesomeIcon
                    icon={ DestinationTypeIcons[params.row.type] }
                    style={ {marginRight: '1em'} }
                />
                { params.row.type }
            </>
        ),
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
];


export function DestinationListPage(): JSX.Element {
    useAdminGuard();

    const navigate = useNavigate();

    const [search, setSearch] = useState('');
    const [destinations, setDestinations] = useState<Destination[]>();
    const [isLoading, setIsLoading] = useState(false);
    const [loadError, setLoadError] = useState<string>();

    useEffect(() => {
        setIsLoading(true);
        setLoadError(undefined);

        DestinationsService
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
            isLoading={ isLoading }
            error={ loadError }

            columns={ columns }
            rows={ filtered ?? [] }
            onRowClick={ (dest) => {
                navigate(`/destinations/${ dest.id }`);
            } }

            search={ search }
            searchPlaceholder="Schnittstelle suchen..."
            onSearchChange={ setSearch }

            actions={ [{
                label: 'Neue Schnittstelle',
                icon: faPlus,
                tooltip: 'Neue Schnittstelle anlegen',
                link: '/destinations/new',
            }] }
        />
    );
}
