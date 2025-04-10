import {Box, Button, Typography} from '@mui/material';
import React from 'react';
import {useSelector} from 'react-redux';
import {selectUser} from '../../../../slices/user-slice';
import {stringOrDefault} from "../../../../utils/string-utils";
import {AppConfig} from "../../../../app-config";
import {StatusTablePropsItem} from "../../../../components/status-table/status-table-props";
import {StatusTable} from "../../../../components/status-table/status-table";
import {ApiOutlined, BadgeOutlined, LockOutlined, MailOutlined} from "@mui/icons-material";
import OpenInNewIcon from "@mui/icons-material/OpenInNew";
import {GenericDetailsSkeleton} from "../../../../components/generic-details-page/generic-details-skeleton";

export function AccountDetailsPageIndex() {
    const user = useSelector(selectUser);

    if (user == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    const userInfoItems: StatusTablePropsItem[] = [
        {
            label: 'Name',
            icon: <BadgeOutlined />,
            children: stringOrDefault(user?.firstName, '(Kein Vorname hinterlegt)') + " " + stringOrDefault(user?.lastName, '(Kein Nachname hinterlegt)'),
        },
        {
            label: 'E-Mail-Adresse',
            icon: <MailOutlined />,
            children: stringOrDefault(user?.email, 'Keine E-Mail-Adresse hinterlegt'),
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
    ];

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
                    href={`${AppConfig.staff.host}/realms/${AppConfig.staff.realm}/account/#/personal-info`}
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