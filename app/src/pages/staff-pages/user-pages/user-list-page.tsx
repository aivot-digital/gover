import {useAuthGuard} from "../../../hooks/use-auth-guard";
import React, {useEffect, useState} from "react";
import {User} from "../../../models/entities/user";
import {UsersService} from "../../../services/users-service";
import {useNavigate} from "react-router-dom";
import {useUserGuard} from "../../../hooks/use-user-guard";
import {GridColDef} from "@mui/x-data-grid";
import {TablePageWrapper} from "../../../components/table-page-wrapper/table-page-wrapper";
import AddOutlinedIcon from "@mui/icons-material/AddOutlined";


const columns: GridColDef<User>[] = [
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
        valueGetter: params => params.row.admin ? 'Globale Administrator:in' : 'Standard Nutzer:in',
        flex: 1,
    },
];


export function UserListPage() {
    useAuthGuard();
    useUserGuard(user => user != null && user.admin);

    const navigate = useNavigate();

    useEffect(() => {
        UsersService
            .list()
            .then(setUsers);
    }, []);

    const [search, setSearch] = useState('');
    const [users, setUsers] = useState<User[]>();

    const filteredUsers = users == null ? undefined : users
        .filter(user => user.name.toLowerCase().includes(search.toLowerCase()));

    return (
        <TablePageWrapper
            title="Benutzerverwaltung"
            isLoading={filteredUsers == null}

            search={search}
            onSearchChange={setSearch}
            searchPlaceholder="Benutzer suchen..."

            rows={filteredUsers ?? []}
            columns={columns}
            onRowClick={user => navigate(`/users/${user.id}`)}

            actions={[{
                label: 'Benutzer:in hinzufügen',
                icon: <AddOutlinedIcon/>,
                onClick: () => navigate('/users/new'),
                tooltip: 'Neue Benutzer:in hinzufügen',
            }]}
        />
    );
}
