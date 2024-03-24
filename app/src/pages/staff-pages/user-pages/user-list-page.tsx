import React, {useEffect, useState} from 'react';
import {type User} from '../../../models/entities/user';
import {useNavigate} from 'react-router-dom';
import {type GridColDef} from '@mui/x-data-grid';
import {TablePageWrapper} from '../../../components/table-page-wrapper/table-page-wrapper';
import {delayPromise} from '../../../utils/with-delay';
import {filterItems} from '../../../utils/filter-items';
import {useAdminGuard} from '../../../hooks/use-admin-guard';
import {useApi} from '../../../hooks/use-api';
import {useUsersApi} from '../../../hooks/use-users-api';
import AddOutlinedIcon from "@mui/icons-material/AddOutlined";
import {AppConfig} from "../../../app-config";

const columns: Array<GridColDef<User>> = [
    {
        field: 'enabled',
        headerName: 'Aktiv',
        type: 'boolean',
    },
    {
        field: 'firstName',
        headerName: 'Vorname',
        flex: 1,
    },
    {
        field: 'lastName',
        headerName: 'Nachname',
        flex: 1,
    },
    {
        field: 'email',
        headerName: 'E-Mail-Adresse',
        flex: 1,
    },
];

export function UserListPage(): JSX.Element {
    useAdminGuard();

    const api = useApi();
    const navigate = useNavigate();

    const [search, setSearch] = useState('');
    const [users, setUsers] = useState<User[]>();


    const [isLoading, setIsLoading] = useState(true);
    const [loadingError, setLoadingError] = useState<string>();

    useEffect(() => {
        setIsLoading(true);
        setLoadingError(undefined);

        delayPromise(useUsersApi(api).list())
            .then(setUsers)
            .catch((err) => {
                console.error(err);
                setLoadingError('Die Liste der Mitarbeiter:innen konnte nicht geladen werden.');
            })
            .finally(() => {
                setIsLoading(false);
            });
    }, []);

    const filteredUsers = filterItems(users, ['firstName', 'lastName'], search);

    return (
        <TablePageWrapper
            title="Mitarbeiter:innen"
            isLoading={isLoading}
            error={loadingError}

            hint={{
                text: 'Hier können Sie Mitarbeiter:innen anlegen, um diesen so Zugriff auf Gover zu gewähren.',
                moreLink: 'https://wiki.teamaivot.de/de/dokumentation/gover/benutzerhandbuch/home' /* TODO: Link anpassen */,
            }}

            search={search}
            onSearchChange={setSearch}
            searchPlaceholder="Suchen..."

            rows={filteredUsers ?? []}
            columns={columns}
            onRowClick={(user) => {
                navigate(`/users/${user.id}`);
            }}

            actions={[{
                label: 'Mitarbeiter:innen verwalten',
                icon: <AddOutlinedIcon/>,
                tooltip: 'Mitabeiter:innen verwalten',
                href: `${AppConfig.staff.host}/admin/${AppConfig.staff.realm}/console/#/${AppConfig.staff.realm}/users`,
                target: '_blank',
            }]}
        />
    );
}
