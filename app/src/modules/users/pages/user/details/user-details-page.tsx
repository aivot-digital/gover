import {PageWrapper} from '../../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../../components/generic-details-page/generic-details-page';
import {useAdminGuard} from '../../../../../hooks/use-admin-guard';
import {type User} from '../../../../../models/entities/user';
import {UsersApiService} from '../../../users-api-service';
import {stringOrDefault} from '../../../../../utils/string-utils';
import React from 'react';
import {PersonOutlined} from '@mui/icons-material';
import {resolveUserName} from '../../../utils/resolve-user-name';

export function UserDetailsPage() {
    useAdminGuard();

    return (
        <PageWrapper
            title="Mitarbeiter:in bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<User, number, undefined>
                header={{
                    icon: <PersonOutlined />,
                    title: 'Mitarbeiter:in bearbeiten',
                    helpDialog: {
                        title: 'Hilfe zu Mitarbeiter:innen',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Mitarbeiter:innen sind Benutzer:innen, die Zugriff auf das System haben und die Anwendung nutzen können.
                                    In dieser Oberfläche können Sie die im System verfügbaren Mitarbeiter:innen einsehen.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Informationen zu Mitarbeitenden werden von einem Identity Provider (IDP) System bereitgestellt.
                                    Änderungen an den hier angezeigten Daten sind nur über die Verwaltungsoberfläche des IDP möglich.
                                </Typography>
                            </>
                        ),
                    },
                }}
                tabs={[
                    {
                        path: '/users/:id',
                        label: 'Allgemeine Informationen',
                    },
                    {
                        path: '/users/:id/departments-and-roles',
                        label: 'Fachbereiche und Rollen',
                        isDisabled: (item) => !item?.id,
                    },
                ]}
                initializeItem={(api) => new UsersApiService(api).initialize()}
                fetchData={(api, id: number) => new UsersApiService(api).retrieve(String(id))}
                getTabTitle={(item: User) => {
                    return stringOrDefault(item.fullName, 'Kein Name hinterlegt');
                }}
                getHeaderTitle={(item, isNewItem, notFound) => {
                    if (notFound || item == null) {
                        return "Mitarbeiter:in nicht gefunden";
                    }

                    return `Mitarbeiter:in: ${resolveUserName(item)}`;
                }}
                parentLink={{
                    label: "Liste der Mitarbeiter:innen",
                    to: "/users",
                }}
            />
        </PageWrapper>
    );
}