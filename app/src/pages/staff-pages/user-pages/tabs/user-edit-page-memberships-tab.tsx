import React, {useEffect, useState} from 'react';
import {TableWrapper} from '../../../../components/table-wrapper/table-wrapper';
import {type GridColDef} from '@mui/x-data-grid';
import {UserRoleLabels} from '../../../../data/user-role';
import {Link, useNavigate} from 'react-router-dom';
import {AlertComponent} from '../../../../components/alert/alert-component';
import {type User} from '../../../../models/entities/user';
import {useApi} from '../../../../hooks/use-api';
import {useMembershipsApi} from '../../../../hooks/use-memberships-api';
import {DepartmentMembershipWithDepartment} from '../../../../models/entities/department-membership';
import {Skeleton} from "@mui/material";

const columns: Array<GridColDef<DepartmentMembershipWithDepartment>> = [
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
    const api = useApi();
    const navigate = useNavigate();

    const [memberships, setMemberships] = useState<DepartmentMembershipWithDepartment[]>();
    const [search, setSearch] = useState('');

    useEffect(() => {
        useMembershipsApi(api)
            .listWithDepartments({user: props.user.id})
            .then(setMemberships);
    }, []);

    if (memberships == null) {
        return (
            <Skeleton
                variant="rectangular"
                height={200}
            />
        );
    }

    if (memberships.length === 0) {
        return (
            <AlertComponent
                color="info"
                title="Keinem Fachbereich zugeordnet"
            >
                Diese Mitarbeiter:in ist noch keinem Fachbereich zugeordnet. Fügen Sie diese einem
                Ihrer <Link to="/departments">Fachbereiche</Link> hinzu.
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
                navigate(`/departments/${dep.departmentId}`);
            }}
            title="Fachbereiche und Rollen"
            search={search}
            searchPlaceholder="Fachbereich suchen"
            onSearchChange={setSearch}
            smallTitle={true}
        />
    );
}
