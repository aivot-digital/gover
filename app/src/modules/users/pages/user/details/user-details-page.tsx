import {PageWrapper} from '../../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../../components/generic-details-page/generic-details-page';
import {NEW_ID_INDICATOR} from '../../../../../components/generic-details-page/generic-details-page';
import {type User} from '../../../../../models/entities/user';
import {UsersApiService} from '../../../users-api-service';
import {stringOrDefault} from '../../../../../utils/string-utils';
import React from 'react';
import PersonOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Person';
import {resolveUserName} from '../../../utils/resolve-user-name';
import {useParams} from 'react-router-dom';
import {Permission} from '../../../../../data/permissions/permission';
import {ServerEntityType} from '../../../../../shells/staff/data/server-entity-type';

export function UserDetailsPage() {
    const params = useParams();
    const isNewUser = params.id === NEW_ID_INDICATOR;

    return (
        <PageWrapper
            title={isNewUser ? 'Mitarbeiter:in anlegen' : 'Mitarbeiter:in bearbeiten'}
            fullWidth
            background
        >
            <GenericDetailsPage<User, string, undefined>
                permissionCheck={{
                    create: Permission.USER_CREATE,
                    read: Permission.USER_READ,
                    update: Permission.USER_UPDATE,
                    scope: {
                        type: 'system',
                    },
                }}
                header={(_, isNewItem) => ({
                    icon: <PersonOutlined />,
                    title: isNewItem ? 'Mitarbeiter:in anlegen' : 'Mitarbeiter:in bearbeiten',
                    helpDialog: {
                        title: 'Hilfe zu Mitarbeiter:innen',
                        tooltip: 'Hilfe anzeigen',
                        content: isNewItem ? (
                            <>
                                <Typography>
                                    Legen Sie hier neue Mitarbeiter:innen für Gover an und vergeben Sie die passende Systemrolle.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Für neue Konten wird ein temporäres Passwort gesetzt. Optional können die initialen Zugangsdaten direkt von Gover per E-Mail versendet werden.
                                    Beim ersten Login muss die Mitarbeiter:in ein neues Passwort vergeben, die eigene E-Mail-Adresse bestätigen und gegebenenfalls eine Zwei-Faktor-Authentifizierung einrichten, sofern dies in der System-Konfiguration vorgesehen ist.
                                </Typography>
                            </>
                        ) : (
                            <>
                                <Typography>
                                    Mitarbeiter:innen sind Benutzer:innen, die Zugriff auf das System haben und die Anwendung nutzen können.
                                    In dieser Oberfläche können Sie die im System verfügbaren Mitarbeiter:innen einsehen.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Die hier gepflegten Basisdaten werden mit dem Identity Provider (IDP) synchron gehalten.
                                    Weitergehende konto- oder sicherheitsbezogene Änderungen erfolgen direkt in der Verwaltungsoberfläche von Keycloak.
                                </Typography>
                            </>
                        ),
                    },
                })}
                tabs={[
                    {
                        path: '/users/:id',
                        label: 'Allgemeine Informationen',
                    },
                    {
                        path: '/users/:id/department-memberships',
                        label: 'Mitgliedschaften in Organisationseinheiten',
                        onlyExisting: true,
                        requiredPermission: {
                            permission: Permission.DEPARTMENT_MEMBERSHIP_READ,
                            scope: {
                                type: 'anyDepartment',
                            },
                        },
                    },
                    {
                        path: '/users/:id/team-memberships',
                        label: 'Mitgliedschaften in Teams',
                        onlyExisting: true,
                        requiredPermission: {
                            permission: Permission.TEAM_MEMBERSHIP_READ,
                            scope: {
                                type: 'anyTeam',
                            },
                        },
                    },
                    {
                        path: '/users/:id/deputies',
                        label: 'Stellvertreter:innen',
                        onlyExisting: true,
                        requiredPermission: Permission.DEPUTY_READ,
                    },
                ]}
                initializeItem={(api) => new UsersApiService().initialize()}
                fetchData={(api, id: string) => new UsersApiService().retrieve(id)}
                getTabTitle={(item: User) => {
                    return stringOrDefault(item.fullName, 'Kein Name hinterlegt');
                }}
                getHeaderTitle={(item, isNewItem, notFound) => {
                    if (notFound || item == null) {
                        return 'Mitarbeiter:in nicht gefunden';
                    }

                    if (isNewItem) {
                        return 'Mitarbeiter:in anlegen';
                    }

                    return `Mitarbeiter:in: ${resolveUserName(item)}`;
                }}
                parentLink={{
                    label: 'Liste der Mitarbeiter:innen',
                    to: '/users',
                }}
                entityType={ServerEntityType.Users}
            />
        </PageWrapper>
    );
}
