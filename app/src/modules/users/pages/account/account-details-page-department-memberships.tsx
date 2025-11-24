import React from 'react';
import {useSelector} from 'react-redux';
import {selectUser} from '../../../../slices/user-slice';
import {DepartmentMembershipsApiService} from '../../../departments/department-memberships-api-service';
import EditOutlined from '@mui/icons-material/EditOutlined';
import {GenericList} from '../../../../components/generic-list/generic-list';
import {DepartmentMembershipWithRoles} from '../../../departments/dtos/department-membership-response-dto';
import {CellLink} from '../../../../components/cell-link/cell-link';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import {isAdmin} from '../../../../utils/is-admin';
import {UserRoleChips} from '../../../user-roles/components/user-role-chips';

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

            <GenericList<DepartmentMembershipWithRoles>
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
                            const isMembershipAdmin = isAdmin(user) || params.row.roles.some(role => role.userRoleOrgUnitMemberPermissionEdit);
                            return (isMembershipAdmin ? <CellLink
                                to={`/departments/${params.row.orgUnitId}`}
                                title={`Fachbereich bearbeiten`}
                            >
                                {String(params.row.orgUnitName)}
                            </CellLink> : params.row.orgUnitName);
                        },
                    },
                    {
                        field: 'role',
                        headerName: 'Rolle',
                        flex: 1,
                        sortable: false,
                        renderCell: (params) => (
                            <UserRoleChips roles={params.row.roles}/>
                        ),
                    },
                ]}
                fetch={(options) => {
                    return new DepartmentMembershipsApiService()
                        .listDepartmentMembershipsWithRoles(0, 999, 'organizationalUnitName', options.order, {
                            userId: user?.id,
                            organizationalUnitSearch: options.search,
                        });
                }}
                getRowIdentifier={(item) => item.membershipId.toString()}
                searchLabel="Fachbereich suchen"
                searchPlaceholder="Titel des Fachbereichs eingeben…"
                defaultSortField="orgUnitName"
                rowMenuItems={[]}
                noDataPlaceholder="Keine Fachbereiche vorhanden"
                loadingPlaceholder="Lade Fachbereiche…"
                noSearchResultsPlaceholder="Keine Fachbereiche gefunden"
                rowActions={(item) => {
                    const isMembershipAdmin = isAdmin(user) || item.roles.some(role => role.userRoleOrgUnitMemberPermissionEdit);
                    return isMembershipAdmin ? [{
                        icon: <EditOutlined />,
                        to: `/departments/${item.orgUnitId}`,
                        tooltip: 'Fachbereich bearbeiten',
                    }] : [];
                }}
                preSearchElements={[]}
            />
        </Box>
    );
}
