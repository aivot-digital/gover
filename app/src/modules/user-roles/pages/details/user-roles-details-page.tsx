import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import React, {useCallback} from 'react';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import {UserRoleResponseDTO} from '../../dtos/user-role-response-dto';
import {UserRolesApiService} from '../../user-roles-api-service';
import {ModuleIcons} from "../../../../shells/staff/data/module-icons";
import {useCheckSystemPermission, useHasSystemPermission} from '../../../permissions/hooks/use-permissions';
import {Permission} from '../../../../data/permissions/permission';

export function UserRolesDetailsPage() {
    useHasSystemPermission(Permission.DOMAIN_ROLE_READ);
    const canCreateDomainRole = useCheckSystemPermission(Permission.DOMAIN_ROLE_CREATE);
    const canUpdateDomainRole = useCheckSystemPermission(Permission.DOMAIN_ROLE_UPDATE);
    const isEditable = useCallback((item: UserRoleResponseDTO | undefined) => {
        if (item == null) {
            return false;
        }

        return item.id === 0 ? canCreateDomainRole : canUpdateDomainRole;
    }, [canCreateDomainRole, canUpdateDomainRole]);

    return (
        <PageWrapper
            title="Domänenrolle bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<UserRoleResponseDTO, number, undefined>
                isEditable={isEditable}
                header={{
                    icon: ModuleIcons.roles,
                    title: 'Domänenrolle bearbeiten',
                    helpDialog: {
                        title: 'Hilfe zu Domänenrollen',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography
                                    variant="body1"
                                    component="p"
                                >
                                    Konfigurieren Sie hier eine Domänenrolle, die Berechtigungen innerhalb
                                    fachlicher Domänen wie Organisationseinheiten oder Teams festlegt.
                                </Typography>
                                <Typography
                                    variant="body1"
                                    component="p"
                                >
                                    Domänenrollen ergänzen Systemrollen um kontextbezogene Rechte. Sie
                                    wirken nur dort, wo Mitarbeiter:innen über eine
                                    Mitgliedschaft tatsächlich zugewiesen sind.
                                </Typography>
                            </>
                        ),
                    },
                }}
                tabs={[
                    {
                        path: '/user-roles/:id',
                        label: 'Konfiguration',
                    },
                    {
                        path: '/user-roles/:id/department-memberships',
                        label: 'Zuordnungen in Organisationseinheiten',
                        isDisabled: (item) => !item?.id,
                    },
                    {
                        path: '/user-roles/:id/team-memberships',
                        label: 'Zuordnungen in Teams',
                        isDisabled: (item) => !item?.id,
                    },
                ]}
                initializeItem={(api) => new UserRolesApiService().initialize()}
                fetchData={(api, id: number) => new UserRolesApiService().retrieve(id)}
                getTabTitle={(item: UserRoleResponseDTO) => {
                    if (item.id === 0) {
                        return 'Neue Domänenrolle';
                    } else {
                        return item.name ?? 'Unbenannte Domänenrolle';
                    }
                }}
                getHeaderTitle={(item, isNewItem, notFound) => {
                    if (notFound) return 'Domänenrolle nicht gefunden';
                    if (isNewItem) return 'Neue Domänenrolle anlegen';
                    return `Domänenrolle: ${item?.name ?? 'Unbenannt'}`;
                }}
                parentLink={{
                    label: 'Liste der Domänenrollen',
                    to: '/user-roles',
                }}
                entityType={ServerEntityType.UserRoles}
            />
        </PageWrapper>
    );
}
