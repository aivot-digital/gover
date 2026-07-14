import React, {useContext} from 'react';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
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
import {GenericDetailsSkeleton} from "../../../../components/generic-details-page/generic-details-skeleton";
import {GenericList} from "../../../../components/generic-list/generic-list";
import {
    VDepartmentMembershipWithDetailsService
} from "../../../departments/services/v-department-membership-with-details-service";
import {Permission} from '../../../../data/permissions/permission';
import {useCheckAnyDepartmentPermission} from '../../../permissions/hooks/use-permissions';
import {type Page} from '../../../../models/dtos/page';

const columns: Array<GridColDef<VDepartmentMembershipWithDetailsEntity>> = [
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
        field: 'departmentName',
        headerName: 'Organisationseinheit',
        flex: 1,
        renderCell: (params) => (
            <CellLink
                to={`/departments/${params.row.departmentId}`}
                title="Organisationseinheit anzeigen"
            >
                {String(params.row.departmentName)}
            </CellLink>
        ),
    },
];

export function UserRolesDetailsPageDepartmentMemberships() {
    const {
        item: userRole,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<UserRoleResponseDTO, undefined>;
    const canReadDepartmentMemberships = useCheckAnyDepartmentPermission(Permission.DEPARTMENT_MEMBERSHIP_READ);

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
                    Zugeordnete Mitarbeiter:innen
                </Typography>

                <Typography sx={{
                    mb: 3,
                    maxWidth: 900,
                }}>
                    Eine Übersicht der Mitarbeiter:innen, denen diese Domänenrolle in verschiedenen
                    Organisationseinheiten zugewiesen sind.
                </Typography>

                <GenericList<VDepartmentMembershipWithDetailsEntity>
                    disableFullWidthToggle={true}
                    sx={{
                        mx: '-16px',
                        mb: '-16px',
                    }}
                    columnDefinitions={columns}
                    fetch={(options) => {
                        if (!canReadDepartmentMemberships) {
                            const page: Page<VDepartmentMembershipWithDetailsEntity> = {
                                content: [],
                                page: {
                                    number: 0,
                                    size: options.size,
                                    totalElements: 0,
                                    totalPages: 0,
                                },
                            };
                            return Promise.resolve(page);
                        }

                        return new VDepartmentMembershipWithDetailsService()
                            .list(options.page, options.size, options.sort, options.order, {
                                domainRoleId: userRole.id,
                                fullName: options.search,
                            });
                    }}
                    getRowIdentifier={(item) => item.membershipId.toString()}
                    searchLabel="Mitarbeiter:in suchen"
                    searchPlaceholder="Name der Mitarbeiter:in eingeben…"
                    defaultSortField="userFullName"
                    noDataPlaceholder={
                        <EmptyDataListPlaceholder
                            title={canReadDepartmentMemberships ? 'Keine Mitarbeiter:innen zugeordnet' : 'Keine Zuordnungen sichtbar'}
                            description={canReadDepartmentMemberships
                                ? 'Diese Zuordnungen vergeben kontextbezogene Rechte an Mitarbeiter:innen innerhalb einer Organisationseinheit.'
                                : `Für die Anzeige von Zuordnungen in Organisationseinheiten ist die Berechtigung ${Permission.DEPARTMENT_MEMBERSHIP_READ} erforderlich.`}
                        />
                    }
                    loadingPlaceholder="Lade Mitarbeiter:innen…"
                    noSearchResultsPlaceholder="Keine Mitarbeiter:innen gefunden"
                />
            </Box>
        </>
    );
}
