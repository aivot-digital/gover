import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import React from 'react';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import {useUserIsAdmin} from '../../../../hooks/use-admin-guard';
import {UserRoleResponseDTO} from '../../dtos/user-role-response-dto';
import {UserRolesApiService} from '../../user-roles-api-service';
import {ModuleIcons} from "../../../../shells/staff/data/module-icons";

export function UserRolesDetailsPage() {
    const userIsAdmin = useUserIsAdmin();

    return (
        <PageWrapper
            title="Domänenrolle bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<UserRoleResponseDTO, number, undefined>
                isEditable={() => userIsAdmin}
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
                                    Mitgliedschaft tatsächlich zugewiesen ist.
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
