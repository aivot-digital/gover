import React, {useContext} from 'react';
import {type GridColDef} from '@mui/x-data-grid';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import {
    VDepartmentMembershipWithDetailsEntity
} from "../../../departments/entities/v-department-membership-with-details-entity";
import {CellLink} from "../../../../components/cell-link/cell-link";
import {
    GenericDetailsPageContext,
    GenericDetailsPageContextType
} from "../../../../components/generic-details-page/generic-details-page-context";
import {UserRoleResponseDTO} from "../../dtos/user-role-response-dto";
import {useAccessGuard} from "../../../../hooks/use-admin-guard";
import {GenericDetailsSkeleton} from "../../../../components/generic-details-page/generic-details-skeleton";
import {GenericList} from "../../../../components/generic-list/generic-list";
import {
    VDepartmentUserRoleAssignmentWithDetailsService
} from "../../../departments/services/v-department-user-role-assignment-with-details-service";
import {
    VDepartmentUserRoleAssignmentWithDetailsEntity
} from "../../../departments/entities/v-department-user-role-assignment-with-details-entity";

const columns: Array<GridColDef<VDepartmentMembershipWithDetailsEntity>> = [
    {
        field: 'fullName',
        headerName: 'Mitarbeiter:in',
        flex: 1,
        renderCell: (params) => (
            <CellLink
                to={`/users/${params.row.userId}`}
                title="Mitarbeiter:in anzeigen"
            >
                {String(params.row.fullName)}
            </CellLink>
        ),
    },
    {
        field: 'name',
        headerName: 'Fachbereich',
        flex: 1,
        renderCell: (params) => (
            <CellLink
                to={`/departments/${params.row.departmentId}`}
                title="Fachbereich bearbeiten"
            >
                {String(params.row.name)}
            </CellLink>
        ),
    },
];

export function UserRolesDetailsPageDepartmentMemberships() {
    const {
        item: userRole,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<UserRoleResponseDTO, undefined>;

    if (userRole == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    return (
        <>
            <Box sx={{pt: 2}}>
                <Typography
                    variant="h5"
                    sx={{mb: 1}}
                >
                    Zuggeordnete Mitarbeiter:innen
                </Typography>

                <Typography sx={{
                    mb: 3,
                    maxWidth: 900,
                }}>
                    Eine Übersicht der Mitarbeiter:innen, die dieser Rolle in verschiedenen
                    Organisationseinheiten zugewiesen sind.
                </Typography>

                <GenericList<VDepartmentUserRoleAssignmentWithDetailsEntity>
                    disableFullWidthToggle={true}
                    sx={{
                        mx: '-16px',
                        mb: '-16px',
                    }}
                    columnDefinitions={columns}
                    fetch={(options) => {
                        return new VDepartmentUserRoleAssignmentWithDetailsService()
                            .list(options.page, options.size, options.sort, options.order, {
                                userRoleId: userRole.id,
                                fullName: options.search,
                            });
                    }}
                    getRowIdentifier={(item) => item.id.toString()}
                    searchLabel="Mitarbeiter:in suchen"
                    searchPlaceholder="Name der Mitarbeiter:in eingeben…"
                    defaultSortField="fullName"
                    noDataPlaceholder="Keine Mitarbeiter:innen vorhanden"
                    loadingPlaceholder="Lade Mitarbeiter:innen…"
                    noSearchResultsPlaceholder="Keine Mitarbeiter:innen gefunden"
                />
            </Box>
        </>
    );
}
