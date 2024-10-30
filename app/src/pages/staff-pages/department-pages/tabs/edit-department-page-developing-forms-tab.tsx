import React, {useState} from 'react';
import {type GridColDef} from '@mui/x-data-grid';
import {TableWrapper} from '../../../../components/table-wrapper/table-wrapper';
import {filterItems} from '../../../../utils/filter-items';
import {useNavigate} from 'react-router-dom';
import {FormListProjection} from '../../../../models/entities/form';

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
];

interface EditDepartmentPageMembersTabProps {
    applications: FormListProjection[];
}


export function EditDepartmentPageDevelopingFormsTab(props: EditDepartmentPageMembersTabProps): JSX.Element {
    const navigate = useNavigate();

    const [search, setSearch] = useState('');

    const filteredApplications = filterItems(props.applications, 'title', search);

    return (
        <TableWrapper
            columns={columns}
            rows={filteredApplications}
            onRowClick={(row) => {
                navigate(`/forms/${row.id}`);
            }}
            title="Die folgenden Formulare sind dem Fachbereich zur Entwicklung zugeordnet"
            search={search}
            searchPlaceholder="Formular suchen"
            onSearchChange={setSearch}
            smallTitle={true}
        />
    );
}
