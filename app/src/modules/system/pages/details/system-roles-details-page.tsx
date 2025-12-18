import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import React from 'react';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import {useUserIsAdmin} from '../../../../hooks/use-admin-guard';
import {ModuleIcons} from "../../../../shells/staff/data/module-icons";
import {SystemRoleEntity} from "../../entities/system-role-entity";
import {SystemRolesApiService} from "../../services/system-roles-api-service";

export function SystemRolesDetailsPage() {
    const userIsAdmin = useUserIsAdmin();

    return (
        <PageWrapper
            title="Systemrolle bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<SystemRoleEntity, number, void>
                isEditable={() => userIsAdmin}
                header={{
                    icon: ModuleIcons.roles,
                    title: 'Systemrolle bearbeiten',
                    helpDialog: {
                        title: 'Hilfe zu Systemrolle',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography
                                    variant="body1"
                                    component="p"
                                >
                                    Domänenrollen definieren Berechtigungen und Zugriffsrechte für Benutzer:innen innerhalb der Anwendung.
                                </Typography>
                                <Typography
                                    variant="body1"
                                    component="p"
                                >
                                    Alle Geheimnisse sind verschlüsselt und nur für autorisierte Nutzer:innen oder Dienste mit entsprechender Berechtigung zugänglich.
                                </Typography>
                            </>
                        ),
                    },
                }}
                tabs={[
                    {
                        path: '/system-roles/:id',
                        label: 'Allgemeine Informationen',
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
                    label: 'Liste der Systemrolle',
                    to: '/system-roles',
                }}
                entityType={ServerEntityType.SystemRoles}
            />
        </PageWrapper>
    );
}