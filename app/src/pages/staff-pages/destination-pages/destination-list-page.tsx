import {useAuthGuard} from "../../../hooks/use-auth-guard";
import React, {useEffect, useState} from "react";
import {useNavigate} from "react-router-dom";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {Destination} from "../../../models/entities/destination";
import {DestinationsService} from "../../../services/destinations-service";
import {GridColDef} from "@mui/x-data-grid";
import {useUserGuard} from "../../../hooks/use-user-guard";
import {DestinationType, DestinationTypeIcons} from "../../../data/destination-type/destination-type";
import {TablePageWrapper} from "../../../components/table-page-wrapper/table-page-wrapper";
import AddOutlinedIcon from "@mui/icons-material/AddOutlined";

const columns: GridColDef<Destination>[] = [
    {
        field: 'type',
        headerName: 'Typ',
        renderCell: params => (
            <>
                <FontAwesomeIcon
                    icon={DestinationTypeIcons[params.row.type]}
                    style={{marginRight: '1em'}}
                />
                {params.row.type}
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
        valueGetter: params => params.row.type === DestinationType.HTTP ? params.row.apiAddress : params.row.mailTo,
        flex: 1,
    },
];


export function DestinationListPage() {
    useAuthGuard();
    useUserGuard(user => user?.admin ?? false);

    const navigate = useNavigate();

    const [search, setSearch] = useState('');
    const [destinations, setDestinations] = useState<Destination[]>();

    useEffect(() => {
        DestinationsService
            .list()
            .then(setDestinations);
    }, []);

    const filtered = destinations != null ? destinations.filter(dest => dest.name.toLowerCase().includes(search.toLowerCase())) : undefined;

    return (
        <TablePageWrapper
            title="Schnittstellen"
            isLoading={filtered == null}

            columns={columns}
            rows={filtered ?? []}
            onRowClick={dest => navigate(`/destinations/${dest.id}`)}

            search={search}
            searchPlaceholder="Schnittstelle suchen..."
            onSearchChange={setSearch}

            actions={[{
                label: 'Neue Schnittstelle',
                icon: <AddOutlinedIcon/>,
                tooltip: 'Neue Schnittstelle anlegen',
                onClick: () => navigate(`/destinations/new`),
            }]}
        />
    );
}
