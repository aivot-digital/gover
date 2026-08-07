import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Box, Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import React from 'react';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
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
import {Permission} from '../../../../data/permissions/permission';
import {
    isMostPrivilegedSystemRole,
    MostPrivilegedSystemRoleBadge,
} from '../../components/most-privileged-system-role-badge';

export function SystemRolesDetailsPage() {
    const defaultSystemRoleId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.users.defaultSystemRole));
    const mostPrivilegedSystemRoleId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.systemRoles.mostPrivilegedRole));

    return (
        <PageWrapper
            title="Systemrolle bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<SystemRoleEntity, number, void>
                permissionCheck={{
                    create: Permission.SYSTEM_ROLE_CREATE,
                    read: Permission.SYSTEM_ROLE_READ,
                    update: Permission.SYSTEM_ROLE_UPDATE,
                    scope: {
                        type: 'system',
                    },
                }}
                header={(item, isNewItem, notFound) => ({
                    icon: ModuleIcons.roles,
                    title: 'Systemrolle bearbeiten',
                    badge: !isNewItem && !notFound && (
                        isDefaultUserSystemRole(item?.id, defaultSystemRoleId) ||
                        isMostPrivilegedSystemRole(item?.id, mostPrivilegedSystemRoleId)
                    )
                        ? (
                            <Box
                                component="span"
                                sx={{display: 'inline-flex', flexWrap: 'wrap', gap: 1}}
                            >
                                {isDefaultUserSystemRole(item?.id, defaultSystemRoleId) &&
                                    <DefaultUserSystemRoleBadge showHintIcon/>}
                                {isMostPrivilegedSystemRole(item?.id, mostPrivilegedSystemRoleId) &&
                                    <MostPrivilegedSystemRoleBadge showHintIcon/>}
                            </Box>
                        )
                        : undefined,
                    helpDialog: {
                        title: 'Hilfe zu Systemrollen',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography
                                    variant="body1"
                                    component="p"
                                >
                                    Konfigurieren Sie hier eine Systemrolle, die Berechtigungen auf
                                    Systemebene und damit für die gesamte Prosuna-Anwendung festlegt.
                                </Typography>
                                <Typography
                                    variant="body1"
                                    component="p"
                                >
                                    Weisen Sie Systemrollen mit Bedacht zu. Mitarbeitende mit dieser Rolle
                                    erhalten die hinterlegten globalen Berechtigungen unabhängig von ihrer
                                    Zugehörigkeit zu Teams oder Organisationseinheiten.
                                </Typography>
                            </>
                        ),
                    },
                })}
                tabs={[
                    {
                        path: '/system-roles/:id',
                        label: 'Konfiguration',
                    },
                    {
                        path: '/system-roles/:id/members',
                        label: 'Zugeordnete Mitarbeiter:innen',
                        onlyExisting: true,
                        requiredPermission: Permission.USER_READ,
                    },
                ]}
                initializeItem={(api) => SystemRolesApiService.initialize()}
                fetchData={(api, id: number) => new SystemRolesApiService().retrieve(id)}
                getTabTitle={(item) => {
                    if (item.id === 0) {
                        return 'Neue Systemrolle';
                    } else {
                        return item.name ?? 'Unbenannte Systemrolle';
                    }
                }}
                getHeaderTitle={(item, isNewItem, notFound) => {
                    if (notFound) return 'Systemrolle nicht gefunden';
                    if (isNewItem) return 'Neue Systemrolle anlegen';
                    return `Systemrolle: ${item?.name ?? 'Unbenannt'}`;
                }}
                parentLink={{
                    label: 'Liste der Systemrollen',
                    to: '/system-roles',
                }}
                entityType={ServerEntityType.SystemRoles}
            />
        </PageWrapper>
    );
}
