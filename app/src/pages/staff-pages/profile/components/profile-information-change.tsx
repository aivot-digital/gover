import {Alert, Button, TextField, Typography} from '@mui/material';
import React, {FormEvent, useCallback, useState} from 'react';
import {User} from '../../../../models/entities/user';
import {UsersService} from '../../../../services/users.service';
import {refreshUser, selectUser} from '../../../../slices/user-slice';
import strings from './profile-information-change-strings.json';
import {Localization} from '../../../../locale/localization';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {validateEmail} from "../../../../utils/validate-email";
import {isStringNullOrEmpty} from "../../../../utils/string-utils";

const __ = Localization(strings);

export function ProfileInformationChange() {
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
                setNameError(__.nameErrorLength);
                return false;
            }

            if (!validateEmail(editedUser.email)) {
                setEmailError(__.emailErrorInvalid);
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
                {__.userDataTitle}
            </Typography>
            <TextField
                label={__.nameLabel}
                placeholder={__.namePlaceholder}
                required
                value={(editedUser ?? user).name}
                onChange={event => {
                    setEditedUser({
                        ...(editedUser ?? user),
                        name: event.target.value,
                    });
                    setNameError(undefined);
                    setUserChanged(false);
                }}
                helperText={nameError}
                error={!isStringNullOrEmpty(nameError)}
            />
            <TextField
                label={__.emailLabel}
                placeholder={__.emailPlaceholder}
                type="email"
                required
                value={(editedUser ?? user).email}
                onChange={event => {
                    setEditedUser({
                        ...(editedUser ?? user),
                        email: event.target.value,
                    });
                    setEmailError(undefined);
                    setUserChanged(false);
                }}
                helperText={emailError}
                error={!isStringNullOrEmpty(emailError)}
            />
            <TextField
                label={__.roleLabel}
                value={(editedUser ?? user).role}
                disabled
            />

            {
                userChanged &&
                <Alert sx={{mt: 2}}>
                    {__.changeSuccessful}
                </Alert>
            }

            <Button
                type="submit"
                sx={{mt: 2}}
                disabled={editedUser == null}
            >
                {__.saveChangesLabel}
            </Button>
        </form>
    );
}
