import React from 'react';
import {useSelector} from 'react-redux';
import {selectUser} from '../../../../slices/user-slice';
import EditOutlined from '@mui/icons-material/EditOutlined';
import {GenericList} from '../../../../components/generic-list/generic-list';
import {CellLink} from '../../../../components/cell-link/cell-link';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import {isAdmin} from '../../../../utils/is-admin';
import {UserRoleChips} from '../../../user-roles/components/user-role-chips';
import {VDepartmentMembershipWithDetailsEntityWithRoles} from '../../../departments/entities/v-department-membership-with-details-entity';
import {VDepartmentMembershipWithDetailsService} from '../../../departments/services/v-department-membership-with-details-service';

export function AccountDetailsPageDepartmentMemberships() {
    const user = useSelector(selectUser);

    return (
        <Box sx={{pt: 2}}>
            <Typography
                variant="h5"
                sx={{mb: 1}}
            >
                Mitgliedschaften in Fachbereichen
            </Typography>

            <Typography sx={{mb: 3, maxWidth: 900}}>
                Eine Übersicht der Fachbereiche, in denen Sie Mitglied sind, und den dazugehörigen Rollen.
                Wenn Sie noch keinem Fachbereich zugeordnet sind, bitten Sie eine Administrator:in, Sie zu einem Fachbereich hinzuzufügen.
            </Typography>

            <GenericList<VDepartmentMembershipWithDetailsEntityWithRoles>
                disableFullWidthToggle={true}
                sx={{
                    mx: '-16px',
                    mb: '-16px',
                }}
                columnDefinitions={[
                    {
                        field: 'orgUnitName',
                        headerName: 'Fachbereich',
                        flex: 1,
                        renderCell: (params) => {
                            const isMembershipAdmin = isAdmin(user) || params.row.roles.some(role => role.departmentPermissionEdit);
                            return (isMembershipAdmin ? <CellLink
                                to={`/departments/${params.row.departmentId}`}
                                title={`Fachbereich bearbeiten`}
                            >
                                {String(params.row.name)}
                            </CellLink> : params.row.name);
                        },
                    },
                    {
                        field: 'role',
                        headerName: 'Rolle',
                        flex: 1,
                        sortable: false,
                        renderCell: (params) => (
                            <UserRoleChips roles={params.row.roles} />
                        ),
                    },
                ]}
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
                rowActions={(item) => {
                    const isMembershipAdmin = isAdmin(user) || item.roles.some(role => role.departmentPermissionEdit);
                    return isMembershipAdmin ? [{
                        icon: <EditOutlined />,
                        to: `/departments/${item.departmentId}`,
                        tooltip: 'Fachbereich bearbeiten',
                    }] : [];
                }}
                preSearchElements={[]}
            />
        </Box>
    );
}
