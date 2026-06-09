import React, {useContext} from 'react';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
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
        headerName: 'Name der Organisationseinheit',
        flex: 1,
        renderCell: (params) => (
            <CellLink
                to={`/departments/${params.id}`}
                title="Organisationseinheit anzeigen"
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
                Organisationseinheiten mit diesem Erscheinungsbild
            </Typography>

            <Typography sx={{mb: 2, maxWidth: 900}}>
                Eine Liste aller Organisationseinheiten, die dieses Erscheinungsbild verwenden.
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
                searchLabel="Organisationseinheit suchen"
                searchPlaceholder="Name der Organisationseinheit eingeben…"
                defaultSortField="name"
                rowMenuItems={[]}
                noDataPlaceholder={
                    <EmptyDataListPlaceholder
                        title="Keine Organisationseinheiten zugeordnet"
                        description="Organisationseinheiten können ein Erscheinungsbild vorgeben, das für ihre Formulare verwendet wird."
                    />
                }
                loadingPlaceholder="Lade Organisationseinheiten…"
                noSearchResultsPlaceholder="Keine Organisationseinheiten gefunden"
                rowActions={(item) => [{
                    icon: <EditOutlined />,
                    to: `/departments/${item.id}`,
                    tooltip: 'Organisationseinheiten anzeigen',
                }]}
                preSearchElements={[]}
            />
        </Box>
    );
}
