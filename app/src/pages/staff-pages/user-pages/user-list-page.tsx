import { useAuthGuard } from '../../../hooks/use-auth-guard';
import React, { useEffect, useState } from 'react';
import { type User } from '../../../models/entities/user';
import { UsersService } from '../../../services/users-service';
import { faPlus } from '@fortawesome/pro-light-svg-icons';
import { useNavigate } from 'react-router-dom';
import { useUserGuard } from '../../../hooks/use-user-guard';
import { type GridColDef } from '@mui/x-data-grid';
import { TablePageWrapper } from '../../../components/table-page-wrapper/table-page-wrapper';
import { delayPromise } from '../../../utils/with-delay';
import { filterItems } from '../../../utils/filter-items';

const columns: Array<GridColDef<User>> = [
    {
        field: 'name',
        headerName: 'Name',
        flex: 1,
    },
    {
        field: 'email',
        headerName: 'E-Mail-Adresse',
        flex: 1,
    },
    {
        field: 'admin',
        headerName: 'Rolle',
        valueGetter: (params) => params.row.admin ? 'Globale Administrator:in' : 'Standard Nutzer:in',
        flex: 1,
    },
];

export function UserListPage(): JSX.Element {
    useAuthGuard();
    useUserGuard((user) => user?.admin ?? false);

    const navigate = useNavigate();

    const [search, setSearch] = useState('');
    const [users, setUsers] = useState<User[]>();

    const [isLoading, setIsLoading] = useState(true);
    const [loadingError, setLoadingError] = useState<string>();

    useEffect(() => {
        setIsLoading(true);
        setLoadingError(undefined);

        delayPromise(UsersService.list())
            .then(setUsers)
            .catch((err) => {
                console.error(err);
                setLoadingError('Die Liste der Mitarbeiter:innen konnte nicht geladen werden.');
            })
            .finally(() => {
                setIsLoading(false);
            });
    }, []);

    const filteredUsers = filterItems(users, 'name', search);

    return (
        <TablePageWrapper
            title="Mitarbeiter:innen"
            isLoading={ isLoading }
            error={ loadingError }

            search={ search }
            onSearchChange={ setSearch }
            searchPlaceholder="Suchen..."

            rows={ filteredUsers ?? [] }
            columns={ columns }
            onRowClick={ (user) => {
                navigate(`/users/${ user.id }`);
            } }

            actions={ [{
                label: 'Mitarbeiter:in hinzufügen',
                icon: faPlus,
                link: '/users/new',
                tooltip: 'Neue Mitarbeiter:in hinzufügen',
            }] }
        />
    );
}
