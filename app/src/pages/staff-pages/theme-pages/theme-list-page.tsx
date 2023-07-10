import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { faPlus } from '@fortawesome/pro-light-svg-icons';
import { type GridColDef } from '@mui/x-data-grid';
import { TablePageWrapper } from '../../../components/table-page-wrapper/table-page-wrapper';
import { filterItems } from '../../../utils/filter-items';
import { ThemesService } from '../../../services/themes-service';
import { type Theme } from '../../../models/entities/theme';
import { useAdminGuard } from '../../../hooks/use-admin-guard';

const columns: Array<GridColDef<Theme>> = [
    {
        field: 'name',
        headerName: 'Name',
        flex: 1,
    },
];

export function ThemeListPage(): JSX.Element {
    useAdminGuard();

    const navigate = useNavigate();

    const [search, setSearch] = useState('');
    const [themes, setThemes] = useState<Theme[]>();
    const [isLoading, setIsLoading] = useState(false);
    const [loadError, setLoadError] = useState<string>();

    useEffect(() => {
        setIsLoading(true);
        setLoadError(undefined);

        ThemesService
            .list()
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
            title="Farbpaletten"
            isLoading={ isLoading }
            error={ loadError }

            columns={ columns }
            rows={ filtered ?? [] }
            onRowClick={ (dest) => {
                navigate(`/themes/${ dest.id }`);
            } }

            search={ search }
            searchPlaceholder="Farbpalette suchen..."
            onSearchChange={ setSearch }

            actions={ [{
                label: 'Neue Farbpalette',
                icon: faPlus,
                tooltip: 'Neue Farbpalette anlegen',
                link: '/themes/new',
            }] }
        />
    );
}
