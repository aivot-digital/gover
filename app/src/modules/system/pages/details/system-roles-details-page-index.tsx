import {Box, Button, Typography} from '@mui/material';
import React, {type ReactNode, useContext, useEffect, useMemo, useState} from 'react';
import {GenericDetailsPageContext, type GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {useNavigate} from 'react-router-dom';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {useFormManager} from '../../../../hooks/use-form-manager';
import {useChangeBlocker} from '../../../../hooks/use-change-blocker';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {ConfirmDialog} from '../../../../dialogs/confirm-dialog/confirm-dialog';
import {AlertComponent} from '../../../../components/alert/alert-component';
import * as yup from 'yup';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {addSnackbarMessage, removeSnackbarMessage, SnackbarSeverity, SnackbarType} from '../../../../slices/shell-slice';
import {type SystemRoleEntity} from '../../entities/system-role-entity';
import {SystemRolesApiService} from '../../services/system-roles-api-service';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import Grid from '@mui/material/Grid';
import {PermissionEditor} from '../../../permissions/components/permission-editor';
import {PermissionScope} from '../../../permissions/enums/permission-scope';

export const SystemRoleSchema = yup.object({
    name: yup.string()
        .trim()
        .min(3, 'Der Name muss mindestens 3 Zeichen lang sein.')
        .max(64, 'Der Name darf maximal 64 Zeichen lang sein.')
        .required('Der Name ist ein Pflichtfeld.'),
    description: yup.string()
        .trim()
        .min(3, 'Die Beschreibung muss mindestens 3 Zeichen lang sein.')
        .max(255, 'Die Beschreibung darf maximal 255 Zeichen lang sein.')
        .required('Die Beschreibung ist ein Pflichtfeld.'),
});

export function SystemRolesDetailsPageIndex(): ReactNode {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const {
        item: systemRole,
        setItem,
        isBusy,
        setIsBusy,
        isEditable,
    } = useContext<GenericDetailsPageContextType<SystemRoleEntity, void>>(GenericDetailsPageContext);

    useEffect(() => {
        if (isEditable) {
            return;
        }

        dispatch(addSnackbarMessage({
            key: 'access-denied-system-roles-details',
            message: 'Diese Systemrolle kann nur von Administrator:innen bearbeitet werden. Sie haben Lesezugriff.',
            severity: SnackbarSeverity.Warning,
            type: SnackbarType.Dismissable,
        }));

        return () => {
            dispatch(removeSnackbarMessage('access-denied-system-roles-details'));
        };
    }, [isEditable]);

    const {
        currentItem: editedSystemRole,
        errors,
        hasNotChanged,
        handleInputBlur,
        handleInputChange,
        handleInputPatch,
        validate,
        reset,
    } = useFormManager<SystemRoleEntity>(systemRole, SystemRoleSchema as any);

    const apiService = useMemo(() => new SystemRolesApiService(), []);

    const changeBlocker = useChangeBlocker(systemRole, editedSystemRole);
    const [showConfirmDialog, setShowConfirmDialog] = useState(false);

    if (editedSystemRole == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    const handleSave = (): void => {
        if (editedSystemRole != null) {
            const validationResult = validate();

            if (!validationResult) {
                dispatch(showErrorSnackbar('Bitte überprüfen Sie Ihre Eingaben.'));
                return;
            }

            setIsBusy(true);

            if (editedSystemRole.id === 0) {
                apiService
                    .create(editedSystemRole as any)
                    .then((newRole) => {
                        setItem(newRole);
                        reset();

                        dispatch(showSuccessSnackbar('Neue Systemrolle erfolgreich angelegt.'));

                        // use setTimeout instead of useEffect to prevent unnecessary rerender
                        setTimeout(() => {
                            navigate(`/system-roles/${newRole.id}`, {
                                replace: true,
                            })?.catch(console.error);
                        }, 0);
                    })
                    .catch((err) => {
                        console.error(err);
                        dispatch(showErrorSnackbar('Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
                    })
                    .finally(() => {
                        setIsBusy(false);
                    });
            } else {
                apiService
                    .update(editedSystemRole.id, editedSystemRole as any)
                    .then((updatedRole) => {
                        setItem(updatedRole);
                        reset();

                        dispatch(showSuccessSnackbar('Änderungen an der Systemrolle erfolgreich gespeichert.'));
                    })
                    .catch((err) => {
                        console.error(err);
                        dispatch(showErrorSnackbar('Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
                    })
                    .finally(() => {
                        setIsBusy(false);
                    });
            }
        }
    };

    const handleDelete = (): void => {
        if (editedSystemRole.id !== 0) {
            setIsBusy(true);

            apiService
                .destroy(editedSystemRole.id)
                .then(() => {
                    reset(); // prevent change blocker by resetting unsaved changes
                    navigate('/system-roles', {
                        replace: true,
                    })?.catch(console.error);
                    dispatch(showSuccessSnackbar('Die Systemrolle wurde erfolgreich gelöscht.'));
                })
                .catch((err) => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Beim Löschen der Systemrolle ist ein Fehler aufgetreten.'));
                    setIsBusy(false);
                });
        }
    };

    return (
        <Box>
            <Typography
                variant="h5"
                sx={{mt: 1.5, mb: 1}}
            >
                Systemrolle konfigurieren
            </Typography>
            <Typography sx={{mb: 3, maxWidth: 900}}>
                Systemrollen definieren Berechtigungen für Benutzer:innen auf Systemebene.
                Seien Sie vorsichtig bei der Vergabe von Berechtigungen, insbesondere bei solchen, die Zugriff auf sensible Daten oder kritische Funktionen ermöglichen.
            </Typography>

            <Grid
                container
                spacing={2}
            >
                <Grid
                    size={{
                        xs: 12,
                        md: 6,
                    }}
                >
                    <TextFieldComponent
                        label="Name"
                        required
                        value={editedSystemRole.name}
                        onChange={handleInputChange('name')}
                        onBlur={handleInputBlur('name')}
                        disabled={isBusy || !isEditable}
                        error={errors.name}
                        minCharacters={3}
                        maxCharacters={64}
                    />

                    <TextFieldComponent
                        label="Beschreibung"
                        required
                        value={editedSystemRole.description}
                        onChange={handleInputChange('description')}
                        onBlur={handleInputBlur('description')}
                        multiline={true}
                        disabled={isBusy || !isEditable}
                        error={errors.description}
                        minCharacters={3}
                        maxCharacters={255}
                    />
                </Grid>
            </Grid>

            <PermissionEditor
                originalPermissions={systemRole?.permissions ?? []}
                value={editedSystemRole.permissions ?? []}
                onChange={(next) => handleInputPatch({permissions: next})}
                isBusy={isBusy}
                isEditable={isEditable}
                scope={PermissionScope.System}
            />

            <Box
                sx={{
                    display: 'flex',
                    marginTop: 3,
                    gap: 2,
                }}
            >
                <Button
                    onClick={handleSave}
                    disabled={isBusy || hasNotChanged || !isEditable}
                    variant="contained"
                    color="primary"
                    startIcon={<SaveOutlinedIcon />}
                >
                    Speichern
                </Button>

                {editedSystemRole.id !== 0 && (
                    <Button
                        onClick={() => reset()}
                        disabled={isBusy || hasNotChanged || !isEditable}
                        color="error"
                    >
                        Zurücksetzen
                    </Button>
                )}

                {editedSystemRole.id !== 0 && (
                    <Button
                        variant="outlined"
                        onClick={() => setShowConfirmDialog(true)}
                        disabled={isBusy || !isEditable}
                        color="error"
                        sx={{marginLeft: 'auto'}}
                        startIcon={<Delete />}
                    >
                        Löschen
                    </Button>
                )}
            </Box>

            {changeBlocker.dialog}

            <ConfirmDialog
                title="Systemrolle löschen"
                onCancel={() => setShowConfirmDialog(false)}
                onConfirm={showConfirmDialog ? handleDelete : undefined}
                confirmationText={editedSystemRole.name ?? ''}
                isDestructive
                confirmButtonText="Ja, endgültig löschen"
            >
                <Typography>
                    Möchten Sie diese Systemrolle wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.
                </Typography>
                <AlertComponent color={'warning'}>
                    Vergewissern Sie sich, dass die Systemrolle nicht mehr benötigt wird, bevor Sie fortfahren.
                </AlertComponent>
            </ConfirmDialog>
        </Box>
    );
}
