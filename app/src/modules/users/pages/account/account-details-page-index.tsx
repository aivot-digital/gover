import {ApiOutlined, BadgeOutlined, LockOutlined, MailOutlined} from '@mui/icons-material';
import {Box, Button, Typography} from '@mui/material';
import React, {useEffect, useMemo, useState} from 'react';
import {useSelector} from 'react-redux';
import {selectUser} from '../../../../slices/user-slice';
import {stringOrDefault} from '../../../../utils/string-utils';
import {StatusTablePropsItem} from '../../../../components/status-table/status-table-props';
import {StatusTable} from '../../../../components/status-table/status-table';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {SystemRolesApiService} from '../../../system/services/system-roles-api-service';
import SupervisedUserCircle
    from '@aivot/mui-material-symbols-400-outlined/dist/supervised-user-circle/SupervisedUserCircle';

export function AccountDetailsPageIndex() {
    const user = useSelector(selectUser);
    const [systemRoleLabel, setSystemRoleLabel] = useState('Keine Systemrolle zugewiesen');

    useEffect(() => {
        if (user?.systemRoleId == null) {
            setSystemRoleLabel('Keine Systemrolle zugewiesen');
            return;
        }

        let isCancelled = false;

        setSystemRoleLabel('Systemrolle wird geladen…');

        new SystemRolesApiService()
            .retrieve(user.systemRoleId)
            .then((role) => {
                if (!isCancelled) {
                    setSystemRoleLabel(role.name);
                }
            })
            .catch(() => {
                if (!isCancelled) {
                    setSystemRoleLabel(`Unbekannte Rolle (#${user.systemRoleId})`);
                }
            });

        return () => {
            isCancelled = true;
        };
    }, [user?.systemRoleId]);

    const userInfoItems: StatusTablePropsItem[] = useMemo(() => [
        {
            label: 'Name',
            icon: <BadgeOutlined />,
            children: stringOrDefault(user?.firstName, '(Kein Vorname hinterlegt)') + ' ' + stringOrDefault(user?.lastName, '(Kein Nachname hinterlegt)'),
        },
        {
            label: 'E-Mail-Adresse',
            icon: <MailOutlined />,
            children: stringOrDefault(user?.email, 'Keine E-Mail-Adresse hinterlegt'),
        },
        {
            label: 'Systemrolle',
            icon: <SupervisedUserCircle />,
            children: systemRoleLabel,
        },
        {
            label: 'Passwort',
            icon: <LockOutlined />,
            children: '************',
        },
        {
            label: 'Verwendeter IDP',
            icon: <ApiOutlined />,
            children: 'Gover Identity Provider (basierend auf Keycloak)',
        },
    ], [systemRoleLabel, user?.email, user?.firstName, user?.lastName]);

    if (user == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    return (
        <Box sx={{pt: 2}}>
            <Typography
                variant="h5"
                sx={{mb: 1}}
            >
                Kontoinformationen
            </Typography>

            <Typography sx={{mb: 2, maxWidth: 900}}>
                Ihre Kontoinformationen werden von einem Identity Provider (IDP) System bereitgestellt.
                Änderungen an den hier angezeigten Daten sind nur über die Verwaltungsoberfläche des IDP möglich.
                Bitte beachten Sie, dass Änderungen erst nach dem nächsten Login sichtbar werden.
            </Typography>

            <StatusTable
                cardSx={{
                    mt: 3,
                }}
                cardVariant="outlined"
                items={userInfoItems}
            />

            <Box
                sx={{
                    display: 'flex',
                    marginTop: 5,
                    gap: 2,
                }}
            >
                <Button
                    target="_blank"
                    href={`${AppConfig.oidc.hostname}/realms/${AppConfig.oidc.realm}/account/#/personal-info`}
                    variant="contained"
                    color="primary"
                    startIcon={<OpenInNewIcon />}
                >
                    Daten verwalten
                </Button>
            </Box>
        </Box>
    );
}
