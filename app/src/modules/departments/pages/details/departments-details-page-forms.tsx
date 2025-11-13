import React, {useContext} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {GenericList} from '../../../../components/generic-list/generic-list';
import {FormsApiService} from '../../../forms/forms-api-service';
import {GridColDef} from '@mui/x-data-grid';
import {Department} from '../../models/department';
import {EditOutlined} from '@mui/icons-material';
import {Box, Typography} from '@mui/material';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {FormDetailsResponseDTO} from '../../../forms/dtos/form-details-response-dto';
import {FormStatusChip} from '../../../forms/components/form-status-chip';

const filters = [
    {
        label: 'Entwicklung',
        value: 'dev',
    },
    {
        label: 'Zuständig',
        value: 'responsible',
    },
    {
        label: 'Bewirtschaftung',
        value: 'managing',
    },
];

const columns: GridColDef<FormDetailsResponseDTO>[] = [
    {
        field: 'internalTitle',
        headerName: 'Titel des Formulars',
        flex: 2,
        renderCell: (params) => (
            <CellLink
                to={`/forms/${params.id}/${params.row.version}`}
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
            <FormStatusChip
                status={params.row.status}
                size="small"
                variant="outlined"
            />
        ),
    },
];

export function DepartmentsDetailsPageForms() {
    const {
        item,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<Department, undefined>;

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

            <GenericList<FormDetailsResponseDTO>
                disableFullWidthToggle={true}
                sx={{
                    mx: '-16px',
                    mb: '-16px',
                }}
                columnDefinitions={columns}
                filters={filters}
                defaultFilter="dev"
                fetch={(options) => {
                    return new FormsApiService(options.api)
                        .listVersions(
                            options.page,
                            options.size,
                            options.sort,
                            options.order,
                            {
                                internalTitle: options.search,
                                developingDepartmentId: (options.filter == null || options.filter === 'dev') ? item.id : undefined,
                                managingDepartmentId: options.filter === 'managing' ? item.id : undefined,
                                responsibleDepartmentId: options.filter === 'responsible' ? item.id : undefined,
                            },
                        );
                }}
                getRowIdentifier={(item) => `${item.id}_${item.version}`}
                searchLabel="Formular suchen"
                searchPlaceholder="Titel des Formulars eingeben…"
                defaultSortField="internalTitle"
                rowMenuItems={[]}
                noDataPlaceholder="Keine Formulare vorhanden"
                loadingPlaceholder="Lade Formulare…"
                noSearchResultsPlaceholder="Keine Formulare gefunden"
                rowActions={(item: FormDetailsResponseDTO) => [{
                    icon: <EditOutlined />,
                    to: `/forms/${item.id}/${item.version}`,
                    tooltip: 'Formular anzeigen',
                }]}
                preSearchElements={[]}
            />
        </Box>
    );
}