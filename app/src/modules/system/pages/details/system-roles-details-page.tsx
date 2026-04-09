import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import React from 'react';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import {useUserIsAdmin} from '../../../../hooks/use-admin-guard';
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

export function SystemRolesDetailsPage() {
    const userIsAdmin = useUserIsAdmin();
    const defaultSystemRoleId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.users.defaultSystemRole));

    return (
        <PageWrapper
            title="Systemrolle bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<SystemRoleEntity, number, void>
                isEditable={() => userIsAdmin}
                header={(item, isNewItem, notFound) => ({
                    icon: ModuleIcons.roles,
                    title: 'Systemrolle bearbeiten',
                    badge: !isNewItem && !notFound && isDefaultUserSystemRole(item?.id, defaultSystemRoleId)
                        ? <DefaultUserSystemRoleBadge showHintIcon />
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
                                    Systemebene und damit für die gesamte Gover-Anwendung festlegt.
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
                        isDisabled: (item) => !item?.id,
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
