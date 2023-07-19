import React, {useState} from 'react';
import {type ListApplication} from '../../../../models/entities/list-application';
import {type GridColDef} from '@mui/x-data-grid';
import {TableWrapper} from '../../../../components/table-wrapper/table-wrapper';
import {filterItems} from '../../../../utils/filter-items';
import {useNavigate} from 'react-router-dom';

const columns: Array<GridColDef<ListApplication>> = [
    {
        field: 'title',
        headerName: 'Title',
        flex: 1,
    },
    {
        field: 'version',
        headerName: 'Version',
        flex: 1,
    },
];

interface EditDepartmentPageMembersTabProps {
    applications: ListApplication[];
}


export function EditDepartmentPageFormsTab(props: EditDepartmentPageMembersTabProps): JSX.Element {
    const navigate = useNavigate();

    const [search, setSearch] = useState('');

    const filteredApplications = filterItems(props.applications, 'title', search);

    return (
        <TableWrapper
            columns={columns}
            rows={filteredApplications}
            onRowClick={(row) => {
                navigate(`/edit/${row.id}`);
            }}
            title="Die folgenden Formulare sind dem Fachbereich zugeordnet"
            search={search}
            searchPlaceholder="Formular suchen"
            onSearchChange={setSearch}
            smallTitle={true}
        />
    );
}
