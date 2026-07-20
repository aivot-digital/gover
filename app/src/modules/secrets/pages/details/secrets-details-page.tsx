import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import KeyOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Key';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import {SecretsApiService} from '../../secrets-api-service';
import {Secret} from '../../models/secret';
import React from 'react';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import {Permission} from '../../../../data/permissions/permission';

export function SecretsDetailsPage() {
    return (
        <PageWrapper
            title="Geheimnis bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<Secret, string, undefined>
                permissionCheck={{
                    create: Permission.SECRET_CREATE,
                    read: Permission.SECRET_READ,
                    update: Permission.SECRET_UPDATE,
                    scope: {
                        type: 'system',
                    },
                }}
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
                initializeItem={(api) => new SecretsApiService(api).initialize()}
                fetchData={(api, id: string) => new SecretsApiService(api).retrieve(id)}
                getTabTitle={(item: Secret) => {
                    if (item.key === '') {
                        return 'Neues Geheimnis';
                    } else {
                        return item.name;
                    }
                }}
                getHeaderTitle={(item, isNewItem, notFound) => {
                    if (notFound) return 'Geheimnis nicht gefunden';
                    if (isNewItem) return 'Neues Geheimnis anlegen';
                    return `Geheimnis: ${item?.name ?? 'Unbenannt'}`;
                }}
                parentLink={{
                    label: 'Liste der Geheimnisse',
                    to: '/secrets',
                }}
                entityType={ServerEntityType.Secrets}
            />
        </PageWrapper>
    );
}
