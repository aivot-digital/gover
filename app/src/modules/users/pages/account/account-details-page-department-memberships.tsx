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
import {
    VDepartmentMembershipWithDetailsEntity
} from '../../../departments/entities/v-department-membership-with-details-entity';
import {
    VDepartmentMembershipWithDetailsService
} from '../../../departments/services/v-department-membership-with-details-service';

export function AccountDetailsPageDepartmentMemberships() {
    const user = useSelector(selectUser);

    return (
        <Box sx={{pt: 2}}>
            <Typography
                variant="h5"
                sx={{mb: 1}}
            >
                Mitgliedschaften in Organisationseinheiten
            </Typography>

            <Typography sx={{mb: 3, maxWidth: 900}}>
                Eine Übersicht der Organisationseinheiten, in denen Sie Mitglied sind, und den dazugehörigen Rollen.
                Wenn Sie noch keiner Organisationseinheit zugeordnet sind, bitten Sie eine Administrator:in, Sie zu
                einer Organisationseinheit hinzuzufügen.
            </Typography>

            <GenericList<VDepartmentMembershipWithDetailsEntity>
                disableFullWidthToggle={true}
                sx={{
                    mx: '-16px',
                    mb: '-16px',
                }}
                columnDefinitions={[
                    {
                        field: 'orgUnitName',
                        headerName: 'Organisationseinheit',
                        flex: 1,
                        renderCell: (params) => {
                            return (
                                <CellLink
                                    to={`/departments/${params.row.departmentId}`}
                                    title={`Organisationseinheit bearbeiten`}
                                >
                                    {String(params.row.departmentName)}
                                </CellLink>
                            );
                        },
                    },
                    {
                        field: 'role',
                        headerName: 'Rolle',
                        flex: 1,
                        sortable: false,
                        renderCell: (params) => (
                            <UserRoleChips roles={params.row.domainRoles.map(item => ({
                                id: item.id!,
                                name: item.name ?? '',
                            }))}/>
                        ),
                    },
                ]}
                fetch={(options) => {
                    return new VDepartmentMembershipWithDetailsService()
                        .list(options.page, options.size, options.sort, options.order, {
                            userId: user?.id,
                            name: options.search,
                        });
                }}
                getRowIdentifier={(item) => item.membershipId.toString()}
                searchLabel="Organisationseinheit suchen"
                searchPlaceholder="Name der Organisationseinheit eingeben…"
                defaultSortField="departmentName"
                rowMenuItems={[]}
                noDataPlaceholder="Keine Organisationseinheiten vorhanden"
                loadingPlaceholder="Lade Organisationseinheiten…"
                noSearchResultsPlaceholder="Keine Organisationseinheiten gefunden"
                rowActions={(item) => [{
                    icon: <EditOutlined/>,
                    to: `/departments/${item.departmentId}`,
                    tooltip: 'Organisationseinheit bearbeiten',
                }]}
                preSearchElements={[]}
            />
        </Box>
    );
}
