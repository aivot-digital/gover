import React, {useContext} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {GenericList} from '../../../../components/generic-list/generic-list';
import {GridColDef} from '@mui/x-data-grid';
import {EditOutlined} from '@mui/icons-material';
import {Box, Typography} from '@mui/material';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {FormStatusChipGroup} from '../../../forms/components/form-status-chip-group';
import {DepartmentEntity} from '../../entities/department-entity';
import {FormApiService} from '../../../forms/services/form-api-service';
import {FormEntity} from '../../../forms/entities/form-entity';

const filters = [
    {
        label: 'Im Eigentum',
        value: 'developed',
    },
    {
        label: 'Für diese Organisationseinheit freigegeben',
        value: 'accessible',
    },
];

const columns: GridColDef<FormEntity>[] = [
    {
        field: 'internalTitle',
        headerName: 'Titel des Formulars',
        flex: 2,
        renderCell: (params) => (
            <CellLink
                to={`/forms/${params.id}`}
                title="Formular anzeigen"
            >
                {String(params.value)}
            </CellLink>
        ),
    },
    {
        field: 'version',
        headerName: 'Version',
        flex: 1,
    },
    {
        field: 'status',
        headerName: 'Status',
        flex: 1,
        renderCell: (params) => (
            <FormStatusChipGroup
                form={params.row}
            />
        ),
    },
];

export function DepartmentsDetailsPageForms() {
    const {
        item,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<DepartmentEntity, undefined>;

    if (item == null) {
        return null;
    }

    return (
        <Box>
            <Typography
                variant="h5"
                sx={{mt: 1.5, mb: 1}}
            >
                Formulare des Fachbereichs
            </Typography>

            <Typography sx={{mb: 3, maxWidth: 900}}>
                Eine Liste aller Formulare, die von diesem Fachbereich entwickelt, bewirtschaftet oder verwaltet werden.
            </Typography>

            <GenericList<FormEntity>
                disableFullWidthToggle={true}
                sx={{
                    mx: '-16px',
                    mb: '-16px',
                }}
                columnDefinitions={columns}
                filters={filters}
                defaultFilter="developed"
                fetch={(options) => {
                    return new FormApiService()
                        .list(
                            options.page,
                            options.size,
                            'internalTitle',
                            options.order,
                            {
                                internalTitle: options.search,
                                developingDepartmentId: options.filter !== 'accessible' ? item.id : undefined,
                                developingDepartmentIdNot: options.filter === 'accessible' ? item.id : undefined,
                            },
                        );
                }}
                getRowIdentifier={(item) => item.id.toString()}
                searchLabel="Formular suchen"
                searchPlaceholder="Titel des Formulars eingeben…"
                defaultSortField="internalTitle"
                rowMenuItems={[]}
                noDataPlaceholder="Keine Formulare vorhanden"
                loadingPlaceholder="Lade Formulare…"
                noSearchResultsPlaceholder="Keine Formulare gefunden"
                rowActions={(item) => [{
                    icon: <EditOutlined />,
                    to: `/forms/${item.id}`,
                    tooltip: 'Formular anzeigen',
                }]}
                preSearchElements={[]}
            />
        </Box>
    );
}