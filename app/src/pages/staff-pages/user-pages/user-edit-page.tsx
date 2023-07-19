import React, {useEffect, useState} from 'react';
import {useParams} from 'react-router-dom';
import {type User} from '../../../models/entities/user';
import {Typography} from '@mui/material';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {UsersService} from '../../../services/users-service';
import {CheckboxFieldComponent} from '../../../components/checkbox-field/checkbox-field-component';
import {validateEmail} from '../../../utils/validate-email';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {useChangeBlocker} from '../../../hooks/use-change-blocker';
import {delayPromise} from '../../../utils/with-delay';
import {FormPageWrapper} from '../../../components/form-page-wrapper/form-page-wrapper';
import {AlertComponent} from '../../../components/alert/alert-component';
import {useAdminGuard} from '../../../hooks/use-admin-guard';
import {UserEditPageMembershipsTab} from './tabs/user-edit-page-memberships-tab';

type Errors = {
    [key in keyof User]?: string;
};

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

export function UserEditPage(): JSX.Element {
    useAdminGuard();

    const dispatch = useAppDispatch();

    const userId = useParams().id;

    const [originalUser, setOriginalUser] = useState<User>();
    const [editedUser, setEditedUser] = useState<User>();
    const [retypedPassword, setRetypedPassword] = useState('');
    const [errors, setErrors] = useState<Errors>({});

    const [isUserLoading, setIsUserLoading] = useState(true);
    const [isUserNotFound, setIsUserNotFound] = useState(false);

    const hasChanged = useChangeBlocker(originalUser, editedUser);

    useEffect(() => {
        setIsUserLoading(true);
        setIsUserNotFound(false);

        if (userId == null || userId === 'new') {
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
            setIsUserLoading(false);
        } else {
            delayPromise(UsersService.retrieve(parseInt(userId)))
                .then((user) => {
                    setOriginalUser(user);
                    setEditedUser(user);
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

    const handlePatch = (patch: Partial<User>): void => {
        if (editedUser == null) {
            return;
        }
        setEditedUser({
            ...editedUser,
            ...patch,
        });
    };

    const handleSave = (): void => {
        if (editedUser == null) {
            return;
        }

        const errors = validateUser(editedUser, retypedPassword);

        if (Object.keys(errors).length > 0) {
            setErrors(errors);
        } else {
            setErrors({});

            setIsUserLoading(true);

            UsersService
                .save(editedUser.id === 0 ? undefined : editedUser.id, editedUser)
                .then((createdUser) => {
                    setOriginalUser(createdUser);
                    setEditedUser(createdUser);
                    dispatch(showSuccessSnackbar('Benutzer:in erfolgreich gespeichert'));
                    setRetypedPassword('');
                })
                .catch((err) => {
                    if (err.status === 409) {
                        setErrors({
                            email: 'Es existiert bereits eine Benutzer:in mit dieser E-Mail-Adresse',
                        });
                    } else {
                        console.error(err);
                        dispatch(showErrorSnackbar('Benutzer:in konnte nicht gespeichert werden'));
                    }
                })
                .finally(() => {
                    setIsUserLoading(false);
                });
        }
    };

    const handleReset = (): void => {
        if (originalUser != null) {
            setEditedUser(originalUser);
            setRetypedPassword('');
        }
    };

    return (
        <FormPageWrapper
            title="Benutzer bearbeiten"
            isLoading={isUserLoading}
            is404={isUserNotFound}

            hasChanged={hasChanged}
            onSave={handleSave}
            onReset={(editedUser?.id ?? 0) !== 0 ? handleReset : undefined}

            tabs={
                editedUser != null && editedUser.id !== 0 ?
                    [
                        {
                            label: 'Fachbereiche und Rollen',
                            content: <UserEditPageMembershipsTab user={editedUser}/>,
                        },
                    ] :
                    undefined
            }
        >
            <TextFieldComponent
                label="Name"
                value={editedUser?.name}
                onChange={(val) => {
                    handlePatch({
                        name: val,
                    });
                }}
                required
                maxCharacters={96}
                minCharacters={3}
                hint="Der Name wird in der Gover-Anwendung angezeigt."
                error={errors.name}
            />

            <TextFieldComponent
                label="E-Mail-Adresse"
                type="email"
                value={editedUser?.email}
                onChange={(val) => {
                    handlePatch({
                        email: val,
                    });
                }}
                required
                maxCharacters={255}
                hint="Mit der E-Mail-Adresse muss sich die Benutzer:in anmelden. Bitte beachten Sie, dass die E-Mail-Adresse einzigartig sein muss."
                error={errors.email}
            />

            <CheckboxFieldComponent
                label="Globale Administrator:in"
                value={editedUser?.admin ?? false}
                onChange={(val) => {
                    handlePatch({
                        admin: val,
                    });
                }}
                hint="Globale Administrator:innen können Benutzer:innen und Fachbereiche verwalten. Sie haben systemweiten Zugriff auf alle Fachbereiche, Formulare und Anträge."
            />

            <CheckboxFieldComponent
                label="Benutzer ist Aktiv"
                value={editedUser?.active ?? false}
                onChange={(val) => {
                    handlePatch({
                        active: val,
                    });
                }}
                hint="Deaktivieren sie Benutzer:innen damit diese sich nicht mehr anmelden können."
            />

            <Typography
                variant="h5"
                component="h2"
            >
                Passwort
            </Typography>

            <TextFieldComponent
                label="Passwort"
                type="password"
                value={editedUser?.password}
                onChange={(val) => {
                    handlePatch({
                        password: val,
                    });
                }}
                required={editedUser?.id === 0}
                maxCharacters={72}
                minCharacters={8}
                error={errors.password}
            />

            <TextFieldComponent
                label="Passwort wiederholen"
                type="password"
                value={retypedPassword}
                onChange={(val) => {
                    setRetypedPassword(val ?? '');
                }}
                required={editedUser?.id === 0}
                maxCharacters={72}
                minCharacters={8}
                error={errors.password}
            />

            {
                editedUser?.id === 0 &&
                <AlertComponent
                    color="info"
                    title="Versand der Einladung"
                >
                    Bitte beachten Sie, nach dem Speichern der Mitarbeiter:in automatisch eine E-Mail mit den
                    Login-Daten an die angegebene E-Mail-Adresse versendet wird.
                </AlertComponent>
            }
        </FormPageWrapper>
    );
}
