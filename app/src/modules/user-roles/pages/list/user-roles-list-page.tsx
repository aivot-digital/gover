import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import KeyOutlinedIcon from '@mui/icons-material/KeyOutlined';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Typography} from '@mui/material';
import {EditOutlined} from '@mui/icons-material';
import React from 'react';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import {useAccessGuard} from '../../../../hooks/use-admin-guard';
import Visibility from '@aivot/mui-material-symbols-400-outlined/dist/visibility/Visibility';
import {UserRoleResponseDTO} from '../../dtos/user-role-response-dto';
import {UserRolesApiService} from '../../user-roles-api-service';
import {ModuleIcons} from "../../../../shells/staff/data/module-icons";

export function UserRolesListPage() {
    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });

    return (
        <PageWrapper
            title="Domänenrollen"
            fullWidth
            background
        >
            <GenericListPage<UserRoleResponseDTO>
                header={{
                    icon: ModuleIcons.roles,
                    title: 'Domänenrollen',
                    actions: [
                        {
                            label: 'Neue Domänenrolle',
                            icon: <AddOutlinedIcon />,
                            to: '/user-roles/new',
                            variant: 'contained',
                            disabled: !hasAccess,
                        },
                    ],
                    helpDialog: {
                        title: 'Hilfe zu Domänenrollen',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography
                                    variant="body1"
                                    component="p"
                                >
                                    Verwalten Sie hier die Domänenrollen, die Berechtigungen und Zugriffsrechte für Benutzer:innen innerhalb der Anwendung definieren.
                                </Typography>
                            </>
                        ),
                    },
                }}
                searchLabel="Domänenrolle suchen"
                searchPlaceholder="Name der Domänenrolle eingeben…"
                fetch={(options) => {
                    return new UserRolesApiService()
                        .list(
                            options.page,
                            options.size,
                            options.sort,
                            options.order,
                            {name: options.search},
                        );
                }}
                columnIcon={() => ModuleIcons.roles}
                columnDefinitions={[
                    {
                        field: 'name',
                        headerName: 'Name',
                        flex: 1,
                        renderCell: (params) => (
                            <CellLink
                                to={`/user-roles/${params.id}`}
                                title={hasAccess ? 'Domänenrolle bearbeiten' : 'Domänenrolle anzeigen'}
                            >
                                {String(params.value)}
                            </CellLink>
                        ),
                    },
                    {
                        field: 'description',
                        headerName: 'Beschreibung',
                        flex: 2,
                    },
                ]}
                getRowIdentifier={row => row.id.toString()}
                noDataPlaceholder="Keine Domänenrolle angelegt"
                noSearchResultsPlaceholder="Keine Domänenrolle gefunden"
                rowActionsCount={1}
                rowActions={(item: UserRoleResponseDTO) => [
                    {
                        icon: hasAccess ? <EditOutlined /> : <Visibility />,
                        to: `/user-roles/${item.id}`,
                        tooltip: hasAccess ? 'Domänenrolle bearbeiten' : 'Domänenrolle anzeigen',
                    },
                ]}
                defaultSortField="name"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}