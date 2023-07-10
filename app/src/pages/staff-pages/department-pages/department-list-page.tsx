import React, { useEffect, useState } from 'react';
import { faPlus } from '@fortawesome/pro-light-svg-icons';
import { useNavigate } from 'react-router-dom';
import { type Department } from '../../../models/entities/department';
import { DepartmentsService } from '../../../services/departments-service';
import { UserRole } from '../../../data/user-role';
import { useAppSelector } from '../../../hooks/use-app-selector';
import { selectUser } from '../../../slices/user-slice';
import { type GridColDef } from '@mui/x-data-grid';
import { TablePageWrapper } from '../../../components/table-page-wrapper/table-page-wrapper';
import { delayPromise } from '../../../utils/with-delay';
import { useAdminMembershipGuard } from '../../../hooks/use-admin-membership-guard';

const columns: Array<GridColDef<Department>> = [
    {
        field: 'name',
        headerName: 'Name',
        flex: 1,
    },
];

export function DepartmentListPage(): JSX.Element {
    useAdminMembershipGuard();

    const navigate = useNavigate();

    const user = useAppSelector(selectUser);

    const [search, setSearch] = useState('');
    const [departments, setDepartments] = useState<Department[]>();

    const [isLoading, setIsLoading] = useState(false);
    const [loadError, setLoadError] = useState<string>();

    useEffect(() => {
        if (user != null) {
            setIsLoading(true);
            setLoadError(undefined);

            const filter = user.admin ?
                undefined :
                {
                    member: 'true',
                    roleId: UserRole.Admin,
                };

            delayPromise(DepartmentsService.list(filter))
                .then(setDepartments)
                .catch((err) => {
                    console.error(err);
                    setLoadError('Die Liste der Fachbereiche konnte nicht geladen werden.');
                })
                .finally(() => {
                    setIsLoading(false);
                });
        }
    }, [user]);

    const filteredDepartments = departments == null ?
        undefined :
        departments
            .filter((dep) => dep.name.toLowerCase().includes(search.toLowerCase()));

    return (
        <TablePageWrapper
            title="Fachbereiche"
            isLoading={ isLoading }
            error={ loadError }

            search={ search }
            onSearchChange={ setSearch }
            searchPlaceholder="Fachbereich suchen..."

            rows={ filteredDepartments ?? [] }
            columns={ columns }
            onRowClick={ (dep) => {
                navigate(`/departments/${ dep.id }`);
            } }

            actions={
                (user?.admin ?? false) ?
                    [{
                        label: 'Fachbereich hinzufügen',
                        icon: faPlus,
                        onClick: () => {
                            navigate('/departments/new');
                        },
                        tooltip: 'Neuen Fachbereich hinzufügen',
                    }] :
                    []
            }
        />
    );
}
