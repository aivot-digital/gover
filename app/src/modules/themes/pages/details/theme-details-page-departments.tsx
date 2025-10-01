import React, {useContext} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {GenericList} from '../../../../components/generic-list/generic-list';
import {FormsApiService} from '../../../forms/forms-api-service';
import {GridColDef} from '@mui/x-data-grid';
import {EditOutlined} from '@mui/icons-material';
import {Box, Typography} from '@mui/material';
import {CellLink} from '../../../../components/cell-link/cell-link';
import type {Theme} from '../../models/theme';
import {DepartmentResponseDTO} from '../../../departments/dtos/department-response-dto';
import {DepartmentsApiService} from '../../../departments/departments-api-service';

const columns: GridColDef<DepartmentResponseDTO>[] = [
    {
        field: 'name',
        headerName: 'Name des Fachbereichs',
        flex: 1,
        renderCell: (params) => (
            <CellLink
                to={`/departments/${params.id}`}
                title="Department anzeigen"
            >
                {String(params.value)}
            </CellLink>
        ),
    },
];

export function ThemeDetailsPageDepartments() {
    const {
        item,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<Theme, undefined>;

    if (item == null) {
        return null;
    }

    return (
        <Box>
            <Typography
                variant="h5"
                sx={{mt: 1.5, mb: 1}}
            >
                Fachbereiche mit diesem Farbschema
            </Typography>

            <Typography sx={{mb: 2, maxWidth: 900}}>
                Eine Liste aller Fachbereiche, die dieses Farbschema für ihr optisches Erscheinungsbild verwenden.
            </Typography>

            <GenericList<DepartmentResponseDTO>
                disableFullWidthToggle={true}
                sx={{
                    mx: '-16px',
                    mb: '-16px',
                }}
                columnDefinitions={columns}
                defaultFilter="dev"
                fetch={(options) => {
                    return new DepartmentsApiService(options.api)
                        .list(
                            options.page,
                            options.size,
                            options.sort,
                            options.order,
                            {
                                themeId: item.id ?? undefined,
                            },
                        );
                }}
                getRowIdentifier={(item) => item.id.toString()}
                searchLabel="Fachbereich suchen"
                searchPlaceholder="Name des Fachbereichs eingeben…"
                defaultSortField="name"
                rowMenuItems={[]}
                noDataPlaceholder="Keine Formulare vorhanden"
                loadingPlaceholder="Lade Formulare…"
                noSearchResultsPlaceholder="Keine Formulare gefunden"
                rowActions={(item: DepartmentResponseDTO) => [{
                    icon: <EditOutlined />,
                    to: `/departments/${item.id}`,
                    tooltip: 'Fachbereich anzeigen',
                }]}
                preSearchElements={[]}
            />
        </Box>
    );
}