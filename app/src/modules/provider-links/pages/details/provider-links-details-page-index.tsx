import {Box, Button, Typography} from '@mui/material';
import React, {useContext, useEffect, useMemo, useState} from 'react';
import {GenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {useApi} from '../../../../hooks/use-api';
import {useNavigate} from 'react-router-dom';
import {ProviderLinksApiService} from '../../provider-links-api-service';
import {ProviderLink} from '../../models/provider-link';
import {useFormManager} from '../../../../hooks/use-form-manager';
import {useChangeBlocker} from '../../../../hooks/use-change-blocker';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';
import {ConfirmDialog} from '../../../../dialogs/confirm-dialog/confirm-dialog';
import * as yup from 'yup';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {addSnackbarMessage, removeSnackbarMessage, SnackbarSeverity, SnackbarType} from '../../../../slices/shell-slice';

export const ProviderLinkSchema = yup.object({
    text: yup.string()
        .trim()
        .min(3, 'Der Linktext muss mindestens 3 Zeichen lang sein.')
        .max(255, 'Der Linktext darf maximal 255 Zeichen lang sein.')
        .required('Der Linktext ist ein Pflichtfeld.'),
    link: yup.string()
        .trim()
        .url('Bitte eine gültige URL eingeben.')
        .max(500, 'Der Link darf maximal 500 Zeichen lang sein.')
        .required('Der Link ist ein Pflichtfeld.'),
});

export function ProviderLinksDetailsPageIndex() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const api = useApi();
    const {
        item,
        setItem,
        isBusy,
        setIsBusy,
        isEditable,
    } = useContext(GenericDetailsPageContext);

    useEffect(() => {
        if (isEditable) {
            return;
        }

        dispatch(addSnackbarMessage({
            key: 'provider-links-no-access',
            message: 'Die Links können nur von Administrator:innen bearbeitet werden. Sie haben Lesezugriff.',
            type: SnackbarType.Dismissable,
            severity: SnackbarSeverity.Warning,
        }));

        return () => {
            dispatch(removeSnackbarMessage('provider-links-no-access'));
        };
    }, [isEditable]);

    const {
        currentItem,
        errors,
        hasNotChanged,
        handleInputBlur,
        handleInputChange,
        validate,
        reset,
    } = useFormManager<ProviderLink>(item, ProviderLinkSchema as any);

    const apiService = useMemo(() => new ProviderLinksApiService(api), [api]);

    const link = currentItem;
    const changeBlocker = useChangeBlocker(item, currentItem);
    const [showConfirmDialog, setShowConfirmDialog] = useState(false);

    if (link == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    const handleSave = () => {
        if (link != null) {

            const validationResult = validate();

            if (!validationResult) {
                dispatch(showErrorSnackbar('Bitte überprüfen Sie Ihre Eingaben.'));
                return;
            }

            setIsBusy(true);

            if (link.id === 0) {
                apiService
                    .create(link)
                    .then((newProviderLink) => {
                        setItem(newProviderLink);
                        reset();

                        dispatch(showSuccessSnackbar('Neuer Link erfolgreich angelegt.'));

                        // use setTimeout instead of useEffect to prevent unnecessary rerender
                        setTimeout(() => {
                            navigate(`/provider-links/${newProviderLink.id}`, {replace: true});
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
                    .update(link.id, link)
                    .then((updatedProviderLink) => {
                        setItem(updatedProviderLink);
                        reset();

                        dispatch(showSuccessSnackbar('Änderungen an Link erfolgreich gespeichert.'));
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
        if (link.id !== 0) {
            setIsBusy(true);

            apiService
                .destroy(link.id)
                .then(() => {
                    reset(); // prevent change blocker by resetting unsaved changes
                    navigate('/provider-links', {
                        replace: true,
                    });
                    dispatch(showSuccessSnackbar('Der Link wurde erfolgreich gelöscht.'));
                })
                .catch(err => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Beim Löschen des Links ist ein Fehler aufgetreten.'));
                    setIsBusy(false);
                });
        }
    };

    return (
        <Box>
            <TextFieldComponent
                label="Linktext *"
                value={link.text}
                multiline={true}
                rows={2}
                onChange={handleInputChange('text')}
                onBlur={handleInputBlur('text')}
                error={errors.text}
                disabled={isBusy || !isEditable}
                sx={{
                    marginTop: 0,
                }}
            />

            <TextFieldComponent
                label="Link *"
                value={link.link}
                onChange={handleInputChange('link')}
                onBlur={handleInputBlur('link')}
                error={errors.link}
                disabled={isBusy || !isEditable}
            />

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
                    startIcon={<SaveOutlinedIcon />}
                >
                    Speichern
                </Button>

                {
                    link.id !== 0 &&
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
                    link.id !== 0 &&
                    <Button
                        variant={'outlined'}
                        onClick={() => setShowConfirmDialog(true)}
                        disabled={isBusy || !isEditable}
                        color="error"
                        sx={{
                            marginLeft: 'auto',
                        }}
                        startIcon={<DeleteOutlinedIcon />}
                    >
                        Löschen
                    </Button>
                }
            </Box>

            {changeBlocker.dialog}

            <ConfirmDialog
                title="Link löschen"
                onCancel={() => setShowConfirmDialog(false)}
                onConfirm={showConfirmDialog ? handleDelete : undefined}
                isDestructive
                confirmButtonText="Ja, endgültig löschen"
            >
                <Typography>Möchten Sie diesen Link wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.</Typography>
            </ConfirmDialog>
        </Box>
    );
}