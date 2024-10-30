import React, {useEffect, useState} from 'react';
import {useParams} from 'react-router-dom';
import {type User} from '../../../models/entities/user';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {useChangeBlocker} from '../../../hooks/use-change-blocker';
import {delayPromise} from '../../../utils/with-delay';
import {FormPageWrapper} from '../../../components/form-page-wrapper/form-page-wrapper';
import {useAdminGuard} from '../../../hooks/use-admin-guard';
import {UserEditPageMembershipsTab} from './tabs/user-edit-page-memberships-tab';
import {useApi} from '../../../hooks/use-api';
import {useUsersApi} from '../../../hooks/use-users-api';
import {Alert, AlertTitle, Grid, Link} from '@mui/material';
import {isAdmin} from '../../../utils/is-admin';
import {AppConfig} from '../../../app-config';
import {stringOrDefault} from '../../../utils/string-utils';
import {AlertComponent} from '../../../components/alert/alert-component';

type Errors = {
    [key in keyof User]?: string;
};

export function UserEditPage(): JSX.Element {
    useAdminGuard();
    const api = useApi();

    const userId = useParams().id;

    const [originalUser, setOriginalUser] = useState<User>();

    const [isUserLoading, setIsUserLoading] = useState(true);
    const [isUserNotFound, setIsUserNotFound] = useState(false);

    useEffect(() => {
        setIsUserLoading(true);
        setIsUserNotFound(false);

        if (userId != null) {
            delayPromise(useUsersApi(api).retrieve(userId))
                .then((user) => {
                    setOriginalUser(user);
                })
                .catch((err) => {
                    console.error(err);
                    setIsUserNotFound(true);
                })
                .finally(() => {
                    setIsUserLoading(false);
                });
        }
    }, [userId]);

    return (
        <FormPageWrapper
            title="Mitarbeiter:in bearbeiten"
            isLoading={isUserLoading}
            is404={isUserNotFound}

            onSave={undefined}
            onReset={undefined}

            tabs={
                originalUser != null ?
                    [
                        {
                            label: 'Fachbereiche und Rollen',
                            content: <UserEditPageMembershipsTab user={originalUser} />,
                        },
                    ] :
                    undefined
            }
        >
            <Grid
                container
                spacing={2}
            >
                <Grid
                    item
                    xs={12}
                >
                    <TextFieldComponent
                        label="E-Mail-Adresse"
                        type="email"
                        value={stringOrDefault(originalUser?.email, 'Keine E-Mail-Adresse hinterlegt')}
                        onChange={(val) => {
                        }}
                        display={true}
                    />
                </Grid>

                <Grid
                    item
                    xs={12}
                    md={6}
                >
                    <TextFieldComponent
                        label="Vorname"
                        value={stringOrDefault(originalUser?.firstName, 'Kein Vorname hinterlegt')}
                        onChange={(val) => {
                        }}
                        display={true}
                    />
                </Grid>

                <Grid
                    item
                    xs={12}
                    md={6}
                >
                    <TextFieldComponent
                        label="Nachname"
                        value={stringOrDefault(originalUser?.lastName, 'Kein Nachname hinterlegt')}
                        onChange={(val) => {
                        }}
                        display={true}
                    />
                </Grid>
            </Grid>

            {
                !originalUser?.enabled &&
                <AlertComponent
                    title="Inaktive Mitarbeiter:in"
                    color="warning"
                    sx={{
                        mt: 8,
                    }}
                >
                    Diese Mitarbeiter:in ist inaktiv und kann sich nicht anmelden.
                </AlertComponent>
            }

            <AlertComponent
                color="info"
                title="Daten ändern"
                sx={{
                    mt: 8,
                }}
            >
                Diese Benutzerdaten können Sie in der <Link
                target="_blank"
                href={`${AppConfig.staff.host}/admin/${AppConfig.staff.realm}/console/#/${AppConfig.staff.realm}/users/${originalUser?.id}/settings`}
            >Eingabemaske des Identity-Providers</Link> ändern. Bitte beachten Sie, dass Änderungen erst nach dem nächsten
                Login durch die Mitarbeiter:in sichtbar werden.
            </AlertComponent>

            {
                isAdmin(originalUser) &&
                <Alert
                    severity="info"
                    sx={{
                        mt: 4,
                    }}
                >
                    <AlertTitle>
                        Globale Administrator:in
                    </AlertTitle>

                    Bei dieser Mitarbeiter:in handelt es sich um eine globale Administrator:in.
                </Alert>
            }
        </FormPageWrapper>
    );
}
