import React, {useContext} from 'react';
import {useApi} from '../../../../../hooks/use-api';
import {DepartmentMembership} from '../../../../departments/models/department-membership';
import {DepartmentMembershipsApiService} from '../../../../departments/department-memberships-api-service';
import type {GridColDef} from '@mui/x-data-grid';
import {UserRoleLabels} from '../../../../../data/user-role';
import {EditOutlined} from '@mui/icons-material';
import {GenericList} from '../../../../../components/generic-list/generic-list';
import {CellLink} from '../../../../../components/cell-link/cell-link';
import {Box, Typography} from '@mui/material';
import {type User} from '../../../../../models/entities/user';
import {DepartmentMembershipResponseDTO} from '../../../../departments/dtos/department-membership-response-dto';
import {
    GenericDetailsPageContext,
    GenericDetailsPageContextType,
} from '../../../../../components/generic-details-page/generic-details-page-context';
import {GenericDetailsSkeleton} from '../../../../../components/generic-details-page/generic-details-skeleton';

const columns: Array<GridColDef<DepartmentMembershipResponseDTO>> = [
    {
        field: 'departmentName',
        headerName: 'Fachbereich',
        flex: 1,
        renderCell: (params) => (
            <CellLink
                to={`/departments/${params.row.departmentId}`}
                title={`Fachbereich bearbeiten`}
            >
                {String(params.row.departmentName)}
            </CellLink>
        ),
    },
    {
        field: 'role',
        headerName: 'Rolle',
        flex: 1,
        valueGetter: (params) => UserRoleLabels[params.row.role],
    },
];

export function UserDetailsPageDepartmentMemberships() {
    const {
        item,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<User, undefined>;

    const user = item;

    if (user == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    return (
        <Box sx={{pt: 2}}>
            <Typography
                variant="h5"
                sx={{mb: 1}}
            >
                Mitgliedschaften in Fachbereichen
            </Typography>

            <Typography sx={{mb: 3, maxWidth: 900}}>
                Eine Übersicht der Fachbereiche, in denen diese Mitarbeiter:in Mitglied ist, und die dazugehörigen Rollen.
            </Typography>

            <GenericList<DepartmentMembershipResponseDTO>
                disableFullWidthToggle={true}
                sx={{
                    mx: '-16px',
                    mb: '-16px',
                }}
                columnDefinitions={columns}
                fetch={(options) => {
                    return new DepartmentMembershipsApiService(options.api)
                        .listAll({
                            userId: user?.id,
                            departmentName: options.search,
                        });
                }}
                getRowIdentifier={(item) => item.id.toString()}
                searchLabel="Fachbereich suchen"
                searchPlaceholder="Titel des Fachbereichs eingeben…"
                defaultSortField="departmentName"
                rowMenuItems={[]}
                noDataPlaceholder="Keine Fachbereiche vorhanden"
                loadingPlaceholder="Lade Fachbereiche…"
                noSearchResultsPlaceholder="Keine Fachbereiche gefunden"
                rowActions={(item: DepartmentMembershipResponseDTO) => [{
                    icon: <EditOutlined />,
                    to: `/departments/${item.departmentId}`,
                    tooltip: 'Fachbereich bearbeiten',
                }]}
                preSearchElements={[]}
            />
        </Box>
    );
}
