import { Alert, Button, Typography } from '@mui/material';
import React, { type FormEvent, useCallback, useState } from 'react';
import { type User } from '../../../../models/entities/user';
import { UsersService } from '../../../../services/users-service';
import { refreshUser, selectUser } from '../../../../slices/user-slice';
import { useAppDispatch } from '../../../../hooks/use-app-dispatch';
import { useAppSelector } from '../../../../hooks/use-app-selector';
import { validateEmail } from '../../../../utils/validate-email';
import { TextFieldComponent } from '../../../../components/text-field/text-field-component';


export function ProfileInformationChange(): JSX.Element {
    const dispatch = useAppDispatch();

    const [editedUser, setEditedUser] = useState<User>();
    const [nameError, setNameError] = useState<string>();
    const [emailError, setEmailError] = useState<string>();
    const [userChanged, setUserChanged] = useState(false);

    const user = useAppSelector(selectUser);

    const handleSave = useCallback((event: FormEvent) => {
        event.preventDefault();

        if (user != null && editedUser != null) {
            if (editedUser.name.length < 4 || editedUser.name.length >= 32) {
                setNameError('Bitte geben Sie einen Namen mit mehr als 4 und weniger als 32 Zeichen ein.');
                return false;
            }

            if (!validateEmail(editedUser.email)) {
                setEmailError('Bitte geben Sie eine gültige E-Mail-Adresse ein.');
                return false;
            }

            UsersService.update(user.id, editedUser)
                .then((_) => {
                    dispatch(refreshUser());
                    setUserChanged(true);
                });
            setEditedUser(undefined);
            (document.activeElement as HTMLInputElement)?.blur();
        }

        return false;
    }, [user, editedUser, dispatch]);

    if (user == null) {
        return <div></div>;
    }

    return (
        <form onSubmit={handleSave}>
            <Typography variant="subtitle1">
                Benutzerdaten
            </Typography>

            <TextFieldComponent
                label="Name"
                placeholder="Max Mustermann"
                required
                value={(editedUser ?? user).name}
                onChange={(val) => {
                    setEditedUser({
                        ...(editedUser ?? user),
                        name: val ?? '',
                    });
                    setNameError(undefined);
                    setUserChanged(false);
                }}
                error={nameError}
                maxCharacters={32}
            />

            <TextFieldComponent
                label="E-Mail"
                placeholder="max.muster@gover.digital"
                type="email"
                required
                value={(editedUser ?? user).email}
                onChange={(val) => {
                    setEditedUser({
                        ...(editedUser ?? user),
                        email: val ?? '',
                    });
                    setEmailError(undefined);
                    setUserChanged(false);
                }}
                error={emailError}
            />

            {
                userChanged &&
                <Alert sx={{ mt: 2 }}>
                    Daten erfolgreich geändert!
                </Alert>
            }

            <Button
                type="submit"
                sx={{ mt: 2 }}
                disabled={editedUser == null}
            >
                Änderungen Speichern
            </Button>
        </form>
    );
}
