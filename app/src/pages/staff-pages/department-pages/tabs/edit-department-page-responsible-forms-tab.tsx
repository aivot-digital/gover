import React, {useEffect, useState} from 'react';
import {type GridColDef} from '@mui/x-data-grid';
import {TableWrapper} from '../../../../components/table-wrapper/table-wrapper';
import {filterItems} from '../../../../utils/filter-items';
import {useNavigate} from 'react-router-dom';
import {FormListProjection} from '../../../../models/entities/form';
import {SelectFieldComponentOption} from "../../../../components/select-field/select-field-component-option";
import {useDepartmentsApi} from "../../../../hooks/use-departments-api";
import {showErrorSnackbar} from "../../../../slices/snackbar-slice";
import {useAppDispatch} from "../../../../hooks/use-app-dispatch";
import {useApi} from "../../../../hooks/use-api";

const columns: Array<GridColDef<FormListProjection>> = [
    {
        field: 'title',
        headerName: 'Titel',
        flex: 1,
    },
    {
        field: 'version',
        headerName: 'Version',
        flex: 1,
    },
    {
        field: 'developingDepartment',
        headerName: 'Entwickelnder Fachbereich',
        flex: 1,
    },
];

interface EditDepartmentPageMembersTabProps {
    applications: FormListProjection[];
}


export function EditDepartmentPageResponsibleFormsTab(props: EditDepartmentPageMembersTabProps): JSX.Element {
    const dispatch = useAppDispatch();
    const api = useApi();
    const navigate = useNavigate();

    const [search, setSearch] = useState('');

    const [departments, setDepartments] = useState<SelectFieldComponentOption[]>([]);
    const applicationsWithDepartment = props.applications.map(obj => ({
        ...obj,
        developingDepartment: departments.find(item => item.value === obj.developingDepartmentId?.toString())?.label ?? undefined
    }))
    const filteredApplications = filterItems(applicationsWithDepartment, 'title', search);

    useEffect(() => {
        const idSet = props.applications.reduce((idSet, app) => {
            if (app.developingDepartmentId != null) {
                idSet.add(app.developingDepartmentId);
            }
            return idSet;
        }, new Set<number>());

        useDepartmentsApi(api)
            .list({
                ids: Array.from(idSet),
            })
            .then((deps) => deps.map((department) => ({
                value: department.id.toString(),
                label: department.name,
            })))
            .then(setDepartments)
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Laden der Fachbereiche!'));
            });
    }, [props.applications]);

    return (
        <TableWrapper
            columns={columns}
            rows={filteredApplications}
            onRowClick={(row) => {
                navigate(`/forms/${row.id}`);
            }}
            title="Die folgenden Formulare sind dem Fachbereich zur Zuständigkeit zugeordnet"
            search={search}
            searchPlaceholder="Formular suchen"
            onSearchChange={setSearch}
            smallTitle={true}
        />
    );
}
