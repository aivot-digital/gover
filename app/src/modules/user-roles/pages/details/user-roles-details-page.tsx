import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import KeyOutlinedIcon from '@mui/icons-material/KeyOutlined';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import React from 'react';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import {useUserIsAdmin} from '../../../../hooks/use-admin-guard';
import {UserRoleResponseDTO} from '../../dtos/user-role-response-dto';
import {UserRolesApiService} from '../../user-roles-api-service';

export function UserRolesDetailsPage() {
    const userIsAdmin = useUserIsAdmin();

    return (
        <PageWrapper
            title="Geheimnis bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<UserRoleResponseDTO, number, undefined>
                isEditable={() => userIsAdmin}
                header={{
                    icon: <KeyOutlinedIcon />,
                    title: 'Geheimnis bearbeiten',
                    helpDialog: {
                        title: 'Hilfe zu Geheimnissen',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography
                                    variant="body1"
                                    paragraph
                                >
                                    Verwalten Sie hier sicher die Geheimnisse Ihrer Webanwendung, wie API-Schlüssel, Passwörter oder andere vertrauliche Daten.
                                    Diese werden getrennt vom Code gespeichert, um Sicherheitsrisiken zu minimieren und eine einfache Aktualisierung ohne Anpassung der Anwendung zu ermöglichen.
                                </Typography>
                                <Typography
                                    variant="body1"
                                    paragraph
                                >
                                    Alle Geheimnisse sind verschlüsselt und nur für autorisierte Nutzer:innen oder Dienste mit entsprechender Berechtigung zugänglich.
                                </Typography>
                            </>
                        ),
                    },
                }}
                tabs={[
                    {
                        path: '/secrets/:id',
                        label: '',
                    },
                ]}
                initializeItem={(api) => new UserRolesApiService().initialize()}
                fetchData={(api, id: number) => new UserRolesApiService().retrieve(id)}
                getTabTitle={(item: UserRoleResponseDTO) => {
                    if (item.id === 0) {
                        return 'Neue Rolle';
                    } else {
                        return item.name ?? 'Unbenannte Rolle';
                    }
                }}
                getHeaderTitle={(item, isNewItem, notFound) => {
                    if (notFound) return 'Geheimnis nicht gefunden';
                    if (isNewItem) return 'Neues Geheimnis anlegen';
                    return `Geheimnis: ${item?.name ?? 'Unbenannt'}`;
                }}
                parentLink={{
                    label: 'Liste der Geheimnisse',
                    to: '/user-roles',
                }}
                entityType={ServerEntityType.Secrets}
            />
        </PageWrapper>
    );
}