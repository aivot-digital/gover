import React, {useEffect, useState} from 'react';
import {TableWrapper} from '../../../../components/table-wrapper/table-wrapper';
import {type GridColDef} from '@mui/x-data-grid';
import {UserRoleLabels} from '../../../../data/user-role';
import {useNavigate} from 'react-router-dom';
import {AlertComponent} from '../../../../components/alert/alert-component';
import {selectUser} from '../../../../slices/user-slice';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {Skeleton} from '@mui/material';
import {delayPromise} from '../../../../utils/with-delay';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../../../slices/snackbar-slice';
import {useApi} from '../../../../hooks/use-api';
import {useMembershipsApi} from '../../../../hooks/use-memberships-api';
import {DepartmentMembershipWithDepartment} from '../../../../models/entities/department-membership';

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


export function UserEditPageMembershipsTab(): JSX.Element {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const api = useApi();

    const user = useAppSelector(selectUser);

    const [memberships, setMemberships] = useState<DepartmentMembershipWithDepartment[]>();
    const [search, setSearch] = useState('');

    useEffect(() => {
        if (user != null) {
            delayPromise(useMembershipsApi(api).listWithDepartments({user: user.id}))
                .then(setMemberships)
                .catch((err) => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Fehler beim Laden der Fachbereiche'));
                });
        }
    }, [user]);

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
                Sie sind noch keinem Fachbereich zugeordnet.
                Bitten Sie einen Administrator Sie zu einem Fachbereich hinzuzufügen.
            </AlertComponent>
        );
    }

    return (
        <TableWrapper
            columns={columns}
            rows={memberships ?? []}
            onRowClick={(membership) => {
                navigate(`/departments/${membership.departmentId}`);
            }}
            title="Fachbereiche und Rollen"
            search={search}
            searchPlaceholder="Fachbereich suchen"
            onSearchChange={setSearch}
            smallTitle={true}
        />
    );
}
