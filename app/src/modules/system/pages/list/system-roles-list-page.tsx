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
                            label: 'Neue Systemrolle',
                            icon: <AddOutlinedIcon/>,
                            to: '/system-roles/new',
                            variant: 'contained',
                            disabled: !hasAccess,
                        },
                    ],
                    helpDialog: {
                        title: 'Hilfe zu Systemrollen',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography
                                    variant="body1"
                                    component="p"
                                >
                                    Systemrollen definieren Berechtigungen auf Systemebene und gelten
                                    anwendungsweit in Gover.
                                </Typography>
                                <Typography
                                    variant="body1"
                                    component="p"
                                >
                                    Sie steuern damit grundlegende Zugriffe, etwa auf Administration,
                                    Konfiguration und andere globale Funktionen. Im Unterschied zu
                                    Domänenrollen sind Systemrollen nicht an einzelne Organisationseinheiten
                                    oder Teams gebunden.
                                </Typography>
                            </>
                        ),
                    },
                }}
                searchLabel="Systemrolle suchen"
                searchPlaceholder="Name der Systemrolle eingeben…"
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
                                title={hasAccess ? 'Systemrolle bearbeiten' : 'Systemrolle anzeigen'}
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
                noDataPlaceholder="Keine Systemrolle angelegt"
                noSearchResultsPlaceholder="Keine Systemrolle gefunden"
                rowActionsCount={1}
                rowActions={(item) => [
                    {
                        icon: hasAccess ? <EditOutlined/> : <Visibility/>,
                        to: `/system-roles/${item.id}`,
                        tooltip: hasAccess ? 'Systemrolle bearbeiten' : 'Systemrolle anzeigen',
                    },
                ]}
                defaultSortField="name"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}
