import React, {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {type GridColDef} from '@mui/x-data-grid';
import {TablePageWrapper} from '../../../components/table-page-wrapper/table-page-wrapper';
import {AssetService} from '../../../services/asset-service';
import {delayPromise} from '../../../utils/with-delay';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';

interface Asset {
    id: string;
    link: string;
}

const columns: Array<GridColDef<Asset>> = [
    {
        field: 'id',
        headerName: 'Name',
        flex: 1,
    },
    {
        field: 'link',
        headerName: 'Link',
        flex: 1,
        renderCell: (params) => (
            <a
                href={params.row.link}
                target="_blank"
                onClick={(event) => {
                    event.stopPropagation();
                }}
                rel="noreferrer"
            >
                {params.row.link}
            </a>
        ),
    },
];

export function AssetListPage(): JSX.Element {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const [search, setSearch] = useState('');
    const [assets, setAssets] = useState<Asset[]>();
    const [isBusy, setIsBusy] = useState(false);

    useEffect(() => {
        setIsBusy(true);
        delayPromise(AssetService.list())
            .then((ass) => ass.map((name) => ({
                id: name,
                link: AssetService.getLink(name),
            })))
            .then(setAssets)
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Die Liste der Dokumente & Medieninhalte konnte nicht geladen werden.'));
            })
            .finally(() => {
                setIsBusy(false);
            });
    }, []);

    const filtered = assets != null ? assets.filter((ass) => ass.id.toLowerCase().includes(search.toLowerCase())) : undefined;

    return (
        <TablePageWrapper
            title="Dokumente & Medieninhalte"
            isLoading={isBusy}

            hint={{
                text: 'Hier können Sie Dokumente und Medieninhalte anlegen, auf die Sie dann in Ihren Formularen bezug nehmen können.',
                moreLink: 'https://wiki.teamaivot.de/de/dokumentation/gover/benutzerhandbuch/home' /* TODO: Link anpassen */,
            }}

            columns={columns}
            rows={filtered ?? []}
            onRowClick={(ass) => {
                navigate(`/assets/${ass.id}`);
            }}

            search={search}
            searchPlaceholder="Suchen..."
            onSearchChange={setSearch}

            actions={[{
                label: 'Dokument/Medieninhalt hinzufügen',
                icon: <AddOutlinedIcon/>,
                tooltip: 'Neues Dokument oder neuen Medieninhalt anlegen',
                onClick: () => {
                    navigate('/assets/new');
                },
            }]}
        />
    );
}
