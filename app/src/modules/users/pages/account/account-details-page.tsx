import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import AccountCircleOutlinedIcon from '@mui/icons-material/AccountCircleOutlined';

export function AccountDetailsPage() {
    return (
        <PageWrapper
            title="Konto verwalten"
            fullWidth
            background
        >
            <GenericDetailsPage<undefined, number, undefined>
                header={{
                    icon: <AccountCircleOutlinedIcon />,
                    title: 'Konto verwalten',
                    helpDialog: {
                        title: 'Hilfe zur Kontoverwaltung',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Ihr Konto ist die zentrale Anlaufstelle für Ihre persönlichen Informationen und Einstellungen. Hier können Sie Ihre Kontoinformationen einsehen und bearbeiten, Ihre Mitgliedschaften in Organisationseinheiten verwalten und Benachrichtigungseinstellungen anpassen.
                                </Typography>
                                <Typography sx={{ mt: 2 }}>
                                    Wenn Sie Fragen zur Kontoverwaltung haben, wenden Sie sich bitte an den Administrator.
                                </Typography>
                            </>
                        ),
                    },
                }}
                tabs={[
                    {
                        path: '/account',
                        label: 'Kontoinformationen',
                    },
                    {
                        path: '/account/memberships-and-roles',
                        label: 'Organisationseinheiten und Rollen',
                    },
                    {
                        path: '/account/notifications',
                        label: 'Benachrichtigungen',
                    },
                ]}
                initializeItem={() => {return undefined}}
                fetchData={async () => {return undefined}}
                getTabTitle={() => { return "Konto verwalten" }}
            />
        </PageWrapper>
    );
}
