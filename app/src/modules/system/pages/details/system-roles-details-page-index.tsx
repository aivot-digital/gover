import {Box, Button, Table, TableBody, TableCell, TableContainer, TableRow, Typography} from '@mui/material';
import React, {useContext, useEffect, useMemo, useState} from 'react';
import {
    GenericDetailsPageContext,
    GenericDetailsPageContextType
} from '../../../../components/generic-details-page/generic-details-page-context';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {useNavigate} from 'react-router-dom';
import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {useFormManager} from '../../../../hooks/use-form-manager';
import {useChangeBlocker} from '../../../../hooks/use-change-blocker';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {ConfirmDialog} from '../../../../dialogs/confirm-dialog/confirm-dialog';
import {AlertComponent} from '../../../../components/alert/alert-component';
import * as yup from 'yup';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {
    addSnackbarMessage,
    removeSnackbarMessage,
    SnackbarSeverity,
    SnackbarType
} from '../../../../slices/shell-slice';
import {CheckboxFieldComponent} from '../../../../components/checkbox-field/checkbox-field-component';
import {PermissionGroups} from "../../../../data/permissions/permission-groups";
import {PermissionLabelsDe} from "../../../../data/permissions/permission-labels";
import {SystemRoleEntity} from "../../entities/system-role-entity";
import {SystemRolesApiService} from "../../services/system-roles-api-service";

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

export function SystemRolesDetailsPageIndex() {
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
            key: 'access-denied-secrets-details',
            message: 'Dieses Geheimnis kann nur von Administrator:innen bearbeitet werden. Sie haben Lesezugriff',
            severity: SnackbarSeverity.Warning,
            type: SnackbarType.Dismissable,
        }));

        return () => {
            dispatch(removeSnackbarMessage('access-denied-secrets-details'));
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
            <GenericDetailsSkeleton/>
        );
    }

    const handleSave = () => {
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
                    .then((newSecret) => {
                        setItem(newSecret);
                        reset();

                        dispatch(showSuccessSnackbar('Neues Geheimnis erfolgreich angelegt.'));

                        // use setTimeout instead of useEffect to prevent unnecessary rerender
                        setTimeout(() => {
                            navigate(`/user-roles/${newSecret.id}`, {replace: true});
                        }, 0);
                    })
                    .catch(err => {
                        console.error(err);
                        dispatch(showErrorSnackbar('Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
                    })
                    .finally(() => {
                        setIsBusy(false);
                    });
            } else {
                apiService
                    .update(editedSystemRole.id, editedSystemRole as any)
                    .then((updatedSecret) => {
                        setItem(updatedSecret);
                        reset();

                        dispatch(showSuccessSnackbar('Änderungen an Geheimnis erfolgreich gespeichert.'));
                    })
                    .catch(err => {
                        console.error(err);
                        dispatch(showErrorSnackbar('Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
                    })
                    .finally(() => {
                        setIsBusy(false);
                    });
            }
        }
    };

    const handleDelete = () => {
        if (editedSystemRole.id !== 0) {
            setIsBusy(true);

            apiService
                .destroy(editedSystemRole.id)
                .then(() => {
                    reset(); // prevent change blocker by resetting unsaved changes
                    navigate('/secrets', {
                        replace: true,
                    });
                    dispatch(showSuccessSnackbar('Das Geheimnis wurde erfolgreich gelöscht.'));
                })
                .catch(err => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Beim Löschen des Geheimnisses ist ein Fehler aufgetreten.'));
                    setIsBusy(false);
                });
        }
    };

    return (
        <Box>
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

            <TableContainer>
                <Table size="small">
                    <TableBody>
                        {
                            PermissionGroups.map((group) => (
                                <TableRow key={group.label}>
                                    <TableCell>
                                        {group.label}
                                    </TableCell>

                                    <TableCell>
                                        {
                                            group.permissions.map((permission) => (
                                                <CheckboxFieldComponent
                                                    key={permission}
                                                    label={PermissionLabelsDe[permission]}
                                                    value={editedSystemRole.permissions.includes(permission)}
                                                    onChange={(val) => {
                                                        const patch: Partial<SystemRoleEntity> = {};

                                                        let newPermissions = [...editedSystemRole.permissions];

                                                        if (val) {
                                                            // Add permission
                                                            if (!newPermissions.includes(permission)) {
                                                                newPermissions.push(permission);
                                                            }
                                                        } else {
                                                            // Remove permission
                                                            newPermissions = newPermissions.filter((perm) => perm !== permission);
                                                        }

                                                        handleInputPatch(patch);
                                                    }}
                                                    sx={{
                                                        m: 0,
                                                        p: 0,
                                                    }}
                                                />
                                            ))
                                        }
                                    </TableCell>
                                </TableRow>
                            ))
                        }
                    </TableBody>
                </Table>
            </TableContainer>

            <Box
                sx={{
                    display: 'flex',
                    marginTop: 2,
                    gap: 2,
                }}
            >
                <Button
                    onClick={handleSave}
                    disabled={isBusy || hasNotChanged || !isEditable}
                    variant="contained"
                    color="primary"
                    startIcon={<SaveOutlinedIcon/>}
                >
                    Speichern
                </Button>

                {
                    editedSystemRole.id !== 0 &&
                    <Button
                        onClick={() => {
                            reset();
                        }}
                        disabled={isBusy || hasNotChanged || !isEditable}
                        color="error"
                    >
                        Zurücksetzen
                    </Button>
                }

                {
                    editedSystemRole.id !== 0 &&
                    <Button
                        variant="outlined"
                        onClick={() => setShowConfirmDialog(true)}
                        disabled={isBusy || !isEditable}
                        color="error"
                        sx={{
                            marginLeft: 'auto',
                        }}
                        startIcon={<DeleteOutlinedIcon/>}
                    >
                        Löschen
                    </Button>
                }
            </Box>

            {changeBlocker.dialog}

            <ConfirmDialog
                title="Geheimnis löschen"
                onCancel={() => setShowConfirmDialog(false)}
                onConfirm={showConfirmDialog ? handleDelete : undefined}
                confirmationText={editedSystemRole.name ?? ''}
                isDestructive
                confirmButtonText="Ja, endgültig löschen"
            >
                <Typography>
                    Möchten Sie dieses Geheimnis wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.
                </Typography>
                <AlertComponent color={'warning'}>
                    Vergewissern Sie sich, dass dieses Geheimnis nicht mehr benötigt wird, bevor Sie fortfahren. Wir
                    können nicht prüfen, ob es noch an Stellen wie Low-Code-Funktionen oder Konfigurationen von
                    Zahlungsdienstleistern
                    verwendet wird.
                </AlertComponent>
            </ConfirmDialog>
        </Box>
    );
}
