import React, {useEffect, useState} from 'react';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {useNavigate} from 'react-router-dom';
import {type Department} from '../../../models/entities/department';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectMemberships, selectUser} from '../../../slices/user-slice';
import {type GridColDef} from '@mui/x-data-grid';
import {TablePageWrapper} from '../../../components/table-page-wrapper/table-page-wrapper';
import {delayPromise} from '../../../utils/with-delay';
import {useAdminMembershipGuard} from '../../../hooks/use-admin-membership-guard';
import {isAdmin} from '../../../utils/is-admin';
import {useApi} from '../../../hooks/use-api';
import {useDepartmentsApi} from '../../../hooks/use-departments-api';
import {UserRole} from '../../../data/user-role';

const columns: Array<GridColDef<Department>> = [
    {
        field: 'name',
        headerName: 'Name',
        flex: 1,
    },
];

export function DepartmentListPage(): JSX.Element {
    useAdminMembershipGuard();

    const api = useApi();
    const navigate = useNavigate();

    const user = useAppSelector(selectUser);
    const memberships = useAppSelector(selectMemberships);

    const [search, setSearch] = useState('');
    const [departments, setDepartments] = useState<Department[]>();

    const [isLoading, setIsLoading] = useState(false);
    const [loadError, setLoadError] = useState<string>();

    useEffect(() => {
        if (user != null && memberships != null && departments == null) {
            if (isAdmin(user)) {
                setIsLoading(true);
                setLoadError(undefined);

                delayPromise(useDepartmentsApi(api).list())
                    .then(setDepartments)
                    .catch((err) => {
                        console.error(err);
                        setLoadError('Die Liste der Fachbereiche konnte nicht geladen werden.');
                    })
                    .finally(() => {
                        setIsLoading(false);
                    });
            } else {
                setIsLoading(true);
                setLoadError(undefined);

                delayPromise(useDepartmentsApi(api).list({
                    ids: memberships.filter((m) => m.role === UserRole.Admin).map((m) => m.departmentId),
                }))
                    .then(setDepartments)
                    .catch((err) => {
                        console.error(err);
                        setLoadError('Die Liste der Fachbereiche konnte nicht geladen werden.');
                    })
                    .finally(() => {
                        setIsLoading(false);
                    });
            }
        }
    }, [memberships]);

    const filteredDepartments = departments == null ?
        undefined :
        departments
            .filter((dep) => dep.name.toLowerCase().includes(search.toLowerCase()));

    return (
        <TablePageWrapper
            title="Fachbereiche"
            isLoading={isLoading}
            error={loadError}

            hint={{
                text: 'Hier können Sie Fachbereiche anlegen, denen Sie später Ihre Formulare zuordnen.',
                moreLink: 'https://wiki.teamaivot.de/de/dokumentation/gover/benutzerhandbuch/konzepte/fachbereichskonzept' /* TODO: Link anpassen */,
            }}

            search={search}
            onSearchChange={setSearch}
            searchPlaceholder="Fachbereich suchen..."

            rows={filteredDepartments ?? []}
            columns={columns}
            onRowClick={(dep) => {
                navigate(`/departments/${dep.id}`);
            }}

            actions={
                isAdmin(user) ?
                    [{
                        label: 'Fachbereich hinzufügen',
                        icon: <AddOutlinedIcon/>,
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
