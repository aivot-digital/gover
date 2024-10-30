import React, {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {type GridColDef} from '@mui/x-data-grid';
import {TablePageWrapper} from '../../../components/table-page-wrapper/table-page-wrapper';
import {filterItems} from '../../../utils/filter-items';
import {type Theme} from '../../../models/entities/theme';
import {useAdminGuard} from '../../../hooks/use-admin-guard';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {useThemesApi} from '../../../hooks/use-themes-api';
import {useApi} from '../../../hooks/use-api';
import {useAppSelector} from "../../../hooks/use-app-selector";
import {selectSystemConfigValue} from "../../../slices/system-config-slice";
import {SystemConfigKeys} from "../../../data/system-config-keys";

export function ThemeListPage(): JSX.Element {
    useAdminGuard();

    const navigate = useNavigate();
    const api = useApi();

    const [search, setSearch] = useState('');
    const [themes, setThemes] = useState<Theme[]>();
    const [isLoading, setIsLoading] = useState(false);
    const [loadError, setLoadError] = useState<string>();

    const appThemeId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.theme));
    const columns: Array<GridColDef<Theme>> = [
        {
            field: 'name',
            headerName: 'Name',
            flex: 1,
        },
        {
            field: 'note',
            headerName: 'Hinweis',
            flex: 1,
            valueGetter: (params) => params.row.id === Number(appThemeId) ? "Aktives Farbschema der Gover-Instanz" : null,
        },
    ];

    useEffect(() => {
        setIsLoading(true);
        setLoadError(undefined);

        useThemesApi(api)
            .listThemes()
            .then(setThemes)
            .catch((err) => {
                console.error(err);
                setLoadError('Die Liste der Farbpaletten konnte nicht geladen werden.');
            })
            .finally(() => {
                setIsLoading(false);
            });
    }, []);

    const filtered = filterItems(themes, 'name', search);

    return (
        <TablePageWrapper
            title="Farbschemata"
            isLoading={isLoading}
            error={loadError}

            hint={{
                text: 'Hier können Sie Farbschemata anlegen, mit denen Sie die Farben Ihrer Formulare anpassen können.',
                moreLink: 'https://wiki.teamaivot.de/de/dokumentation/gover/benutzerhandbuch/home' /* TODO: Link anpassen */,
            }}

            columns={columns}
            rows={filtered ?? []}
            onRowClick={(dest) => {
                navigate(`/themes/${dest.id}`);
            }}

            search={search}
            searchPlaceholder="Farbschema suchen..."
            onSearchChange={setSearch}

            actions={[{
                label: 'Neues Farbschema',
                icon: <AddOutlinedIcon/>,
                tooltip: 'Neues Farbschema anlegen',
                link: '/themes/new',
            }]}
        />
    );
}
