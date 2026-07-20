import {Box, Button, Typography} from '@mui/material';
import Grid from '@mui/material/Grid';
import React, {useContext, useMemo, useState} from 'react';
import {GenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {useNavigate} from 'react-router-dom';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {useFormManager} from '../../../../hooks/use-form-manager';
import {useChangeBlocker} from '../../../../hooks/use-change-blocker';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {ConfirmDialog} from '../../../../dialogs/confirm-dialog/confirm-dialog';
import {AlertComponent} from '../../../../components/alert/alert-component';
import * as yup from 'yup';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {UserRoleResponseDTO} from '../../dtos/user-role-response-dto';
import {UserRolesApiService} from '../../user-roles-api-service';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import {PermissionEditor} from '../../../permissions/components/permission-editor';
import {Permission} from '../../../../data/permissions/permission';
import {formatMissingPermissionTooltip} from '../../../permissions/utils/permission-utils';
import {useCheckSystemPermission, useRefreshPermissionSet} from '../../../permissions/hooks/use-permissions';
import {DisabledTooltip} from '../../../../components/disabled-tooltip/disabled-tooltip';

export const UserRoleSchema = yup.object({
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

export function UserRolesDetailsPageIndex() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const {
        item,
        setItem,
        isBusy,
        setIsBusy,
        isEditable,
        isNewItem,
    } = useContext(GenericDetailsPageContext);
    const isNewRole = isNewItem === true;
    const editPermission = isNewRole ? Permission.DOMAIN_ROLE_CREATE : Permission.DOMAIN_ROLE_UPDATE;
    const canDeleteDomainRole = useCheckSystemPermission(Permission.DOMAIN_ROLE_DELETE);
    const refreshPermissionSet = useRefreshPermissionSet();

    const {
        currentItem,
        errors,
        hasNotChanged,
        handleInputBlur,
        handleInputChange,
        handleInputPatch,
        validate,
        reset,
    } = useFormManager<UserRoleResponseDTO>(item, UserRoleSchema as any);

    const apiService = useMemo(() => new UserRolesApiService(), []);

    const entity = currentItem;

    const changeBlocker = useChangeBlocker(item, currentItem);
    const [showConfirmDialog, setShowConfirmDialog] = useState(false);

    const refreshPermissionsAfterRoleChange = () => {
        // Role edits can affect the current user through existing assignments, so refresh explicitly after successful mutations.
        refreshPermissionSet({broadcast: true})
            .catch((err) => dispatch(showApiErrorSnackbar(
                err,
                'Die Berechtigungen konnten nach der Änderung der Domänenrolle nicht aktualisiert werden.',
            )));
    };

    if (entity == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    const saveDisabledByPermission = !isEditable;
    const saveDisabledTooltip = saveDisabledByPermission
        ? formatMissingPermissionTooltip(editPermission)
        : undefined;
    const deleteDisabledByPermission = !canDeleteDomainRole;
    const deleteDisabledTooltip = deleteDisabledByPermission
        ? formatMissingPermissionTooltip(Permission.DOMAIN_ROLE_DELETE)
        : undefined;

    const handleSave = () => {
        if (entity != null) {

            const validationResult = validate();

            if (!validationResult) {
                dispatch(showErrorSnackbar('Bitte überprüfen Sie Ihre Eingaben.'));
                return;
            }

            setIsBusy(true);

            if (isNewRole) {
                apiService
                    .create(entity as any)
                    .then((newRole) => {
                        setItem(newRole);
                        reset();
                        refreshPermissionsAfterRoleChange();

                        dispatch(showSuccessSnackbar('Neue Domänenrolle erfolgreich angelegt.'));

                        // use setTimeout instead of useEffect to prevent unnecessary rerender
                        setTimeout(() => {
                            navigate(`/user-roles/${newRole.id}`, {replace: true});
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
                    .update(entity.id, entity as any)
                    .then((updatedRole) => {
                        setItem(updatedRole);
                        reset();
                        refreshPermissionsAfterRoleChange();

                        dispatch(showSuccessSnackbar('Änderungen an der Domänenrolle erfolgreich gespeichert.'));
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
        if (isNewRole) {
            return;
        }

        setIsBusy(true);

        apiService
            .destroy(entity.id)
            .then(() => {
                reset(); // prevent change blocker by resetting unsaved changes
                refreshPermissionsAfterRoleChange();
                navigate('/user-roles', {
                    replace: true,
                });
                dispatch(showSuccessSnackbar('Die Domänenrolle wurde erfolgreich gelöscht.'));
            })
            .catch(err => {
                console.error(err);
                dispatch(showErrorSnackbar('Beim Löschen der Domänenrolle ist ein Fehler aufgetreten.'));
                setIsBusy(false);
            });
    };

    return (
        <Box>
            <Typography
                variant="h5"
                sx={{mt: 1.5, mb: 1}}
            >
                Domänenrolle konfigurieren
            </Typography>
            <Typography sx={{mb: 3, maxWidth: 900}}>
                Domänenrollen definieren Berechtigungen für Mitarbeiter:innen innerhalb fachlicher Domänen,
                zum Beispiel in Organisationseinheiten oder Teams.
                Seien Sie vorsichtig bei der Vergabe von Berechtigungen, insbesondere bei solchen, die Zugriff auf sensible Daten oder kritische Funktionen ermöglichen.
            </Typography>

            <AlertComponent
                color="warning"
                title="Hinweis zum aktuellen Entwicklungsstand von Rollen und Berechtigungen"
                sx={{mb: 3}}
            >
                Rollen und die damit verbundenen Berechtigungen werden in Gover derzeit noch nicht überall vollständig berücksichtigt.
                Funktionen können unvollständig sein, sich ändern oder sich in einzelnen Bereichen noch nicht wie erwartet verhalten.
                Bitte verlassen Sie sich daher aktuell nicht darauf, dass konfigurierte Berechtigungen bereits konsistent an allen Stellen durchgesetzt werden.
            </AlertComponent>

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
                        value={entity.name}
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
                        value={entity.description}
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

            {/* Domain roles are limited to providers that can be resolved against a domain resource. */}
            <PermissionEditor
                originalPermissions={item?.permissions ?? []}
                value={entity.permissions ?? []}
                onChange={(next: string[]) => handleInputPatch({permissions: next} as Partial<UserRoleResponseDTO>)}
                isBusy={isBusy}
                isEditable={isEditable}
                onlyDomainRoleAssignable
            />

            <Box
                sx={{
                    display: 'flex',
                    marginTop: 3,
                    gap: 2,
                }}
            >
                <DisabledTooltip
                    title={saveDisabledTooltip}
                    disabled={isBusy || hasNotChanged || !isEditable}
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
                </DisabledTooltip>

                {
                    !isNewRole &&
                    <DisabledTooltip
                        title={saveDisabledTooltip}
                        disabled={isBusy || hasNotChanged || !isEditable}
                    >
                        <Button
                            onClick={() => {
                                reset();
                            }}
                            disabled={isBusy || hasNotChanged || !isEditable}
                            color="error"
                        >
                            Zurücksetzen
                        </Button>
                    </DisabledTooltip>
                }

                {
                    !isNewRole &&
                    <DisabledTooltip
                        title={deleteDisabledTooltip}
                        disabled={isBusy || deleteDisabledByPermission}
                        wrapperSx={{marginLeft: 'auto'}}
                    >
                        <Button
                            variant="outlined"
                            onClick={() => setShowConfirmDialog(true)}
                            disabled={isBusy || deleteDisabledByPermission}
                            color="error"
                            startIcon={<Delete />}
                        >
                            Löschen
                        </Button>
                    </DisabledTooltip>
                }
            </Box>

            {changeBlocker.dialog}

            <ConfirmDialog
                title="Domänenrolle löschen"
                onCancel={() => setShowConfirmDialog(false)}
                onConfirm={showConfirmDialog ? handleDelete : undefined}
                confirmationText={entity.name ?? ''}
                isDestructive
                confirmButtonText="Ja, endgültig löschen"
            >
                <Typography>
                    Möchten Sie diese Domänenrolle wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.
                </Typography>
                <AlertComponent color={'warning'}>
                    Wenn diese Domänenrolle noch Mitgliedschaften in Organisationseinheiten oder Teams zugewiesen ist,
                    werden diese Zuweisungen ebenfalls entfernt. Das kann dazu führen, dass betroffene
                    Mitarbeiter:innen in den zugehörigen Organisationseinheiten oder Teams keine
                    Berechtigungen mehr haben.
                </AlertComponent>
            </ConfirmDialog>
        </Box>
    );
}
