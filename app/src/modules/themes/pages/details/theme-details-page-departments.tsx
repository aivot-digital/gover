import React, {useContext} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {GenericList} from '../../../../components/generic-list/generic-list';
import {GridColDef} from '@mui/x-data-grid';
import {EditOutlined} from '@mui/icons-material';
import {Box, Typography} from '@mui/material';
import {CellLink} from '../../../../components/cell-link/cell-link';
import type {Theme} from '../../models/theme';
import {DepartmentEntity} from '../../../departments/entities/department-entity';
import {DepartmentApiService} from '../../../departments/services/department-api-service';

const columns: GridColDef<DepartmentEntity>[] = [
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
                Fachbereiche mit diesem Erscheinungsbild
            </Typography>

            <Typography sx={{mb: 2, maxWidth: 900}}>
                Eine Liste aller Fachbereiche, die dieses Erscheinungsbild verwenden.
            </Typography>

            <GenericList<DepartmentEntity>
                disableFullWidthToggle={true}
                sx={{
                    mx: '-16px',
                    mb: '-16px',
                }}
                columnDefinitions={columns}
                defaultFilter="dev"
                fetch={(options) => {
                    return new DepartmentApiService()
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
                rowActions={(item) => [{
                    icon: <EditOutlined />,
                    to: `/departments/${item.id}`,
                    tooltip: 'Fachbereich anzeigen',
                }]}
                preSearchElements={[]}
            />
        </Box>
    );
}
