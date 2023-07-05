import {Alert, Button, TextField, Typography} from '@mui/material';
import React, {FormEvent, useCallback, useState} from 'react';
import {UsersService} from '../../../../services/users-service';
import {isStringNullOrEmpty} from "../../../../utils/string-utils";
import {useAppSelector} from "../../../../hooks/use-app-selector";
import {selectUser} from "../../../../slices/user-slice";


export function ProfilePasswordChange() {
    const user = useAppSelector(selectUser);
    const [newPassword, setNewPassword] = useState('');
    const [newPassword2, setNewPassword2] = useState('');

    const [passwordError, setPasswordError] = useState<string>();
    const [passwordChanged, setPasswordChanged] = useState(false);

    const handlePasswordSet = useCallback((event: FormEvent) => {
        event.preventDefault();

        if (user == null) {
            return false;
        }

        if (newPassword !== newPassword2) {
            setPasswordError('Die beiden Passwörter stimmen nicht überein');
            return false;
        }
        if (newPassword.length < 6) {
            setPasswordError('Das Passwort muss aus mindestens 6 Zeichen bestehen');
            return false;
        }

        setPasswordError(undefined);
        UsersService.update(user.id, {
            ...user,
            password: newPassword,
        })
            .catch(err => {
                setPasswordError('Passwort konnte nicht gesetzt werden');
                console.error(err);
            });
        setNewPassword('');
        setNewPassword2('');
        setPasswordChanged(true);
        (document.activeElement as HTMLInputElement)?.blur();

        return false;
    }, [newPassword, newPassword2]);

    return (
        <form onSubmit={handlePasswordSet}>
            <Typography variant="subtitle1">
                Passwort
            </Typography>
            <TextField
                label="Passwort"
                type="password"
                value={newPassword}
                required
                onChange={event => {
                    setNewPassword(event.target.value);
                    setPasswordChanged(false);
                    setPasswordError(undefined);
                }}
                helperText={passwordError}
                error={!isStringNullOrEmpty(passwordError)}
            />
            <TextField
                label="Passwort erneut eingeben"
                type="password"
                value={newPassword2}
                required
                onChange={event => {
                    setNewPassword2(event.target.value);
                    setPasswordChanged(false);
                    setPasswordError(undefined);
                }}
                helperText={passwordError}
                error={!isStringNullOrEmpty(passwordError)}
            />

            {
                passwordChanged &&
                <Alert sx={{mt: 2}}>
                    Passwort erfolgreich geändert!
                </Alert>
            }

            <Button
                type="submit"
                sx={{mt: 2}}
                disabled={newPassword.length === 0}
            >
                Passwort ändern
            </Button>
        </form>
    );
}
