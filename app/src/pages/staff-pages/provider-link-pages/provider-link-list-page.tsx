import {useAuthGuard} from "../../../hooks/use-auth-guard";
import React, {useEffect, useState} from "react";
import {useNavigate} from "react-router-dom";
import {GridColDef} from "@mui/x-data-grid";
import {useUserGuard} from "../../../hooks/use-user-guard";
import {TablePageWrapper} from "../../../components/table-page-wrapper/table-page-wrapper";
import {ProviderLinksService} from "../../../services/provider-links-service";
import {ProviderLink} from "../../../models/entities/provider-link";
import AddOutlinedIcon from "@mui/icons-material/AddOutlined";


const columns: GridColDef<ProviderLink>[] = [
    {
        field: 'text',
        headerName: 'Text',
        flex: 1,
    },
    {
        field: 'link',
        headerName: 'Link',
        flex: 1,
    },
];


export function ProviderLinkListPage() {
    useAuthGuard();
    useUserGuard(user => user?.admin ?? false);

    const navigate = useNavigate();

    const [search, setSearch] = useState('');
    const [destinations, setDestinations] = useState<ProviderLink[]>();


    useEffect(() => {
        ProviderLinksService
            .list()
            .then(setDestinations);
    }, []);

    const filtered = destinations != null ? destinations.filter(dest => dest.text.toLowerCase().includes(search.toLowerCase())) : undefined;

    return (
        <TablePageWrapper
            title="Links"
            isLoading={filtered == null}

            columns={columns}
            rows={filtered ?? []}
            onRowClick={dest => navigate(`/provider-links/${dest.id}`)}

            search={search}
            searchPlaceholder="Link suchen..."
            onSearchChange={setSearch}

            actions={[{
                label: 'Neuer Link',
                icon: <AddOutlinedIcon/>,
                tooltip: 'Neuen Link anlegen',
                onClick: () => navigate(`/provider-links/new`),
            }]}
        />
    );
}
