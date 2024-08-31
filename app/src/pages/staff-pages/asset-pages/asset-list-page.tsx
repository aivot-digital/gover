import React, {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {type GridColDef} from '@mui/x-data-grid';
import {TablePageWrapper} from '../../../components/table-page-wrapper/table-page-wrapper';
import {delayPromise} from '../../../utils/with-delay';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {useApi} from '../../../hooks/use-api';
import {useAssetsApi} from '../../../hooks/use-assets-api';
import {Asset} from '../../../models/entities/asset';
import {filterItems} from '../../../utils/filter-items';


const columns: Array<GridColDef<Asset>> = [
    {
        field: 'filename',
        headerName: 'Name',
        flex: 1,
    },
    {
        field: 'contentType',
        headerName: 'Internet Media Typ',
        flex: 1,
        valueGetter: (params) => params.row.contentType ?? 'application/octet-stream',
    },
];

export function AssetListPage(): JSX.Element {
    const api = useApi();
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const [search, setSearch] = useState('');
    const [assets, setAssets] = useState<Asset[]>();
    const [isBusy, setIsBusy] = useState(false);

    useEffect(() => {
        setIsBusy(true);
        delayPromise(useAssetsApi(api).list())
            .then(setAssets)
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Die Liste der Dokumente & Medieninhalte konnte nicht geladen werden.'));
            })
            .finally(() => {
                setIsBusy(false);
            });
    }, []);

    const filtered = filterItems(assets, 'filename', search);

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
                navigate(`/assets/${ass.key}`);
            }}

            search={search}
            searchPlaceholder="Suchen..."
            onSearchChange={setSearch}
            getRowId={asset => asset.key}

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
