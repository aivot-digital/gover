import {Box, Button, Typography} from '@mui/material';
import React, {useContext, useMemo, useState} from 'react';
import {GenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {useApi} from '../../../../hooks/use-api';
import {useNavigate} from 'react-router-dom';
import {SecretsApiService} from '../../secrets-api-service';
import {Secret} from '../../models/secret';
import SaveOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Save';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {useFormManager} from '../../../../hooks/use-form-manager';
import {useChangeBlocker} from '../../../../hooks/use-change-blocker';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import ContentPasteOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/ContentPaste';
import {ConfirmDialog} from '../../../../dialogs/confirm-dialog/confirm-dialog';
import {AlertComponent} from '../../../../components/alert/alert-component';
import * as yup from 'yup';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import {copyToClipboardText} from '../../../../utils/copy-to-clipboard';
import {Permission} from '../../../../data/permissions/permission';
import {formatMissingPermissionTooltip} from '../../../permissions/utils/permission-utils';
import {useHasSystemPermission} from '../../../permissions/hooks/use-permissions';
import {DisabledTooltip} from '../../../../components/disabled-tooltip/disabled-tooltip';

export const SecretSchema = yup.object({
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
    value: yup.string()
        .trim()
        .min(1, 'Der Wert darf nicht leer sein.')
        .required('Der Wert ist ein Pflichtfeld.'),
});

export function SecretsDetailsPageIndex() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const api = useApi();
    const {
        item,
        setItem,
        isBusy,
        setIsBusy,
        isEditable,
        isNewItem,
    } = useContext(GenericDetailsPageContext);
    const canDeleteSecret = useHasSystemPermission(Permission.SECRET_DELETE);

    const {
        currentItem,
        errors,
        hasNotChanged,
        handleInputBlur,
        handleInputChange,
        validate,
        reset,
    } = useFormManager<Secret>(item, SecretSchema as any);

    const apiService = useMemo(() => new SecretsApiService(api), [api]);

    const secret = currentItem;
    const changeBlocker = useChangeBlocker(item, currentItem);
    const [showConfirmDialog, setShowConfirmDialog] = useState(false);
    const isNewSecret = isNewItem === true;
    const editPermission = isNewSecret ? Permission.SECRET_CREATE : Permission.SECRET_UPDATE;
    const editDisabledTooltip = !isEditable
        ? formatMissingPermissionTooltip(editPermission)
        : undefined;
    const deleteDisabledTooltip = !canDeleteSecret
        ? formatMissingPermissionTooltip(Permission.SECRET_DELETE)
        : undefined;

    if (secret == null) {
        return (
            <GenericDetailsSkeleton/>
        );
    }

    const handleSave = () => {
        if (secret != null) {

            const validationResult = validate();

            if (!validationResult) {
                dispatch(showErrorSnackbar('Bitte überprüfen Sie Ihre Eingaben.'));
                return;
            }

            setIsBusy(true);

            if (isNewSecret) {
                apiService
                    .create(secret)
                    .then((newSecret) => {
                        setItem(newSecret);
                        reset();

                        dispatch(showSuccessSnackbar('Neues Geheimnis erfolgreich angelegt.'));

                        // use setTimeout instead of useEffect to prevent unnecessary rerender
                        setTimeout(() => {
                            navigate(`/secrets/${newSecret.key}`, {replace: true});
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
                    .update(secret.key, secret)
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
        if (isNewSecret) {
            return;
        }

        setIsBusy(true);

        apiService
            .destroy(secret.key)
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
    };

    return (
        <Box>
            {
                !isNewSecret &&
                <TextFieldComponent
                    label="Schlüssel"
                    value={secret.key}
                    onChange={handleInputChange('key')}
                    onBlur={handleInputBlur('key')}
                    disabled={true}
                    controlSx={{
                        marginTop: 0,
                    }}
                    endAction={
                        [
                            {
                                icon: <ContentPasteOutlinedIcon/>,
                                tooltip: 'Schlüssel (ID) in Zwischenablage kopieren',
                                onClick: async () => {
                                    const success = await copyToClipboardText(secret.key);
                                    if (success) {
                                        dispatch(showSuccessSnackbar('Link in Zwischenablage kopiert!'));
                                    } else {
                                        dispatch(showErrorSnackbar('Fehler beim Kopieren des Links!'));
                                    }
                                },
                            },
                        ]
                    }
                />
            }

            <TextFieldComponent
                label="Name"
                required
                value={secret.name}
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
                value={secret.description}
                onChange={handleInputChange('description')}
                onBlur={handleInputBlur('description')}
                multiline={true}
                disabled={isBusy || !isEditable}
                error={errors.description}
                minCharacters={3}
                maxCharacters={255}
            />

            <TextFieldComponent
                label="Wert"
                required
                value={secret.value}
                onChange={handleInputChange('value')}
                onBlur={handleInputBlur('value')}
                disabled={isBusy || !isEditable}
                error={errors.value}
            />

            <Box
                sx={{
                    display: 'flex',
                    marginTop: 2,
                    gap: 2,
                }}
            >
                <DisabledTooltip
                    title={editDisabledTooltip}
                    disabled={isBusy || hasNotChanged || !isEditable}
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
                </DisabledTooltip>

                {
                    !isNewSecret &&
                    <DisabledTooltip
                        title={editDisabledTooltip}
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
                    !isNewSecret &&
                    <DisabledTooltip
                        title={deleteDisabledTooltip}
                        disabled={isBusy || !canDeleteSecret}
                        wrapperSx={{marginLeft: 'auto'}}
                    >
                        <Button
                            variant="outlined"
                            onClick={() => setShowConfirmDialog(true)}
                            disabled={isBusy || !canDeleteSecret}
                            color="error"
                            startIcon={<Delete/>}
                        >
                            Löschen
                        </Button>
                    </DisabledTooltip>
                }
            </Box>

            {changeBlocker.dialog}

            <ConfirmDialog
                title="Geheimnis löschen"
                onCancel={() => setShowConfirmDialog(false)}
                onConfirm={showConfirmDialog ? handleDelete : undefined}
                confirmationText={secret.name}
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
