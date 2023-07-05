import {useAuthGuard} from "../../../hooks/use-auth-guard";
import React, {FormEvent, useEffect, useState} from "react";
import {useParams} from "react-router-dom";
import {User} from "../../../models/entities/user";
import {Box, Button, Divider} from "@mui/material";
import {TextFieldComponent} from "../../../components/text-field/text-field-component";
import {UsersService} from "../../../services/users-service";
import {CheckboxFieldComponent} from "../../../components/checkbox-field/checkbox-field-component";
import {validateEmail} from "../../../utils/validate-email";
import {useAppDispatch} from "../../../hooks/use-app-dispatch";
import {showErrorSnackbar, showSuccessSnackbar} from "../../../slices/snackbar-slice";
import {useUserGuard} from "../../../hooks/use-user-guard";
import {PageWrapper} from "../../../components/page-wrapper/page-wrapper";
import {useChangeBlocker} from "../../../hooks/use-change-blocker";
import {ApiError} from "../../../services/api-service";

type Errors = {
    [key in keyof User]?: string;
}

function validateUser(user: User, retypedPassword: string): Errors {
    const errors: Errors = {};

    if (user.name.length < 3) {
        errors.name = 'Bitte geben Sie mindestens 3 Zeichen ein';
    }

    if (!validateEmail(user.email)) {
        errors.email = 'Bitte geben Sie eine gültige E-Mail-Adresse ein';
    }

    if (user.id === 0) {
        if (user.password.length === 0) {
            errors.password = 'Bitte geben Sie ein Passwort ein';
        } else if (user.password !== retypedPassword) {
            errors.password = 'Die eingegebenen Passwörter stimmen nicht überein';
        }
    }

    return errors;
}

export function UserEditPage() {
    useAuthGuard();
    useUserGuard(user => user != null && user.admin);

    const dispatch = useAppDispatch();

    const {id} = useParams();

    const [originalUser, setOriginalUser] = useState<User>();
    const [editedUser, setEditedUser] = useState<User>();
    const [retypedPassword, setRetypedPassword] = useState('');
    const [errors, setErrors] = useState<Errors>({});

    const hasChanged = useChangeBlocker(originalUser, editedUser);

    useEffect(() => {
        if (id == null || id === 'new') {
            const newUser: User = {
                id: 0,
                name: '',
                email: '',
                password: '',
                active: true,
                admin: false,
                created: '',
                updated: '',
            };
            setOriginalUser(newUser);
            setEditedUser(newUser);
        } else {
            UsersService
                .retrieve(parseInt(id))
                .then(user => {
                    setOriginalUser(user);
                    setEditedUser(user);
                });
        }
    }, [id]);

    const handleSubmit = (event: FormEvent) => {
        event.preventDefault();

        if (editedUser == null) {
            return;
        }

        const errors = validateUser(editedUser, retypedPassword);

        if (Object.keys(errors).length > 0) {
            setErrors(errors);
        } else {
            setErrors({});

            if (editedUser.id === 0) {
                UsersService
                    .create(editedUser)
                    .then(createdUser => {
                        setOriginalUser(createdUser);
                        setEditedUser(createdUser);
                        dispatch(showSuccessSnackbar('Benutzer:in erfolgreich erstellt!'));
                        setRetypedPassword('');
                    })
                    .catch(err => {
                        if (err.status === 409) {
                            setErrors({
                                email: 'Es existiert bereits eine Benutzer:in mit dieser E-Mail-Adresse',
                            });
                        } else {
                            dispatch(showErrorSnackbar('Benutzer:in konnte nicht erstellt werden!'));
                            console.error(err);
                        }
                    });
            } else {
                UsersService
                    .update(editedUser.id, editedUser)
                    .then(updatedUser => {
                        setOriginalUser(updatedUser);
                        setEditedUser(updatedUser);
                        dispatch(showSuccessSnackbar('Benutzer erfolgreich gespeichert!'));
                        setRetypedPassword('');
                    })
                    .catch(err => {
                        if (err.status === 409) {
                            setErrors({
                                email: 'Es existiert bereits eine Benutzer:in mit dieser E-Mail-Adresse',
                            });
                        } else {
                            dispatch(showErrorSnackbar('Benutzer:in konnte nicht gespeichert werden!'));
                            console.error(err);
                        }
                    });
            }
        }
    };

    return (
        <PageWrapper
            title="Benutzer bearbeiten"
            isLoading={editedUser == null}
        >
            {
                editedUser != null &&
                <form onSubmit={handleSubmit}>
                    <Divider sx={{mb: 4}}>
                        Allgemein
                    </Divider>

                    <TextFieldComponent
                        label="Name"
                        value={editedUser.name}
                        onChange={val => setEditedUser({
                            ...editedUser,
                            name: val ?? '',
                        })}
                        required
                        maxCharacters={96}
                        minCharacters={3}
                        hint="Der Name wird in der Gover-Anwendung angezeigt."
                        error={errors.name}
                    />

                    <TextFieldComponent
                        label="E-Mail-Adresse"
                        type="email"
                        value={editedUser.email}
                        onChange={val => setEditedUser({
                            ...editedUser,
                            email: val ?? '',
                        })}
                        required
                        maxCharacters={255}
                        hint="Mit der E-Mail-Adresse muss sich die Benutzer:in anmelden. Bitte beachten Sie, dass die E-Mail-Adresse einzigartig sein muss."
                        error={errors.email}
                    />

                    <CheckboxFieldComponent
                        label="Globale-Administrator:in"
                        value={editedUser.admin}
                        onChange={val => setEditedUser({
                            ...editedUser,
                            admin: val,
                        })}
                        hint="Globale-Administrator:innen können Benutze:innen und Fachbereiche verwalten und haben Zugriff auf alle Fachbereiche, Formulare und Anträge."
                    />

                    <CheckboxFieldComponent
                        label="Benutzer ist Aktiv"
                        value={editedUser.active}
                        onChange={val => setEditedUser({
                            ...editedUser,
                            active: val,
                        })}
                        hint="Deaktivieren sie Benutze:innen damit diese sich nicht mehr anmelden können."
                    />

                    <Divider sx={{my: 4}}>
                        Passwort {editedUser.id !== 0 ? 'überschreiben' : ''}
                    </Divider>

                    <TextFieldComponent
                        label="Passwort"
                        type="password"
                        value={editedUser.password}
                        onChange={val => setEditedUser({
                            ...editedUser,
                            password: val ?? '',
                        })}
                        required={editedUser.id === 0}
                        maxCharacters={72}
                        minCharacters={8}
                        error={errors.password}
                    />

                    <TextFieldComponent
                        label="Passwort wiederholen"
                        type="password"
                        value={retypedPassword}
                        onChange={val => setRetypedPassword(val ?? '')}
                        required={editedUser.id === 0}
                        maxCharacters={72}
                        minCharacters={8}
                        error={errors.password}
                    />

                    <Box sx={{mt: 4}}>
                        <Button
                            type="submit"
                            disabled={!hasChanged}
                        >
                            Speichern
                        </Button>

                        <Button
                            sx={{ml: 2}}
                            type="reset"
                            color="error"
                            onClick={() => {
                                setEditedUser(originalUser!);
                                setRetypedPassword('');
                                setErrors({});
                            }}
                            disabled={!hasChanged}
                        >
                            Zurücksetzen
                        </Button>
                    </Box>
                </form>
            }
        </PageWrapper>
    );
}