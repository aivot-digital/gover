import {useAuthGuard} from "../../../hooks/use-auth-guard";
import React, {useEffect, useState} from "react";
import {useNavigate} from "react-router-dom";
import {faPlus,} from "@fortawesome/pro-light-svg-icons";
import {GridColDef} from "@mui/x-data-grid";
import {useUserGuard} from "../../../hooks/use-user-guard";
import {TablePageWrapper} from "../../../components/table-page-wrapper/table-page-wrapper";
import {AssetService} from "../../../services/asset-service";

interface Asset {
    id: string;
    link: string;
}

const columns: GridColDef<Asset>[] = [
    {
        field: 'id',
        headerName: 'Name',
        flex: 1,
    },
    {
        field: 'link',
        headerName: 'Link',
        flex: 1,
        renderCell: params => (
            <a
                href={params.row.link}
                target="_blank"
                onClick={event => event.stopPropagation()}
            >
                {params.row.link}
            </a>
        )
    },
];


export function AssetListPage() {
    useAuthGuard();
    useUserGuard(user => user?.admin ?? false);

    const navigate = useNavigate();

    const [search, setSearch] = useState('');
    const [assets, setAssets] = useState<Asset[]>();


    useEffect(() => {
        AssetService
            .list()
            .then(ass => ass.map(name => ({
                id: name,
                link: AssetService.getLink(name),
            })))
            .then(setAssets);
    }, []);

    const filtered = assets != null ? assets.filter(ass => ass.id.toLowerCase().includes(search.toLowerCase())) : undefined;

    return (
        <TablePageWrapper
            title="Anlagen"
            isLoading={filtered == null}

            columns={columns}
            rows={filtered ?? []}
            onRowClick={ass => navigate(`/assets/${ass.id}`)}

            search={search}
            searchPlaceholder="Anlage suchen..."
            onSearchChange={setSearch}

            actions={[{
                label: 'Neue Anlage',
                icon: faPlus,
                tooltip: 'Neue Anlage anlegen',
                onClick: () => navigate(`/assets/new`),
            }]}
        />
    );
}
