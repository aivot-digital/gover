import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Typography} from '@mui/material';
import {EditOutlined} from '@mui/icons-material';
import React from 'react';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {useAccessGuard} from '../../../../hooks/use-admin-guard';
import Visibility from '@aivot/mui-material-symbols-400-outlined/dist/visibility/Visibility';
import {ModuleIcons} from "../../../../shells/staff/data/module-icons";
import {SystemRoleEntity} from "../../entities/system-role-entity";
import {SystemRolesApiService} from "../../services/system-roles-api-service";
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../../data/system-config-keys';
import {
    DefaultUserSystemRoleBadge,
    isDefaultUserSystemRole,
} from '../../components/default-user-system-role-badge';

export function SystemRolesListPage() {
    const defaultSystemRoleId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.users.defaultSystemRole));

    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });

    return (
        <PageWrapper
            title="Systemrollen"
            fullWidth
            background
        >
            <GenericListPage<SystemRoleEntity>
                header={{
                    icon: ModuleIcons.roles,
                    title: 'Systemrollen',
                    actions: [
                        {
                            label: 'Neue Systemrollen',
                            icon: <AddOutlinedIcon/>,
                            to: '/system-roles/new',
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
                                    Verwalten Sie hier die Domänenrollen, die Berechtigungen und Zugriffsrechte für
                                    Benutzer:innen innerhalb der Anwendung definieren.
                                </Typography>
                            </>
                        ),
                    },
                }}
                searchLabel="Domänenrolle suchen"
                searchPlaceholder="Name der Domänenrolle eingeben…"
                fetch={(options) => {
                    return new SystemRolesApiService()
                        .list(
                            options.page,
                            options.size,
                            options.sort,
                            options.order,
                            {
                                name: options.search,
                            },
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
                                to={`/system-roles/${params.id}`}
                                title={hasAccess ? 'Domänenrolle bearbeiten' : 'Domänenrolle anzeigen'}
                            >
                                {String(params.value)}
                                {
                                    isDefaultUserSystemRole(params.row.id, defaultSystemRoleId) &&
                                    <DefaultUserSystemRoleBadge sx={{ml: 1}} />
                                }
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
                rowActions={(item) => [
                    {
                        icon: hasAccess ? <EditOutlined/> : <Visibility/>,
                        to: `/system-roles/${item.id}`,
                        tooltip: hasAccess ? 'Domänenrolle bearbeiten' : 'Domänenrolle anzeigen',
                    },
                ]}
                defaultSortField="name"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}
