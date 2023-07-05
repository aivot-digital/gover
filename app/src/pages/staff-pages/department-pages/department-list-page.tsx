import {useAuthGuard} from "../../../hooks/use-auth-guard";
import React, {useEffect, useState} from "react";
import {faPlus} from "@fortawesome/pro-light-svg-icons";
import {useNavigate} from "react-router-dom";
import {Department} from "../../../models/entities/department";
import {DepartmentsService} from "../../../services/departments-service";
import {useUserGuard} from "../../../hooks/use-user-guard";
import {UserRole} from "../../../data/user-role";
import {useAppSelector} from "../../../hooks/use-app-selector";
import {selectUser} from "../../../slices/user-slice";
import {GridColDef} from "@mui/x-data-grid";
import {TablePageWrapper} from "../../../components/table-page-wrapper/table-page-wrapper";


const columns: GridColDef<Department>[] = [
    {
        field: 'name',
        headerName: 'Name',
        flex: 1,
    },
];


export function DepartmentListPage() {
    useAuthGuard();
    useUserGuard((user, memberships) => (user != null && user.admin) || (memberships != null && memberships.some(mem => mem.role === UserRole.Admin)));

    const navigate = useNavigate();

    const user = useAppSelector(selectUser);

    const [search, setSearch] = useState('');
    const [departments, setDepartments] = useState<Department[]>();

    useEffect(() => {
        if (user != null) {
            if (user.admin) {
                DepartmentsService
                    .list()
                    .then(setDepartments);
            } else {
                DepartmentsService
                    .list({
                        member: 'true',
                        roleId: UserRole.Admin,
                    })
                    .then(setDepartments);
            }
        }
    }, [user]);

    const filteredDepartments = departments == null ? undefined : departments
        .filter(dep => dep.name.toLowerCase().includes(search.toLowerCase()));

    return (
        <TablePageWrapper
            title="Fachbereichsverwaltung"
            isLoading={filteredDepartments == null}

            search={search}
            onSearchChange={setSearch}
            searchPlaceholder="Fachbereich suchen..."

            rows={filteredDepartments ?? []}
            columns={columns}
            onRowClick={dep => navigate(`/departments/${dep.id}`)}

            actions={
                user != null &&
                user.admin ? [{
                    label: 'Fachbereich hinzufügen',
                    icon: faPlus,
                    onClick: () => navigate('/departments/new'),
                    tooltip: 'Neuen Fachbereich hinzufügen',
                }] : []
            }
        />
    );
}
