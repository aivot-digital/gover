import React, {useContext} from 'react';
import {type GridColDef} from '@mui/x-data-grid';
import EditOutlined from '@mui/icons-material/EditOutlined';
import {GenericList} from '../../../../../components/generic-list/generic-list';
import {CellLink} from '../../../../../components/cell-link/cell-link';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import {type User} from '../../../../../models/entities/user';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../../components/generic-details-page/generic-details-page-context';
import {GenericDetailsSkeleton} from '../../../../../components/generic-details-page/generic-details-skeleton';
import {useAccessGuard} from '../../../../../hooks/use-admin-guard';
import Visibility from '@aivot/mui-material-symbols-400-outlined/dist/visibility/Visibility';
import {UserRoleChips} from '../../../../user-roles/components/user-role-chips';
import {VDepartmentMembershipWithDetailsEntityWithRoles} from '../../../../departments/entities/v-department-membership-with-details-entity';
import {VDepartmentMembershipWithDetailsService} from '../../../../departments/services/v-department-membership-with-details-service';


const columns: Array<GridColDef<VDepartmentMembershipWithDetailsEntityWithRoles>> = [
    {
        field: 'name',
        headerName: 'Fachbereich',
        flex: 1,
        renderCell: (params) => (
            <CellLink
                to={`/departments/${params.row.departmentId}`}
                title={`Fachbereich bearbeiten`}
            >
                {String(params.row.name)}
            </CellLink>
        ),
    },
    {
        field: 'role',
        headerName: 'Rollen',
        flex: 1,
        sortable: false,
        renderCell: (params) => (
            <UserRoleChips roles={params.row.roles} />
        ),
    },
];

export function UserDetailsPageDepartmentMemberships() {
    const {
        item: user,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<User, undefined>;

    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });

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

            <GenericList<VDepartmentMembershipWithDetailsEntityWithRoles>
                disableFullWidthToggle={true}
                sx={{
                    mx: '-16px',
                    mb: '-16px',
                }}
                columnDefinitions={columns}
                fetch={(options) => {
                    return new VDepartmentMembershipWithDetailsService()
                        .listDepartmentMembershipsWithRoles(0, 999, 'organizationalUnitName', options.order, {
                            userId: user?.id,
                            departmentSearch: options.search,
                        });
                }}
                getRowIdentifier={(item) => item.id.toString()}
                searchLabel="Fachbereich suchen"
                searchPlaceholder="Titel des Fachbereichs eingeben…"
                defaultSortField="name"
                rowMenuItems={[]}
                noDataPlaceholder="Keine Fachbereiche vorhanden"
                loadingPlaceholder="Lade Fachbereiche…"
                noSearchResultsPlaceholder="Keine Fachbereiche gefunden"
                rowActions={(item) => [{
                    icon: hasAccess ? <EditOutlined /> : <Visibility />,
                    to: `/departments/${item.departmentId}`,
                    tooltip: hasAccess ? 'Fachbereich bearbeiten' : 'Fachbereich anzeigen',
                }]}
                preSearchElements={[]}
            />
        </Box>
    );
}
