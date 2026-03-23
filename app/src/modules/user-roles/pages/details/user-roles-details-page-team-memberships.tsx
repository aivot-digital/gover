import React, {useContext} from 'react';
import {type GridColDef} from '@mui/x-data-grid';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import {CellLink} from "../../../../components/cell-link/cell-link";
import {
    GenericDetailsPageContext,
    GenericDetailsPageContextType
} from "../../../../components/generic-details-page/generic-details-page-context";
import {UserRoleResponseDTO} from "../../dtos/user-role-response-dto";
import {GenericDetailsSkeleton} from "../../../../components/generic-details-page/generic-details-skeleton";
import {GenericList} from "../../../../components/generic-list/generic-list";
import {
    VTeamUserRoleAssignmentWithDetailsEntity
} from "../../../teams/entities/v-team-user-role-assignment-with-details-entity";
import {
    VTeamUserRoleAssignmentWithDetailsApiService
} from "../../../teams/services/v-team-user-role-assignment-with-details-api-service";
import {
    VDepartmentMembershipWithDetailsService
} from '../../../departments/services/v-department-membership-with-details-service';
import {VTeamMembershipWithDetailsService} from '../../../teams/services/v-team-membership-with-details-service';
import {VTeamMembershipWithDetailsEntity} from '../../../teams/entities/v-team-membership-with-details-entity';

const columns: Array<GridColDef<VTeamMembershipWithDetailsEntity>> = [
    {
        field: 'userFullName',
        headerName: 'Mitarbeiter:in',
        flex: 1,
        renderCell: (params) => (
            <CellLink
                to={`/users/${params.row.userId}`}
                title="Mitarbeiter:in anzeigen"
            >
                {String(params.row.userFullName)}
            </CellLink>
        ),
    },
    {
        field: 'teamName',
        headerName: 'Team',
        flex: 1,
        renderCell: (params) => (
            <CellLink
                to={`/teams/${params.row.teamId}`}
                title="Team bearbeiten"
            >
                {String(params.row.teamName)}
            </CellLink>
        ),
    },
];

export function UserRolesDetailsPageTeamMemberships() {
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

                <GenericList<VTeamMembershipWithDetailsEntity>
                    disableFullWidthToggle={true}
                    sx={{
                        mx: '-16px',
                        mb: '-16px',
                    }}
                    columnDefinitions={columns}
                    fetch={(options) => {
                        return new VTeamMembershipWithDetailsService()
                            .list(options.page, options.size, options.sort, options.order, {
                                domainRoleId: userRole.id,
                                fullName: options.search,
                            });
                    }}
                    getRowIdentifier={(item) => item.membershipId.toString()}
                    searchLabel="Mitarbeiter:in suchen"
                    searchPlaceholder="Name der Mitarbeiter:in eingeben…"
                    defaultSortField="userFullName"
                    noDataPlaceholder="Keine Mitarbeiter:innen vorhanden"
                    loadingPlaceholder="Lade Mitarbeiter:innen…"
                    noSearchResultsPlaceholder="Keine Mitarbeiter:innen gefunden"
                />
            </Box>
        </>
    );
}
