import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import KeyOutlinedIcon from '@mui/icons-material/KeyOutlined';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import {SecretsApiService} from '../../secrets-api-service';
import {Secret} from '../../models/secret';
import React, {useCallback} from 'react';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import {useCheckSystemPermission, useHasSystemPermission} from '../../../permissions/hooks/use-permissions';
import {Permission} from '../../../../data/permissions/permission';

export function SecretsDetailsPage() {
    useHasSystemPermission(Permission.SECRET_READ);
    const canCreateSecret = useCheckSystemPermission(Permission.SECRET_CREATE);
    const canUpdateSecret = useCheckSystemPermission(Permission.SECRET_UPDATE);

    const isEditable = useCallback((item: Secret | undefined) => {
        if (item == null) {
            return false;
        }

        return item.key === ''
            ? canCreateSecret
            : canUpdateSecret;
    }, [canCreateSecret, canUpdateSecret]);

    return (
        <PageWrapper
            title="Geheimnis bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<Secret, string, undefined>
                isEditable={isEditable}
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
