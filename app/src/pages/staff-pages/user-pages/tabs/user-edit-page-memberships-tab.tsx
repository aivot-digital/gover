import React, {useEffect, useState} from 'react';
import {TableWrapper} from '../../../../components/table-wrapper/table-wrapper';
import {type GridColDef} from '@mui/x-data-grid';
import {type DepartmentMembershipWithDepartmentDto} from '../../../../models/dtos/department-membership-with-department-dto';
import {UserRoleLabels} from '../../../../data/user-role';
import {UsersService} from '../../../../services/users-service';
import {Link, useNavigate} from 'react-router-dom';
import {AlertComponent} from '../../../../components/alert/alert-component';
import {User} from '../../../../models/entities/user';
import {filterItems} from '../../../../utils/filter-items';

const columns: Array<GridColDef<DepartmentMembershipWithDepartmentDto>> = [
    {
        field: 'name',
        headerName: 'Fachbereich',
        flex: 1,
        valueGetter: (params) => params.row.department.name,
    },
    {
        field: 'role',
        headerName: 'Rolle',
        flex: 1,
        valueGetter: (params) => UserRoleLabels[params.row.role],
    },
];

interface UserEditPageMembershipsTabProps {
    user: User;
}

export function UserEditPageMembershipsTab(props: UserEditPageMembershipsTabProps): JSX.Element {
    const navigate = useNavigate();

    const [memberships, setMemberships] = useState<DepartmentMembershipWithDepartmentDto[]>([]);
    const [search, setSearch] = useState('');

    useEffect(() => {
        UsersService
            .listMemberships(props.user.id)
            .then(setMemberships);
    }, []);

    if (props.user.admin) {
        return (
            <AlertComponent
                color="info"
                title="Globale Administrator:in"
            >
                Diese Mitarbeiter:in ist globale Administrator:in und hat uneingeschränkten Zugriff auf alle Fachbereiche.
            </AlertComponent>
        );
    }

    if (memberships.length === 0) {
        return (
            <AlertComponent
                color="info"
                title="Keinem Fachbereich zugeordnet"
            >
                Dieser Benutzer ist noch keinem Fachbereich zugeordnet.
                Fügen Sie diesen in einem Ihrer <Link to="/departments">Fachbereiche</Link> hinzu.
            </AlertComponent>
        );
    }

    return (
        <TableWrapper
            columns={columns}
            rows={memberships.filter((value) => {
                return search.length === 0 || value.department.name.toLowerCase().includes(search.toLowerCase());
            })}
            onRowClick={(dep) => {
                navigate(`/departments/${dep.department.id}`);
            }}
            title="Fachbereiche und Rollen"
            search={search}
            searchPlaceholder="Fachbereich suchen"
            onSearchChange={setSearch}
            smallTitle={true}
        />
    );
}
