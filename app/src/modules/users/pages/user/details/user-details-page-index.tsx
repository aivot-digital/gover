import {Box, Button, Grid, Typography} from '@mui/material';
import React, {useContext, useId, useState} from 'react';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import {useAppDispatch} from '../../../../../hooks/use-app-dispatch';
import {useNavigate} from 'react-router-dom';
import {
    GenericDetailsPageContext,
    GenericDetailsPageContextType
} from '../../../../../components/generic-details-page/generic-details-page-context';
import {type User} from '../../../../../models/entities/user';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../../../slices/snackbar-slice';
import {isApiError} from '../../../../../models/api-error';
import {GenericDetailsSkeleton} from '../../../../../components/generic-details-page/generic-details-skeleton';
import {ConstraintLinkProps} from '../../../../../dialogs/constraint-dialog/constraint-link-props';
import {ConfirmDialog} from '../../../../../dialogs/confirm-dialog/confirm-dialog';
import {ConstraintDialog} from '../../../../../dialogs/constraint-dialog/constraint-dialog';
import {useAccessGuard} from '../../../../../hooks/use-admin-guard';
import {TextFieldComponent} from '../../../../../components/text-field/text-field-component';
import {CheckboxFieldComponent} from '../../../../../components/checkbox-field/checkbox-field-component';
import {useFormManager} from '../../../../../hooks/use-form-manager';
import * as yup from 'yup';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import {SelectFieldComponent} from '../../../../../components/select-field-2/select-field-component';
import {SystemUserRole} from '../../../models/user';
import {UsersApiService} from '../../../users-api-service';
import {useConfirm} from "../../../../../providers/confirm-provider";

const Schema = yup.object({});

export function UserDetailsPageIndex() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const {
        item: user,
        isNewItem: isNewUser,
        setItem,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<User, undefined>;

    const {
        currentItem: updatedUser,
        handleInputChange,
    } = useFormManager<User>(user, Schema as any);

    const confirm = useConfirm();

    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });

    const [isBusy, setIsBusy] = useState(false);

    const [showConstraintDialog, setShowConstraintDialog] = useState(false);
    const [confirmDeleteAction, setConfirmDeleteAction] = useState<(() => void) | undefined>(undefined);
    const [relatedSubmissions, setRelatedSubmissions] = useState<ConstraintLinkProps[] | undefined>(undefined);

    if (user == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    const handleSave = () => {
        if (isNewUser) {
            new UsersApiService()
                .create(updatedUser!)
                .then(createdUser => {
                    navigate(`/users/${createdUser.id}`, {
                        replace: true,
                    });
                    dispatch(showSuccessSnackbar('Die Mitarbeiter:in wurde erfolgreich erstellt.'));
                    setItem(createdUser);
                })
                .catch(err => {
                    dispatch(showApiErrorSnackbar(err, 'Beim Erstellen der Mitarbeiter:in ist ein Fehler aufgetreten.'));
                });
        } else {
            new UsersApiService()
                .update(user.id, updatedUser!)
                .then(updatedUser => {
                    dispatch(showSuccessSnackbar('Die Mitarbeiter:in wurde erfolgreich gespeichert.'));
                    setItem(updatedUser);
                })
                .catch(err => {
                    dispatch(showApiErrorSnackbar(err, 'Beim Speichern der Mitarbeiter:in ist ein Fehler aufgetreten.'));
                });
        }
    };

    const confirmDelete = () => {
        if (!user.id) return;

        setIsBusy(true);
        new UsersApiService()
            .destroy(user.id)
            .then(() => {
                navigate('/users', {
                    replace: true,
                });
                dispatch(showSuccessSnackbar('Die Mitarbeiter:in wurde erfolgreich gelöscht.'));
            })
            .catch(err => {
                console.error(err);

                if (isApiError(err) && 'message' in err.details) {
                    dispatch(showErrorSnackbar(err.details.message));
                } else {
                    dispatch(showErrorSnackbar('Beim Löschen der Mitarbeiter:in ist ein Fehler aufgetreten.'));
                }
            })
            .finally(() => setIsBusy(false));
    };

    const handlePasswordReset = () => {
        confirm({
            title: 'Passwort zurücksetzen',
            children: (
                <>
                    <Typography>
                        Möchten Sie das Passwort für diese Mitarbeiter:in wirklich zurücksetzen?
                    </Typography>
                    <Typography>
                        Dazu wird an die hinterlegte E-Mail-Adresse ein Link zum Zurücksetzen des Passworts gesendet.
                    </Typography>
                </>
            ),
        })
            .then((confirm) => {
                if (confirm) {
                    new UsersApiService()
                        .resetPassword(user.id)
                        .then(() => {
                            dispatch(showSuccessSnackbar('Der Passwort-Zurücksetzen-Link wurde erfolgreich versendet.'));
                        })
                        .catch(err => {
                            dispatch(showApiErrorSnackbar(err, 'Beim Zurücksetzen des Passworts ist ein Fehler aufgetreten.'));
                        });
                }
            });
    };

    return (
        <>
            <Box
                sx={{
                    pt: 1.5,
                }}
            >
                <Grid
                    container
                    columnSpacing={2}
                >
                    <Grid size={6}>
                        <TextFieldComponent
                            label="Vorname"
                            value={updatedUser?.firstName}
                            onChange={handleInputChange('firstName')}
                            required
                        />
                    </Grid>
                    <Grid size={6}>
                        <TextFieldComponent
                            label="Nachname"
                            value={updatedUser?.lastName}
                            onChange={handleInputChange('lastName')}
                            required
                        />
                    </Grid>
                    <Grid size={6}>
                        <TextFieldComponent
                            label="E-Mail-Adresse"
                            value={updatedUser?.email}
                            onChange={handleInputChange('email')}
                            required
                        />
                    </Grid>
                    <Grid size={6}>
                        <SelectFieldComponent
                            label="Konto aktiviert"
                            value={updatedUser?.globalRole}
                            onChange={handleInputChange('globalRole')}
                            options={[
                                {
                                    label: 'Mitarbeiter:in',
                                    value: SystemUserRole.Default,
                                },
                                {
                                    label: 'Systemadministrator:in',
                                    value: SystemUserRole.SystemAdmin,
                                },
                                {
                                    label: 'Superadministrator:in',
                                    value: SystemUserRole.SuperAdmin,
                                },
                            ]}
                            required
                        />
                    </Grid>
                    <Grid size={6}>
                        <CheckboxFieldComponent
                            label="Konto aktiviert"
                            value={updatedUser?.enabled}
                            onChange={handleInputChange('enabled')}
                            variant="switch"
                        />
                    </Grid>
                </Grid>

                <Box
                    sx={{
                        display: 'flex',
                        marginTop: 5,
                        gap: 2,
                    }}
                >
                    {
                        hasAccess &&
                        <Button
                            variant="contained"
                            color="primary"
                            startIcon={<OpenInNewIcon />}
                            disabled={isBusy}
                            onClick={handleSave}
                        >
                            Speichern
                        </Button>
                    }

                    {
                        hasAccess &&
                        <Button
                            variant="contained"
                            color="primary"
                            startIcon={<OpenInNewIcon />}
                            disabled={isBusy}
                            onClick={handlePasswordReset}
                        >
                            Passwort zurücksetzen
                        </Button>
                    }

                    {
                        user.deletedInIdp &&
                        <Button
                            onClick={() => setConfirmDeleteAction(() => confirmDelete)}
                            variant="outlined"
                            color="error"
                            startIcon={<Delete />}
                            sx={{
                                ml: 'auto',
                            }}
                            disabled={isBusy}
                        >
                            Mitarbeiter:in löschen
                        </Button>
                    }
                </Box>
            </Box>

            <ConfirmDialog
                title="Mitarbeiter:in löschen"
                onCancel={() => setConfirmDeleteAction(undefined)}
                onConfirm={confirmDeleteAction}
                isDestructive
                confirmButtonText="Ja, endgültig löschen"
            >
                <Typography>
                    Möchten Sie diese Mitarbeiter:in wirklich endgültig löschen? Diese Aktion kann nicht rückgängig gemacht werden.
                </Typography>
            </ConfirmDialog>

            <ConstraintDialog
                open={showConstraintDialog}
                onClose={() => setShowConstraintDialog(false)}
                message="Dieser Mitarbeiter:in kann (noch) nicht gelöscht werden, da sie noch offenen Anträgen zugewiesen ist."
                solutionText="Bitte übertragen Sie die Anträge an eine andere Mitarbeiter:in und versuchen Sie es erneut:"
                links={relatedSubmissions}
            />
        </>
    );
}