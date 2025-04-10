import React from 'react';
import {useSelector} from 'react-redux';
import {selectUser} from '../../../../slices/user-slice';
import {DepartmentMembershipsApiService} from '../../../departments/department-memberships-api-service';
import {UserRole, UserRoleLabels} from '../../../../data/user-role';
import {EditOutlined} from '@mui/icons-material';
import {GenericList} from '../../../../components/generic-list/generic-list';
import {DepartmentMembershipResponseDTO} from '../../../departments/dtos/department-membership-response-dto';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {Box, Typography} from '@mui/material';
import {isAdmin} from '../../../../utils/is-admin';

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

            <GenericList<DepartmentMembershipResponseDTO>
                disableFullWidthToggle={true}
                sx={{
                    mx: '-16px',
                    mb: '-16px',
                }}
                columnDefinitions={[
                    {
                        field: 'departmentName',
                        headerName: 'Fachbereich',
                        flex: 1,
                        renderCell: (params) => {
                            const isMembershipAdmin = isAdmin(user) || params.row.role === UserRole.Admin;
                            return (isMembershipAdmin ? <CellLink
                                to={`/departments/${params.row.departmentId}`}
                                title={`Fachbereich bearbeiten`}
                            >
                                {String(params.row.departmentName)}
                            </CellLink> : params.row.departmentName);
                        },
                    },
                    {
                        field: 'role',
                        headerName: 'Rolle',
                        flex: 1,
                        renderCell: (params) => UserRoleLabels[params.row.role],
                    },
                ]}
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
                loadingPlaceholder="Lade Fachbereiche..."
                noSearchResultsPlaceholder="Keine Fachbereiche gefunden"
                rowActions={(item: DepartmentMembershipResponseDTO) => {
                    const isMembershipAdmin = isAdmin(user) || item.role === UserRole.Admin;
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
