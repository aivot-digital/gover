import React, {useEffect, useState} from 'react';
import {type User} from '../../../models/entities/user';
import {UsersService} from '../../../services/users-service';
import {useNavigate} from 'react-router-dom';
import {type GridColDef} from '@mui/x-data-grid';
import {TablePageWrapper} from '../../../components/table-page-wrapper/table-page-wrapper';
import {delayPromise} from '../../../utils/with-delay';
import {filterItems} from '../../../utils/filter-items';
import {useAdminGuard} from '../../../hooks/use-admin-guard';
import {Box, FormControlLabel, Switch} from '@mui/material';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';

const columns: Array<GridColDef<User>> = [
    {
        field: 'active',
        type: 'boolean',
        headerName: 'Aktiv',
    },
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
    useAdminGuard();

    const navigate = useNavigate();

    const [search, setSearch] = useState('');
    const [users, setUsers] = useState<User[]>();

    const [includeInactive, setIncludeInactive] = useState(false);

    const [isLoading, setIsLoading] = useState(true);
    const [loadingError, setLoadingError] = useState<string>();

    useEffect(() => {
        setIsLoading(true);
        setLoadingError(undefined);

        delayPromise(UsersService.list({
            excludeInactive: (!includeInactive).toString(),
        }))
            .then(setUsers)
            .catch((err) => {
                console.error(err);
                setLoadingError('Die Liste der Mitarbeiter:innen konnte nicht geladen werden.');
            })
            .finally(() => {
                setIsLoading(false);
            });
    }, [includeInactive]);

    const filteredUsers = filterItems(users, 'name', search);

    return (
        <TablePageWrapper
            title="Mitarbeiter:innen"
            isLoading={isLoading}
            error={loadingError}

            hint={{
                text: 'Hier können Sie Mitarbeiter:innen anlegen, um diesen so Zugriff auf Gover zu gewähren.',
                moreLink: 'https://wiki.teamaivot.de/de/dokumentation/gover/benutzerhandbuch' /* TODO: Link anpassen */,
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
                label: 'Mitarbeiter:in hinzufügen',
                icon: <AddOutlinedIcon/>,
                link: '/users/new',
                tooltip: 'Neue Mitarbeiter:in hinzufügen',
            }]}
        >
            <Box
                sx={{
                    display: 'flex',
                    my: 2,
                }}
            >
                <FormControlLabel
                    control={
                        <Switch
                            checked={includeInactive}
                            onChange={(event) => {
                                setIncludeInactive(event.target.checked);
                            }}
                        />
                    }
                    label="Inklusive inaktiver Mitarbeiter:innen anzeigen"
                />
            </Box>
        </TablePageWrapper>
    );
}
