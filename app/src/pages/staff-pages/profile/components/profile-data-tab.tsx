import React from 'react';
import {selectUser} from '../../../../slices/user-slice';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {Alert, AlertTitle, Box, Divider, Grid, Link} from '@mui/material';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {AppConfig} from "../../../../app-config";
import {isStringNotNullOrEmpty, stringOrDefault} from '../../../../utils/string-utils';
import {AlertComponent} from '../../../../components/alert/alert-component';


export function ProfileDataTab(): JSX.Element {
    const user = useAppSelector(selectUser);

    return (
        <>
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
                        onChange={() => {
                        }}
                        value={stringOrDefault(user?.email, 'Keine E-Mail-Adresse hinterlegt')}
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
                        onChange={() => {
                        }}
                        value={stringOrDefault(user?.firstName, 'Kein Vorname hinterlegt')}
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
                        onChange={() => {
                        }}
                        value={stringOrDefault(user?.lastName, 'Kein Nachname hinterlegt')}
                        display={true}
                    />
                </Grid>
            </Grid>

            <AlertComponent
                color="info"
                title="Daten ändern"
                sx={{
                    mt: 8,
                }}
            >
                Ihre Benutzerdaten können Sie in der <Link
                target="_blank"
                href={`${AppConfig.staff.host}/realms/${AppConfig.staff.realm}/account/#/personal-info`}
            >Eingabemaske
                des Identity-Providers</Link> ändern. Bitte beachten Sie, dass Änderungen erst nach dem nächsten Login sichtbar werden.
            </AlertComponent>
        </>
    );
}
