import {Alert, Button, TextField, Typography} from '@mui/material';
import React, {FormEvent, useCallback, useState} from 'react';
import {UsersService} from '../../../../services/users.service';
import {isNullOrEmpty} from '../../../../utils/is-null-or-empty';
import strings from './profile-password-change-strings.json';
import {Localization} from '../../../../locale/localization';

const __ = Localization(strings);

export function ProfilePasswordChange() {
    const [newPassword, setNewPassword] = useState('');
    const [newPassword2, setNewPassword2] = useState('');

    const [passwordError, setPasswordError] = useState<string>();
    const [passwordChanged, setPasswordChanged] = useState(false);

    const handlePasswordSet = useCallback((event: FormEvent) => {
        event.preventDefault();

        if (newPassword !== newPassword2) {
            setPasswordError(__.passwordErrorNotMatching);
            return false;
        }
        if (newPassword.length < 6) {
            setPasswordError(__.passwordErrorLength);
            return false;
        }

        setPasswordError(undefined);
        UsersService.setPassword(newPassword)
            .catch(err => {
                setPasswordError(__.passwordErrorFailed);
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
                {__.passwordTitle}
            </Typography>
            <TextField
                label={__.passwordLabel}
                type="password"
                value={newPassword}
                required
                onChange={event => {
                    setNewPassword(event.target.value);
                    setPasswordChanged(false);
                    setPasswordError(undefined);
                }}
                helperText={passwordError}
                error={!isNullOrEmpty(passwordError)}
            />
            <TextField
                label={__.retypePasswordLabel}
                type="password"
                value={newPassword2}
                required
                onChange={event => {
                    setNewPassword2(event.target.value);
                    setPasswordChanged(false);
                    setPasswordError(undefined);
                }}
                helperText={passwordError}
                error={!isNullOrEmpty(passwordError)}
            />

            {
                passwordChanged &&
                <Alert sx={{mt: 2}}>
                    {__.passwordChangeSuccess}
                </Alert>
            }

            <Button
                type="submit"
                sx={{mt: 2}}
                disabled={newPassword.length === 0}
            >
                {__.changePasswordLabel}
            </Button>
        </form>
    );
}
